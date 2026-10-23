-- Run this ONCE in MySQL (database: hospitality_db) if these columns are missing.
-- If a line says "Duplicate column name", that column already exists - just skip it.
USE hospitality_db;

ALTER TABLE employees ADD COLUMN username VARCHAR(50);
ALTER TABLE employees ADD COLUMN password VARCHAR(100);
ALTER TABLE employees ADD COLUMN profile_image VARCHAR(500);

-- Every employee who must log in needs a username and password, e.g.:
-- UPDATE employees SET username = 'emp001', password = '1234' WHERE employee_id = 'EMP001';
