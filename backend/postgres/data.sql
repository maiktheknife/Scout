/* Clean */
truncate person cascade;

/* Insert */
insert into person(email, name, plus_id) values ('user1', 'User1', 'plusId1');
insert into person(email, name, plus_id) values ('user2', 'User2', 'plusId2');
insert into person(email, name, plus_id) values ('user3', 'User3', 'plusId3');

insert into person_person(person_id, friend_id, accepted) values ((select person_id from person where email = 'user1'), (select person_id from person where email = 'user2'), true);
insert into person_person(person_id, friend_id, accepted) values ((select person_id from person where email = 'user1'), (select person_id from person where email = 'user3'), false);
insert into person_person(person_id, friend_id, accepted) values ((select person_id from person where email = 'user2'), (select person_id from person where email = 'user1'), true);

insert into location(person_id, latitude, longitude, altitude, accuracy, address, updated_on) values ((select person_id from person where email = 'user1'), 10,20,31.58864,10.001, 'address', current_timestamp);
insert into location(person_id, latitude, longitude, altitude, accuracy, address, updated_on) values ((select person_id from person where email = 'user2'), 15,10.48,11.58864,10.001, null, current_timestamp);

insert into person(email, name, plus_id) values ('user1', 'User1', 'plusId1');
insert into person(email, name, plus_id) values ('user2', 'User2', 'plusId2');

insert into person_person(person_id, friend_id, accepted) values ((select person_id from person where email = 'user1'), 51, false);
insert into person_person(person_id, friend_id, accepted) values ((select person_id from person where email = 'user2'), 51, false);
