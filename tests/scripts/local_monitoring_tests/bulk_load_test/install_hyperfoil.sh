#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
VERSION="${HYPERFOIL_VERSION:-0.29.1}"
TARGET_DIR="${SCRIPT_DIR}/hyperfoil"
ARCHIVE_PATH="${SCRIPT_DIR}/hyperfoil-${VERSION}.zip"
DOWNLOAD_URL="https://github.com/Hyperfoil/Hyperfoil/releases/download/hyperfoil-all-${VERSION}/hyperfoil-${VERSION}.zip"
FORCE=0

usage() {
    cat <<EOF
Usage:
    $(basename "$0") [--force] [install]
    $(basename "$0") [--force] install-only
    $(basename "$0") [--force] cli [args...]
    $(basename "$0") [--force] standalone [args...]
    $(basename "$0") [--force] run [args...]
    $(basename "$0") [--force] --version

Single entrypoint for Hyperfoil install and execution.

Options:
    --force                   Reinstall Hyperfoil before executing a command.
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
    HYPERFOIL_VERSION=0.27.2 $(basename "$0") cli
EOF
}

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'ERROR: %s\n' "$*" >&2
    exit 1
}

run_default_bulk_test() {
    local benchmark_file="${SCRIPT_DIR}/kruize-bulk-hyperfoil.yaml"

    [[ -f "$benchmark_file" ]] || fail "Bulk benchmark file not found: ${benchmark_file}"

    if grep -q "KRUIZE_URL_PLACEHOLDER" "$benchmark_file"; then
        fail "Set a real Kruize URL in ${benchmark_file} before running the bulk test"
    fi

    log "Running bulk Hyperfoil benchmark: ${benchmark_file}"
    exec "${TARGET_DIR}/bin/run.sh" "$benchmark_file"
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