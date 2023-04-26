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
import sgc.bungie.api.processor.activity.ActivityReportTool;

public class RoleMemberListCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String discordRoleID = slashCommandInteraction.getOptionByName("DiscordRoleID").get().getStringValue()
                                .get();
                LOGGER.info("Running RoleMemberListCommand with option " + discordRoleID);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater
                                        .setContent("Building Discord Role Member List for " + discordRoleID)
                                        .update();

                        try {
                                String discordRoleName = ActivityReportTool.getDiscordRoleName(discordRoleID);
                                interactionOriginalResponseUpdater
                                                .setContent("Building Discord Role Member List for " + discordRoleName)
                                                .update();
                                String discordRoleMembers = ActivityReportTool.getDiscordRoleMembers(discordRoleID);

                                new MessageBuilder().setContent(discordRoleName + " Discord Role Member List")
                                                .addEmbed(new EmbedBuilder()
                                                                .setAuthor(slashCommandInteraction.getUser())
                                                                .setTitle(discordRoleName + " Discord Role Member List")
                                                                .setDescription("Discord Role Member List Completed")
                                                                .setFooter("#AreYouShrouded")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.CYAN))
                                                .addAttachment(discordRoleMembers.getBytes(),
                                                                String.format("%s [%s].csv", discordRoleName,
                                                                                LocalDate.now().format(
                                                                                                DateTimeFormatter.BASIC_ISO_DATE)))
                                                .send(slashCommandInteraction.getChannel().get());

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(discordRoleID + " Discord Role Member List")
                                                                .setDescription("An error occured while building a Discord Role Member List for "
                                                                                + discordRoleID)
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
