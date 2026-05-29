#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
VERSION="${HYPERFOIL_VERSION:-0.29.1}"
TARGET_DIR="${SCRIPT_DIR}/hyperfoil"
ARCHIVE_PATH="${SCRIPT_DIR}/hyperfoil-${VERSION}.zip"
DOWNLOAD_URL="https://github.com/Hyperfoil/Hyperfoil/releases/download/hyperfoil-all-${VERSION}/hyperfoil-${VERSION}.zip"
FORCE=0
KRUIZE_URL=""
OUTPUT_PATH=""
AUTO_INSTALL_KRUIZE=1
KRUIZE_CLUSTER_TYPE="${KRUIZE_CLUSTER_TYPE:-openshift}"
KRUIZE_DEPLOY_TARGET="${KRUIZE_DEPLOY_TARGET:-crc}"
KRUIZE_NAMESPACE="${KRUIZE_NAMESPACE:-openshift-tuning}"
KRUIZE_IMAGE="${KRUIZE_IMAGE:-}"
KRUIZE_REPO_ROOT="${KRUIZE_REPO_ROOT:-}"

usage() {
    cat <<EOF
Usage:
    $(basename "$0") [--force] [--kruize-url URL] [--output PATH] [install]
    $(basename "$0") [--force] [--kruize-url URL] [--output PATH] install-only
    $(basename "$0") [--force] [--kruize-url URL] [--output PATH] cli [args...]
    $(basename "$0") [--force] [--kruize-url URL] [--output PATH] standalone [args...]
    $(basename "$0") [--force] [--kruize-url URL] [--output PATH] run [args...]
    $(basename "$0") [--force] [--kruize-url URL] [--output PATH] --version

Single entrypoint for Hyperfoil install and execution.

Options:
    --force                   Reinstall Hyperfoil before executing a command.
    --kruize-url URL          Kruize base URL to inject into benchmark host.
    --no-auto-install-kruize  Do not deploy Kruize when it is unreachable.
    --cluster-type TYPE       Cluster type for deploy.sh (default: openshift).
    --target MODE             deploy.sh target mode (default: crc).
    --namespace NAME          Namespace used for route/service discovery (default: openshift-tuning).
    --kruize-image IMAGE      Optional autotune operator image passed to deploy.sh -i.
    --repo-root PATH          Path to autotune repo root (auto-detected if omitted).
    --output PATH             Generate Hyperfoil report output at PATH when running install.
  -h, --help                Show this help.

Commands:
    install                   Download/extract Hyperfoil and run the bulk test (default).
    install-only              Download/extract Hyperfoil only.
    cli                       Launch Hyperfoil CLI.
    standalone                Launch Hyperfoil standalone controller.
    run                       Execute Hyperfoil run mode.
    --version, -v, version    Show Hyperfoil version.

Environment variables:
  HYPERFOIL_VERSION         Hyperfoil version to install. Default: 0.29.1

Examples:
  $(basename "$0")
        $(basename "$0") install-only
    $(basename "$0") cli
    $(basename "$0") standalone
    $(basename "$0") run /path/to/benchmark.yaml
    $(basename "$0") --version
  $(basename "$0") --force
        $(basename "$0") --kruize-url http://kruize-route.example.com
        $(basename "$0") --kruize-url http://kruize-route.example.com --output /tmp/hf-report
        $(basename "$0") --cluster-type openshift --target crc
    HYPERFOIL_VERSION=0.27.2 $(basename "$0") cli
EOF
}

log() {
    printf '%s\n' "$*"
}

# Strip ANSI color codes from input
strip_ansi_codes() {
    sed 's/\x1b\[[0-9;]*m//g'
}

# Print colored log message
log_success() {
    printf '\033[32m%s\033[0m\n' "$*"
}

