import csv
import subprocess
import sys

def perform_experiment(config):
    output = subprocess.run(["bash", "scripts/applyconfig.sh", str(config["cpu_request"]), str(config["memory_request"])], stdout=subprocess.PIPE).stdout.decode('utf-8')

    orig_stdout = sys.stdout
    f = open('total-output.txt', 'a')
    sys.stdout = f

    if output == '':
        sys.stdout = orig_stdout
        f.close()
        return "Nan", 0
    else:
        print(output)
        sys.stdout = orig_stdout
        f.close()
        orig_stdout = sys.stdout
        f = open('output.txt', 'a')
        sys.stdout = f
        rows = output.split("\n")
        # print(rows)
        data = rows[1]
        print(data)
        sla = data.split(" , ")[2]
        with open("data.csv", "r+") as file:
            reader = csv.reader(file)
            csv_data = list(reader)
            row_count = len(csv_data)
            writer = csv.writer(file)
            if row_count == 0:
                column = [c.strip() for c in rows[0].split(',')]
                writer.writerow(column)
                column = [c.strip() for c in rows[1].split(',')]
                writer.writerow(column)
                # for row in rows:
                    # if len(row) != 0:
                        # column = [c.strip() for c in row.split(',')]
                        # writer.writerow(column)
            else:
                column = [c.strip() for c in rows[1].split(',')]
                writer.writerow(column)
        sys.stdout = orig_stdout
        f.close()
        return sla, 1 

