package sgc.manual;

import java.io.IOException;
import java.net.URISyntaxException;

import sgc.discord.bot.BotApplication;

public class ManualDMTest {

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        BotApplication.sendErrorMessageDM(
                new Exception("This is a Test Exception", new Throwable("This is a Test Throwable")));
    }
}
