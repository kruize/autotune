"""
Copyright (c) 2026 IBM Corporation and others.

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
from helpers.list_layers_schema import list_layers_schema

# Supported values
LAYER_PRESENCE_TYPES_SUPPORTED = ("presence", "queries", "label")
LAYER_PRESENCE_TYPE_NOT_SUPPORTED = "Layer presence type not supported!"

PRESENCE_VALUES_SUPPORTED = ("always", "conditional")
PRESENCE_VALUE_NOT_SUPPORTED = "Presence value not supported!"

VALUE_TYPES_SUPPORTED = ("double", "int", "integer", "categorical", "string")
VALUE_TYPE_NOT_SUPPORTED = "Value type not supported!"

DATASOURCES_SUPPORTED = ("prometheus",)
DATASOURCE_NOT_SUPPORTED = "Datasource not supported!"

JSON_NULL_VALUES = ("is not of type 'string'", "is not of type 'integer'", "is not of type 'number'", "is not of type 'array'", "is not of type 'object'")
VALUE_MISSING = " cannot be empty or null!"


def validate_list_layers_json(list_layers_json, json_schema):
    errorMsg = ""
    try:
        # create a validator with the format checker
        print("Validating json against the json schema...")
        validator = jsonschema.Draft7Validator(json_schema, format_checker=FormatChecker())

        # validate the JSON data against the schema
        errors = ""
        errors = list(validator.iter_errors(list_layers_json))
        print("Validating json against the json schema...done")
        errorMsg = validate_list_layers_json_values(list_layers_json)

        if errors:
            custom_err = ValidationError(errorMsg)
            errors.append(custom_err)
            return errors
        else:
            return errorMsg
    except ValidationError as err:
        print("Received a ValidationError")

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


def validate_list_layers_json_values(layers_json):
    validationErrorMsg = ""

    for layer in layers_json:
        # Validate top-level required fields are not empty
        for key in ["apiVersion", "kind", "layer_name"]:
            if key in layer:
                if not (str(layer[key]) and str(layer[key]).strip()):
                    validationErrorMsg = ",".join([validationErrorMsg, f"{key}" + VALUE_MISSING])

        # Validate metadata
        if "metadata" in layer:
            metadata = layer["metadata"]
            if "name" in metadata:
                if not (str(metadata["name"]) and str(metadata["name"]).strip()):
                    validationErrorMsg = ",".join([validationErrorMsg, "metadata.name" + VALUE_MISSING])


        # Validate layer_presence
        if "layer_presence" in layer:
            presence = layer["layer_presence"]

            # Check that at least one presence type exists
            presence_types_found = [pt for pt in LAYER_PRESENCE_TYPES_SUPPORTED if pt in presence]
            if not presence_types_found:
                validationErrorMsg = ",".join([validationErrorMsg, "layer_presence must contain at least one of: presence, queries, or label!"])
            elif len(presence_types_found) > 1:
                validationErrorMsg = ",".join([validationErrorMsg, "layer_presence must contain only one of: presence, queries, or label!"])

            # Validate 'presence' field if exists
            if "presence" in presence:
                if not (str(presence["presence"]) and str(presence["presence"]).strip()):
                    validationErrorMsg = ",".join([validationErrorMsg, "layer_presence.presence" + VALUE_MISSING])
                elif str(presence["presence"]) not in PRESENCE_VALUES_SUPPORTED:
                    validationErrorMsg = ",".join([validationErrorMsg, PRESENCE_VALUE_NOT_SUPPORTED])

            # Validate 'queries' field if exists
            if "queries" in presence:
                if not isinstance(presence["queries"], list) or len(presence["queries"]) == 0:
                    validationErrorMsg = ",".join([validationErrorMsg, "layer_presence.queries must be a non-empty array!"])
                else:
                    for query in presence["queries"]:
                        for field in ["datasource", "query", "key"]:
                            if field in query:
                                if not (str(query[field]) and str(query[field]).strip()):
                                    validationErrorMsg = ",".join([validationErrorMsg, f"query.{field}" + VALUE_MISSING])
                                elif field == "datasource" and str(query[field]) not in DATASOURCES_SUPPORTED:
                                    validationErrorMsg = ",".join([validationErrorMsg, DATASOURCE_NOT_SUPPORTED])

            # Validate 'label' field if exists
            if "label" in presence:
                if not isinstance(presence["label"], list) or len(presence["label"]) == 0:
                    validationErrorMsg = ",".join([validationErrorMsg, "layer_presence.label must be a non-empty array!"])
                else:
                    for label in presence["label"]:
                        for field in ["name", "value"]:
                            if field in label:
                                if not (str(label[field]) and str(label[field]).strip()):
                                    validationErrorMsg = ",".join([validationErrorMsg, f"label.{field}" + VALUE_MISSING])

        # Validate tunables
        if "tunables" in layer:
            tunables = layer["tunables"]
            if not isinstance(tunables, list) or len(tunables) == 0:
                validationErrorMsg = ",".join([validationErrorMsg, "tunables must be a non-empty array!"])
            else:
                tunable_names = []
                for tunable in tunables:
                    # Check for required fields
                    if "name" in tunable:
                        tunable_name = tunable["name"]
                        if not (str(tunable_name) and str(tunable_name).strip()):
                            validationErrorMsg = ",".join([validationErrorMsg, "tunable.name" + VALUE_MISSING])
                        else:
                            # Check for duplicate tunable names
                            if tunable_name in tunable_names:
                                validationErrorMsg = ",".join([validationErrorMsg, f"Duplicate tunable name: {tunable_name}!"])
                            else:
                                tunable_names.append(tunable_name)

                    # Validate value_type
                    if "value_type" in tunable:
                        value_type = tunable["value_type"]
                        if not (str(value_type) and str(value_type).strip()):
                            validationErrorMsg = ",".join([validationErrorMsg, "tunable.value_type" + VALUE_MISSING])
                        elif str(value_type) not in VALUE_TYPES_SUPPORTED:
                            validationErrorMsg = ",".join([validationErrorMsg, VALUE_TYPE_NOT_SUPPORTED])
                        else:
                            # Validate categorical tunables have choices
                            if value_type == "categorical":
                                if "choices" not in tunable or not isinstance(tunable["choices"], list) or len(tunable["choices"]) == 0:
                                    validationErrorMsg = ",".join([validationErrorMsg, "Categorical tunable must have non-empty choices array!"])
                            # Validate bounded tunables have bounds and step
                            else:
                                if "lower_bound" in tunable or "upper_bound" in tunable or "step" in tunable:
                                    # If any bound field exists, all should exist
                                    if "lower_bound" not in tunable:
                                        validationErrorMsg = ",".join([validationErrorMsg, "Bounded tunable missing lower_bound!"])
                                    if "upper_bound" not in tunable:
                                        validationErrorMsg = ",".join([validationErrorMsg, "Bounded tunable missing upper_bound!"])
                                    if "step" not in tunable:
                                        validationErrorMsg = ",".join([validationErrorMsg, "Bounded tunable missing step!"])

                                    # Validate step is positive
                                    if "step" in tunable:
                                        if not isinstance(tunable["step"], (int, float)) or tunable["step"] <= 0:
                                            validationErrorMsg = ",".join([validationErrorMsg, "Tunable step must be a positive number!"])

    return validationErrorMsg.lstrip(',')
