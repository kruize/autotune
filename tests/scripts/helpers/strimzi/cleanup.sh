kubectl -n kafka delete $(kubectl get strimzi -o name -n kafka)
kubectl delete pvc -l strimzi.io/name=my-cluster-kafka -n kafka
kubectl -n kafka delete -f 'https://strimzi.io/install/latest?namespace=kafka'
kubectl delete namespace kafka
