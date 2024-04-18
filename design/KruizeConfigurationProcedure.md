### **Kruize Configuration Procedure**

The purpose of this document is to provide instructions on how to set up the Kruize configuration needed for deployment.
It's important to note that this document only covers the procedure itself and does not go into detail about the
significance of each environment variable. However, there will be separate documents created to address this.

### **Configure using Kruize Config Object**

To configure Kruize, one option is to use the Kubernetes Config object through a YAML file. The YAML file must contain
the following information:

```
apiVersion: v1
kind: ConfigMap
metadata:
  name: kruizeconfig
  namespace: monitoring
data:
  dbconfigjson: |
    {
      "database": {
        "adminPassword": "admin",
        "adminUsername": "admin",
        "hostname": "postgres-service",
        "name": "kruizeDB",
        "password": "admin",
        "port": 5432,
        "sslMode": "require",
        "username": "admin"
      }
    }
  kruizeconfigjson: |
    {
      "clustertype":"kubernetes",
      "k8stype" : "minikube",
      "authtype": "",
      "monitoringagent":"prometheus",
      "monitoringservice":"prometheus-k8s",
      "monitoringendpoint":"prometheus-k8s",
      "savetodb":"true",
      "dbdriver":"jdbc:postgresql://",
      "hibernate": {
        "dialect": "org.hibernate.dialect.PostgreSQLDialect",
        "driver": "org.postgresql.Driver",
        "c3p0minsize": 2,
        "c3p0maxsize": 5,
        "c3p0timeout": 300,
        "c3p0maxstatements": 50,
        "hbm2ddlauto": "update",
        "showsql": "true"
      },
      "cloudwatch": {
        "accessKeyId": "",
        "secretAccessKey": "",
        "region": "",
        "logGroup": "kruize-logs",
        "logStream": "kruize-stream",
        "logLevel": "INFO"
      }
    }
```

As shown in the previous example, two JSON configurations are created:

* dbconfigjson: Configuration related to the database
* kruizeconfigjson: Configuration related to Kruize

These Kubernetes configurations are volume-mounted with the Kruize deployment.

```
env:
  - name: LOGGING_LEVEL
    value: "info"
  - name: ROOT_LOGGING_LEVEL
    value: "error"
  - name: DB_CONFIG_FILE
    value: "/etc/config/dbconfigjson"
  - name: KRUIZE_CONFIG_FILE
    value: "/etc/config/kruizeconfigjson"
```

### **Configure using Environment Variables**

Another way to configure Kruize is through environment variables. The naming convention for these variables must follow
a pattern similar to the JSON configurations.

For instance, if you want to configure the database using environment variables, it should be structured as follows :

```
env:
- name: LOGGING_LEVEL 
  value: "info"
- name: ROOT_LOGGING_LEVEL 
  value: "error"
- name: DB_CONFIG_FILE 
  value: "/etc/config/dbconfigjson"
- name: KRUIZE_CONFIG_FILE 
  value: "/etc/config/kruizeconfigjson"
- name: database_username 
  value: newadmin
- name: database_hostname 
  value: localhost
- name: hibernate_showsql 
  value: false
```

It's important to note that the environment variables can also be used to override the Kruize Config Object settings if
the relevant variables are present in the environment.


