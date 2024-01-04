package de.goldendeveloper.youtube.manager;

import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.DCBotBuilder;
import de.goldendeveloper.mysql.exceptions.NoConnectionException;
import de.goldendeveloper.youtube.manager.youtube.YouTubeDiscordNotifier;

import java.sql.SQLException;

public class Main {

    private static MysqlConnection mysqlConnection;
    private static CustomConfig customConfig;
    private static DCBot dcBot;

    public static void main(String[] args) throws NoConnectionException, SQLException {
        customConfig = new CustomConfig();
        DCBotBuilder dcBotBuilder = new DCBotBuilder(args, true);
        dcBot = dcBotBuilder.build();
        mysqlConnection = new MysqlConnection(customConfig.getMysqlHostname(), customConfig.getMysqlUsername(), customConfig.getMysqlPassword(), customConfig.getMysqlPort());
        new YouTubeDiscordNotifier();
    }

    public static MysqlConnection getMysqlConnection() {
        return mysqlConnection;
    }

    public static CustomConfig getCustomConfig() {
        return customConfig;
    }

    public static DCBot getDcBot() {
        return dcBot;
    }
}
