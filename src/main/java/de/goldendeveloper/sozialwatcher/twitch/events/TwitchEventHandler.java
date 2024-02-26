package de.goldendeveloper.sozialwatcher.twitch.events;

import com.github.philippheuer.events4j.simple.domain.EventSubscriber;
import com.github.twitch4j.chat.events.channel.*;
import com.github.twitch4j.events.*;
import com.github.twitch4j.helix.domain.SubscriptionEvent;
import de.goldendeveloper.mysql.entities.SearchResult;
import de.goldendeveloper.mysql.entities.Table;
import de.goldendeveloper.sozialwatcher.Main;
import de.goldendeveloper.sozialwatcher.MysqlConnection;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.util.HashMap;
import java.util.List;

@SuppressWarnings("unused")
public class TwitchEventHandler {

    @EventSubscriber
    public void onChannelGoLive(ChannelGoLiveEvent e) {
        if (Main.getMysqlConnection().getMysql().existsDatabase(MysqlConnection.dbName)) {
            if (Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).existsTable(MysqlConnection.twitchTableName)) {
                Table table = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).getTable(MysqlConnection.twitchTableName);
                if (table.existsColumn(MysqlConnection.colmDcServer) && table.existsColumn(MysqlConnection.colmTwitchChannel)) {
                    String twChannel = e.getChannel().getName();
                    HashMap<Channel, Role> notify = getMessageChannel(table, twChannel);
                    for (Channel channel : notify.keySet()) {
                        Role role = notify.get(channel);
                        if (role != null) {
                            if (channel.getType().equals(ChannelType.TEXT)) {
                                TextChannel textChannel = Main.getDcBot().getDiscord().getBot().getTextChannelById(channel.getId());
                                if (textChannel != null) {
                                    textChannel.sendMessage(role.getAsMention() + " " + twChannel + " ist nun Live auf Twitch!")
                                            .setEmbeds(sendTwitchNotifyEmbed(e.getStream().getTitle(), e.getChannel().getName(), e.getStream().getGameName(), e.getStream().getViewerCount()))
                                            .queue();
                                }
                            } else if (channel.getType().equals(ChannelType.NEWS)) {
                                NewsChannel newsChannel = Main.getDcBot().getDiscord().getBot().getNewsChannelById(channel.getId());
                                if (newsChannel != null) {
                                    newsChannel.sendMessage(role.getAsMention() + twChannel + " ist nun Live auf Twitch!")
                                            .setEmbeds(sendTwitchNotifyEmbed(e.getStream().getTitle(), e.getChannel().getName(), e.getStream().getGameName(), e.getStream().getViewerCount()))
                                            .queue();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static HashMap<Channel, Role> getMessageChannel(Table table, String TwitchName) {
        HashMap<Channel, Role> channels = new HashMap<>();
        table.getRows().forEach(row -> {
            HashMap<String, SearchResult> sr = row.getData();
            if (sr.get(MysqlConnection.colmTwitchChannel).getAsString().equalsIgnoreCase(TwitchName)) {
                if (!sr.get(MysqlConnection.colmDcStreamNotifyChannel).getAsString().equalsIgnoreCase("0")) {
                    Channel channel = Main.getDcBot().getDiscord().getBot().getGuildChannelById(sr.get(MysqlConnection.colmDcStreamNotifyChannel).getAsString());
                    Role role = Main.getDcBot().getDiscord().getBot().getRoleById(sr.get(MysqlConnection.colmDcStreamNotifyRole).getAsString());
                    if (role != null) {
                        channels.put(channel, role);
                    }
                }
            }
        });
        return channels;
    }

    @EventSubscriber
    public void onChannelMessage(ChannelMessageEvent e) {
        if (e.getMessage().startsWith("!dc") || e.getMessage().startsWith("!discord")) {
            e.getTwitchChat().sendMessage(e.getChannel().getName(), "Um auf meinen Discord zu Joinen klicke auf den Link: " + sendDiscordInvite(e.getChannel().getName()));
        }
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

    private String sendDiscordInvite(String channel) {
        Table table = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).getTable(MysqlConnection.twitchTableName);
        if (table.getColumn(MysqlConnection.colmTwitchChannel).getAll().getAsString().contains(channel)) {
            HashMap<String, SearchResult> row = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).getTable(MysqlConnection.twitchTableName).getRow(table.getColumn(MysqlConnection.colmTwitchChannel), channel).getData();
            long dcID = row.get(MysqlConnection.colmDcServer).getAsLong();
            List<Invite> invites = Main.getDcBot().getDiscord().getBot().getGuildById(dcID).retrieveInvites().complete();
            if (getValidInvite(invites) != null) {
                return getValidInvite(invites);
            } else {
                return Main.getDcBot().getDiscord().getBot().getGuildById(dcID).getDefaultChannel().createInvite().complete().getUrl();
            }
        }
        return "";
    }

    private String getValidInvite(List<Invite> invites) {
        return invites.stream().filter(invite -> !invite.isTemporary()).map(Invite::getUrl).findFirst().orElse(null);
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