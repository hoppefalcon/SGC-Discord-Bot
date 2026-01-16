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
import sgc.bungie.api.processor.*;

public class ClanAltNamesReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                System.gc();
                String bungieClanID = slashCommandInteraction.getOptionByName("Clan").get().getStringValue().get();
                LOGGER.info("Running ClanAltNamesReportCommand with option " + bungieClanID);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater
                                        .setContent("Building a clan alt names report for " + bungieClanID)
                                        .update();

                        try {
                                Clan clan = RaidReportTool.getClanInformation(bungieClanID);
                                interactionOriginalResponseUpdater
                                                .setContent("Building a clan alt names report for " + clan.getName())
                                                .update();

                                new MessageBuilder().setContent(clan.getName() + " alt names Report")
                                                .addEmbed(new EmbedBuilder()
                                                                .setAuthor(slashCommandInteraction.getUser())
                                                                .setTitle(clan.getName() + " alt names Report")
                                                                .setDescription("alt names Report Completed")
                                                                .setFooter("#AreYouShrouded")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.CYAN))
                                                .addAttachment(RaidReportTool.getClanMembersAltNamesCSVByteArray(clan),
                                                                String.format("%s [%s].csv", clan.getName(),
                                                                                LocalDate.now().format(
                                                                                                DateTimeFormatter.BASIC_ISO_DATE)))
                                                .send(slashCommandInteraction.getChannel().get());

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder().setTitle(bungieClanID + " Raid Report")
                                                                .setDescription("An error occured while building a Raid Report for "
                                                                                + bungieClanID)
                                                                .setFooter("ERROR")
                                                                .setThumbnail(getClass().getClassLoader()
                                                                                .getResourceAsStream("thumbnail.jpg"))
                                                                .setColor(Color.RED))
                                                .update();
                        }
                });
        }

}
