package sgc.discord.bot.commands.impl;

import java.awt.Color;
import java.time.LocalDate;
import java.time.ZoneId;

import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;
import sgc.discord.bot.commands.Command;
import sgc.SGC_Clan;
import sgc.bungie.api.processor.*;

public class ClanInternalActivityReportCommand implements Command {

        private static final Logger LOGGER = BotApplication.getLogger();

        @Override
        public void handle(SlashCommandInteraction slashCommandInteraction) {
                String bungieClanID = slashCommandInteraction.getOptionByName("Clan").get().getStringValue().get();

                int timeframe = Integer.parseInt(
                                slashCommandInteraction.getOptionByName("Timeframe").get().getStringValue().get());
                LOGGER.info("Running ClanInternalActivityReportCommand with option " + bungieClanID);

                slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

                        interactionOriginalResponseUpdater
                                        .setContent("Building a clan internal activity report for " + bungieClanID)
                                        .update();

                        try {
                                SGC_Clan clan = SGC_Clan.getGetClanByBungieId(bungieClanID);
                                LocalDate endDate = LocalDate.now(ZoneId.of("America/New_York"));
                                LocalDate startDate = endDate.minusDays(timeframe);
                                interactionOriginalResponseUpdater
                                                .setContent("Building a clan internal activity report for "
                                                                + clan.name())
                                                .update();

                                new MessageBuilder()
                                                .setContent(
                                                                String.format("%s Internal Activity Report",
                                                                                clan.name()))
                                                .addEmbed(
                                                                new EmbedBuilder()
                                                                                .setAuthor(slashCommandInteraction
                                                                                                .getUser())
                                                                                .setTitle(String.format(
                                                                                                "%s Internal Activity Report",
                                                                                                clan.name()))
                                                                                .setDescription(String.format(
                                                                                                "%s to %s",
                                                                                                startDate.toString(),
                                                                                                endDate.toString()))
                                                                                .setFooter("#AreYouShrouded")
                                                                                .setThumbnail(RaidReportTool.class
                                                                                                .getClassLoader()
                                                                                                .getResourceAsStream(
                                                                                                                "thumbnail.jpg"))
                                                                                .setColor(Color.ORANGE))
                                                .addAttachment(RaidReportTool
                                                                .getClanInternalActivityReport(clan, startDate, endDate,
                                                                                interactionOriginalResponseUpdater)
                                                                .getBytes(),
                                                                String.format("%s_Internal_Activity_Report_%s_to_%s.csv",
                                                                                clan.name(),
                                                                                startDate.toString(),
                                                                                endDate.toString()))
                                                .send(slashCommandInteraction.getChannel()
                                                                .get());

                        } catch (Exception e) {
                                LOGGER.error(e.getMessage(), e);
                                interactionOriginalResponseUpdater.setContent("")
                                                .addEmbed(new EmbedBuilder()
                                                                .setTitle(bungieClanID
                                                                                + " clan internal activity report")
                                                                .setDescription("An error occured while building a clan internal activity report for "
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
