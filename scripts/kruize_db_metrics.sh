#!/bin/bash
# Function to display the usage instructions.
usage() {
  echo "Usage: $0 -P -h <optional:host> -p <optional:port> -d <optional:dbname> -U <optional:username> -W <optional:password> -e <workload_name> -s <optional:monitoring_start_time YYYY-MM-DD HH:MM:SS> -t <optional:monitoring_end_time YYYY-MM-DD HH:MM:SS> -n <optional:day to debug default 1 days>"
  echo "Usage: $0 -G -h <host> -H <request header> -e <workload_name pattern matched> -s <optional:monitoring_start_time YYYY-MM-DD HH:MM:SS> -t <optional:monitoring_end_time YYYY-MM-DD HH:MM:SS> -n <optional:day to debug default 1 days>"
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
    local required_duration_minutes="${12}"

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
      "duration_sum_minutes/required": "$duration_sum_minutes/$required_duration_minutes",
      "missing_dates": [$missing_dates]
    }
}
EOF
}

execute_in_gabi() {
  local sql="$1"
  count=0
  sql=$(echo "$sql" | sed "s/'/'\\\''/g")
  curl_command="$gabi -d '{\"query\":\"${sql}\"}'"
  #printf "$curl_command"
  count=$(eval $curl_command |  jq -c '.result[1:]')
  # Extract the elements from the JSON array
  count=$(echo "$count" | jq -r '.[]')
  # Remove double quotes from the resulting string
  count="${count//\"}"
  count=$(echo "$count" | sed 's/\[\|\]//g')
  count=$(echo -n "$count" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  count=$(echo -n "$count" | sed 's/\n//g')
  count="${count#"${count%%[![:space:]]*}"}"
  echo "${count}"
  #$count
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
use_psql=""
use_gabi=""
custom_headers=""
ilike=""
# Parse command-line arguments
while getopts "G:P:h:H:p:d:U:W:e:s:t:n:y" opt; do
  case $opt in
    G) use_gabi="true";;
    P) use_psql="true";;
    h) host="$OPTARG";;
    H) custom_headers="$OPTARG";;
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

# Determine the method to use based on arguments
if [ -n "$use_psql" ] && [ -n "$use_gabi" ]; then
  echo "Error: You can't use both -G and -P options together."
  usage
  exit 1
elif [ -z "$use_psql" ] && [ -z "$use_gabi" ]; then
  echo "Error: You must use either -P or -G option."
  usage
  exit 1
fi

if [ -n "$use_gabi" ]; then
  if [ -z "$host" ] || [ -z "$experiment_name" ] || [ -z "$custom_headers" ] ; then
    echo "All parameters are mandatory when using -u option."
    usage
    exit 1
  fi
  gabi="curl"
  gabi+=" -s '$host'"
  gabi+=" -H '$custom_headers'"
else
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
fi

ilike="'%$experiment_name%'"
GET_EXP_NAME_SQL="SELECT experiment_name FROM public.kruize_experiments WHERE experiment_name ilike $ilike"

