import subprocess


def perform_experiment(config):
    output = subprocess.run(["bash", "scripts/applyconfig.sh", str(config["cpu_request"]), str(config["memory_request"])], stdout=subprocess.PIPE).stdout.decode('utf-8')

    if output == '':
        return "Nan", 0
    else:
        # print(output)
        rows = output.split("\n")
        # print(rows)
        data = rows[1]
        # print(data)
        sla = data.split(" , ")[2]
        return sla, 1 

