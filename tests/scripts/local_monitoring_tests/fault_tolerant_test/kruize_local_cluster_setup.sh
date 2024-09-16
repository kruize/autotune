#!/bin/sh

function check_err() {
	err=$?
	if [ ${err} -ne 0 ]; then
		echo "$*"
		exit 1
	fi
}

function clone_benchmarks() {
	repo_name="benchmarks"
	echo
	echo "#######################################"
	echo "Cloning ${repo_name} git repos"
	if [ ! -d ${repo_name} ]; then
		git clone git@github.com:kruize/${repo_name}.git >/dev/null 2>/dev/null
		if [ $? -ne 0 ]; then
			git clone https://github.com/kruize/${repo_name}.git 2>/dev/null
		fi
		check_err "ERROR: git clone of kruize/${repo_name} failed."
	fi

	echo "done"
	echo "#######################################"
	echo


}

function deploy_tfb() {
	namespace=$1
	echo
	echo "#######################################"
	pushd benchmarks >/dev/null
		echo "Installing TechEmpower (Quarkus REST EASY) benchmark into cluster"
		pushd techempower/manifests >/dev/null
			kubectl apply -f default_manifests -n ${namespace}
			check_err "ERROR: TechEmpower app failed to start, exiting"
		popd >/dev/null
	popd >/dev/null
	echo "#######################################"
	echo

}

function get_urls() {
	namespace=$1
	kubectl_cmd="kubectl -n ${namespace}"
	TECHEMPOWER_PORT=$(${kubectl_cmd} get svc tfb-qrh-service --no-headers -o=custom-columns=PORT:.spec.ports[*].nodePort)
	TECHEMPOWER_IP=$(${kubectl_cmd} get pods -l=app=tfb-qrh-deployment -o wide -o=custom-columns=NODE:.spec.nodeName --no-headers)

	if [ ${CLUSTER_TYPE} == "minikube" ]; then
		MINIKUBE_IP=$(minikube ip)
		export TECHEMPOWER_URL="${MINIKUBE_IP}:${TECHEMPOWER_PORT}"
	elif [ ${CLUSTER_TYPE} == "openshift" ]; then
		export TECHEMPOWER_URL="${TECHEMPOWER_IP}:${TECHEMPOWER_PORT}"
	fi
}



function apply_tfb_load() {
	benchmark_load=1
	APP_NAMESPACE=$1
	if [ ${benchmark_load} -eq 0 ]; then
		return;
	fi

	echo
	echo "###################################################################"
	echo " Starting 20 min background load against the techempower benchmark "
	echo "###################################################################"
	echo

	TECHEMPOWER_LOAD_IMAGE="quay.io/kruizehub/tfb_hyperfoil_load:0.25.2"
	# 20 mins = 1200 seconds
	# LOAD_DURATION=1200
	get_urls ${APP_NAMESPACE}
	if [ ${CLUSTER_TYPE} == "minikube" ]; then
		TECHEMPOWER_ROUTE=${TECHEMPOWER_URL}
	elif [ ${CLUSTER_TYPE} == "openshift" ]; then
		#TECHEMPOWER_ROUTE=$(oc get route -n ${APP_NAMESPACE} --template='{{range .items}}{{.spec.host}}{{"\n"}}{{end}}')
		TECHEMPOWER_ROUTE=$(oc status -n ${APP_NAMESPACE} | grep "tfb" | grep port | cut -d " " -f1 | cut -d "/" -f3)
	fi
	echo "TECHEMPOWER_ROUTE = $TECHEMPOWER_ROUTE"
	# docker run -d --rm --network="host"  ${TECHEMPOWER_LOAD_IMAGE} /opt/run_hyperfoil_load.sh ${TECHEMPOWER_ROUTE} <END_POINT> <DURATION> <THREADS> <CONNECTIONS>
	docker run -d --rm --network="host"  ${TECHEMPOWER_LOAD_IMAGE} /opt/run_hyperfoil_load.sh ${TECHEMPOWER_ROUTE} queries?queries=20 1200 1024 8096

}

function delete_benchmarks() {
	namespace=$1
	echo "#######################################"
	pushd benchmarks >/dev/null
		echo "Uninstalling TechEmpower (Quarkus REST EASY) benchmark into cluster"
		pushd techempower >/dev/null
			kubectl delete -f manifests -n ${namespace}
			check_err "ERROR: Uninstalling TechEmpower app failed, exiting"
		popd >/dev/null
	popd >/dev/null
	echo "#######################################"
	echo
}

function delete_repos() {
	echo "Deleting git repos"
	rm -rf benchmarks
}


CLUSTER_TYPE=$1
rm -rf benchmarks
clone_benchmarks

deploy_tfb "default"

kubectl get pods -n default

kubectl get svc -n default


if [ ${CLUSTER_TYPE} == "openshift" ]; then
	oc expose svc/tfb-qrh-service -n default
	oc get route -n default
fi

apply_tfb_load "default"


