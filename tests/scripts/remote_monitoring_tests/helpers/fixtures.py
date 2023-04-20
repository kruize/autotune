import pytest

@pytest.fixture
def cluster_type(request):
    return request.config.getoption('--cluster_type')
