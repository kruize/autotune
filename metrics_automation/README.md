## Metrics Automation

Monitoring the production metrics associated with the ros-ocp-backend and Kruize services is essential and requires manual effort from developers to fetch the different metrics by running different prometheus queries with respective authentication tokens and updating them in the slack channel every 24hrs.

To eliminate this manual effort involved in getting the daily metrics, we automated fetching and publishing of all the required production metrics to [#metrics_bot channel](https://kruizeworkspace.slack.com/archives/C06HSEBV5T3) in Kruize slack.

You need to get the slack bot token from the bot admin and set it as an environment variable by running `export SLACK_BOT_TOKEN='<token value>'`

Now by running the kruize_metrics_automated.py script with required parameters all the metrics details get sent to the slack via an automation bot.

```
usage: kruize_metrics_automated.py 
            [ -c ] : Cluster type. Supported types:openshift/minikube
            [ -t ] : Time duration in minutes
            [ -tt ] : Open shift authentication token
			[ -gt ] : Gabi authentication token
			[ -p1 ] : Production prometheus url for Kruize metrics 
			[ -p2 ] : Production prometheus url for AWS metrics 
            [ -g ] : Gabi url 
			
```

For example,

```
$ python3 kruize_metrics_automated.py -c openshift -p1 <specify prometheus url 1> -t 24h -p2 <specify prometheus url 2> -tt <specify openshift token> -gt <specify gabi token> -g <specify gabi url>

```
