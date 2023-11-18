package de.goldendeveloper.youtube.manager.youtube;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import de.goldendeveloper.mysql.entities.*;
import de.goldendeveloper.youtube.manager.Main;
import de.goldendeveloper.youtube.manager.MysqlConnection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;

import java.io.IOException;
import java.security.GeneralSecurityException;
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
            SearchResults results = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).getTable(MysqlConnection.youtubeTableName).getColumn("YoutubeChannelID").getAll();
            for (String channel : results.getAsString()) {
                channelRequest.setId(Collections.singletonList(channel));
                getOrCheck(channelRequest.execute(), youtube, channel);
            }
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public void getOrCheck(ChannelListResponse channelResponse, YouTube youtube, String youtubeChannelId) throws IOException {
        String uploadPlaylistId = channelResponse.getItems().get(0).getContentDetails().getRelatedPlaylists().getUploads();
        YouTube.PlaylistItems.List playlistRequest = youtube.playlistItems().list(Collections.singletonList("snippet"));

        playlistRequest.setKey(Main.getCustomConfig().getYtApiKey()).setPlaylistId(uploadPlaylistId).setMaxResults(1L);
        PlaylistItem playlistItem = playlistRequest.execute().getItems().get(0);
        String videoId = playlistItem.getSnippet().getResourceId().getVideoId();

        Database database = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName);
        Table table = database.getTable(MysqlConnection.youtubeTableName);
        Row row = table.getRow(table.getColumn("YoutubeChannelID"), youtubeChannelId);
        HashMap<String, SearchResult> data = row.getData();
        String discordChannelId = data.get("DiscordChannelID").getAsString();
        String discordServerId = data.get("DiscordServerID").getAsString();

        YouTube.Channels.List request = youtube.channels()
                .list(Collections.singletonList("snippet"))
                .setId(Collections.singletonList(youtubeChannelId))
                .setKey(Main.getCustomConfig().getYtApiKey());
        String channelName = request.execute().getItems().get(0).getSnippet().getTitle();

        if (data.get("LastVideoID") != null) {
            if (!videoId.equals(data.get("LastVideoID").getAsString())) {
                sendDiscordNotification(playlistItem, discordServerId, discordChannelId, channelName);
                row.set(table.getColumn("LastVideoID"), videoId);
            }
        } else {
            sendDiscordNotification(playlistItem, discordServerId, discordChannelId, channelName);
            row.set(table.getColumn("LastVideoID"), videoId);
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
                String description = String.format(
                        """
                                🌟 **Neues Video veröffentlicht!** 🌟

                                Hey Leute, wir haben gerade ein brandneues Video hochgeladen!
                                ➤ **Titel:** %s
                                ➤ **Kanal:** %s

                                Klickt auf den Link unten, um das Video anzusehen und vergesst nicht, zu liken und zu abonnieren!
                                Wir freuen uns auf euer Feedback in den Kommentaren. Viel Spaß beim Anschauen! 🎉""",
                        videoTitle, ytChannelName);

                channel.sendMessageEmbeds(
                        new EmbedBuilder().setTitle(videoTitle, videoUrl)
                                .setDescription(description)
                                .setColor(0xdd2e44)
                                .setImage(videoThumbnailUrl)
                                .addField("Video ansehen", "[Hier klicken](" + videoUrl + ")", false)
                                .build()
                ).queue();
            } else {
                System.out.println("Channel not found");
            }
        } else {
            System.out.println("Guild not found: " + discordServerId);
        }
    }
}