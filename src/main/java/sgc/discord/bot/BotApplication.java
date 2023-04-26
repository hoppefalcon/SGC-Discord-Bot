package sgc.discord.bot;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.server.Server;
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
import sgc.bungie.api.processor.activity.ActivityReportTool;
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

	private static final DiscordApi API = new DiscordApiBuilder().setToken(BOT_TOKEN).setAllIntents().login().join();

	public static Server SGC_SERVER = API.getServerById("100291727209807872").get();

	private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

	public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
			.appendPattern("MMM dd, yyyy HH:mm:ss a")
			.appendPattern(" ")
			.appendZoneText(TextStyle.SHORT)
			.toFormatter();
	public static final ZoneId ZID = ZoneId.of("US/Eastern");
	private static boolean firstRun = true;

	public static void main(String[] args) {
		ActivityReportTool.initiateGoogleSheetsAuth();
		SpringApplication.run(BotApplication.class, args);
		ActivityReportTool.setDiscordAPI(API);
		scheduleActivitySheetUpdate(2, 6);
	}

	private static void scheduleActivitySheetUpdate(int targetHour, int increment) {
		long delay = 0;
		Runnable taskWrapper;

		if (firstRun) {
			int checks = 24 / increment;
			long shortestDelay = 0;
			int nextTarget = targetHour;
			for (int index = 0; index < checks; index++) {
				long newDelay = computeNextDelay(nextTarget);
				if (index == 0) {
					shortestDelay = newDelay;
				} else {
					if (newDelay < shortestDelay) {
						shortestDelay = newDelay;
						targetHour = nextTarget;
					}
				}
				nextTarget += increment;
			}
			delay = 0;
			firstRun = false;
			final int confimedTargetHour = targetHour;
			taskWrapper = new Runnable() {

				@Override
				public void run() {
					ActivityReportTool.runActivitySheets();
					scheduleActivitySheetUpdate(confimedTargetHour, increment);
				}

			};
		} else {
			delay = computeNextDelay(targetHour);
			final int confimedTargetHour = targetHour;
			LOGGER.info(String.format("The next scheduled Actitivy Sheet Update is at %s",
					getNextTargetTime(targetHour).format(BotApplication.DATE_TIME_FORMATTER)));
			taskWrapper = new Runnable() {

				@Override
				public void run() {
					ActivityReportTool.runActivitySheets();
					scheduleActivitySheetUpdate((confimedTargetHour + increment) % 24, increment);
				}

			};
		}

		executorService.schedule(taskWrapper, delay, TimeUnit.SECONDS);
	}

	private static ZonedDateTime getNextTargetTime(int targetHour) {
		ZonedDateTime zonedNow = ZonedDateTime.now(ZID);
		ZonedDateTime zonedNextTarget = zonedNow.withHour(targetHour).withMinute(0).withSecond(0);
		if (zonedNow.compareTo(zonedNextTarget) > 0)
			zonedNextTarget = zonedNextTarget.plusDays(1);

		return zonedNextTarget;
	}

	private static long computeNextDelay(int targetHour) {
		Duration duration = Duration.between(ZonedDateTime.now(ZID), getNextTargetTime(targetHour));
		return duration.getSeconds();
	}

	@Bean
	@ConfigurationProperties(value = "discord-api")
	public DiscordApi discordApi() {
		LOGGER.info(String.format("SGC Discord Bot Started (IP: %s | Port: %s)",
				serverProperties.getAddress(), serverProperties.getPort()));

		RaidReportTool.initializeClanIdMap();

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

		final SlashCommandOptionBuilder discordRoleIdOption = new SlashCommandOptionBuilder().setName("DiscordRoleID")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("The Discord Role ID (Must Enable Developer Mode)");

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

				
		commandList.add(new SlashCommandBuilder().setName("discord-role-member-list")
		.setDescription("Pulls a list of members with the given Discord Role.")
		.addOption(discordRoleIdOption.build()));

		API.bulkOverwriteGlobalApplicationCommands(commandList).join();

		API.addSlashCommandCreateListener(slashCommandListener);

		return API;
	}
}
