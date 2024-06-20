package io.github.coho04.sozialwatcher.twitch.discord.commands;

import io.github.coho04.dcbcore.DCBot;
import io.github.coho04.dcbcore.interfaces.CommandInterface;
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
