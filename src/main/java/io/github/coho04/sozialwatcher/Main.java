package io.github.coho04.sozialwatcher;

import io.github.coho04.sozialwatcher.twitch.Twitch;
import io.github.coho04.sozialwatcher.twitch.discord.commands.Settings;
import io.github.coho04.sozialwatcher.twitch.discord.commands.TwitchChannel;
import io.github.coho04.sozialwatcher.twitch.discord.events.CustomEvents;
import io.github.coho04.sozialwatcher.youtube.YouTubeDiscordNotifier;
import io.github.coho04.dcbcore.DCBot;
import io.github.coho04.dcbcore.DCBotBuilder;

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
        System.out.println("Java application started successfully");
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
