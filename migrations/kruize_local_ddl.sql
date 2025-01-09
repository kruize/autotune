create table IF NOT EXISTS kruize_lm_experiments (experiment_id varchar(255) not null, cluster_name varchar(255), experiment_name varchar(255), extended_data jsonb, meta_data jsonb, mode varchar(255), performance_profile varchar(255), status varchar(255), target_cluster varchar(255), version varchar(255),experiment_type varchar(255),datasource varchar(255),creation_date timestamp(6) ,updated_date timestamp(6) , primary key (experiment_id));
create table IF NOT EXISTS kruize_authentication (id serial, authentication_type varchar(255), credentials jsonb, service_type varchar(255), primary key (id));
create table IF NOT EXISTS kruize_datasources (version varchar(255), name varchar(255), provider varchar(255), serviceName varchar(255), namespace varchar(255), url varchar(255), authentication_id serial, FOREIGN KEY (authentication_id) REFERENCES kruize_authentication(id), primary key (name));
create table IF NOT EXISTS kruize_dsmetadata (id serial, version varchar(255), datasource_name varchar(255), cluster_name varchar(255), namespace varchar(255), workload_type varchar(255), workload_name varchar(255), container_name varchar(255), container_image_name varchar(255), primary key (id));
alter table kruize_lm_experiments  add column metadata_id bigint references kruize_dsmetadata(id);
alter table if exists kruize_lm_experiments add constraint UK_lm_experiment_name unique (experiment_name);
create table IF NOT EXISTS kruize_metric_profiles (api_version varchar(255), kind varchar(255), metadata jsonb, name varchar(255) not null, k8s_type varchar(255), profile_version float(53) not null, slo jsonb, primary key (name));
create table IF NOT EXISTS kruize_lm_recommendations (interval_end_time timestamp(6) not null, experiment_name varchar(255) not null, cluster_name varchar(255), extended_data jsonb, version varchar(255),experiment_type varchar(255), primary key (experiment_name, interval_end_time)) PARTITION BY RANGE (interval_end_time);
create table IF NOT EXISTS kruize_bulkjobs (job_id uuid not null, end_time timestamp(6), start_time timestamp(6), notifications jsonb,experiments jsonb, processed_count integer, status varchar(255), total_count integer, webhook varchar(255), primary key (job_id));




