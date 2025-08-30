DELIMITER $$

CREATE PROCEDURE sp_user_report()
BEGIN
SELECT
    u.id AS user_id,
    u.username,
    u.email,
    u.department,
    u.status,
    u.last_login
FROM users u
ORDER BY u.username;
END$$

DELIMITER ;