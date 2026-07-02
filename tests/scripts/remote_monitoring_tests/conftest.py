"""
Pytest configuration file for API version switching.
This file is automatically loaded by pytest before running tests.
"""

import os
import pytest

def pytest_configure(config):
    """
    Called after command line options have been parsed.
    Sets the API version based on environment variable.
    """
    # Check for API version from environment variable set by test_autotune.sh
    # Default to 'false' (old/legacy APIs) for backward compatibility
    use_new_api_env = os.getenv('USE_NEW_RECOMMENDATION_API', 'false')

    if use_new_api_env.lower() == 'true':
        pytest.USE_NEW_API = True
        print("\n[pytest] Configured to use NEW API (v1): /kruize/api/v1/recommendations")
    else:
        pytest.USE_NEW_API = False
        print("\n[pytest] Configured to use OLD/LEGACY APIs: /updateRecommendations, /generateRecommendations, /listRecommendations")

def pytest_addoption(parser):
    parser.addoption(
        '--cluster_type', action='store', default='minikube', help='Cluster type'
    )
