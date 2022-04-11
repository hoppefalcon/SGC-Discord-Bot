package sgc.sherpa.sheets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenericActivity {
    private final String UID;
    private final Platform memberClanPlatform;
    private ArrayList<Clan> extraSGCClans = new ArrayList<>();
    private boolean playedWithClanMember = false;
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

    public boolean isPlayedWithClanMember() {
        return playedWithClanMember;
    }

    public void setPlayedWithClanMember(boolean playedWithClanMember) {
        this.playedWithClanMember = playedWithClanMember;
    }

    public boolean isAllSGCActivity() {
        return allSGCActivity;
    }

    public void setAllSGCActivity(boolean allSGCActivity) {
        this.allSGCActivity = allSGCActivity;
    }

    public void addExtraSGCClan(Clan clan) {
        if (!extraSGCClans.contains(clan)) {
            extraSGCClans.add(clan);
        }
    }

    public int getEarnedPoints() {
        int total = 0;
        if (playedWithClanMember) {
            total += 1;
        }
        if (allSGCActivity) {
            total += 1;
        }
        total += extraSGCClans.size();
        List<Platform> platforms = Arrays.asList(memberClanPlatform);
        for (Clan clan : extraSGCClans) {
            if (!platforms.contains(clan.getClanPlatform())) {
                platforms.add(clan.getClanPlatform());
                total += 1;
            }
        }
        total = (int) Math.ceil(total + MODE.getWeeklyActivityWeight());
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
