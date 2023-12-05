# Kubernetes Environment Variables

The following environment variables are set using the `kubectl apply` command with the provided YAML configuration.

## Logging Configuration

- **LOGGING_LEVEL**
    - Description: Sets the logging level for application logs.
    - Value: "info"

- **ROOT_LOGGING_LEVEL**
    - Description: Sets the root logging level for the application.
    - Value: "error"

## Database Configuration

- **dbdriver**
    - Description: Database driver configuration for JDBC.
    - Value: "jdbc:postgresql://"

## Cluster Configuration

- **clustertype**
    - Description: Defines the cluster type.
    - Value: "kubernetes"

- **k8stype**
    - Description: Kubernetes type configuration.
    - Value: "openshift"

- **authtype**
    - Description: Authentication type for the cluster.
    - Value: "openshift"

## Monitoring Configuration

- **monitoringagent**
    - Description: Specifies the monitoring agent used.
    - Value: "prometheus"

- **monitoringservice**
    - Description: Monitoring service configuration.
    - Value: "prometheus-k8s"

- **monitoringendpoint**
    - Description: Monitoring endpoint configuration.
    - Value: "prometheus-k8s"

## Database Interaction Configuration

- **savetodb**
    - Description: Indicates whether to save to the database.
    - Value: "true"

## Hibernate Configuration

- **hibernate_dialect**
    - Description: Hibernate dialect configuration.
    - Value: "org.hibernate.dialect.PostgreSQLDialect"

- **hibernate_driver**
    - Description: Hibernate database driver configuration.
    - Value: "org.postgresql.Driver"

- **hibernate_c3p0minsize**
    - Description: Minimum size for the C3P0 connection pool.
    - Value: "5"

- **hibernate_c3p0maxsize**
    - Description: Maximum size for the C3P0 connection pool.
    - Value: "10"


- **hibernate_c3p0timeout**
    - Description: Timeout configuration for the C3P0 connection pool.
    - Value: "300"

- **hibernate_c3p0maxstatements**
    - Description: Maximum statements configuration for the C3P0 connection pool.
    - Value: "100"

- **hibernate_hbm2ddlauto**
    - Description: Hibernate DDL auto configuration.
    - Value: "none"

- **hibernate_showsql**
    - Description: Enable or disable showing SQL statements in the logs.
    - Value: "false"

- **hibernate_timezone**
    - Description: Timezone configuration for Hibernate.
    - Value: "UTC"

## Other Configuration

- **deletepartitionsthreshold**
    - Description: Threshold for deleting partitions.
    - Value: "16"
    - Details: The value represents the number of days, indicating the duration for which partitions belonging to Kruize
      that are older than the specified number of days from today's date will be deleted. For example, if the value is
      set to "16," Kruize will automatically delete partitions older than 16 days, helping manage and optimize storage
      resources.