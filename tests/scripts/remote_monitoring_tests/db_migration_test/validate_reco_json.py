import sys, getopt
import datetime
import json
sys.path.append("../..")

from helpers.fixtures import *
from helpers.generate_rm_jsons import *
from helpers.kruize import *
from helpers.list_reco_json_schema import *
from helpers.medium_term_list_reco_json_schema import *
from helpers.long_term_list_reco_json_schema import *
from helpers.list_reco_json_validate import *
from helpers.utils import *

failed = 0

def validate_engine(terms_obj, cpu):
	global failed
	cpu_format_type = "cores"
	memory_format_type = "MiB"
	engines_list = ["cost", "performance"]

	# Extract recommendation engine objects
	recommendation_engines_object = None
	if "recommendation_engines" in terms_obj:
		recommendation_engines_object = terms_obj["recommendation_engines"]
		if None != recommendation_engines_object:
			for engine_entry in engines_list:
				if engine_entry in terms_obj["recommendation_engines"]:
					engine_obj = terms_obj["recommendation_engines"][engine_entry]
					reco_config = engine_obj["config"]
					usage_list = ["requests", "limits"]
					for usage in usage_list:
						if cpu == True:
							if reco_config[usage]["cpu"]["amount"] <= 0:
								print(f"cpu amount in recommendation config is {reco_config[usage]['cpu']['amount']}")
								failed += 1
							if reco_config[usage]["cpu"]["format"] != cpu_format_type:
								print(f"cpu format in recommendation config is {reco_config[usage]['cpu']['format']} instead of {cpu_format_type}")
								failed += 1
						if reco_config[usage]["memory"]["amount"] <= 0:
							print(f"cpu amount in recommendation config is {reco_config[usage]['memory']['amount']}")
							failed += 1
						if reco_config[usage]["memory"]["format"] != memory_format_type:
							print(f"memory format in recommendation config is {reco_config[usage]['memory']['format']} instead of {memory_format_type}")
							failed += 1


def validate_reco_json(json_file, end_time):
	global failed
	cpu = True
	list_reco_json = json.load(open(json_file))

	# Validate the json values
	for containers in list_reco_json[0]["kubernetes_objects"][0]["containers"]:
			actual_container_name = containers["container_name"]
			if actual_container_name == "tfb-server-0": 
				cpu = False


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
			validate_engine(short_term_recommendation, cpu)

			medium_term_recommendation = data_section[end_time]["recommendation_terms"]["medium_term"]

			long_term_recommendation = data_section[end_time]["recommendation_terms"]["long_term"]

def main(argv):
	json_file = ""
	end_time = ""
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

	validate_reco_json(json_file, end_time)
	if failed == 0:
		sys.exit(0)
	else:
		print(f"Validation of recommendations in {json_file} failed!")
		sys.exit(1)


if __name__ == '__main__':
	main(sys.argv[1:])
