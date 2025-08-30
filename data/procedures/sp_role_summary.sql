DELIMITER $$

CREATE PROCEDURE sp_role_summary()
BEGIN
    SELECT
        (SELECT COUNT(*) FROM role) AS roleCount,
        (SELECT r.name
         FROM role r
                  LEFT JOIN users_roles ur ON r.id = ur.role_id
         GROUP BY r.name
         ORDER BY COUNT(ur.user_id) DESC
         LIMIT 1) AS topRole,
        (SELECT r.name
         FROM role r
                  LEFT JOIN users_roles ur ON r.id = ur.role_id
         GROUP BY r.name
         ORDER BY COUNT(ur.user_id) ASC
         LIMIT 1) AS leastRole;
END$$

DELIMITER ;

GRANT EXECUTE ON PROCEDURE oracleflexcube.sp_role_summary TO 'root'@'host';
FLUSH PRIVILEGES;