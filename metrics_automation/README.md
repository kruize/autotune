## Metrics Automation

Monitoring the production metrics associated with the ros-ocp-backend and Kruize services is essential and requires manual effort from developers to fetch the different metrics by running different prometheus queries with respective authentication tokens and updating them in the slack channel every 24hrs.

To eliminate this manual effort involved in getting the daily metrics, we automated fetching and publishing of all the required production metrics to [#metrics_bot channel](https://kruizeworkspace.slack.com/archives/C06HSEBV5T3) in Kruize slack.

Pre-requisites: enable the python environment via  `source automate_env/bin/activate`

Now by running the kruize_metrics_automated.py script with required parameters all the metrics details get sent to the slack via an automation bot.

Get OC  tokoen from here : https://console-openshift-console.apps.crcp01ue1.o9m8.p1.openshiftapps.com/

```
usage: kruize_metrics_automated.py 
            [ -c ] : Cluster type. Supported types:openshift/minikube
            [ -t ] : Time duration in minutes
            [ -tt ] : Open shift authentication token
			[ -gt ] : Gabi authentication token
			[ -p1 ] : Production prometheus url for Kruize metrics "https://prometheus.crcp01ue1.devshift.net/" 
			[ -p2 ] : Production prometheus url for AWS metrics "https://prometheus.app-sre-prod-01.devshift.net"
			
```

For example,

```
$ python3 kruize_metrics_automated.py -c openshift -p1 https://prometheus.crcp01ue1.devshift.net/api/v1/query -t 24h -p2 https://prometheus.app-sre-prod-01.devshift.net -tt sha256~wRPDZZ-LXs8XYhuKXl7nxmAqVKJdSM1qcqzPd3lNlck -gt sha256~mOCNA4X8ZYiWTjtlAY5CprgPawY-ZnOE5YdRxUoc_78

```

currently we do not provide aws free storage space value as we are facing an auth issue