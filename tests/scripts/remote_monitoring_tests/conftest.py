import sys
from pathlib import Path

# Add the scripts directory to Python path so helpers can be imported
scripts_dir = Path(__file__).resolve().parent.parent
if str(scripts_dir) not in sys.path:
    sys.path.insert(0, str(scripts_dir))

def pytest_addoption(parser):
    parser.addoption(
        '--cluster_type', action='store', default='minikube', help='Cluster type'
    )

