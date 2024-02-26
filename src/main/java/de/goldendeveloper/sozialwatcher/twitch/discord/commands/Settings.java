package de.goldendeveloper.sozialwatcher.twitch.discord.commands;

import de.goldendeveloper.dcbcore.DCBot;
import de.goldendeveloper.dcbcore.interfaces.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Settings implements CommandInterface {

    public static String cmdSettings = "settings";
    public static String cmdSettingsSubTwitchChannel = "twitch-info-channel";
    public static String cmdSettingsSubTwitchChannelOptionAction = "action";

    @Override
    public CommandData commandData() {
        return Commands.slash(cmdSettings, "Zeigt die Einstellungen an.");
    }

    @Override
    public void runSlashCommand(SlashCommandInteractionEvent slashCommandInteractionEvent, DCBot dcBot) {
            //Todo: add Logic
    }
}
