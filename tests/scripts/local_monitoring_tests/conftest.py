def pytest_addoption(parser):
    parser.addoption(
        '--cluster_type', action='store', default='minikube', help='Cluster type'
    )

