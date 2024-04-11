package de.goldendeveloper.sozialwatcher.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import de.goldendeveloper.sozialwatcher.Main;
import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.*;

public class YouTubeDiscordNotifier {

    public YouTubeDiscordNotifier() {
        Timer timer = new Timer(false);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkForNewVideo();
            }
        }, 0, 60000);
    }

    public void checkForNewVideo() {
        try {
            YouTube youtube = new YouTube.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(), null).setApplicationName("youtube-discord-notifier").build();
            YouTube.Channels.List channelRequest = youtube.channels().list(Collections.singletonList("contentDetails"));
            channelRequest.setKey(Main.getCustomConfig().getYtApiKey());
            try (Connection connection = Main.getMysql().getSource().getConnection()) {
                String selectQuery = "SELECT youtube_channel FROM youtube_channel group by youtube_channel;";
                PreparedStatement statement = connection.prepareStatement(selectQuery);
                statement.execute("USE `GD-SozialWatcher`");
                ResultSet rs = statement.executeQuery();
                while (rs.next()) {
                    String youtubeChannelId = rs.getString("youtube_channel_id");
                    channelRequest.setId(Collections.singletonList(youtubeChannelId));
                    getOrCheck(channelRequest.execute(), youtube, youtubeChannelId);
                }
            } catch (SQLException exception) {
                System.out.println(exception.getMessage());
                Sentry.captureException(exception);
            }
        } catch (GoogleJsonResponseException e) {
            System.out.println("Api not reachable. Retrying in 1 minute");
        } catch (IOException | GeneralSecurityException exception) {
            Sentry.captureException(exception);
            throw new RuntimeException(exception);
        }
    }

    public void getOrCheck(ChannelListResponse channelResponse, YouTube youtube, String youtubeChannelId) throws IOException {
        String uploadPlaylistId = channelResponse.getItems().getFirst().getContentDetails().getRelatedPlaylists().getUploads();
        YouTube.PlaylistItems.List playlistRequest = youtube.playlistItems().list(Collections.singletonList("snippet"));

        playlistRequest.setKey(Main.getCustomConfig().getYtApiKey()).setPlaylistId(uploadPlaylistId).setMaxResults(1L);
        PlaylistItem playlistItem = playlistRequest.execute().getItems().getFirst();
        String videoId = playlistItem.getSnippet().getResourceId().getVideoId();

        YouTube.Channels.List request = youtube.channels().list(Collections.singletonList("snippet")).setId(Collections.singletonList(youtubeChannelId)).setKey(Main.getCustomConfig().getYtApiKey());
        String channelName = request.execute().getItems().getFirst().getSnippet().getTitle();

        try (Connection connection = Main.getMysql().getSource().getConnection()) {
            String selectQuery = "SELECT guild_id, discord_text_channel_id, last_video_uuid FROM youtube_guild yg" + " JOIN discord_guild dg ON yg.discord_guild_id = dg.id" + " JOIN youtube_channel yc on yg.youtube_channel_id = yc.id" + " WHERE yc.youtube_channel = ?;";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.execute("USE `GD-SozialWatcher`");
            statement.setString(1, youtubeChannelId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    String lastVideoId = rs.getString("last_video_uuid");
                    String discordServerId = rs.getString("guild_id");
                    String discordChannelId = rs.getString("discord_text_channel_id");
                    if (!lastVideoId.isEmpty() && !lastVideoId.isBlank()) {
                        if (!videoId.equals(lastVideoId)) {
                            updateLastVideoId(connection, videoId, youtubeChannelId);
                            sendDiscordNotification(playlistItem, discordServerId, discordChannelId, channelName);
                        }
                    } else {
                        updateLastVideoId(connection, videoId, youtubeChannelId);
                        sendDiscordNotification(playlistItem, discordServerId, discordChannelId, channelName);
                    }
                }
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
    }

    public void sendDiscordNotification(PlaylistItem playlistItem, String discordServerId, String discordChannelId, String ytChannelName) {
        String videoTitle = playlistItem.getSnippet().getTitle();
        String videoUrl = "https://www.youtube.com/watch?v=" + playlistItem.getSnippet().getResourceId().getVideoId();
        String videoThumbnailUrl = playlistItem.getSnippet().getThumbnails().getDefault().getUrl();

        Guild guild = Main.getDcBot().getDiscord().getBot().getGuildById(discordServerId);
        if (guild != null) {
            NewsChannel channel = guild.getNewsChannelById(discordChannelId);
            if (channel != null) {
                String description = String.format("""
                        🌟 **Neues Video veröffentlicht!** 🌟

                        Hey Leute, wir haben gerade ein brandneues Video hochgeladen!
                        ➤ **Titel:** %s
                        ➤ **Kanal:** %s

                        Klickt auf den Link unten, um das Video anzusehen und vergesst nicht, zu liken und zu abonnieren!
                        Wir freuen uns auf euer Feedback in den Kommentaren. Viel Spaß beim Anschauen! 🎉""", videoTitle, ytChannelName);

                channel.sendMessage("@everyone").addEmbeds(new EmbedBuilder().setTitle(videoTitle, videoUrl).setDescription(description).setColor(0xdd2e44).setImage(videoThumbnailUrl).addField("Video ansehen", "[Hier klicken](" + videoUrl + ")", false).build()).queue();
            } else {
                System.out.println("Channel not found");
            }
        } else {
            System.out.println("Guild not found: " + discordServerId);
        }
    }

    private void updateLastVideoId(Connection connection, String videoId, String youtubeChannelId) throws SQLException {
        String updateQuery = "UPDATE youtube_channel SET last_video_uuid = ? where youtube_channel = ?";
        PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
        updateStatement.execute("USE `GD-SozialWatcher`");
        updateStatement.setString(1, videoId);
        updateStatement.setString(2, youtubeChannelId);
        updateStatement.executeUpdate();
    }
}