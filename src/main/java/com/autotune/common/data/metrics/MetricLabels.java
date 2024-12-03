package com.autotune.common.data.metrics;

/**
 *
 * This class stores labels such as container, pod, namespace, owner and workload kind/name from the output of a query.
 *
 * Example query output for imageOwners metric
 * {
 *   "metric": {
 *     "container": "openshift-apiserver",
 *     "pod": "apiserver-5bfcb9858b-57pqc",
 *     "namespace": "openshift-apiserver",
 *     "owner_kind": "ReplicaSet",
 *     "owner_name": "apiserver-5bfcb9858b"
 *   },
 *   "values": [
 *     [
 *       1730691554,
 *       "1"
 *     ]
 *   ]
 * }
 */
public class MetricLabels {
    private String name;
    private String container;
    private String namespace;
    private String pod;
    private String owner_kind;
    private String owner_name;
    private String workload_kind;
    private String workload_name;

    // Constructor with a flag to distinguish between owner_kind/owner_name and workload_kind/workload_name
    public MetricLabels(String name, String container, String namespace, String ownerOrWorkloadKind,
                        String ownerOrWorkloadName, String pod, boolean isOwner) {
        this.name = name;
        this.container = container;
        this.namespace = namespace;
        this.pod = pod;

        if (isOwner) {
            // field3 and field4 are owner_kind and owner_name
            this.owner_kind = ownerOrWorkloadKind;
            this.owner_name = ownerOrWorkloadName;
        } else {
            // field3 and field4 are value1 and value2
            this.workload_kind = ownerOrWorkloadKind;
            this.workload_name = ownerOrWorkloadName;
        }
    }

    public MetricLabels(String name, String container, String namespace, String pod) {
        this.name = name;
        this.container = container;
        this.namespace = namespace;
        this.pod = pod;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container){
        this.container = container;
    }


    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getPod() {
        return pod;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public String getOwner_kind() {
        return owner_kind;
    }

    public void setOwner_kind(String owner_kind) {
        this.owner_kind = owner_kind;
    }

    public String getOwner_name() {
        return owner_name;
    }

    public void setOwner_name(String owner_name) {
        this.owner_name = owner_name;
    }

    public String getWorkload_kind() {
        return workload_kind;
    }

    public void setWorkload_kind(String workload_kind) {
        this.workload_kind = workload_kind;
    }

    public String getWorkload_name() {
        return workload_name;
    }

    public void setWorkload_name(String workload_name) {
        this.workload_name = workload_name;
    }

    @Override
    public String toString() {
        return "MetricLabels{" +
                "container='" + container + '\'' +
                ", namespace='" + namespace + '\'' +
                ", pod='" + pod + '\'' +
                ", owner_kind='" + owner_kind + '\'' +
                ", owner_name='" + owner_name + '\'' +
                ", workload_kind='" + workload_kind + '\'' +
                ", workload_name='" + workload_name + '\'' +
                '}';
    }
}