package com.biblioteca.functions.prestamos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String jdbcUrl = getEnv("ORACLE_JDBC_URL",
                "jdbc:oracle:thin:@//" + getEnv("ORACLE_DB_HOST", "localhost")
                        + ":" + getEnv("ORACLE_DB_PORT", "1521")
                        + "/" + getEnv("ORACLE_DB_SERVICE", "FREEPDB1"));
        String username = getEnv("ORACLE_APP_USER", getEnv("ORACLE_USERNAME", "biblioteca"));
        String password = getEnv("ORACLE_APP_PASSWORD", getEnv("ORACLE_PASSWORD", "biblioteca123"));
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
