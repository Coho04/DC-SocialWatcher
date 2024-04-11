package de.goldendeveloper.sozialwatcher.twitch.discord.commands;

import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.interfaces.CommandInterface;
import de.goldendeveloper.sozialwatcher.Main;
import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.sql.*;
import java.util.OptionalInt;

public class TwitchChannel implements CommandInterface {

    public static String cmdTwitchChannel = "twitch-channel";
    public static String cmdTwitchChannelRemove = "remove";
    public static String cmdTwitchChannelAdd = "add";

    public static String discordChannel = "discord-channel";
    public static String discordRole = "discord-role";
    public static String twitchChannel = "twitch-channel";

    @Override
    public CommandData commandData() {
        return Commands.slash(cmdTwitchChannel, "Füge die Benachrichtigung eines Twitch Kanals dem Discord Server hinzu!")
                .addSubcommands(
                        new SubcommandData(cmdTwitchChannelAdd, "Setzte den Info Channel für deine Twitch Live Streams")
                                .addOption(OptionType.CHANNEL, discordChannel, "Hier bitte den Discord Benachrichtigung`s Channel angeben!", true)
                                .addOption(OptionType.ROLE, discordRole, "Hier bitte die Discord Benachrichtigung`s Rolle angeben!", true)
                                .addOption(OptionType.STRING, twitchChannel, "Hier bitte den Twitch Benachrichtigung`s Channel angeben!", true),
                        new SubcommandData(cmdTwitchChannelRemove, "Entferne einen Twitch Channel von deinem Discord Server!")
                                .addOption(OptionType.STRING, twitchChannel, "Hier bitte den Twitch Benachrichtigung`s Channel angeben!", true)
                ).setGuildOnly(true);
    }

    @Override
    public void runSlashCommand(SlashCommandInteractionEvent e, DCBot dcBot) {
        e.deferReply(true).queue();
        InteractionHook hook = e.getHook().setEphemeral(false);
        assert e.getSubcommandName() != null;
        if (e.getSubcommandName().equalsIgnoreCase(cmdTwitchChannelAdd)) {
            Channel discordChannel = e.getOption(TwitchChannel.discordChannel).getAsChannel();
            String twitchChannel = e.getOption(TwitchChannel.twitchChannel).getAsString();
            Role discordRole = e.getOption(TwitchChannel.discordRole).getAsRole();
            assert e.getGuild() != null;
            if (!twitchChannel.isEmpty()) {
                if (insertIntoTwitch(e.getGuild(), discordChannel, discordRole, twitchChannel)) {
                    hook.sendMessage("Der Twitch Channel wurde erfolgreich hinzugefügt!").queue();
                    Main.getTwitch().addChannel(twitchChannel);
                } else {
                    hook.sendMessage("Der Twitch Channel existiert bereits!").queue();
                }
            } else {
                hook.sendMessage("ERROR: Etwas ist schief gelaufen wir konnten deine Angaben nicht erfassen!").queue();
            }
        } else if (e.getSubcommandName().equalsIgnoreCase(cmdTwitchChannelRemove)) {
            String channel = e.getOption(twitchChannel).getAsString();
            assert e.getGuild() != null;
            try (Connection connection = Main.getMysqlConnection().getSource().getConnection()) {
                String selectQuery = "SELECT COUNT(1) FROM twitch_guilds WHERE discord_guild_id = (SELECT id FROM discord_guild WHERE guild_id = ?) AND twitch_channel_id = (SELECT id FROM twitch_channel WHERE twitch_channel = ?);";
                PreparedStatement statement = connection.prepareStatement(selectQuery);
                statement.execute("USE 'GD-SozialWatcher'");
                statement.setLong(1, e.getGuild().getIdLong());
                statement.setString(2, channel);
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        if (rs.getInt(1) > 0) {
                            String deleteQuery = "DELETE FROM twitch_guilds WHERE discord_guild_id = (SELECT id FROM discord_guild WHERE guild_id = ?) AND twitch_channel_id = (SELECT id FROM twitch_channel WHERE twitch_channel = ?);";
                            PreparedStatement deleteStatement = connection.prepareStatement(deleteQuery);
                            deleteStatement.execute("USE `GD-SozialWatcher`");
                            deleteStatement.setLong(1, e.getGuild().getIdLong());
                            deleteStatement.setString(2, channel);
                            deleteStatement.execute();
                            hook.sendMessage("Der Twitch Channel wurde erfolgreich von dem Discord Server entfernt!").queue();
                        } else {
                            hook.sendMessage("Der Twitch Channel existiert bereits!").queue();
                        }
                    }
                }
            } catch (SQLException exception) {
                System.out.println(exception.getMessage());
                Sentry.captureException(exception);
            }
        }
    }


    public boolean insertIntoTwitch(Guild guild, Channel discordChannel, Role discordRole, String twitchChannel) {
        String insertIntoCQuery = "INSERT INTO twitch_guilds (twitch_channel_id, discord_guild_id, discord_text_channel_id, discord_role_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = Main.getMysqlConnection().getSource().getConnection()) {
            OptionalInt discordGuildExists = getRowIdOrInsertRow(conn, "discord_guild", "guild_id", guild.getId());
            OptionalInt twitchChannelExists = getRowIdOrInsertRow(conn, "twitch_channel", "twitch_channel", twitchChannel);
            if (discordGuildExists.isPresent() && twitchChannelExists.isPresent()) {
                try (PreparedStatement stmt = conn.prepareStatement(insertIntoCQuery)) {
                    stmt.execute("USE `GD-SozialWatcher`");
                    stmt.setInt(1, twitchChannelExists.getAsInt());
                    stmt.setInt(2, discordGuildExists.getAsInt());
                    stmt.setLong(3, discordChannel.getIdLong());
                    stmt.setLong(4, discordRole.getIdLong());
                    stmt.executeUpdate();
                    return true;
                }
            } else {
                return false;
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
            return false;
        }
    }

    private OptionalInt getRowIdOrInsertRow(Connection conn, String table, String column, String value) throws SQLException {
        String selectQuery = String.format("SELECT id FROM %s WHERE %s = ?", table, column);
        try (PreparedStatement stmt = conn.prepareStatement(selectQuery)) {
            stmt.setString(1, value);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return OptionalInt.of(rs.getInt("id"));
            }
        }

        String insertQuery = String.format("INSERT INTO %s (%s) VALUES (?)", table, column);
        try (PreparedStatement stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, value);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Erstellen der Zeile fehlgeschlagen, keine Zeilen betroffen.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return OptionalInt.of(generatedKeys.getInt(1));
                }
            }
        }
        return OptionalInt.empty();
    }
}
