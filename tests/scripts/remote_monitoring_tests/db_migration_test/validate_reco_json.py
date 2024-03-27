import sys, getopt
import datetime
import json
sys.path.append("..")

from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.list_reco_json_schema import *
from helpers.medium_term_list_reco_json_schema import *
from helpers.long_term_list_reco_json_schema import *
from helpers.list_reco_json_validate import *
from helpers.utils import *


def validate_reco_json(json_file, end_time):
    failed = 0

    list_reco_json = json.load(open(json_file))


    # Validate the json values
    for containers in list_reco_json[0]["kubernetes_objects"][0]["containers"]:
        actual_container_name = containers["container_name"]
    
        recommendation_section = containers["recommendations"]

        if recommendation_section is None:
            print("Recommendation section is null")
            failed += 1

        high_level_notifications = recommendation_section["notifications"]

        # Check for Recommendation level notifications
        if NOTIFICATION_CODE_FOR_RECOMMENDATIONS_AVAILABLE not in high_level_notifications:
            print("Recommendations available code not present in highl level notifications")
            failed += 1

        data_section = recommendation_section["data"]
        
        # Check if recommendation exists
        if end_time not in data_section:
            print("Interval end time not in data section")
            failed += 1

        # Check for timestamp level notifications
        timestamp_level_notifications = data_section[end_time]["notifications"]
        if NOTIFICATION_CODE_FOR_SHORT_TERM_RECOMMENDATIONS_AVAILABLE not in timestamp_level_notifications:
            print("Short term recommendations available code not present in timestamp level notifications")
            failed += 1
        if NOTIFICATION_CODE_FOR_MEDIUM_TERM_RECOMMENDATIONS_AVAILABLE not in timestamp_level_notifications:
            print("Medium term recommendations available code not present in timestamp level notifications")
            failed += 1
        if NOTIFICATION_CODE_FOR_LONG_TERM_RECOMMENDATIONS_AVAILABLE not in timestamp_level_notifications:
            print("Long term recommendations available code not present in timestamp level notifications")
            failed += 1

        # Check for current recommendation
        recommendation_current = None
        if "current" in data_section[end_time]:
            recommendation_current = data_section[end_time]["current"]

        short_term_recommendation = data_section[end_time]["recommendation_terms"]["short_term"]
        
        medium_term_recommendation = data_section[end_time]["recommendation_terms"]["medium_term"]
        
        long_term_recommendation = data_section[end_time]["recommendation_terms"]["long_term"]

    return failed



def main(argv):
    json_file = ""
    end_time = "" 
    failed = 0
    try:
        opts, args = getopt.getopt(argv,"h:f:e:")
    except getopt.GetoptError:
        print("validate_reco_json.py -f <reco json file> -e <interval end time>")
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print("validate_reco_json.py -f <reco json file> -e <interval end time>")
            sys.exit(0)
        elif opt == '-f':
            json_file = arg
        elif opt == '-e':
            end_time = str(arg)

    failed = validate_reco_json(json_file, end_time)
    if failed == 0:
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == '__main__':
    main(sys.argv[1:])
