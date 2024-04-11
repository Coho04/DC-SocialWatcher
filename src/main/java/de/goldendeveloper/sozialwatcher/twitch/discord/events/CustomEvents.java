package de.goldendeveloper.sozialwatcher.twitch.discord.events;

import de.goldendeveloper.sozialwatcher.Main;
import de.goldendeveloper.sozialwatcher.twitch.discord.commands.Settings;
import io.sentry.Sentry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomEvents extends ListenerAdapter {

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent e) {
        if (e.getName().equalsIgnoreCase(Settings.cmdSettings)) {
            assert e.getSubcommandName() != null;
            if (e.getSubcommandName().equalsIgnoreCase(Settings.cmdSettingsSubTwitchChannel)) {
                if (e.getFocusedOption().getName().equalsIgnoreCase(Settings.cmdSettingsSubTwitchChannelOptionAction)) {
                    e.replyChoices(
                            new Command.Choice("hinzufügen", "add"),
                            new Command.Choice("entfernen", "remove")
                    ).queue();
                } else if (e.getFocusedOption().getName().equalsIgnoreCase(Settings.cmdSettingsSubTwitchChannelOptionAction) && e.getOption(Settings.cmdSettingsSubTwitchChannelOptionAction) != null && e.getOption(Settings.cmdSettingsSubTwitchChannelOptionAction).getAsString().equalsIgnoreCase("remove")) {
                    assert e.getGuild() != null;
                    List<String> channels = getGuildTwitchChannel(e.getGuild());
                    e.replyChoices(channels.stream().map(channel -> new Command.Choice(channel, channel)).toList()).queue();
                }
            }
        }
    }

    public List<String> getGuildTwitchChannel(Guild guild) {
        List<String> channels = new ArrayList<>();
        try (Connection connection = Main.getMysql().getSource().getConnection()) {
            String selectQuery = "SELECT COUNT(1),twitch_channel FROM twitch_channel WHERE id in (SELECT twitch_channel_id FROM twitch_guilds where discord_guild_id = ?);";
            PreparedStatement statement = connection.prepareStatement(selectQuery);
            statement.execute("USE `GD-SozialWatcher`");
            statement.setLong(1, guild.getIdLong());
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                channels.add(rs.getString("twitch_channel"));
            }
        } catch (SQLException exception) {
            System.out.println(exception.getMessage());
            Sentry.captureException(exception);
        }
        return channels;
    }
}
