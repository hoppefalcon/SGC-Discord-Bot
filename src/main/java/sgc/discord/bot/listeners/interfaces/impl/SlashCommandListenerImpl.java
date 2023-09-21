package sgc.discord.bot.listeners.interfaces.impl;

import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.springframework.stereotype.Component;

import sgc.discord.bot.commands.impl.ClanChatReportCommand;
import sgc.discord.bot.commands.impl.ClanInternalActivityReportCommand;
import sgc.discord.bot.commands.impl.ClanRaidReportCommand;
import sgc.discord.bot.commands.impl.InfographicCommand;
import sgc.discord.bot.commands.impl.InformationMessageCommand;
import sgc.discord.bot.commands.impl.RaidCarnageReportCommand;
import sgc.discord.bot.commands.impl.RoleMemberListCommand;
import sgc.discord.bot.commands.impl.SGCActivityReportCommand;
import sgc.discord.bot.commands.impl.UserCommunityActivityReportCommand;
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
                new UserCommunityActivityReportCommand().handle(slashCommandInteraction);
                break;

            case "discord-role-member-list":
                new RoleMemberListCommand().handle(slashCommandInteraction);
                break;

            case "discord-clan-chat-report":
                new ClanChatReportCommand().handle(slashCommandInteraction);
                break;

            case "pc-clan-iar":
                new ClanInternalActivityReportCommand().handle(slashCommandInteraction);
                break;

            case "xbox-clan-iar":
                new ClanInternalActivityReportCommand().handle(slashCommandInteraction);
                break;

            case "psn-clan-iar":
                new ClanInternalActivityReportCommand().handle(slashCommandInteraction);
                break;

            case "infographic":
                new InfographicCommand().handle(slashCommandInteraction);
                break;

            case "information":
                new InformationMessageCommand().handle(slashCommandInteraction);
                break;
            default:
                break;
        }
    }

}
