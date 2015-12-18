/* Clean */

drop table if exists person_person cascade;
drop table if exists location cascade;
drop table if exists device cascade;
drop table if exists person cascade;

drop sequence if exists person_id_seq;
drop sequence if exists process_id_seq;

/* SET datestyle = "ISO, MDY"; */

/* Sequences */

create sequence person_id_seq start with 1;
create sequence location_id_seq start with 1;

/* Tables */

create table person (
	person_id	integer 	    not null    default nextval('person_id_seq'),
	email		varchar(64)     not null,
	name		varchar(50) 	not null,
	plus_id		varchar(200)	not null,
	created_on	timestamp 		not null	default current_timestamp,
	constraint	person_pk		primary key (person_id),
	constraint	person_u		unique (email) 
);

create table person_person (
	person_id	integer		    not null,
	friend_id	integer		    not null,
	accepted	boolean		    not null,
	since		timestamp   	not null	default current_timestamp,
	constraint  person_person_pk       primary key (person_id, friend_id),
	constraint  person_person_fk_p     foreign key (person_id) references person(person_id) on update cascade on delete cascade,
	constraint  person_person_fk_f     foreign key (friend_id) references person(person_id) on update cascade on delete cascade
);

create table device (
	person_id	integer 		not null,
    gcm_reg_id  varchar(300)	not null,
	constraint device_fk_p      foreign key (person_id) references person(person_id) on update cascade on delete cascade
);

create table location (
	location_id	integer			not null	default nextval('location_id_seq'),
	person_id 	integer			not null,
	latitude	real			not null,
	longitude	real			not null,
    altitude	real,
	accuracy	real			not null,
	address     varchar(100),
	updated_on	timestamp with time zone not null,
	constraint	location_pk		primary key (location_id),
	constraint	location_fk_p	foreign key (person_id) references person(person_id) on update cascade on delete cascade 
);

alter sequence person_id_seq owned by person.person_id;
alter sequence location_id_seq owned by location.location_id;

create or replace view v_person as select person_id, name, email, plus_id, created_on from person;
create or replace view v_person_person as select person_id, friend_id, accepted, since from person_person;
create or replace view v_device as select person_id, gcm_reg_id from device;
create or replace view v_location as select location_id, person_id, latitude, longitude, altitude, accuracy, address, updated_on from location;

create or replace view v_location_last as select l.person_id, l.latitude, l.longitude, l.altitude, l.accuracy, l.address, l.updated_on from location l inner join (select person_id, max(updated_on) as maxdate from location group by person_id) ll on l.person_id = ll.person_id and l.updated_on = ll.maxdate;

COMMENT ON TABLE person IS 'Personen (Nutzer)';
COMMENT ON TABLE person_person IS 'wer ist mit wem befreudet';
COMMENT ON TABLE device IS 'Google gcm ids';
COMMENT ON TABLE location IS 'Standortverlauf';
