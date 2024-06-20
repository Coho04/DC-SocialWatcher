package io.github.coho04.sozialwatcher;

import io.github.coho04.dcbcore.Config;

public class CustomConfig extends Config {

    public int getMysqlPort() {
        return Integer.parseInt(dotenv.get("MYSQL_PORT"));
    }

    public String getYtApiKey() {
        return dotenv.get("YT_API_KEY");
    }

    public String getMysqlHostname() {
        return dotenv.get("MYSQL_HOSTNAME");
    }

    public String getMysqlPassword() {
        return dotenv.get("MYSQL_PASSWORD");
    }

    public String getMysqlUsername() {
        return dotenv.get("MYSQL_USERNAME");
    }

    public String getTwitchClientID() {
        return dotenv.get("TWITCH_CLIENT_ID");
    }

    public String getTwitchClientSecret() {
        return dotenv.get("TWITCH_CLIENT_SECRET");
    }

    public String getTwitchCredential() {
        return dotenv.get("TWITCH_CREDENTIAL");
    }

}