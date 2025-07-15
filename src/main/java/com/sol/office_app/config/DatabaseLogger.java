package com.sol.office_app.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Component
public class DatabaseLogger {
//    @Autowired
//    private DataSource dataSource;
//
//    @PostConstruct
//    public void logDatabaseDetails() throws SQLException {
//        try (Connection conn = dataSource.getConnection()) {
//            DatabaseMetaData metaData = conn.getMetaData();
//            System.out.println("🚀 JDBC URL: " + metaData.getURL());
//            System.out.println("🧩 Driver Name: " + metaData.getDriverName());
//            System.out.println("📦 DB Version: " + metaData.getDatabaseProductVersion());
//            System.out.println("🔁 AutoCommit: " + conn.getAutoCommit());
//            System.out.println("🔐 Isolation Level: " + conn.getTransactionIsolation());
//        }
//    }
}
