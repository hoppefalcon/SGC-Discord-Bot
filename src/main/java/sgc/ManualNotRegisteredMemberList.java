package sgc;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sgc.bungie.api.processor.activity.ActivityReportTool;

public class ManualNotRegisteredMemberList {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualActivitytoCodeWalk.class);
        private static final String BOT_TOKEN = System.getenv("DISCORD_TOKEN");

        private static final DiscordApi API = new DiscordApiBuilder().setToken(BOT_TOKEN).setAllIntents().login()
                        .join();

        public static void main(String[] args) throws Exception {
                ActivityReportTool.setDiscordAPI(API);
                String clanTag = "SOL";

                String discordRoleMembers = ActivityReportTool.getClanNonRegisteredMembers(
                                ActivityReportTool.CLAN_ROLE_ID_MAP.get(clanTag.toUpperCase()));

                System.out.println(discordRoleMembers);
        }

}
