package com.bugs.bunny.DatabaseCalls;

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

    public String getSqliteDatabaseName() {
        return sqliteDatabaseName;
    }

    public static String getClientIdKey() {
        return clientIdKey;
    }

    public static String getClientSecretKey() {
        return clientSecretKey;
    }

    private Connection connectToSqliteDatabase(String databaseName) {
        try {
            Class.forName("org.sqlite.JDBC");
            sqliteDatabaseConnection = DriverManager.getConnection(
                    "jdbc:sqlite:./src/com/bugs/bunny/model/Databases/SQLite/"
                    + databaseName
            );
        } catch (ClassNotFoundException cnfex) {
            cnfex.printStackTrace();
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        }

        return sqliteDatabaseConnection;
    }

    protected static void createSqliteDatabaseTable(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            statement.executeUpdate("drop table if exists " +
                    OAuthCredentialsTable.NAME +
                    ";");
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
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
        }
    }

    protected static void populateSqliteDatabaseTable(
            Connection connection,
            String[] columnsToUpdate                                          ) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "insert into " + OAuthCredentialsTable.NAME +
                            "(" +
                            commaSeparatedList(columnsToUpdate) +
                            ")" +
                            " values (?, ?);"
            );
            preparedStatement.setString(
                    1,
                    SQLiteDatabaseManager.encrypt(clientId, clientIdKey)
            );
            preparedStatement.setString(
                    2,
                    SQLiteDatabaseManager.encrypt(clientSecret, clientSecretKey)
            );
            preparedStatement.addBatch();

            connection.setAutoCommit(false);

            preparedStatement.executeBatch();

            connection.setAutoCommit(true);
        } catch (SQLException sqlex) {
            sqlex.printStackTrace();
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
        } catch (NoSuchAlgorithmException nsaex) {
            nsaex.printStackTrace();
        } catch (NoSuchPaddingException nspex) {
            nspex.printStackTrace();
        } catch (InvalidKeyException ikex) {
            ikex.printStackTrace();
        } catch (IllegalBlockSizeException ibsex) {
            ibsex.printStackTrace();
        } catch (BadPaddingException bpex) {
            bpex.printStackTrace();
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
        } catch (NoSuchAlgorithmException nsqex) {
            nsqex.printStackTrace();
        } catch (NoSuchPaddingException nspex) {
            nspex.printStackTrace();
        } catch (InvalidKeyException ikex) {
            ikex.printStackTrace();
        } catch (IllegalBlockSizeException ibsex) {
            ibsex.printStackTrace();
        } catch (BadPaddingException bpex) {
            bpex.printStackTrace();
        }

        return decryptedText;
    }
}
