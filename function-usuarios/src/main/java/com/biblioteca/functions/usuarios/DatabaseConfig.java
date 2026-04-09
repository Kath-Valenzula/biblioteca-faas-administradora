package com.biblioteca.functions.usuarios;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final String[] WALLET_FILES = {
            "cwallet.sso", "ewallet.p12", "ewallet.pem",
            "keystore.jks", "ojdbc.properties", "sqlnet.ora",
            "tnsnames.ora", "truststore.jks"
    };

    private static volatile Path walletDir;

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String tnsAdmin = getEnv("TNS_ADMIN", null);
        if (tnsAdmin == null || tnsAdmin.isBlank()) {
            tnsAdmin = extractWalletToTemp();
        }

        System.setProperty("oracle.net.tns_admin", tnsAdmin);
        System.setProperty("TNS_ADMIN", tnsAdmin);

        String defaultUrl = "jdbc:oracle:thin:@bibliotecadb_tp?TNS_ADMIN=" + tnsAdmin;
        String jdbcUrl = getEnv("ORACLE_JDBC_URL", defaultUrl);
        // Si la URL viene de env var sin TNS_ADMIN, agregarlo
        if (!jdbcUrl.contains("TNS_ADMIN")) {
            jdbcUrl = jdbcUrl + "?TNS_ADMIN=" + tnsAdmin;
        }
        String username = getEnv("ORACLE_APP_USER", getEnv("ORACLE_USERNAME", "ADMIN"));
        String password = getEnv("ORACLE_APP_PASSWORD", getEnv("ORACLE_PASSWORD", ""));
        if (password.isBlank()) {
            throw new SQLException("ORACLE_APP_PASSWORD no esta configurada");
        }

        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    private static String extractWalletToTemp() {
        if (walletDir != null && Files.exists(walletDir)) {
            return walletDir.toString();
        }
        synchronized (DatabaseConfig.class) {
            if (walletDir != null && Files.exists(walletDir)) {
                return walletDir.toString();
            }
            try {
                Path tempDir = Files.createTempDirectory("oracle-wallet");
                for (String file : WALLET_FILES) {
                    try (InputStream is = DatabaseConfig.class.getClassLoader()
                            .getResourceAsStream("wallet/" + file)) {
                        if (is != null) {
                            Files.copy(is, tempDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                walletDir = tempDir;
                return tempDir.toString();
            } catch (IOException ex) {
                throw new RuntimeException("No fue posible extraer el wallet de Oracle", ex);
            }
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
