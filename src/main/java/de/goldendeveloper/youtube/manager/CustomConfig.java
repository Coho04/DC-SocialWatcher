package de.goldendeveloper.youtube.manager;

import de.goldendeveloper.dcbcore.Config;

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
}