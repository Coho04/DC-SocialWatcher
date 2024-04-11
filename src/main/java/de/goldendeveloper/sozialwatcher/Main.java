package de.goldendeveloper.sozialwatcher;

import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.DCBotBuilder;
import de.goldendeveloper.sozialwatcher.twitch.Twitch;
import de.goldendeveloper.sozialwatcher.twitch.discord.commands.Settings;
import de.goldendeveloper.sozialwatcher.twitch.discord.commands.TwitchChannel;
import de.goldendeveloper.sozialwatcher.twitch.discord.events.CustomEvents;
import de.goldendeveloper.sozialwatcher.youtube.YouTubeDiscordNotifier;

public class Main {

    private static Mysql mysql;
    private static CustomConfig customConfig;
    private static DCBot dcBot;
    private static Twitch twitch;

    public static void main(String[] args) {
        customConfig = new CustomConfig();
        DCBotBuilder dcBotBuilder = new DCBotBuilder(args, true);
        dcBotBuilder.registerEvents(new CustomEvents());
        dcBotBuilder.registerCommands(new Settings(), new TwitchChannel());
        dcBot = dcBotBuilder.build();
        mysql = new Mysql();
        twitch = new Twitch();
        new YouTubeDiscordNotifier();
    }

    public static Mysql getMysql() {
        return mysql;
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
