-- One schema per service (database-per-service). Each service owns and migrates only its schema.
CREATE DATABASE IF NOT EXISTS office_identity     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS office_report       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS office_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- In production give each service its own user limited to its own schema.
GRANT ALL PRIVILEGES ON office_identity.*     TO 'office'@'%';
GRANT ALL PRIVILEGES ON office_report.*       TO 'office'@'%';
GRANT ALL PRIVILEGES ON office_notification.* TO 'office'@'%';
FLUSH PRIVILEGES;
