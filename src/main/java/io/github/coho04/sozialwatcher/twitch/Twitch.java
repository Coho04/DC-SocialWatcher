package io.github.coho04.sozialwatcher.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.simple.SimpleEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import io.github.coho04.sozialwatcher.Main;
import io.github.coho04.sozialwatcher.twitch.events.TwitchEventHandler;
import io.sentry.Sentry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Twitch {

    private TwitchClient twitchClient;

    public Twitch() {
        OAuth2Credential credential = new OAuth2Credential("twitch", Main.getCustomConfig().getTwitchCredential());
        try {
            twitchClient = TwitchClientBuilder.builder()
                    .withClientId(Main.getCustomConfig().getTwitchClientID())
                    .withClientSecret(Main.getCustomConfig().getTwitchClientSecret())
                    .withChatAccount(credential)
                    .withDefaultAuthToken(credential)
                    .withEnableChat(true)
                    .withEnableHelix(true)
                    .withEnableHelix(true)
                    .build();
        } catch (Exception exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
        twitchClient.getEventManager().getEventHandler(SimpleEventHandler.class).registerListener(new TwitchEventHandler());
        try (Connection connection = Main.getMysql().getSource().getConnection()) {
            String selectQuery = "SELECT twitch_channel FROM twitch_channel group by twitch_channel;";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.execute("USE `sozial_watcher_db`");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String twitchChannel = rs.getString("twitch_channel");
                addChannel(twitchChannel);
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
    }

    public void addChannel(String channel) {
        twitchClient.getClientHelper().enableStreamEventListener(channel);
        twitchClient.getClientHelper().enableFollowEventListener(channel);
        twitchClient.getClientHelper().enableClipEventListener(channel);
        if (!twitchClient.getChat().isChannelJoined(channel)) {
            twitchClient.getChat().joinChannel(channel);
        }
    }

    public TwitchClient getBot() {
        return twitchClient;
    }
}
