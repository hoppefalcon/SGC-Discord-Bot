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
import sgc.discord.infographics.GoogleDriveUtil;
import sgc.discord.infographics.Infographic;
import sgc.discord.messages.Message;

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
		GoogleDriveUtil.initiateGoogleSheetsAuth();
		SpringApplication.run(BotApplication.class, args);
		ActivityReportTool.setDiscordAPI(API);
		scheduleActivitySheetUpdate(2, 6);
	}

	/**
	 * Schedules the activity sheet update task based on the target hour and
	 * increment.
	 * If it's the first run, it calculates the shortest delay among multiple target
	 * hours,
	 * executes the activity sheets, and reschedules the task accordingly.
	 * Otherwise, it calculates the delay based on the target hour, executes the
	 * activity sheets,
	 * and reschedules the task with the next target hour and the given increment.
	 *
	 * @param targetHour The target hour for the activity sheet update.
	 * @param increment  The increment value to determine the next target hour.
	 */
	private static void scheduleActivitySheetUpdate(int targetHour, int increment) {
		System.gc();
		long delay;
		Runnable taskWrapper;

		if (firstRun) {
			int checks = 24 / increment;
			long shortestDelay = 0;
			int nextTarget = targetHour;

			// Find the shortest delay among multiple target hours
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
			final int confirmedTargetHour = targetHour;

			// Create a task wrapper to execute activity sheets and reschedule
			taskWrapper = () -> {
				ActivityReportTool.runActivitySheets();
				scheduleActivitySheetUpdate(confirmedTargetHour, increment);
			};
		} else {
			delay = computeNextDelay(targetHour);
			final int confirmedTargetHour = targetHour;
			LOGGER.info("The next scheduled Activity Sheet Update is at {}",
					getNextTargetTime(targetHour).format(BotApplication.DATE_TIME_FORMATTER));

			// Create a task wrapper to execute activity sheets and reschedule
			taskWrapper = () -> {
				ActivityReportTool.runActivitySheets();
				scheduleActivitySheetUpdate((confirmedTargetHour + increment) % 24, increment);
			};
		}

		// Schedule the task with the calculated delay
		executorService.schedule(taskWrapper, delay, TimeUnit.SECONDS);
	}

	// Calculate the next target time based on the provided target hour
	private static ZonedDateTime getNextTargetTime(int targetHour) {
		ZonedDateTime now = ZonedDateTime.now(ZID);
		ZonedDateTime nextTarget = now.withHour(targetHour).withMinute(0).withSecond(0);

		// If the current time is already past the target hour, move to the next day
		if (now.compareTo(nextTarget) > 0) {
			nextTarget = nextTarget.plusDays(1);
		}

		return nextTarget;
	}

	// Compute the delay until the next target time
	private static long computeNextDelay(int targetHour) {
		ZonedDateTime now = ZonedDateTime.now(ZID);
		ZonedDateTime nextTarget = getNextTargetTime(targetHour);

		// Calculate the duration between the current time and the next target time
		Duration duration = Duration.between(now, nextTarget);

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

		final SlashCommandOptionBuilder discordChannelIdOption = new SlashCommandOptionBuilder()
				.setName("DiscordChannelID")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("The Discord Channel ID (Must Enable Developer Mode)");

		final SlashCommandOptionBuilder daysOption = new SlashCommandOptionBuilder()
				.setName("Days").setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("Days to look back in history");

		final SlashCommandOptionBuilder timeframe = new SlashCommandOptionBuilder().setName("Timeframe")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("How far back should the report go?");
		timeframe.addChoice("1 Week", "7");
		timeframe.addChoice("2 Weeks", "14");
		timeframe.addChoice("1 Month", "30");

		final SlashCommandOptionBuilder infographicOption = new SlashCommandOptionBuilder().setName("Infographic")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("Which Infographic are you looking for?");
		for (Infographic infographic : Infographic.values()) {
			infographicOption.addChoice(infographic.name(), infographic.folderID);
		}

		final SlashCommandOptionBuilder infomationMessageOption = new SlashCommandOptionBuilder()
				.setName("InfomationMessage")
				.setType(SlashCommandOptionType.STRING).setRequired(true)
				.setDescription("Which Infomation Message are you looking for?");
		for (Message message : Message.values()) {
			infomationMessageOption.addChoice(message.name(), message.name());
		}

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

		commandList.add(new SlashCommandBuilder().setName("user-cpotw-report").setDescription(
				"Pulls the a Community Activity Report for the User. (Requires Start Date, and End Date)")
				.addOption(bungieIdOption.build())
				.addOption(userWeeklyClearStartOption.build())
				.addOption(userWeeklyClearEndOption.build()));

		commandList.add(new SlashCommandBuilder().setName("user-potw-report").setDescription(
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

		commandList.add(new SlashCommandBuilder().setName("discord-clan-chat-report")
				.setDescription("Pulls a list of members with the given Discord Role.")
				.addOption(discordRoleIdOption.build())
				.addOption(discordChannelIdOption.build())
				.addOption(daysOption.build()));

		commandList.add(new SlashCommandBuilder().setName("pc-clan-iar")
				.setDescription("Pulls a PC clan internal activity report.")
				.addOption(pcClanOption.build())
				.addOption(timeframe.build()));

		commandList.add(new SlashCommandBuilder().setName("xbox-clan-iar")
				.setDescription("Pulls a Xbox clan internal activity report.")
				.addOption(xbClanOption.build())
				.addOption(timeframe.build()));

		commandList.add(new SlashCommandBuilder().setName("psn-clan-iar")
				.setDescription("Pulls a Playstation clan internal activity report.")
				.addOption(psClanOption.build())
				.addOption(timeframe.build()));

		commandList.add(new SlashCommandBuilder().setName("infographic")
				.setDescription("Pulls an SGC infographic of choice.")
				.addOption(infographicOption.build()));

		commandList.add(new SlashCommandBuilder().setName("information")
				.setDescription("Posts a selected SGC Information Message.")
				.addOption(infomationMessageOption.build()));

		API.bulkOverwriteGlobalApplicationCommands(commandList).join();

		API.addSlashCommandCreateListener(slashCommandListener);

		return API;
	}
}
