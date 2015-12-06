-- Execute as mysql root
create database omakase character set utf8;
grant usage on *.* to 'omakase'@'%' identified by 'omakase';
grant all privileges on omakase.* to 'omakase'@'%';