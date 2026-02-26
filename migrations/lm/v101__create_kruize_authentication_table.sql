create table IF NOT EXISTS kruize_authentication (id serial, authentication_type varchar(255), credentials jsonb, service_type varchar(255), primary key (id));
