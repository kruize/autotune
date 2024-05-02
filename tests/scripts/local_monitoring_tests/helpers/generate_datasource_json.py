import csv
import json

def generate_datasource_json(csv_file, json_file):
    datasources = []
    with open(csv_file, 'r', newline='') as csvfile:
        reader = csv.DictReader(csvfile)
        for row in reader:
            data_source = {
                "name": row['name'] if row['name'] != 'null' else "",
                "provider": row['provider'] if row['provider'] != 'null' else "",
                "serviceName": row['serviceName'] if row['serviceName'] != 'null' else "",
                "namespace": row['namespace'] if row['namespace'] != 'null' else "",
                "url": row['url'] if row['url'] != 'null' else ""
            }
            datasources.append(data_source)

    with open(json_file, 'w') as jsonfile:
        json.dump(datasources, jsonfile, indent=4)

csv_file_path = '../csv_data/datasources.csv'
json_file_path = '../json_files/datasources.json'

generate_datasource_json(csv_file_path, json_file_path)
