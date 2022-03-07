package sgc.raid.report.bot.commands;

import org.javacord.api.interaction.SlashCommandInteraction;

public interface Command {
    public void handle(SlashCommandInteraction slashCommandInteraction);
}
