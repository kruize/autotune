import subprocess
import sys


def setup_virtual_env():
    subprocess.check_call([sys.executable, "-m", "venv", "env"])
    # subprocess.check_call(["source", "env/bin/activate"])


setup_virtual_env()

python_bin = "./env/bin/python"
script_file = "./hpo.py"

subprocess.Popen([python_bin, script_file])

