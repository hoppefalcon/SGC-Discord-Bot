package sgc.discord.bot.listeners.interfaces.impl;

import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.springframework.stereotype.Component;

import sgc.discord.bot.commands.impl.ClanRaidReportCommand;
import sgc.discord.bot.commands.impl.RaidCarnageReportCommand;
import sgc.discord.bot.commands.impl.SGCActivityReportCommand;
import sgc.discord.bot.commands.impl.UserRaidReportCommand;
import sgc.discord.bot.commands.impl.UserWeeklyRaidReportCommand;
import sgc.discord.bot.listeners.interfaces.SlashCommandListener;

@Component
public class SlashCommandListenerImpl implements SlashCommandListener {

    @Override
    public void onSlashCommandCreate(SlashCommandCreateEvent event) {
        SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
        switch (slashCommandInteraction.getCommandName()) {
            case "user-raid-report":
                new UserRaidReportCommand().handle(slashCommandInteraction);
                break;

            case "pc-clan-raid-report":
                new ClanRaidReportCommand().handle(slashCommandInteraction);
                break;

            case "xbox-clan-raid-report":
                new ClanRaidReportCommand().handle(slashCommandInteraction);
                break;

            case "psn-clan-raid-report":
                new ClanRaidReportCommand().handle(slashCommandInteraction);
                break;

            case "user-weekly-raid-report":
                new UserWeeklyRaidReportCommand().handle(slashCommandInteraction);
                break;

            case "raid-carnage-report":
                new RaidCarnageReportCommand().handle(slashCommandInteraction);
                break;

            case "sgc-activity-report":
                new SGCActivityReportCommand().handle(slashCommandInteraction);
                break;

            case "user-activity-report":
                new UserRaidReportCommand().handle(slashCommandInteraction);
                break;

            default:
                break;
        }
    }

}
