create table IF NOT EXISTS kruize_pod_status (pod_name varchar(255) not null, last_checked timestamp(6), reason varchar(255), status varchar(255), updated_at timestamp(6), primary key (pod_name))
