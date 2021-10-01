#!/bin/bash

cd /opt/app

# Start the HPO service
python3 hyperparameter_tuning/service.py &

# Wait for optuna to start
sleep 2

# Start Autotune
bash target/bin/Autotune
