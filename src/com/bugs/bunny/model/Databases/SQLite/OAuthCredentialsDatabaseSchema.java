package com.bugs.bunny.model.Databases.SQLite;

public class OAuthCredentialsDatabaseSchema {
    public static final class OAuthCredentialsTable {
        public static final String NAME = "OAuthCredentials";
    }

    public static final class OAuthCredentialsTableColumns {
        public static final String CLIENT_ID = "encrypted_client_id";
        public static final String CLIENT_SECRET = "encrypted_client_secret";
        public static final String CODE = "encrypted_code";
        public static final String ACCESS_TOKEN = "encrypted_access_token";
    }
}
