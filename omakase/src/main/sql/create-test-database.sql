-- Creates the database used by the integration tests
-- Execute as mysql root
create database omakase_test character set utf8;
grant usage on *.* to 'omakase-test'@'%' identified by 'omakase-test';
grant all privileges on omakase_test.* to 'omakase-test'@'%';