package sgc.discord.bot;

import java.util.HashSet;
import java.util.Set;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.interaction.SlashCommandBuilder;
import org.javacord.api.interaction.SlashCommandOptionBuilder;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;

import sgc.bungie.api.processor.RaidReportTool;
import sgc.discord.bot.listeners.interfaces.SlashCommandListener;

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

	@Autowired
	private ServerProperties serverProperties;

	private static final String BOT_TOKEN = System.getenv("DISCORD_TOKEN");

	public static void main(String[] args) {
		SpringApplication.run(BotApplication.class, args);
	}

	@Bean
	@ConfigurationProperties(value = "discord-api")
	public DiscordApi discordApi() {
		LOGGER.info(String.format("SGC Discord Bot Started (IP: %s | Port: %s)",
				serverProperties.getAddress(), serverProperties.getPort()));

		RaidReportTool.initializeClanIdMap();
		DiscordApi api = new DiscordApiBuilder().setToken(BOT_TOKEN)
				.setAllNonPrivilegedIntents().login().join();

		final SlashCommandOptionBuilder pcClanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getPcClanIdMap().forEach((clanCallsign, clan) -> {
			pcClanOption.addChoice(clanCallsign, clan);
		});

		final SlashCommandOptionBuilder xbClanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getXbClanIdMap().forEach((clanCallsign, clan) -> {
			xbClanOption.addChoice(clanCallsign, clan);
		});

		final SlashCommandOptionBuilder psClanOption = new SlashCommandOptionBuilder().setName("Clan")
				.setType(SlashCommandOptionType.STRING).setRequired(true).setDescription("Clan for Raid Report");

		RaidReportTool.getPsClanIdMap().forEach((clanCallsign, clan) -> {
			psClanOption.addChoice(clanCallsign, clan);
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

		Set<SlashCommandBuilder> commandList = new HashSet<>();

		commandList.add(new SlashCommandBuilder().setName("user-raid-report")
				.setDescription("Pulls the Raid Report of the user. (Requires full Bungie ID)")
				.addOption(bungieIdOption.build()));

		commandList.add(new SlashCommandBuilder().setName("pc-clan-raid-report")
				.setDescription("Pulls a full PC clan raid report.")
				.addOption(pcClanOption.build()));

		commandList.add(new SlashCommandBuilder().setName("xbox-clan-raid-report")
				.setDescription("Pulls a full Xbox clan raid report.")
				.addOption(xbClanOption.build()));

		commandList.add(new SlashCommandBuilder().setName("psn-clan-raid-report")
				.setDescription("Pulls a full Playstation clan raid report.")
				.addOption(psClanOption.build()));

		commandList.add(new SlashCommandBuilder().setName("user-weekly-raid-report").setDescription(
				"Pulls the Weekly Raid Report of the user. (Requires full Bungie ID, Start Date, and End Date)")
				.addOption(bungieIdOption.build())
				.addOption(userWeeklyClearStartOption.build())
				.addOption(userWeeklyClearEndOption.build()));

		commandList.add(new SlashCommandBuilder().setName("sgc-activity-report").setDescription(
				"Pulls the a Community Activity Report for the SGC. (Requires Start Date, and End Date)")
				.addOption(userWeeklyClearStartOption.build())
				.addOption(userWeeklyClearEndOption.build()));

		commandList.add(new SlashCommandBuilder().setName("user-activity-report").setDescription(
				"Pulls the a Community Activity Report for the User. (Requires Start Date, and End Date)")
				.addOption(bungieIdOption.build())
				.addOption(userWeeklyClearStartOption.build())
				.addOption(userWeeklyClearEndOption.build()));

		commandList.add(new SlashCommandBuilder().setName("raid-carnage-report")
				.setDescription("Pulls a Full Raid Carnage Report.")
				.addOption(carnageIdOption.build()));

		api.bulkOverwriteGlobalApplicationCommands(commandList).join();

		api.addSlashCommandCreateListener(slashCommandListener);

		return api;
	}
}
