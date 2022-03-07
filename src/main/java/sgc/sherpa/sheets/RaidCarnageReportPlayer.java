package sgc.sherpa.sheets;

public class RaidCarnageReportPlayer {
    private final String bungieGlobalDisplayName;
    private final String bungieGlobalDisplayNameCode;
    private final BungieMembershipType membershipType;
    private final String characterClass;
    private final boolean completed;
    private final double deaths;
    private final double assists;
    private final double kills;
    private final double opponentsDefeated;
    private final double efficiency;
    private final double killsDeathsAssists;
    private final double activityDurationSeconds;

    /**
     * @param bungieGlobalDisplayName
     * @param bungieGlobalDisplayNameCode
     * @param membershipType
     * @param characterClass
     * @param completed
     * @param deaths
     * @param assists
     * @param kills
     * @param opponentsDefeated
     * @param efficiency
     * @param killsDeathsAssists
     * @param activityDurationSeconds
     */
    public RaidCarnageReportPlayer(String bungieGlobalDisplayName, String bungieGlobalDisplayNameCode,
            int membershipType, String characterClass, boolean completed, double deaths, double assists, double kills,
            double opponentsDefeated, double efficiency, double killsDeathsAssists, double activityDurationSeconds) {
        this.bungieGlobalDisplayName = bungieGlobalDisplayName;
        this.bungieGlobalDisplayNameCode = bungieGlobalDisplayNameCode;
        this.membershipType = BungieMembershipType.getBungieMembershipType(membershipType);
        this.characterClass = characterClass;
        this.completed = completed;
        this.deaths = deaths;
        this.assists = assists;
        this.kills = kills;
        this.opponentsDefeated = opponentsDefeated;
        this.efficiency = efficiency;
        this.killsDeathsAssists = killsDeathsAssists;
        this.activityDurationSeconds = activityDurationSeconds;
    }

    /**
     * @return the bungieGlobalDisplayName
     */
    public String getBungieGlobalDisplayName() {
        return bungieGlobalDisplayName;
    }

    /**
     * @return the bungieGlobalDisplayNameCode
     */
    public String getBungieGlobalDisplayNameCode() {
        return bungieGlobalDisplayNameCode;
    }

    /**
     * @return the membershipType
     */
    public BungieMembershipType getMembershipType() {
        return membershipType;
    }

    /**
     * @return the characterClass
     */
    public String getCharacterClass() {
        return characterClass;
    }

    /**
     * @return the completed
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * @return the deaths
     */
    public double getDeaths() {
        return deaths;
    }

    /**
     * @return the assists
     */
    public double getAssists() {
        return assists;
    }

    /**
     * @return the kills
     */
    public double getKills() {
        return kills;
    }

    /**
     * @return the opponentsDefeated
     */
    public double getOpponentsDefeated() {
        return opponentsDefeated;
    }

    /**
     * @return the efficiency
     */
    public double getEfficiency() {
        return efficiency;
    }

    /**
     * @return the killsDeathsAssists
     */
    public double getKillsDeathsAssists() {
        return killsDeathsAssists;
    }

    /**
     * @return the activityDurationSeconds
     */
    public double getActivityDurationSeconds() {
        return activityDurationSeconds;
    }

    public String getFullBungieID() {
        return bungieGlobalDisplayName + "#" + bungieGlobalDisplayNameCode;
    }

    public String getCsvOutput() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getFullBungieID()).append(",");
        stringBuilder.append(membershipType.getName()).append(",");
        stringBuilder.append(characterClass).append(",");
        stringBuilder.append(completed).append(",");
        stringBuilder.append(kills).append(",");
        stringBuilder.append(assists).append(",");
        stringBuilder.append(opponentsDefeated).append(",");
        stringBuilder.append(deaths).append(",");
        stringBuilder.append(efficiency).append(",");
        stringBuilder.append(killsDeathsAssists).append(",");

        int hours = (int) activityDurationSeconds / 3600;
        int minutes = ((int) activityDurationSeconds % 3600) / 60;
        int seconds = (int) activityDurationSeconds % 60;
        stringBuilder.append(String.format("%dh %dm %ds", hours, minutes, seconds)).append(",");

        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
