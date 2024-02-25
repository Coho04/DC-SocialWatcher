package de.goldendeveloper.sozialwatcher.twitch.discord.commands;

import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.interfaces.CommandInterface;
import de.goldendeveloper.mysql.entities.Row;
import de.goldendeveloper.mysql.entities.RowBuilder;
import de.goldendeveloper.mysql.entities.Table;
import de.goldendeveloper.sozialwatcher.Main;
import de.goldendeveloper.sozialwatcher.MysqlConnection;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

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
                                .addOption(OptionType.CHANNEL, discordChannel, "Hier bitte den Discord Benachrichtigung´s Channel angeben!", true)
                                .addOption(OptionType.ROLE, discordRole, "Hier bitte die Discord Benachrichtigung´s Rolle angeben!", true)
                                .addOption(OptionType.STRING, twitchChannel, "Hier bitte den Twitch Benachrichtigung´s Channel angeben!", true),
                        new SubcommandData(cmdTwitchChannelRemove, "Entferne einen Twitch Channel von deinem Discord Server!")
                                .addOption(OptionType.STRING, twitchChannel, "Hier bitte den Twitch Benachrichtigung´s Channel angeben!", true)
                ).setGuildOnly(true);
    }

    @Override
    public void runSlashCommand(SlashCommandInteractionEvent e, DCBot dcBot) {
        e.deferReply(true).queue();
        InteractionHook hook = e.getHook().setEphemeral(false);
        if (e.getSubcommandName().equalsIgnoreCase(cmdTwitchChannelAdd)) {
            Channel DiscordChannel = e.getOption(discordChannel).getAsChannel();
            String TwitchChannel = e.getOption(twitchChannel).getAsString();
            Role DiscordRole = e.getOption(discordRole).getAsRole();
            if (!TwitchChannel.isEmpty()) {
                Table table = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).getTable(MysqlConnection.twitchTableName);
                if (!isInDatabase(DiscordChannel, DiscordRole, TwitchChannel, e.getGuild(), table)) {
                    table.insert(
                            new RowBuilder()
                                    .with(table.getColumn(MysqlConnection.colmDcServer), e.getGuild().getId())
                                    .with(table.getColumn(MysqlConnection.colmDcStreamNotifyRole), DiscordRole.getId())
                                    .with(table.getColumn(MysqlConnection.colmTwitchChannel), TwitchChannel)
                                    .with(table.getColumn(MysqlConnection.colmDcStreamNotifyChannel), DiscordChannel.getId())
                                    .build()
                    );
                    hook.sendMessage("Der Twitch Channel wurde erfolgreich hinzugefügt!").queue();
                } else {
                    hook.sendMessage("Der Twitch Channel existiert bereits!").queue();
                }
                Main.getTwitch().addChannel(TwitchChannel);
            } else {
                hook.sendMessage("ERROR: Etwas ist schief gelaufen wir konnten deine Angaben nicht erfassen!").queue();
            }
        } else if (e.getSubcommandName().equalsIgnoreCase(cmdTwitchChannelRemove)) {
            Table table = Main.getMysqlConnection().getMysql().getDatabase(MysqlConnection.dbName).getTable(MysqlConnection.twitchTableName);
            String channel = e.getOption(twitchChannel).getAsString();
            getRowsWithTwitchChannel(table, channel).forEach(Row::drop);
            hook.sendMessage("Der Twitch Channel wurde erfolgreich von dem Discord Server entfernt!").queue();
        }
    }

    public List<Row> getRowsWithTwitchChannel(Table table, String channel) {
        return table.getRows().stream().filter(row -> row.getData().get(MysqlConnection.colmTwitchChannel).getAsString().equalsIgnoreCase(channel)).toList();
    }

    public Boolean isInDatabase(Channel channel, Role role, String TwitchChannel, Guild guild, Table table) {
        return table.getRows().stream()
                .filter(row -> row.getData().get(MysqlConnection.colmDcServer).getAsString().equalsIgnoreCase(guild.getId()))
                .filter(row -> row.getData().get(MysqlConnection.colmDcStreamNotifyChannel).getAsString().equalsIgnoreCase(channel.getId()))
                .filter(row -> row.getData().get(MysqlConnection.colmDcStreamNotifyRole).getAsString().equalsIgnoreCase(role.getId()))
                .anyMatch(row -> row.getData().get(MysqlConnection.colmTwitchChannel).getAsString().equalsIgnoreCase(TwitchChannel));
    }
}
