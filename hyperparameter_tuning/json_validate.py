import jsonschema
from jsonschema import validate, draft7_format_checker

trial_generate_schema = {
    "type": "object",
    "properties": {
        "experiment_id": {"type": "string"},
        "url": {
            "type": "string",
            "format": "uri"
        },
        "operation": {
            "enum": [
                "EXP_TRIAL_GENERATE_NEW",
                "EXP_TRIAL_GENERATE_SUBSEQUENT",
            ]
        }
    },
    "required": ["experiment_id", "url", "operation"],
    "additionalProperties": False
}


def validate_trial_generate_json(trial_generate_json):
    try:
        validate(instance=trial_generate_json, schema=trial_generate_schema, format_checker=draft7_format_checker)
    except jsonschema.exceptions.ValidationError as err:
        print(err)
        return False
    return True
