CREATE TABLE telemetry (id INT PRIMARY KEY, sensor_name TEXT, sensor_value INT, sensor_ts TIMESTAMP);


INSERT INTO telemetry (id, sensor_name, sensor_value, sensor_ts) VALUES (1, 'temperature', 21, current_timestamp(0));
INSERT INTO telemetry (id, sensor_name, sensor_value, sensor_ts) VALUES (2, 'temperature', 22, current_timestamp(0));
INSERT INTO telemetry (id, sensor_name, sensor_value, sensor_ts) VALUES (3, 'pressure', 31, current_timestamp(0));
INSERT INTO telemetry (id, sensor_name, sensor_value, sensor_ts) VALUES (4, 'pressure', 355, current_timestamp(0));

UPDATE telemetry SET sensor_value = 999 where id = 4;
DELETE from telemetry where id = 4;