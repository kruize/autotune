#!/bin/bash
#todo add count results and recommendations
#todo Consider %clustername%
# Function to display the usage instructions.
usage() {
  echo "Usage: $0 -h <host> -p <port> -d <dbname> -U <username> -W <password> -e <cluster_name> -s <monitoring_start_time YYYY-MM-DD HH:MM:SS> -t <monitoring_end_time YYYY-MM-DD HH:MM:SS> -n <day to debug default 1 days>"
}

# Define a function to generate the JSON object
generate_json() {
    local experiment_name="$1"
    local metrics_available_from_date="$2"
    local metrics_available_to_date="$3"
    local from_date="$4"
    local to_date="$5"
    local results_count="$6"
    local recommendations_count="$7"
    local duration_sum_minutes="$8"
    local days="$9"
    local missing_dates="${10}"
    local last_recommendation_date="${11}"

    # Construct the JSON object
    cat <<EOF
{
    "experiment_name": "$experiment_name",
    "metrics_available_from_date": "$metrics_available_from_date",
    "metrics_available_to_date": "$metrics_available_to_date",
    "debug": {
      "for_last" : "$days days",
      "from_date": "$from_date",
      "to_date": "$to_date",
      "results_count": $results_count,
      "recommendations_count": $recommendations_count,
      "last_recommendation_date" : "$last_recommendation_date",
      "duration_sum_minutes": $duration_sum_minutes,
      "missing_dates": [$missing_dates]
    }
}
EOF
}

# Initialize variables with default values
host=""
port=""
dbname=""
username=""
password=""
experiment_name=""
monitoring_start_time=""
monitoring_end_time=""
finalOutput=()

# Parse command-line arguments
while getopts "h:p:d:U:W:e:s:t:n:y" opt; do
  case $opt in
    h) host="$OPTARG";;
    p) port="$OPTARG";;
    d) dbname="$OPTARG";;
    U) username="$OPTARG";;
    W) password="$OPTARG";;
    e) experiment_name="$OPTARG";;
    s) monitoring_start_time="$OPTARG";;
    t) monitoring_end_time="$OPTARG";;
    n) days_to_debug="$OPTARG";;
    y) non_interactive="true";;
    \?) echo "Invalid option: -$OPTARG" >&2
        usage
        exit 1
        ;;
  esac
done

# Check if all mandatory parameters are provided
if [ -z "$experiment_name" ]; then
  echo " -e <cluster_name> parameters are mandatory."
  usage
  exit 1
fi

psql='psql'
[ -n "$host" ] && psql+=" -h $host"
[ -n "$port" ] && psql+=" -p $port"
[ -n "$dbname" ] && psql+=" -d $dbname"
[ -n "$username" ] && psql+=" -U $username"

# Connect to the PostgreSQL database and execute the query
exp_to_check_in_String=$(PGPASSWORD="$password" $psql -t -A -c "SELECT experiment_name FROM public.kruize_experiments WHERE experiment_name ilike '%$experiment_name%'")

IFS=$'\n' read -d '' -ra experiments_array <<< "$exp_to_check_in_String"

# Get the size of the array
experiment_count=${#experiments_array[@]}

echo "Total $experiment_count experiments found !"


# Loop through the array and do something with each experiment_name
for experiment_name in "${experiments_array[@]}"; do
    criteria=""
    [ -n "$monitoring_start_time" ] && criteria+=" and interval_start_time >= '$monitoring_start_time'"
    [ -n "$monitoring_end_time" ] && criteria+=" and interval_end_time <= '$monitoring_end_time'"
    # Query to get the min and max dates
    min_max_dates=$(PGPASSWORD="$password" $psql -t -A -c "SELECT min(interval_start_time), max(interval_end_time) FROM public.kruize_results WHERE experiment_name='$experiment_name' $criteria")
    # Split the min_max_dates into minDate and maxDate
    IFS='|' read -ra dates <<< "$min_max_dates"
    minDate="${dates[0]}"
    maxDate="${dates[1]}"
    if [[ "$min_max_dates" == "|" ]]; then
      finalOutput+=$(generate_json $experiment_name "" "" "" "" "0" "0" "0" $days_to_debug "" "")" , "
    else
      if [ -n "$days_to_debug" ]; then
        minCalculatedDate=$(date -u -d "$maxDate $days_to_debug day ago" "+%Y-%m-%d %H:%M:%S")
      else
        days_to_debug=1
        minCalculatedDate=$(date -u -d "$maxDate 1 day ago" "+%Y-%m-%d %H:%M:%S")
      fi
      #Query find results count
      results_count=$(PGPASSWORD="$password" $psql -t -A -c "SELECT count(*) FROM public.kruize_results WHERE experiment_name='$experiment_name' and interval_start_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'")
      #Query find recommendations count
      recommendations_count=$(PGPASSWORD="$password" $psql -t -A -c "SELECT count(*) FROM public.kruize_recommendations WHERE experiment_name='$experiment_name' and interval_end_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'")
      #Query find last recommendations date
      last_recommendation_date=$(PGPASSWORD="$password" $psql -t -A -c "SELECT max(interval_end_time) FROM public.kruize_recommendations WHERE experiment_name='$experiment_name' and interval_end_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'")
      # Query to get the sum of duration_minutes
      duration_sum=$(PGPASSWORD="$password" $psql -t -A -c "SELECT sum(duration_minutes) FROM public.kruize_results WHERE experiment_name='$experiment_name' and interval_start_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'")
      # Generate a list of dates to check
      dates_to_check=()
      currentDate="$minCalculatedDate"
      while [[ "$currentDate" < "$maxDate" ]]; do
        dates_to_check+=("$currentDate")
        currentDate=$(date -u -d "$currentDate 15 minutes" "+%Y-%m-%d %H:%M:%S")
      done
      # Initialize an empty string
      dates_to_check_in_String=""
      # Loop through the elements and format them
      for element in "${dates_to_check[@]}"; do
        # Enclose each element with single quotes and add it to the dates_to_check_in_String string
        dates_to_check_in_String+="'$element',"
      done
      # Remove the trailing comma
      dates_to_check_in_String=${dates_to_check_in_String%,}
      # Caution very long "IN" lists can have performance implications, and extremely long queries may not be practical in some cases due to memory and execution time constraints.
      # Query to get missing dates and corresponding records
      missing_dates_records=$(PGPASSWORD="$password" $psql -t -A -c "SELECT interval_start_time FROM public.kruize_results WHERE experiment_name='$experiment_name' AND interval_start_time in ($dates_to_check_in_String);")
      # Find the missing dates
      missing_dates=()
      for date in "${dates_to_check[@]}"; do
        if [[ ! $missing_dates_records == *"$date"* ]]; then
          missing_dates+=("$date")
        fi
      done
      missing_dates_in_String=""
      # Loop through the elements and format them
      for element in "${missing_dates[@]}"; do
        missing_dates_in_String+="\"$element\","
      done
      # Remove the trailing comma
      missing_dates_in_String=${missing_dates_in_String%,}

      finalOutput+=$(generate_json $experiment_name "${minDate}" "$maxDate" "$minCalculatedDate" "$maxDate" "$results_count" "$recommendations_count" $duration_sum $days_to_debug "$missing_dates_in_String" "$last_recommendation_date" )" , "
    fi
done
echo "["
echo "${finalOutput[@]}"
echo "{}]"

