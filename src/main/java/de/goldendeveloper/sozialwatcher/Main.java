package de.goldendeveloper.sozialwatcher;

import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.DCBotBuilder;
import de.goldendeveloper.mysql.exceptions.NoConnectionException;
import de.goldendeveloper.sozialwatcher.twitch.Twitch;
import de.goldendeveloper.sozialwatcher.twitch.discord.commands.Settings;
import de.goldendeveloper.sozialwatcher.twitch.discord.commands.TwitchChannel;
import de.goldendeveloper.sozialwatcher.twitch.discord.events.CustomEvents;
import de.goldendeveloper.sozialwatcher.youtube.YouTubeDiscordNotifier;

import java.sql.SQLException;

public class Main {

    private static MysqlConnection mysqlConnection;
    private static CustomConfig customConfig;
    private static DCBot dcBot;
    private static Twitch twitch;

    public static void main(String[] args) throws NoConnectionException, SQLException {
        customConfig = new CustomConfig();
        DCBotBuilder dcBotBuilder = new DCBotBuilder(args, true);
        dcBotBuilder.registerEvents(new CustomEvents());
        dcBotBuilder.registerCommands(new Settings(), new TwitchChannel());
        dcBot = dcBotBuilder.build();
        mysqlConnection = new MysqlConnection(customConfig.getMysqlHostname(), customConfig.getMysqlUsername(), customConfig.getMysqlPassword(), customConfig.getMysqlPort());
        twitch = new Twitch();
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

    public static Twitch getTwitch() {
        return twitch;
    }
}
