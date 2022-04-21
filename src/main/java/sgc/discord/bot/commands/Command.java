package sgc.discord.bot.commands;

import org.javacord.api.interaction.SlashCommandInteraction;

public interface Command {
    public void handle(SlashCommandInteraction slashCommandInteraction);
}
