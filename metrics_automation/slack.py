import os
import io
import json
import requests
from typing import List, Dict
    
os.environ['SLACK_APP_TOKEN'] = '<request_bot_admin_for_token_replace_here>'
os.environ['SLACK_APP_CHANNEL'] = '#metrics_bot'

def post_message_to_slack(text: str, blocks: List[Dict[str, str]] = None):
    print(os.getenv("SLACK_APP_TOKEN"))
    
    return requests.post('https://slack.com/api/chat.postMessage', {
        'token': os.getenv("SLACK_APP_TOKEN"),
        'channel': os.getenv("SLACK_APP_CHANNEL"),
        'text': text,
        'blocks': json.dumps(blocks) if blocks else None
    }).json()	

def metrics_data_to_slack(data: dict):
    start_block= [
        {
            "type": "header",
            "text": {
                "type": "plain_text",
                "text": f"24hrs Data Update for {data['date'][0:10]}"
            }
        },
        {
            "type": "section",
            "fields": [
                {
                    "type": "mrkdwn",
                    "text": f"*🛠️ Kruize Resources:*\n- Instances: {data['instances']}\n- Max CPU: {data['max_cpu']} c\n- Max MEM: {data['max_mem']} GB"
                },
                {
                    "type": "mrkdwn",
                    "text": f"*📈 Database Metrics:*\n- DB Size: {data['db_size']}\n- AWS FSS (Avg): {data['aws_fss']}GB\n- Total Experiments: {data['total_experiments']}\n- Kafka lag: {data['kafka_lag']}"
                }
            ]
        },
        {
            "type": "section",
            "fields": [
                        {
                    "type": "mrkdwn",
                    "text": f"*⏱ Latency (Max/Avg):*\n- UpdateRecommendations: {data['update_recommendations']['max']} / {data['update_recommendations']['avg']}s \n- UpdateResults: {data['update_results']['max']} / {data['update_results']['avg']}s\n- loadResultsbyExpName: {data['load_results_by_exp_name']['max']} / {data['load_results_by_exp_name']['avg']}s"
                },
                {
                    "type": "mrkdwn",
                    "text": f"*📊 24hr API Call Statistics (Success/Failure):*\n- UpdateRecommendations: {data['update_recommendations']['success_count']} / {data['update_recommendations']['failure_count']} \n- UpdateResults: {data['update_results']['success_count']} / {data['update_results']['failure_count']} "
                }
                
            ]
        },
        {
            "type": "section",
            "fields": [
                {
                    "type": "mrkdwn",
                    "text": "*🚨 Alerts:*\n- "
                }
            ]
        }
    ]
    post_message_to_slack("DONE!", start_block)


# Process end
def post_end_process_to_slack(process_name: str):
    # end_time = get_now_str()
    end_block = [
        {
		"type": "header",
		"text": {
			"type": "plain_text",
			"text": "Process successful :large_green_circle:"
		    }
        },
        {
        "type": "section",
        "fields": [{
            "type": "mrkdwn",
            "text": f"Process: _{process_name}_ finished successfully at "
            }
        ]
        }
    ]
    post_message_to_slack("Process ended successfully", end_block)

# Process failed
def post_failed_process_to_slack(process_name: str):
    # failed_time = get_now_str()
    failed_block = [
        {
		"type": "header",
		"text": {
			"type": "mrkdwn",
			"text": "Process Failed :rotating_light:"
		    }
        },
        {
        "type": "section",
        "fields": [{
            "type": "mrkdwn",
            "text": f"Process: _{process_name}_ failed at"
            }
        ]
        }
    ]
    post_message_to_slack("Process failed!", failed_block)