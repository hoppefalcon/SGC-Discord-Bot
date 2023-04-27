package sgc.discord.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.SGC_Clan;
import sgc.bungie.api.processor.activity.ActivityReportTool;

public class ClanChatReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String discordRoleID = slashCommandInteraction.getOptionByName("DiscordRoleID").get()
                                .getStringValue().get();
                String discordChannelID = slashCommandInteraction.getOptionByName("DiscordChannelID").get()
                                .getStringValue().get();
                int days = Integer.parseInt(slashCommandInteraction.getOptionByName("Days").get()
                                .getStringValue().get());
                LOGGER.info(String.format(
                                "Running RoleMemberListCommand with options (DiscordRoleID: %s, DiscordChannelID: %s, Days: %d)",
                                discordRoleID, discordChannelID, days));

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater
                                        .setContent("Building Discord Role Member List for " + discordRoleID)
                                        .update();

                        try {
                                String discordClanDiscordActivityForForum = ActivityReportTool
                                                .getClanDiscordActivityForForum(
                                                                SGC_Clan.getGetClanByRoleId(discordRoleID),
                                                                discordChannelID, days);

                                new MessageBuilder()
                                                .setContent(SGC_Clan.getGetClanByRoleId(discordRoleID).name()
                                                                + " Discord Clan Chat Activity")
                                                .addEmbed(new EmbedBuilder()
                                                                .setAuthor(slashCommandInteraction.getUser())
                                                                .setTitle(SGC_Clan.getGetClanByRoleId(discordRoleID)
                                                                                .name() + " Discord Clan Chat Activity")
                                                                .setDescription("Discord Clan Chat Activity Completed")
                                                                .setFooter("#AreYouShrouded")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.CYAN))
                                                .addAttachment(discordClanDiscordActivityForForum.getBytes(),
                                                                String.format("%s [%s].csv",
                                                                                SGC_Clan.getGetClanByRoleId(
                                                                                                discordRoleID).name(),
                                                                                LocalDate.now().format(
                                                                                                DateTimeFormatter.BASIC_ISO_DATE)))
                                                .send(slashCommandInteraction.getChannel().get());

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(discordRoleID + " Discord Clan Chat Activity")
                                                                .setDescription(String.format(
                                                                                "An error occured while building a Discord Clan Chat Activity for (DiscordRoleID: %s, DiscordChannelID: %s, Days: %d)",
                                                                                discordRoleID, discordChannelID, days))
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
