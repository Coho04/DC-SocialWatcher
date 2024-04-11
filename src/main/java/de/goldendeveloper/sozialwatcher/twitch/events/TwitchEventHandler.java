package de.goldendeveloper.sozialwatcher.twitch.events;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.events.*;
import com.github.twitch4j.helix.domain.SubscriptionEvent;
import de.goldendeveloper.sozialwatcher.Main;
import io.sentry.Sentry;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("unused")
public class TwitchEventHandler {

    @EventSubscriber
    public void onChannelGoLive(ChannelGoLiveEvent e) {
        String twitchChannel = e.getChannel().getName();
        try (Connection connection = Main.getMysqlConnection().getSource().getConnection()) {
            String selectQuery = "SELECT guild_id, discord_text_channel_id, discord_role_id FROM discord_guild dg JOIN twitch_guilds tg ON dg.id = tg.discord_guild_id WHERE tg.twitch_channel_id = (SELECT id FROM twitch_channel WHERE twitch_channel LIKE ?);";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.execute("use 'GD-SozialWatcher'");
            statement.setString(1, twitchChannel);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String guildId = rs.getString("guild_id");
                String discordTextChannelId = rs.getString("discord_text_channel_id");
                String discordRoleId = rs.getString("discord_role_id");
                Guild guild = Main.getDcBot().getDiscord().getBot().getGuildById(guildId);
                assert guild != null;
                Channel channel = guild.getGuildChannelById(discordTextChannelId);
                Role role = guild.getRoleById(discordRoleId);
                assert role != null;
                assert channel != null;
                if (channel.getType().equals(ChannelType.TEXT)) {
                    TextChannel textChannel = Main.getDcBot().getDiscord().getBot().getTextChannelById(channel.getId());
                    if (textChannel != null) {
                        textChannel.sendMessage(role.getAsMention() + " " + twitchChannel + " ist nun Live auf Twitch!")
                                .setEmbeds(sendTwitchNotifyEmbed(e.getStream().getTitle(), e.getChannel().getName(), e.getStream().getGameName(), e.getStream().getViewerCount()))
                                .queue();
                    }
                } else if (channel.getType().equals(ChannelType.NEWS)) {
                    NewsChannel newsChannel = Main.getDcBot().getDiscord().getBot().getNewsChannelById(channel.getId());
                    if (newsChannel != null) {
                        newsChannel.sendMessage(role.getAsMention() + twitchChannel + " ist nun Live auf Twitch!")
                                .setEmbeds(sendTwitchNotifyEmbed(e.getStream().getTitle(), e.getChannel().getName(), e.getStream().getGameName(), e.getStream().getViewerCount()))
                                .queue();
                    }
                }
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
    }

    @EventSubscriber
    public void onChannelMessage(ChannelMessageEvent e) {
    }

    @EventSubscriber
    public void onFollow(FollowEvent e) {
        e.getTwitchChat().sendMessage(e.getChannel().getName(), String.format("%s ist nun teil der Community %s!", e.getUser().getName(), e.getChannel().getName()));
    }

    @EventSubscriber
    public void onCheer(CheerEvent e) {
        Main.getTwitch().getBot().getChat().sendMessage(e.getChannel().getName(), "Vielen dank, " + e.getUser().getName() + " für deinen Cheer mit " + e.getBits() + " Bits ! <3");
    }

    @EventSubscriber
    public void onSubscription(SubscriptionEvent e) {
        Main.getTwitch().getBot().getChat().sendMessage(e.getEventData().getBroadcasterName(), "Vielen dank für deinen Abo " + e.getEventData().getUserName() + "! <3");
    }

    @EventSubscriber
    public void onGiftSubscription(GiftSubscriptionsEvent e) {
        Main.getTwitch().getBot().getChat().sendMessage(e.getChannel().getName(), "Herzlichen Glückwunsch, " + e.getUser().getName() + " zu deinem Abo! Vielen Dank!<3");
    }

    @EventSubscriber
    public void onDonation(DonationEvent e) {
        e.getTwitchChat().sendMessage(e.getChannel().getName(), String.format("%s hat gespendet %s, Vielen Dank! <3", e.getUser().getName(), e.getAmount()));
    }

    private MessageEmbed sendTwitchNotifyEmbed(String streamTitle, String channelName, String gameName, int viewerCount) {
        return new EmbedBuilder()
                .setAuthor(channelName + " ist nun live auf Twitch!", "https://twitch.tv/" + channelName, Main.getDcBot().getDiscord().getBot().getSelfUser().getEffectiveAvatarUrl())
                .setColor(new Color(100, 65, 164))
                .setTitle(streamTitle, "https://www.twitch.tv/" + channelName)
                .setImage("https://static-cdn.jtvnw.net/previews-ttv/live_user_" + channelName + "-1920x1080.png")
                .setDescription("Spielt nun " + gameName + " für " + viewerCount + " Zuschauern! \n" +
                        "[Schau vorbei](https://twitch.tv/" + channelName + ")")
                .setFooter("@Golden-Developer")
                .build();
    }
}