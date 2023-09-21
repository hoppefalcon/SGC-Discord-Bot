/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sgc.discord.messages;

/**
 * @author chris hoppe
 */
public enum Message {
    SHERPA("Join the Elite SGC Sherpas TODAY!!",
            "SGC Sherpas are renowned for their gaming prowess in the world of Destiny 2. They have mastered all the raids and have crafted strategies for each encounter of every available to ensure its success and get you the clear!\r\n"
                    + //
                    "\r\n" + //
                    "**_Duties Include but not limited to_**\r\n" + //
                    "-Hosting at least 1 Sherpa runs a week teaching at least 1 member\r\n" + //
                    "-Able to adapt to any situation in a raid run to ensure the Sherpee gets the clear\r\n" + //
                    "-Above all else stays calm under pressure and maintains a positive attitude\r\n" + //
                    "\r\n" + //
                    "If you believe that you are among the raider elite and would like to help others get their first clear or that certain elusive raid exotic then becoming a Sherpa might be the fit for you! Application link below.\r\n"
                    + //
                    "\r\n" + //
                    "***WOULD YOU LIKE TO KNOW MORE?***\r\n" + //
                    "https://www.shroudedgaming.com/sherpa-application",
            "https://www.shroudedgaming.com/sherpa-application"),
    ADMIN("Become an SGC Clan Admin Toady!!!",
            "Being a Clan Admin in the SGC means you have actively dedicated your time to growing, maintaining, and ensuring the happiness of one of our many SGC clans that we have to offer.\r\n"
                    + //
                    "\r\n" + //
                    "**_Duties Include but not limited to_**\r\n" + //
                    "-Weekly inactivity purges\r\n" + //
                    "-Maintaining your POTW (Player of the Week) Sheets for Tuesdays community stream\r\n" + //
                    "-Ensuring the health and safety of your clan chat\r\n" + //
                    "\r\n" + //
                    "If you feel you can deliver on these duties and more then becoming and Admin might be for you! Application link below.\r\n"
                    + //
                    "\r\n" + //
                    "***WOULD YOU LIKE TO KNOW MORE?***\r\n" + //
                    "https://www.shroudedgaming.com/admin-application",
            "https://www.shroudedgaming.com/admin-application"),
    OFFICER("Become an SGC Clan Officer Toady!!!",
            "SGC Officers are our social butterflies. Always active in the clan chat to help engage and interact with the members within. They provide announcements both for Destiny and the SGC, making sure that all members are up to date on any and all information relating to Destiny and the SGC.\r\n"
                    + //
                    "\r\n" + //
                    "**_Duties Include but not limited to_**\r\n" + //
                    "-Warning members that have lapsed on our inactivity policy\r\n" + //
                    "-Actively engaging in clan chats with members\r\n" + //
                    "-Posting any Destiny and SGC related announcements the clan needs to know about\r\n" + //
                    "\r\n" + //
                    "If you feel you are the right person for the job and can handle these duties and more then being one of our great Officers might just be for you! Application link below.\r\n"
                    + //
                    "\r\n" + //
                    "***WOULD YOU LIKE TO KNOW MORE?***\r\n" + //
                    "https://www.shroudedgaming.com/officer-application-form",
            "https://www.shroudedgaming.com/officer-application-form");

    public final String title;
    public final String body;
    public final String url;

    private Message(String title, String body, String url) {
        this.title = title;
        this.body = body;
        this.url = url;
    }

    public static Message getFromName(String Name) {
        for (Message message : Message.values()) {
            if (message.name().equals(Name)) {
                return message;
            }
        }
        return null;
    }
}
