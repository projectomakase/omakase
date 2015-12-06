-- Drops the database used by the integration tests
-- Execute as mysql root
revoke all privileges on omakase_test.* from 'omakase-test'@'%';
drop user 'omakase-test'@'%';
drop database if exists omakase_test;

