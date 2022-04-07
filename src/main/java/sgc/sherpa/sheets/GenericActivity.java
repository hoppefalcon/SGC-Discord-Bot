package sgc.sherpa.sheets;

import java.util.ArrayList;
import java.util.List;

public class GenericActivity {
    private final String UID;
    private List<String> extraSGCClans = new ArrayList<>();
    private boolean playedWithClanMember = false;
    private boolean allSGCActivity = false;
    private double team = 0.0;

    public GenericActivity(String uID) {
        UID = uID;
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

    public void addExtraSGCClan(String clanId) {
        if (!extraSGCClans.contains(clanId)) {
            extraSGCClans.add(clanId);
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

}
