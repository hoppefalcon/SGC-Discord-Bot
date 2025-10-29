/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.bungie.api.processor;

import java.util.HashMap;

/**
 * @author chris hoppe
 */
public class Clan {

    private final String clanId;
    private HashMap<String, Member> members = new HashMap<>();
    private String name;
    private String callsign;

    public Clan(String clanId) {
        this.clanId = clanId;
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

    @Override
    public boolean equals(Object o) {

        // If the object is compared with itself then return true
        if (o == this) {
            return true;
        }

        /*
         * Check if o is an instance of Complex or not
         * "null instanceof [type]" also returns false
         */
        if (!(o instanceof Clan)) {
            return false;
        }

        // typecast o to Complex so that we can compare data members
        Clan c = (Clan) o;

        // Compare the data members and return accordingly
        return this.clanId.equals(c.clanId) && this.callsign.equals(c.callsign) && this.name.equals(c.name);
    }

}
