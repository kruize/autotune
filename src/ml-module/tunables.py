def get_all_tunables():
    sla = 'response_time'
    direction = 'minimize'
    tunables = [{"name": "cpu-request", "value_type": "double", "min_value": 1, "max_value": 8}, {"name": "memory-request", "value_type": "double", "min_value": 1, "max_value": 1024}] 
    return sla, direction, tunables

