/* Clean */

set names utf8;
set character set utf8;

drop table if exists friend;
drop table if exists process;
drop table if exists phone;
drop table if exists user;

/* Tabellen */

create table user (
	user_id		integer 		not null	auto_increment,
	name		varchar(50) 	not null,
	email		varchar(64) 	not null,
--	api_key		varchar(32)		not null,
	plus_id		varchar(200)	not null,
	created_on	timestamp 		not null	default current_timestamp,
	constraint	user_pk			primary key (user_id),
	constraint	user_u			unique (email) 
) auto_increment=1, comment='nutzer';

create table phone (
	user_id		integer 		not null,
	gcm_regid	varchar(300)	not null,
	constraint	phone_pk		primary key (user_id),
	constraint	phone_fk_u		foreign key (user_id) references user (user_id) on update cascade on delete cascade
) comment='google gcm ids';

create table friend (
	user_id		integer			not null,
	friend_id	integer			not null,
	accepted	char(1),
	constraint	friend_pk		primary key (user_id, friend_id),
	constraint	friend_fk_u		foreign key (user_id) references user(user_id) on update cascade on delete cascade,
	constraint	friend_fk_f		foreign key (friend_id) references user(user_id) on update cascade on delete cascade
) comment='freunde';

create table process (
	_id			integer			not null	auto_increment,
	user_id 	integer			not null,
	lat			double			not null,
	lon			double			not null,
	acc			double			not null,
	address     varchar(100),
	updated_on	timestamp		not null	default '0000-00-00 00:00:00',
	constraint	process_pk		primary key (_id),
	constraint	process_fk_u	foreign key (user_id) references user(user_id) on update cascade on delete cascade 
) auto_increment=1, comment='locationverlauf';

/* Views 
 * http://stackoverflow.com/questions/15310219/mysql-get-record-with-lowest-value-views-select-contains-a-subquery-in-the-fr
 */

create or replace view v_user as select user_id, name, email, plus_id, created_on from user;
create or replace view v_phone as select user_id, gcm_regid from phone;
create or replace view v_friend as select user_id, friend_id, accepted from friend;
create or replace view v_process as select _id, user_id, lat, lon, acc, address, updated_on from process;

create or replace view v_process_max as select user_id, max(updated_on) as maxdate from process group by user_id; -- WorkAround
create or replace view v_process_last as select p.user_id, p.lat, p.lon, p.acc, p.address, p.updated_on from process p inner join v_process_max pp where p.user_id = pp.user_id and p.updated_on = pp.maxdate;
