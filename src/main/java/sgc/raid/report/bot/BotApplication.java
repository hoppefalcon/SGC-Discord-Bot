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

import sgc.raid.report.bot.listeners.interfaces.SlashCommandListener;
import sgc.sherpa.sheets.RaidReportTool;

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

	private static final String BOT_TOKEN = "OTA1ODY5NTI0MjQ5NzM5Mjg0.YYQWvA.jglOKC9aIrFxB_TBEqOIaL52P6M";

	public static void main(String[] args) {
		RaidReportTool.initializeClanIdMap();
		SpringApplication.run(BotApplication.class, args);
	}

	@Bean
	@ConfigurationProperties(value = "discord-api")
	public DiscordApi discordApi() {
		DiscordApi api = new DiscordApiBuilder().setToken(BOT_TOKEN).login().join();

		final SlashCommandOptionBuilder clanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getClanIdMap().keySet().forEach((name) -> {
			clanOption.addChoice(name, RaidReportTool.getClanIdMap().get(name));
		});

		api.bulkOverwriteGlobalSlashCommands(Arrays.asList(new SlashCommandBuilder().setName("user-raid-report")
				.setDescription("Pulls the Raid Report of the user. (Requires full Bungie ID Guardian#0000)")
				.addOption(new SlashCommandOptionBuilder().setName("BungieId").setType(SlashCommandOptionType.STRING)
						.setRequired(true).setDescription("The Users BungieID with numbers (Guardian#0000)").build()),
				new SlashCommandBuilder().setName("clan-raid-report").setDescription("Pulls a full clan raid report.")
						.addOption(clanOption.build())))
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
