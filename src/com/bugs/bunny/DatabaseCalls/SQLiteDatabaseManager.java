package com.bugs.bunny.DatabaseCalls;

import com.bugs.bunny.model.Databases.SQLite.OAuthCredentialsDatabaseSchema;
import com.bugs.bunny.model.Databases.SQLite.OAuthCredentialsDatabaseSchema.OAuthCredentialsTable;
import com.bugs.bunny.model.Databases.SQLite.OAuthCredentialsDatabaseSchema.OAuthCredentialsTableColumns;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

public class SQLiteDatabaseManager {
    private static String sqliteDatabaseName = "OAuthCredentials.db";
    protected Connection sqliteDatabaseConnection = connectToSqliteDatabase(sqliteDatabaseName);

    private static String clientIdKey = System.getenv("GITHUB_CLIENT_ID_ENCRYPTION_KEY");
    private static String clientId = System.getenv("GITHUB_CLIENT_ID");

    private static String clientSecretKey = System.getenv("GITHUB_CLIENT_SECRET_ENCRYPTION_KEY");
    private static String clientSecret = System.getenv("GITHUB_CLIENT_SECRET");

    private static String gitHubOAuthCodeKey = System.getenv("GITHUB_OAUTH_CODE_ENCRYPTION_KEY");
    private static String encryptedGitHubOAuthCode;

    private static String gitHubOAuthAccessTokenKey = System.getenv("GITHUB_OAUTH_ACCESS_TOKEN_ENCRYPTION_KEY");
    private static String encryptedGitHubOAuthAccessToken;

    public static String getSqliteDatabaseName() {
        return sqliteDatabaseName;
    }

    public static String getClientIdKey() {
        return clientIdKey;
    }

    public static String getClientSecretKey() {
        return clientSecretKey;
    }

    public static String getGitHubOAuthCodeKey() {
        return gitHubOAuthCodeKey;
    }

    public static String getGitHubOAuthAccessTokenKey() {
        return gitHubOAuthAccessTokenKey;
    }

    public static String getEncryptedGitHubOAuthCode() {
        return encryptedGitHubOAuthCode;
    }

    public static void setEncryptedGitHubOAuthCode(String encryptedGitHubOAuthCode) {
        SQLiteDatabaseManager.encryptedGitHubOAuthCode = encryptedGitHubOAuthCode;
    }

    public static String getEncryptedGitHubOAuthAccessToken() {
        return encryptedGitHubOAuthAccessToken;
    }

    public static void setEncryptedGitHubOAuthAccessToken(String encryptedGitHubOAuthAccessToken) {
        SQLiteDatabaseManager.encryptedGitHubOAuthAccessToken = encryptedGitHubOAuthAccessToken;
    }

    private Connection connectToSqliteDatabase(String databaseName) {
        try {
            Class.forName("org.sqlite.JDBC");
            sqliteDatabaseConnection = DriverManager.getConnection(
                    "jdbc:sqlite:./src/com/bugs/bunny/model/Databases/SQLite/"
                    + databaseName
            );
        } catch (ClassNotFoundException | SQLException sqlex) {
            System.out.println(sqlex.getMessage());
        }

        return sqliteDatabaseConnection;
    }

    protected static void createSqliteDatabaseTable(Connection connection) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "select name from sqlite_master where type='table' and name='"
                             + OAuthCredentialsTable.NAME +
                             "';"
             )) {

            boolean doesTheTableExists = resultSet.next();

            if (!doesTheTableExists) {
                statement.executeUpdate("create table " +
                        OAuthCredentialsTable.NAME +
                        "(" +
                        OAuthCredentialsTableColumns.CLIENT_ID +
                        "," +
                        OAuthCredentialsTableColumns.CLIENT_SECRET +
                        "," +
                        OAuthCredentialsTableColumns.CODE +
                        "," +
                        OAuthCredentialsTableColumns.ACCESS_TOKEN +
                        ");"
                );
            }
        } catch (SQLException sqlex) {
            System.out.println(sqlex.getMessage());
        }
    }

    protected static void populateSqliteDatabaseTable(String encryptedText,
                                                      Boolean isToken,
                                                      Connection connection,
                                                      String[] columnsToBeUpdated) {
        if (isToken) {
            setEncryptedGitHubOAuthAccessToken(encryptedText);
        } else {
            setEncryptedGitHubOAuthCode(encryptedText);
        }
        populateSqliteDatabaseTable(connection, columnsToBeUpdated);
    }

    protected static void populateSqliteDatabaseTable(Connection connection, String[] columnsToUpdate) {
        String columns = commaSeparatedList(columnsToUpdate);

        try {
            PreparedStatement preparedStatement = toggleQuery(connection, columnsToUpdate.length, columns);

            if (columnsToUpdate.length == 1) {
                if (columnsToUpdate[0].equals("encrypted_code")) {
                    preparedStatement.setString(1,
                            getEncryptedGitHubOAuthCode());
                } else {
                    preparedStatement.setString(1,
                            getEncryptedGitHubOAuthAccessToken());
                }
            } else {
                preparedStatement.setString(1, encrypt(clientId, clientIdKey));
                preparedStatement.setString(2, encrypt(clientSecret, clientSecretKey));
            }

            preparedStatement.addBatch();

            connection.setAutoCommit(false);

            preparedStatement.executeBatch();

            connection.setAutoCommit(true);
        } catch (SQLException sqlex) {
            System.out.println(sqlex.getMessage());
        }
    }

    private static PreparedStatement toggleQuery(Connection connection, int numberOfcolumns, String columns)
            throws SQLException {
        StringBuilder query = new StringBuilder();

        if (numberOfcolumns == 1) {
            query.append("insert into ");
            query.append(OAuthCredentialsTable.NAME);
            query.append("(");
            query.append(columns);
            query.append(") values (?);");
            return connection.prepareStatement(query.toString());
        } else {
            query.append("insert into ");
            query.append(OAuthCredentialsTable.NAME);
            query.append("(");
            query.append(columns);
            query.append(") values (?, ?);");

            return connection.prepareStatement(query.toString());
        }
    }

    private static String commaSeparatedList(String[] list)  {
        StringBuilder result = new StringBuilder();

        for (String item : list) {
            result.append(item);
            result.append(",");
        }

        String resultString = result.toString();

        return result.toString().substring(0, resultString.length() - 1);
    }

    protected static String encrypt(String text, String key) {
        String encryptedText = "";

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(), "Blowfish"
            );
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(text.getBytes());
            encryptedText = new String(
                    Base64.getEncoder().encodeToString(encrypted)
            );
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                IllegalBlockSizeException | BadPaddingException bpex) {
            System.out.println(bpex.getMessage());
        }

        return encryptedText;
    }

    protected static String decrypt(String encryptedText, String key) {
        String decryptedText = "";

        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    key.getBytes(),
                    "Blowfish"
            );
            Cipher cipher = Cipher.getInstance("Blowfish");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] decrypted = cipher.doFinal(
                    Base64.getDecoder().decode(encryptedText)
            );
            decryptedText = new String(decrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                InvalidKeyException | IllegalBlockSizeException | BadPaddingException bpex) {
            System.out.println(bpex.getMessage());
        }

        return decryptedText;
    }
}
