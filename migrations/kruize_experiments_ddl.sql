create table IF NOT EXISTS kruize_experiments (experiment_id varchar(255) not null, cluster_name varchar(255), datasource jsonb, experiment_name varchar(255), extended_data jsonb, meta_data jsonb, mode varchar(255), performance_profile varchar(255), status varchar(255), target_cluster varchar(255), version varchar(255), primary key (experiment_id));
create table IF NOT EXISTS kruize_performance_profiles (name varchar(255) not null, k8s_type varchar(255), profile_version float(53) not null, slo jsonb, primary key (name));
create table IF NOT EXISTS kruize_recommendations (interval_end_time timestamp(6) not null, experiment_name varchar(255) not null, cluster_name varchar(255), extended_data jsonb, experiment_type varchar(255) null default null, version varchar(255), primary key (experiment_name, interval_end_time)) PARTITION BY RANGE (interval_end_time);
create table IF NOT EXISTS kruize_results (interval_start_time timestamp(6) not null, interval_end_time timestamp(6) not null, experiment_name varchar(255) not null, cluster_name varchar(255) , duration_minutes float(53) not null, extended_data jsonb, meta_data jsonb, version varchar(255), primary key (experiment_name, interval_end_time, interval_start_time)) PARTITION BY RANGE (interval_end_time);
alter table if exists kruize_experiments add constraint UK_experiment_name unique (experiment_name);
create index IF NOT EXISTS idx_recommendation_experiment_name on kruize_recommendations (experiment_name);
create index IF NOT EXISTS idx_recommendation_interval_end_time on kruize_recommendations (interval_end_time);
create index IF NOT EXISTS idx_result_experiment_name on kruize_results (experiment_name);
create index IF NOT EXISTS idx_result_interval_end_time on kruize_results (interval_end_time);