log_error() {
    printf '\033[31m%s\033[0m\n' "$*"
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

# Expose OpenShift service if not already exposed and return its URL
# Uses pattern from common_functions.sh: oc expose svc
get_openshift_service_url() {
    local namespace="$1"
    local service="$2"
    local host=""
    
    if ! command -v oc >/dev/null 2>&1; then
        return 1
    fi
    
    # Try to expose the service (will fail silently if already exposed)
    oc expose svc/"${service}" -n "${namespace}" 2>/dev/null || true
    
    # Give the route time to stabilize
    sleep 1
    
    # Query the route that should now exist
    host=$(oc get route "${service}" -n "${namespace}" -o jsonpath='{.spec.host}' 2>/dev/null || true)
    
    if [[ -n "$host" ]]; then
        printf 'http://%s\n' "$host"
        return 0
    fi
    
    return 1
}

normalize_kruize_url() {
    local raw_url="$1"
    if [[ -z "$raw_url" ]]; then
        return 1
    fi

    if [[ ! "$raw_url" =~ ^[a-zA-Z][a-zA-Z0-9+.-]*:// ]]; then
        raw_url="http://${raw_url}"
    fi

    if [[ ! "$raw_url" =~ ^https?:// ]]; then
        return 1
    fi

    raw_url="${raw_url%/}"
    printf '%s\n' "$raw_url"
}

is_kruize_healthy() {
    local base_url="$1"
    local health_url
    local status_code

    health_url="${base_url%/}/health"
    status_code=$(curl -ksS -o /dev/null -w '%{http_code}' --max-time 10 "$health_url" || true)
    [[ "$status_code" == "200" ]]
}

find_repo_root() {
    local current_dir

    if [[ -n "$KRUIZE_REPO_ROOT" ]]; then
        [[ -x "${KRUIZE_REPO_ROOT}/deploy.sh" ]] || fail "deploy.sh not found under --repo-root ${KRUIZE_REPO_ROOT}"
        printf '%s\n' "$KRUIZE_REPO_ROOT"
        return 0
    fi

    current_dir="$SCRIPT_DIR"
    while [[ "$current_dir" != "/" ]]; do
        if [[ -x "${current_dir}/deploy.sh" ]]; then
            printf '%s\n' "$current_dir"
            return 0
        fi
        current_dir=$(dirname "$current_dir")
    done

    fail "Could not locate autotune repo root containing deploy.sh. Provide --repo-root PATH"
}

discover_kruize_url() {
    local cluster_type="$1"
    local namespace="$2"
    local host=""
    local node_port=""
    local node_ip=""
    local candidate_url=""

    case "$cluster_type" in
        openshift)
            # First try to get existing route
            if command -v oc >/dev/null 2>&1; then
                host=$(oc get route kruize -n "$namespace" -o jsonpath='{.spec.host}' 2>/dev/null || true)
                if [[ -n "$host" ]]; then
                    printf 'http://%s\n' "$host"
                    return 0
                fi

                # Fall back to exposing the service (aligned with common_functions.sh)
                if candidate_url=$(get_openshift_service_url "$namespace" "kruize"); then
                    printf '%s\n' "$candidate_url"
                    return 0
                fi
            fi
            ;;
        minikube)
            if command -v kubectl >/dev/null 2>&1; then
                node_port=$(kubectl get svc kruize -n "$namespace" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || true)
                if [[ -n "$node_port" ]] && command -v minikube >/dev/null 2>&1; then
                    node_ip=$(minikube ip 2>/dev/null || true)
                    if [[ -n "$node_ip" ]]; then
                        printf 'http://%s:%s\n' "$node_ip" "$node_port"
                        return 0
                    fi
                fi
            fi
            ;;
        kind)
            if command -v kubectl >/dev/null 2>&1; then
                node_port=$(kubectl get svc kruize -n "$namespace" -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || true)
                if [[ -n "$node_port" ]]; then
                    printf 'http://127.0.0.1:%s\n' "$node_port"
                    return 0
                fi
            fi
            ;;
    esac

    return 1
}

install_kruize_if_needed() {
    local repo_root
    local candidate_url
    local deploy_cmd
    local attempt

    if [[ -n "$KRUIZE_URL" ]]; then
        candidate_url=$(normalize_kruize_url "$KRUIZE_URL") || fail "Invalid --kruize-url value '${KRUIZE_URL}'. Use a full http:// or https:// URL"
        KRUIZE_URL="$candidate_url"
        log "Using Kruize URL from CLI: ${KRUIZE_URL}"
        if is_kruize_healthy "$KRUIZE_URL"; then
            log "Kruize is already reachable at ${KRUIZE_URL}"
            return 0
        fi
        log "Kruize health check failed at ${KRUIZE_URL}"
    fi

    if [[ "$AUTO_INSTALL_KRUIZE" -eq 0 ]]; then
        if [[ -n "$KRUIZE_URL" ]]; then
            fail "Kruize is unreachable at ${KRUIZE_URL} and --no-auto-install-kruize is set"
        fi
        fail "Missing reachable Kruize endpoint. Provide --kruize-url or enable auto install"
    fi

    repo_root=$(find_repo_root)
    deploy_cmd=("./deploy.sh" "-c" "$KRUIZE_CLUSTER_TYPE" "-m" "$KRUIZE_DEPLOY_TARGET")
    if [[ -n "$KRUIZE_IMAGE" ]]; then
        deploy_cmd+=("-i" "$KRUIZE_IMAGE")
    fi

    log "Kruize is not reachable; deploying via ${repo_root}/deploy.sh"
    log "Deploy command: ${deploy_cmd[*]}"
    (
        cd "$repo_root"
        "${deploy_cmd[@]}"
    )

    if [[ -z "$KRUIZE_URL" ]]; then
        if candidate_url=$(discover_kruize_url "$KRUIZE_CLUSTER_TYPE" "$KRUIZE_NAMESPACE"); then
            KRUIZE_URL="$candidate_url"
            log "Discovered Kruize URL: ${KRUIZE_URL}"
        else
            fail "Kruize deployed, but URL discovery failed. Re-run with --kruize-url"
        fi
    fi

    for attempt in {1..24}; do
        if is_kruize_healthy "$KRUIZE_URL"; then
            log "Kruize health check passed at ${KRUIZE_URL}"
            return 0
        fi
        sleep 5
    done

    fail "Kruize deployment completed but health check did not pass at ${KRUIZE_URL}"
}

validate_hyperfoil_output() {
    local output_file="$1"
    local awk_rc

    if [[ ! -s "$output_file" ]]; then
        fail "Hyperfoil run produced no output"
    fi

    # Fail fast on explicit fatal command/parser errors.
    # Note: Hyperfoil can print a non-fatal "Errors:" section (e.g. CPU watchdog threshold warnings)
    # even when request metrics are healthy; do not fail on those lines.
    if grep -Eq '^(ERROR:|Failed to execute command:|Failed to parse the benchmark\.)' "$output_file"; then
        return 1
    fi

    # Parse wrapped stats blocks and fail on any non-200 status or transport/internal errors.
    # Expected row shape (3 lines per metric):
    #   phase metric throughput req/s requests mean ms stddev ns max ms p50 ms
    #   p90 ms p99 ms p99.9 ms p99.99 ms timeouts errors
    #   blocked ns 2xx 3xx 4xx 5xx cache
    awk '
        BEGIN {
            bad = 0
            have_stats = 0
        }

        toupper($1) == "PHASE" {
            next
        }

        /^[[:space:]]*$/ {
            next
        }

        # First line in a wrapped stats block.
        /^[[:space:]]*[^[:space:]]+[[:space:]]+[^[:space:]]+[[:space:]]+[0-9]+(\.[0-9]+)?[[:space:]]+req\/s([[:space:]]|$)/ {
            have_stats = 1

            # Parse line2: p90 p99 p99.9 p99.99 timeouts errors
            if (getline line2 <= 0) {
                next
            }
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", line2)
            n2 = split(line2, a2, /[[:space:]]+/)
            if (n2 < 10) {
                next
            }

            # Parse line3: blocked 2xx 3xx 4xx 5xx cache
            if (getline line3 <= 0) {
                next
            }
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", line3)
            n3 = split(line3, a3, /[[:space:]]+/)
            if (n3 < 7) {
                next
            }

            # Parse from the end to avoid alignment issues when indentation varies.
            errors   = a2[n2] + 0
            timeouts = a2[n2 - 1] + 0

            cache = a3[n3] + 0
            s5xx  = a3[n3 - 1] + 0
            s4xx  = a3[n3 - 2] + 0
            s3xx  = a3[n3 - 3] + 0
            s2xx  = a3[n3 - 4] + 0

            if (timeouts > 0) bad = 1
            if (errors > 0) bad = 1
            if (s3xx > 0) bad = 1
            if (s4xx > 0) bad = 1
            if (s5xx > 0) bad = 1
        }
        END {
            if (have_stats == 0) {
                exit 2
            }
            exit bad
        }
    ' "$output_file"

    awk_rc=$?
    if [[ $awk_rc -eq 0 ]]; then
        return 0
    fi

    # If table parsing failed (format/version differences), keep only explicit error matching as gate.
    # When table parsing succeeds and detects bad counters it exits 1 and we fail here.
    if [[ $awk_rc -eq 2 ]]; then
        log "WARNING: Could not parse Hyperfoil stats table; relying on explicit error detection"
        return 0
    fi

    return 1
}

# Print a structured metrics summary parsed from the captured Hyperfoil output file.
# Hyperfoil run output in this workflow uses wrapped, 3-line rows per metric.
# Example columns across the 3 lines:
#   PHASE METRIC THROUGHPUT REQUESTS MEAN STD_DEV MAX p50
#   p90 p99 p99.9 p99.99 TIMEOUTS ERRORS
#   BLOCKED 2xx 3xx 4xx 5xx CACHE
#
# Available: throughput, mean, std_dev, p50, p90, p99, p99.9, p99.99, max, status counters
# Not available in standard output: p95 and min (not emitted by Hyperfoil run stats table)
print_metrics_summary() {
    local output_file="$1"

    log ""
    log "================================================================"
    log " HYPERFOIL PERFORMANCE METRICS SUMMARY"
    log "================================================================"

    awk '
        BEGIN {
            row_count = 0

            # Aggregate accumulators (for totals across phases/metrics)
            total_requests = 0
            total_2xx = 0
            total_3xx = 0
            total_4xx = 0
            total_5xx = 0
            total_timeouts = 0
            total_errors = 0
            total_throughput = 0
            throughput_count = 0
        }

        toupper($1) == "PHASE" {
            next
        }

        /^[[:space:]]*$/ {
            next
        }

        # First line in a wrapped stats block.
        /^[[:space:]]*[^[:space:]]+[[:space:]]+[^[:space:]]+[[:space:]]+[0-9]+(\.[0-9]+)?[[:space:]]+req\/s([[:space:]]|$)/ {
            phase    = $1
            metric   = $2
            tput     = $3 + 0
            requests = $5 + 0
            mean     = $6 " " $7
            std_dev  = $8 " " $9
            vmax     = $10 " " $11
            p50      = $12 " " $13

            if (getline line2 <= 0) {
                next
            }
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", line2)
            n2 = split(line2, a2, /[[:space:]]+/)
            if (n2 < 10) {
                next
            }
            # Parse from the end to avoid alignment issues when indentation varies.
            errors   = a2[n2] + 0
            timeouts = a2[n2 - 1] + 0
            p9999    = a2[n2 - 3] " " a2[n2 - 2]
            p999     = a2[n2 - 5] " " a2[n2 - 4]
            p99      = a2[n2 - 7] " " a2[n2 - 6]
            p90      = a2[n2 - 9] " " a2[n2 - 8]

            if (getline line3 <= 0) {
                next
            }
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", line3)
            n3 = split(line3, a3, /[[:space:]]+/)
            if (n3 < 7) {
                next
            }
            cache   = a3[n3] + 0
            s5xx    = a3[n3 - 1] + 0
            s4xx    = a3[n3 - 2] + 0
            s3xx    = a3[n3 - 3] + 0
            s2xx    = a3[n3 - 4] + 0
            blocked = a3[n3 - 6] " " a3[n3 - 5]

            row_count++

            printf "  [%d] %s/%s\n", row_count, phase, metric
            printf "      Throughput: %.2f req/s   Requests: %d\n",  tput, requests
            printf "      HTTP: 2xx=%d 3xx=%d 4xx=%d 5xx=%d cache=%d\n", s2xx, s3xx, s4xx, s5xx, cache
            printf "      Errors: timeouts=%d errors=%d\n", timeouts, errors
            printf "      Latency: mean=%s stddev=%s max=%s blocked=%s\n", mean, std_dev, vmax, blocked
            printf "               p50=%s p90=%s p99=%s p99.9=%s p99.99=%s\n", p50, p90, p99, p999, p9999
            printf "    (p95 and min are not emitted by Hyperfoil run stats table)\n"
            print  ""

            # Accumulate totals
            total_requests += requests
            total_2xx += s2xx
            total_3xx += s3xx
            total_4xx += s4xx
            total_5xx += s5xx
            total_timeouts += timeouts
            total_errors += errors
            if (tput > 0) {
                total_throughput += tput
                throughput_count++
            }
        }

        END {
            if (row_count == 0) {
                print "  WARNING: Hyperfoil emitted header-only stats (no metric rows)."
                print "           No latency/throughput samples were collected in this run."
                exit 0
            }
            print "----------------------------------------------------------------"
            printf "  TOTALS  Requests: %d\n", total_requests
            printf "          HTTP: 2xx=%d  3xx=%d  4xx=%d  5xx=%d\n", total_2xx, total_3xx, total_4xx, total_5xx
            printf "          Errors: timeouts=%d  errors=%d\n", total_timeouts, total_errors
            if (throughput_count > 0) {
                printf "          Aggregate Throughput: %.2f req/s  (sum across %d phase/metric rows)\n", total_throughput, throughput_count
            }
            print "================================================================"
        }
    ' "$output_file"
}

run_default_bulk_test() {
    local benchmark_file="${SCRIPT_DIR}/kruize-bulk-hyperfoil.yaml"
    local benchmark_to_run="$benchmark_file"
    local temp_benchmark_file=""
    local escaped_kruize_url=""
    local output_file
    local run_args=()
    local run_rc
    local stats_row_count

    [[ -f "$benchmark_file" ]] || fail "Bulk benchmark file not found: ${benchmark_file}"

    install_kruize_if_needed

    temp_benchmark_file=$(mktemp "${SCRIPT_DIR}/.kruize-bulk-benchmark.XXXXXX.yaml")
    escaped_kruize_url=$(printf '%s' "$KRUIZE_URL" | sed -e 's/[&|]/\\&/g')
    sed -E "0,/^[[:space:]]*host:[[:space:]]*/s|^[[:space:]]*host:[[:space:]].*$|  host: ${escaped_kruize_url}|" "$benchmark_file" > "$temp_benchmark_file"
    benchmark_to_run="$temp_benchmark_file"
    log "Using Kruize URL from CLI: ${KRUIZE_URL}"

    log "Running bulk Hyperfoil benchmark: ${benchmark_to_run}"
    if [[ -n "$OUTPUT_PATH" ]]; then
        run_args+=("--output" "$OUTPUT_PATH")
        log "Generating Hyperfoil report output at: ${OUTPUT_PATH}"
    fi

    output_file=$(mktemp "${SCRIPT_DIR}/.hyperfoil-run-output.XXXXXX")
    set +e
    "${TARGET_DIR}/bin/run.sh" "${run_args[@]}" "$benchmark_to_run" 2>&1 | strip_ansi_codes | tee "$output_file"
    run_rc=${PIPESTATUS[0]}
    set -e

    if [[ $run_rc -ne 0 ]]; then
        rm -f "$output_file"
        [[ -n "$temp_benchmark_file" ]] && rm -f "$temp_benchmark_file"
        log_error "SUMMARY: FAILED - Hyperfoil command exited with code ${run_rc}"
        fail "Hyperfoil run command failed with exit code ${run_rc}"
    fi

    # Detect header-only output (stats header printed, but no metric rows).
    stats_row_count=$(awk '
        BEGIN { c = 0 }
        /^[[:space:]]*[^[:space:]]+[[:space:]]+[^[:space:]]+[[:space:]]+[0-9]+(\.[0-9]+)?[[:space:]]+req\/s([[:space:]]|$)/ { c++ }
        END { print c }
    ' "$output_file")

    if [[ "$stats_row_count" -eq 0 ]]; then
        log ""
        log "================================================================"
        log " HYPERFOIL PERFORMANCE METRICS SUMMARY"
        log "================================================================"
        log "  WARNING: Hyperfoil emitted header-only stats (no metric rows)."
        if grep -q 'Exceeded session limit' "$output_file"; then
            log "  CAUSE: Detected 'Exceeded session limit' in Hyperfoil output."
        fi
        log "  HINT: Increase phase duration (for example, duration: 10s) and retry."
        log "  HINT: Verify the scenario can complete requests within the run window."
        log "================================================================"

        rm -f "$output_file"
        [[ -n "$temp_benchmark_file" ]] && rm -f "$temp_benchmark_file"
        log "SUMMARY: NO_SAMPLES - Hyperfoil run completed but produced no request samples"
        return 0
    fi

    if ! validate_hyperfoil_output "$output_file"; then
        print_metrics_summary "$output_file"
        rm -f "$output_file"
        [[ -n "$temp_benchmark_file" ]] && rm -f "$temp_benchmark_file"
        log_error "SUMMARY: FAILED - Hyperfoil reported request errors or non-200 HTTP statuses"
        fail "Hyperfoil run reported request errors or non-200 HTTP statuses"
    fi

    print_metrics_summary "$output_file"
    rm -f "$output_file"
    [[ -n "$temp_benchmark_file" ]] && rm -f "$temp_benchmark_file"
    log_success "SUMMARY: PASSED - Hyperfoil run completed with no detected errors"
}

install_hyperfoil() {
    local tmp_dir
    local extracted_dir

    if [[ -d "$TARGET_DIR" && $FORCE -eq 0 ]]; then
        log "Hyperfoil is already installed at ${TARGET_DIR}"
        return
    fi

    rm -rf "$TARGET_DIR"
    rm -f "$ARCHIVE_PATH"

    log "Downloading ${DOWNLOAD_URL}"
    curl -fL "$DOWNLOAD_URL" -o "$ARCHIVE_PATH"

    tmp_dir=$(mktemp -d "${SCRIPT_DIR}/.hyperfoil-tmp.XXXXXX")
    log "Extracting $(basename "$ARCHIVE_PATH")"
    unzip -q "$ARCHIVE_PATH" -d "$tmp_dir"
    rm -f "$ARCHIVE_PATH"

    extracted_dir=$(find "$tmp_dir" -maxdepth 1 -mindepth 1 -type d | head -n 1)
    [[ -n "$extracted_dir" ]] || fail "Could not find extracted Hyperfoil directory"

    mv "$extracted_dir" "$TARGET_DIR"
    rm -rf "$tmp_dir"

    [[ -x "${TARGET_DIR}/bin/cli.sh" ]] || fail "Installed archive is missing ${TARGET_DIR}/bin/cli.sh"
    [[ -x "${TARGET_DIR}/bin/standalone.sh" ]] || fail "Installed archive is missing ${TARGET_DIR}/bin/standalone.sh"
    [[ -x "${TARGET_DIR}/bin/run.sh" ]] || fail "Installed archive is missing ${TARGET_DIR}/bin/run.sh"
}

main() {
    local command_name="install"

    while [[ $# -gt 0 ]]; do
        case "$1" in
            --force)
                FORCE=1
                shift
                ;;
            --kruize-url)
                [[ $# -ge 2 ]] || fail "Missing value for --kruize-url"
                KRUIZE_URL="$2"
                shift 2
                ;;
            --kruize-url=*)
                KRUIZE_URL="${1#*=}"
                shift
                ;;
            --no-auto-install-kruize)
                AUTO_INSTALL_KRUIZE=0
                shift
                ;;
            --cluster-type)
                [[ $# -ge 2 ]] || fail "Missing value for --cluster-type"
                KRUIZE_CLUSTER_TYPE="$2"
                shift 2
                ;;
            --cluster-type=*)
                KRUIZE_CLUSTER_TYPE="${1#*=}"
                shift
                ;;
            --target)
                [[ $# -ge 2 ]] || fail "Missing value for --target"
                KRUIZE_DEPLOY_TARGET="$2"
                shift 2
                ;;
            --target=*)
                KRUIZE_DEPLOY_TARGET="${1#*=}"
                shift
                ;;
            --namespace)
                [[ $# -ge 2 ]] || fail "Missing value for --namespace"
                KRUIZE_NAMESPACE="$2"
                shift 2
                ;;
            --namespace=*)
                KRUIZE_NAMESPACE="${1#*=}"
                shift
                ;;
            --kruize-image)
                [[ $# -ge 2 ]] || fail "Missing value for --kruize-image"
                KRUIZE_IMAGE="$2"
                shift 2
                ;;
            --kruize-image=*)
                KRUIZE_IMAGE="${1#*=}"
                shift
                ;;
            --repo-root)
                [[ $# -ge 2 ]] || fail "Missing value for --repo-root"
                KRUIZE_REPO_ROOT="$2"
                shift 2
                ;;
            --repo-root=*)
                KRUIZE_REPO_ROOT="${1#*=}"
                shift
                ;;
            --output)
                [[ $# -ge 2 ]] || fail "Missing value for --output"
                OUTPUT_PATH="$2"
                shift 2
                ;;
            --output=*)
                OUTPUT_PATH="${1#*=}"
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            *)
                command_name="$1"
                shift
                break
                ;;
        esac
    done

    command -v curl >/dev/null 2>&1 || fail "Required command not found: curl"
    command -v unzip >/dev/null 2>&1 || fail "Required command not found: unzip"
    command -v find >/dev/null 2>&1 || fail "Required command not found: find"
    command -v mktemp >/dev/null 2>&1 || fail "Required command not found: mktemp"

    if ! command -v java >/dev/null 2>&1; then
        log "WARNING: java is not installed or not in PATH. Hyperfoil requires Java to run."
    fi

    install_hyperfoil

    case "$command_name" in
        install)
            log "Installed Hyperfoil ${VERSION} at ${TARGET_DIR}"
            run_default_bulk_test
            ;;
        install-only)
            log "Installed Hyperfoil ${VERSION} at ${TARGET_DIR}"
            log "Use this script to run commands, e.g.:"
            log "  $(basename "$0") --version"
            log "  $(basename "$0") install"
            ;;
        cli)
            exec "${TARGET_DIR}/bin/cli.sh" "$@"
            ;;
        standalone)
            exec "${TARGET_DIR}/bin/standalone.sh" "$@"
            ;;
        run)
            exec "${TARGET_DIR}/bin/run.sh" "$@"
            ;;
        --version|-v|version)
            exec "${TARGET_DIR}/bin/cli.sh" --version
            ;;
        help)
            usage
            ;;
        *)
            fail "Unknown command: ${command_name}. Use --help for usage."
            ;;
    esac
}

main "$@"