if [ -n "$use_gabi" ]; then
  GET_EXP_NAME_SQL_GABI=$(echo "$GET_EXP_NAME_SQL" | sed "s/'/'\\\''/g")
  curl_command="$gabi -d '{\"query\":\"${GET_EXP_NAME_SQL_GABI}\"}'"
  exp_to_check_in_String=$(eval $curl_command |  jq -c '.result[1:] | map(.[0])')
  # Remove leading and trailing brackets [ and ]
  exp_to_check_in_String="${exp_to_check_in_String#\[}"
  exp_to_check_in_String="${exp_to_check_in_String%\]}"
  #echo $exp_to_check_in_String
  # Split the JSON string into an array using ","
  IFS=', ' read -r -a experiments_array <<< "$exp_to_check_in_String"
  # Get the size of the array
  experiment_count=${#experiments_array[@]}
  echo "{\"note\" : \"Total $experiment_count experiments found !\"}"
elif [ -n "$use_psql" ]; then
  # Connect to the PostgreSQL database and execute the query
  exp_to_check_in_String=$(PGPASSWORD="$password" $psql -t -A -c "${GET_EXP_NAME_SQL}")
  IFS=$'\n' read -d '' -ra experiments_array <<< "$exp_to_check_in_String"
  # Get the size of the array
  experiment_count=${#experiments_array[@]}
  echo "Total $experiment_count experiments found !"
fi

# Loop through the array and do something with each experiment_name
for experiment_name in "${experiments_array[@]}"; do
  experiment_name="${experiment_name//\"}"
  GET_MIN_MAX_DATES_SQL="SELECT min(interval_start_time), max(interval_end_time) FROM public.kruize_results WHERE experiment_name='$experiment_name'"
  criteria=" "
  [ -n "$monitoring_start_time" ] && criteria+=" and interval_start_time >= '$monitoring_start_time'"
  [ -n "$monitoring_end_time" ] && criteria+=" and interval_end_time <= '$monitoring_end_time'"
  GET_MIN_MAX_DATES_SQL=${GET_MIN_MAX_DATES_SQL}${criteria}
  if [ -n "$use_gabi" ]; then
    GET_MIN_MAX_DATES_SQL=$(echo "$GET_MIN_MAX_DATES_SQL" | sed "s/'/'\\\''/g")
    curl_command="$gabi -d '{\"query\":\"${GET_MIN_MAX_DATES_SQL}\"}'"
    #echo "$curl_command"
    min_max_dates=$(eval $curl_command |  jq -c '.result[1:]')
    # Extract the elements from the JSON array and join them with a comma
    min_max_dates=$(echo "$min_max_dates" | jq -r '.[] | @csv')
    # Remove double quotes from the resulting string
    min_max_dates="${min_max_dates//\"}"
    IFS=', ' read -r -a dates <<< "$min_max_dates"
    minDate=$(echo ${dates[0]} | sed 's/T/ /; s/Z//')
    maxDate=$(echo ${dates[1]} | sed 's/T/ /; s/Z//')
  elif [ -n "$use_psql" ]; then
    # Query to get the min and max dates
    min_max_dates=$(PGPASSWORD="$password" $psql -t -A -c "$GET_MIN_MAX_DATES_SQL")
    # Split the min_max_dates into minDate and maxDate
    IFS='|' read -ra dates <<< "$min_max_dates"
    minDate="${dates[0]}"
    maxDate="${dates[1]}"
  fi

  if [[ "$min_max_dates" == "|" ]]; then
    finalOutput+=$(generate_json $experiment_name "" "" "" "" "0" "0" "0" $days_to_debug "" "")" , "
  else
    if [ -n "$days_to_debug" ]; then
      minCalculatedDate=$(date -u -d "$maxDate $days_to_debug day ago" "+%Y-%m-%d %H:%M:%S")
    else
      days_to_debug=1
      minCalculatedDate=$(date -u -d "$maxDate 1 day ago" "+%Y-%m-%d %H:%M:%S")
    fi
    if [ -n "$monitoring_start_time" ]; then
      # Convert the date strings to Unix timestamps
      timestamp1=$(date -d "$monitoring_start_time" +%s)
      timestamp2=$(date -d "$minCalculatedDate" +%s)
      if [ "$timestamp2" -le "$timestamp1" ]; then
        minCalculatedDate=$monitoring_start_time
      fi
    fi
  fi
  RES_COUNT_SQL="SELECT count(*) FROM public.kruize_results WHERE experiment_name='${experiment_name}' and interval_start_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'"
  REC_COUNT_SQL="SELECT count(*) FROM public.kruize_recommendations WHERE experiment_name='${experiment_name}' and interval_end_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'"
  LAST_REC_DATE_SQL="SELECT max(interval_end_time) FROM public.kruize_recommendations WHERE experiment_name='${experiment_name}'  and interval_end_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'"
  DURATION_SUM_SQL="SELECT sum(duration_minutes) FROM public.kruize_results WHERE experiment_name='$experiment_name' and interval_start_time >= '$minCalculatedDate' and interval_end_time <= '$maxDate'"
  results_count=0
  if [ -n "$use_gabi" ]; then
     results_count=$(execute_in_gabi "$RES_COUNT_SQL")
     recommendations_count=$(execute_in_gabi "$REC_COUNT_SQL")
     last_recommendation_date=$(execute_in_gabi "$LAST_REC_DATE_SQL")
     duration_sum=$(execute_in_gabi "$DURATION_SUM_SQL")
  elif [ -n "$use_psql" ]; then
    #Query find results count
    results_count=$(PGPASSWORD="$password" $psql -t -A -c "${RES_COUNT_SQL}")
    #Query find recommendations count
    recommendations_count=$(PGPASSWORD="$password" $psql -t -A -c "${REC_COUNT_SQL}")
    #Query find last recommendations date
    last_recommendation_date=$(PGPASSWORD="$password" $psql -t -A -c "${LAST_REC_DATE_SQL}")
    # Query to get the sum of duration_minutes
    duration_sum=$(PGPASSWORD="$password" $psql -t -A -c "${DURATION_SUM_SQL}")
  fi
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
  MISSING_DATES_SQL="SELECT interval_start_time FROM public.kruize_results WHERE experiment_name='$experiment_name' AND interval_start_time in ($dates_to_check_in_String);"
  #echo $MISSING_DATES_SQL
  if [ -n "$use_gabi" ]; then
    missing_dates_records=$(execute_in_gabi "$MISSING_DATES_SQL")
    missing_dates_records=$(echo "$missing_dates_records" | sed 's/T/ /; s/Z//')
  elif [ -n "$use_psql" ]; then
    missing_dates_records=$(PGPASSWORD="$password" $psql -t -A -c "${MISSING_DATES_SQL}")
  fi
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
  mins=$((days_to_debug * 24 * 60))
  temp=$(generate_json $experiment_name "${minDate}" "$maxDate" "$minCalculatedDate" "$maxDate" "$results_count" "$recommendations_count" $duration_sum $days_to_debug "$missing_dates_in_String" "$last_recommendation_date" "$mins" )" , "
  finalOutput+="$temp"

done
echo "["
echo "${finalOutput[@]}"
echo "{}]"

