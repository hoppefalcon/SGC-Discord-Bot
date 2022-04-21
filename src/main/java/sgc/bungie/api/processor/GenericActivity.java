package sgc.bungie.api.processor;

import java.util.ArrayList;

import org.slf4j.Logger;

import sgc.discord.bot.BotApplication;

public class GenericActivity {
    private static final Logger LOGGER = BotApplication.getLogger();

    private final String UID;
    private final Platform memberClanPlatform;
    private ArrayList<Clan> otherSGCClans = new ArrayList<>();
    private int otherSGCMembers = 0;
    private boolean allSGCActivity = false;
    private double team = 0.0;
    private final Mode MODE;

    public GenericActivity(String uid, Mode mode, Platform memberClanPlatform) {
        UID = uid;
        MODE = mode;
        this.memberClanPlatform = memberClanPlatform;
    }

    public String getUID() {
        return UID;
    }

    public boolean isAllSGCActivity() {
        return allSGCActivity;
    }

    public void setAllSGCActivity(boolean allSGCActivity) {
        this.allSGCActivity = allSGCActivity;
    }

    public void addOtherSGCClan(Clan clan) {
        if (!otherSGCClans.contains(clan)) {
            otherSGCClans.add(clan);
        }
    }

    public void addOtherSGCMember() {
        otherSGCMembers++;
    }

    public int getEarnedPoints() {
        int total = 0;

        // One Point For Every SGC Member
        total += otherSGCMembers;

        // One Point For Each Unique Clan, Other Than Your Own
        total += otherSGCClans.size();

        // One Point For Each Unique Platform, Other Than Your Own
        ArrayList<Platform> platforms = new ArrayList<>();
        platforms.add(memberClanPlatform);
        for (Clan clan : otherSGCClans) {
            if (!platforms.contains(clan.getClanPlatform())) {
                try {
                    platforms.add(clan.getClanPlatform());
                    total += 1;
                } catch (Exception ex) {
                    LOGGER.error(
                            String.format("There was an error getting the Clan Platform for %s", clan.getCallsign()),
                            ex);
                }
            }
        }

        // One Point For A Ful SGC Fireteam
        if (allSGCActivity) {
            total += 1;
        }

        // Multiplied By The Activity Weight
        total = (int) Math.ceil(total * MODE.getWeeklyActivityWeight());

        return total;
    }

    public boolean earnsPoints() {
        return getEarnedPoints() > 0;
    }

    public double getTeam() {
        return team;
    }

    public void setTeam(double team) {
        this.team = team;
    }

    public Mode getMODE() {
        return MODE;
    }

}
