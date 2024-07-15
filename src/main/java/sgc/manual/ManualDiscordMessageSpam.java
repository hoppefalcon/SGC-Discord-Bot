package sgc.manual;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.message.MessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManualDiscordMessageSpam {
        private static final Logger LOGGER = LoggerFactory.getLogger(ManualDiscordMessageSpam.class);

        private static final DiscordApi API = new DiscordApiBuilder().setToken(System.getenv("DISCORD_TOKEN"))
                        .setAllIntents().login().join();
        static AtomicInteger counter = new AtomicInteger(1);

        public static void main(String[] args) throws Exception {
                new Task().run();
        }

        static Timer timer = new Timer();

        static class Task extends TimerTask {
                @Override
                public void run() {
                        int delay = (5 + new Random().nextInt(5)) * 1000;
                        timer.schedule(new Task(), delay);
                        System.out.println(counter.getAndIncrement());
                        new MessageBuilder()
                                        .append("<@600084282295320585>")
                                        .send(API.getChannelById("1097277042610942032").get().asTextChannel().get());
                }

        }
}
