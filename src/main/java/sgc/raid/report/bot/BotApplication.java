package sgc.raid.report.bot;

import java.util.Arrays;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import sgc.raid.report.bot.listeners.interfaces.SlashCommandListener;
import sgc.sherpa.sheets.RaidReportTool;

@Controller
@SpringBootApplication
public class BotApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(BotApplication.class);

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return LOGGER;
	}

	@Autowired
	private SlashCommandListener slashCommandListener;

	private static final String BOT_TOKEN = System.getenv("DISCORD-TOKEN");

	public static void main(String[] args) {
		RaidReportTool.initializeClanIdMap();
		SpringApplication.run(BotApplication.class, args);
	}

	@Bean
	@ConfigurationProperties(value = "discord-api")
	public DiscordApi discordApi() {
		DiscordApi api = new DiscordApiBuilder().setToken(BOT_TOKEN).login().join();

		final SlashCommandOptionBuilder pcClanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getPcclanidmap().keySet().forEach((name) -> {
			pcClanOption.addChoice(name, RaidReportTool.getPcclanidmap().get(name));
		});

		final SlashCommandOptionBuilder xbClanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getXbclanidmap().keySet().forEach((name) -> {
			xbClanOption.addChoice(name, RaidReportTool.getXbclanidmap().get(name));
		});

		final SlashCommandOptionBuilder psClanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getPsclanidmap().keySet().forEach((name) -> {
			psClanOption.addChoice(name, RaidReportTool.getPsclanidmap().get(name));
		});

		final SlashCommandOptionBuilder userWeeklyClearStartOption = new SlashCommandOptionBuilder()
				.setName("StartDate").setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("Starting Date for Clear Count (YYYYMMDD)");

		final SlashCommandOptionBuilder userWeeklyClearEndOption = new SlashCommandOptionBuilder().setName("EndDate")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("Ending Date for Clear Count (YYYYMMDD)");

		final SlashCommandOptionBuilder bungieIdOption = new SlashCommandOptionBuilder().setName("BungieId")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("The Users BungieID with numbers (Guardian#0000)");

		final SlashCommandOptionBuilder carnageIdOption = new SlashCommandOptionBuilder().setName("ID")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Postgame Carnage Report ID");

		api.bulkOverwriteGlobalApplicationCommands(Arrays.asList(
				new SlashCommandBuilder().setName("user-raid-report")
						.setDescription("Pulls the Raid Report of the user. (Requires full Bungie ID)")
						.addOption(bungieIdOption.build()),
				new SlashCommandBuilder().setName("pc-clan-raid-report")
						.setDescription("Pulls a full PC clan raid report.").addOption(pcClanOption.build()),
				new SlashCommandBuilder().setName("xbox-clan-raid-report")
						.setDescription("Pulls a full Xbox clan raid report.").addOption(xbClanOption.build()),
				new SlashCommandBuilder().setName("psn-clan-raid-report")
						.setDescription("Pulls a full Playstation clan raid report.").addOption(psClanOption.build()),
				new SlashCommandBuilder().setName("user-weekly-raid-report").setDescription(
						"Pulls the Weekly Raid Report of the user. (Requires full Bungie ID, Start Date, and End Date)")
						.addOption(bungieIdOption.build()).addOption(userWeeklyClearStartOption.build())
						.addOption(userWeeklyClearEndOption.build()),
				// new SlashCommandBuilder().setName("sgc-activity-report").setDescription(
				// "Pulls the Weekly Activity Report for the SGC. (Requires Start Date, and End
				// Date)")
				// .addOption(userWeeklyClearStartOption.build())
				// .addOption(userWeeklyClearEndOption.build()),
				new SlashCommandBuilder().setName("raid-carnage-report")
						.setDescription("Pulls a Full Raid Carnage Report.").addOption(carnageIdOption.build())))
				.join();

		api.addSlashCommandCreateListener(slashCommandListener);

		api.addSlashCommandCreateListener(event -> {
			SlashCommandInteraction slashCommandInteraction = event.getSlashCommandInteraction();
			if (slashCommandInteraction.getCommandName().equals("user-raid-report")) {
				slashCommandInteraction.respondLater().thenAccept(interactionOriginalResponseUpdater -> {

					interactionOriginalResponseUpdater
							.setContent("Building a raid report for "
									+ slashCommandInteraction.getOptionByName("BungieID").get().getStringValue())
							.update();

					// time < 15 minutes
					interactionOriginalResponseUpdater
							.setContent(
									"Thank you for your patience, it took a while but the answer to the universe is 42")
							.update();
				});
			}
		});

		return api;
	}
}
