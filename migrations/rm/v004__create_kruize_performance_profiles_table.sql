create table IF NOT EXISTS kruize_performance_profiles (name varchar(255) not null, k8s_type varchar(255), profile_version float(53) not null, slo jsonb, primary key (name));
