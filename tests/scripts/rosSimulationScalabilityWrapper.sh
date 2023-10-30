#!/bin/bash

# Default values
ip=""
port=""
count=""
minutesjump=""
name_prefix=""
initial_startdate="2023-01-01T00:00:00.000Z"
limit_days="15"
interval_hours="6"

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --ip)
            ip="$2"
            shift 2
            ;;
        --port)
            port="$2"
            shift 2
            ;;
        --count)
            count="$2"
            shift 2
            ;;
        --minutesjump)
            minutesjump="$2"
            shift 2
            ;;
        --name)
            name_prefix="$2"
            shift 2
            ;;
        --initialstartdate)
            initial_startdate="$2"
            shift 2
            ;;
        --limitdays)
            limit_days="$2"
            shift 2
            ;;
        --intervalhours)
            interval_hours="$2"
            shift 2
            ;;
        --clientthread)
            client_thread="$2"
            shift 2
            ;;
	--prometheusserver)
            prometheus_server="$2"
            shift 2
            ;;

        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

if [[ -z "$ip" || -z "$port" || -z "$count" || -z "$minutesjump" || -z "$name_prefix" ]]; then
    echo "Missing required arguments."
    echo "Usage: $0 --ip <IP> --port <port> --count <count> --minutesjump <minutesjump> --name <name_prefix> --initialstartdate <initial_startdate> --limitdays <limit_days> --intervalhours <interval_hours>"
    exit 1
fi

# Calculate the number of iterations based on interval and limit days
iterations=$(( $limit_days * 24 / $interval_hours ))

# Loop for each iteration
for (( i = 0; i < $iterations; i++ )); do
    # Calculate the current start date for this iteration
    current_startdate=$(date -u -d "$initial_startdate + $(( i * interval_hours )) hours" +"%Y-%m-%dT%H:%M:%S.%3NZ")

    # Build the full command
    full_command="python3 -u rosSimulationScalabilityTest.py --ip $ip --port $port --count $count --minutesjump $minutesjump --startdate $current_startdate --name ${name_prefix}"

    # Execute the command
    echo "Executing: $full_command"
    eval "$full_command"

    # Wait for the command to complete before moving to the next iteration
    wait

    echo "Collecting kruize metrics"
    metrics_command="python3 kruize_metrics.py -c openshift -s ${prometheus_server} -p true -t 360m -r kruizeMetrics-${client_thread}.csv"
    eval "${metrics_command}" &

    # Sleep for a short duration to avoid flooding the system with too many requests
    #sleep 5
done
