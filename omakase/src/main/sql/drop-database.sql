-- Execute as mysql root
revoke all privileges on omakase.* from 'omakase'@'%';
drop user 'omakase'@'%';
drop database if exists omakase;

