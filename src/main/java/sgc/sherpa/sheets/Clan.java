/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.sherpa.sheets;

import java.util.HashMap;

/**
 * @author chris hoppe
 */
public class Clan {

    private final String clanId;
    private HashMap<String, Member> members = new HashMap<>();
    private String name;
    private String callsign;
    private final Platform clanPlatform;

    public Clan(String clanId, Platform clanPlatform) {
        this.clanId = clanId;
        this.clanPlatform = clanPlatform;
    }

    public void addMember(Member member) {
        if (!members.containsKey(member.getUID())) {
            members.put(member.getUID(), member);
        }
    }

    public HashMap<String, Member> getMembers() {
        return members;
    }

    public String getClanId() {
        return clanId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }

    public Platform getClanPlatform() {
        return clanPlatform;
    }

}
