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
import sgc.discord.infographics.GoogleDriveUtil;
import sgc.bungie.api.processor.activity.ActivityReportTool;

public class NotRegisteredMembersCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                System.gc();
                String clanTag = slashCommandInteraction.getOptionByName("ClanTag").get().getStringValue()
                                .get();
                LOGGER.info("Running NotRegisteredMembersCommand with option " + clanTag);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater
                                        .setContent("Building Discord Role Not Registered Member List for " + clanTag)
                                        .update();

                        try {
                                interactionOriginalResponseUpdater
                                                .setContent("Building Discord Role Not Registered Member List for "
                                                                + clanTag.toUpperCase())
                                                .update();
                                String discordRoleMembers = ActivityReportTool.getClanNonRegisteredMembers(
                                                ActivityReportTool.CLAN_ROLE_ID_MAP.get(clanTag.toUpperCase()));

                                new MessageBuilder()
                                                .setContent(clanTag.toUpperCase()
                                                                + " | Discord Role Not Registered Member List")
                                                .addEmbed(new EmbedBuilder()
                                                                .setAuthor(slashCommandInteraction.getUser())
                                                                .setTitle(clanTag.toUpperCase()
                                                                                + " | Discord Role Not Registered Member List")
                                                                .setDescription("Discord Role Not Registered Member List Completed")
                                                                .setFooter("#AreYouShrouded")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.CYAN))
                                                .addAttachment(discordRoleMembers.getBytes(),
                                                                String.format("%s [%s].csv", clanTag.toUpperCase(),
                                                                                LocalDate.now().format(
                                                                                                DateTimeFormatter.BASIC_ISO_DATE)))
                                                .send(slashCommandInteraction.getChannel().get());

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(clanTag
                                                                                + " | Discord Role Not Registered Member List")
                                                                .setDescription("An error occured while building a Discord Role Not Registered Member List for "
                                                                                + clanTag)
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
