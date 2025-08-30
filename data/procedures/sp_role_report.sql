DELIMITER $$

CREATE PROCEDURE sp_role_report()
BEGIN
    SELECT
        r.id AS role_id,
        r.name,
        r.description,
        COUNT(ur.user_id) AS total_users
    FROM role r
             LEFT JOIN users_roles ur ON r.id = ur.role_id
    GROUP BY r.id, r.name, r.description
    ORDER BY r.name;
END$$

DELIMITER ;