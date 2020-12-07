"""
Copyright (c) 2020, 2020 Red Hat, IBM Corporation and others.

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

import subprocess
import sys


def setup_virtual_env():
    """Set up a virtual environment."""
    subprocess.check_call([sys.executable, "-m", "venv", "env"])
    # subprocess.check_call(["source", "env/bin/activate"])


setup_virtual_env()

python_bin = "./env/bin/python"
hpo_script = "./hpo.py"

subprocess.Popen([python_bin, hpo_script])

