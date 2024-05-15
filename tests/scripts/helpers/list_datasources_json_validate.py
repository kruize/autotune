"""
Copyright (c) 2023, 2023 Red Hat, IBM Corporation and others.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import json
import jsonschema
from jsonschema import FormatChecker
from jsonschema.exceptions import ValidationError
from helpers.list_datasources_json_schema import list_datasources_json_schema

#TODO - currently only prometheus datasurce provider is supported
DATASOURCE_TYPE_SUPPORTED = "prometheus"

JSON_NULL_VALUES = ("is not of type 'string'", "is not of type 'integer'", "is not of type 'number'")
VALUE_MISSING = " cannot be empty or null!"

def validate_list_datasources_json(list_datasources_json, json_schema):
    errorMsg = ""
    try:
        # create a validator with the format checker
        print("Validating json against the json schema...")
        validator = jsonschema.Draft7Validator(json_schema, format_checker=FormatChecker())

        # validate the JSON data against the schema
        errors = ""
        errors = list(validator.iter_errors(list_datasources_json))
        print("Validating json against the json schema...done")
        errorMsg = validate_list_datasources_json_values(list_datasources_json)

        if errors:
            custom_err = ValidationError(errorMsg)
            errors.append(custom_err)
            return errors
        else:
            return errorMsg
    except ValidationError as err:
        print("Received a VaidationError")

        # Check if the exception is due to empty or null required parameters and prepare the response accordingly
        if any(word in err.message for word in JSON_NULL_VALUES):
            errorMsg = "Parameters" + VALUE_MISSING
            return errorMsg
        # Modify the error response in case of additional properties error
        elif str(err.message).__contains__('('):
            errorMsg = str(err.message).split('(')
            return errorMsg[0]
        else:
            return err.message

def validate_list_datasources_json_values(list_datasources_json):
    validationErrorMsg = ""
    obj_arr = ["datasources"]

    for key in list_datasources_json.keys():

        # Check if any of the key is empty or null
        if not (str(list_datasources_json[key]) and str(list_datasources_json[key]).strip()):
            validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])

        for obj in obj_arr:
            if obj == key:
                for subkey in list_datasources_json[key][0].keys():
                    # Check if any of the key is empty or null
                    if not (str(list_datasources_json[key][0][subkey]) and str(list_datasources_json[key][0][subkey]).strip()):
                        print(f"FAILED - {str(list_datasources_json[key][0][subkey])} is empty or null")
                        validationErrorMsg = ",".join([validationErrorMsg, "Parameters" + VALUE_MISSING])
                    elif str(subkey) == "provider" and str(list_datasources_json[key][0][subkey]) not in DATASOURCE_TYPE_SUPPORTED:
                        validationErrorMsg = ",".join([validationErrorMsg, DATASOURCE_TYPE_SUPPORTED])

    return validationErrorMsg.lstrip(',')
