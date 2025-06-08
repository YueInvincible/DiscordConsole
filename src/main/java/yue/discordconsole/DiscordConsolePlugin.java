package yue.discordconsole;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.*;

public class DiscordConsolePlugin extends JavaPlugin {
    private static final String DISCORD_TOKEN = "YOUR_DISCORD_TOKEN";

    private long consoleChannelId;
    private long logChannelId;
    private JDA jda;
    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        consoleChannelId = getConfig().getLong("consoleChannelId");
        logChannelId = getConfig().getLong("logChannelId");

        // Get all log
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL);
        LogHandler handler = new LogHandler(logQueue);
        handler.setLevel(Level.ALL);
        rootLogger.addHandler(handler);

        try {
            jda = JDABuilder.createDefault(DISCORD_TOKEN)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordCommandListener(this))
                    .build();
            jda.awaitReady();
            getLogger().info("Discord bot is ready.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Cannot start the discord bot", e);
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::sendPendingLogs, 600L, 600L);
    }

    @Override
    public void onDisable() {
        sendPendingLogs();
        if (jda != null) {
            jda.shutdown();
        }
    }


    public long getConsoleChannelId() {
        return consoleChannelId;
    }

    // Public ChannelId
    public void sendPendingLogs() {
        if (logQueue.isEmpty() || jda == null) return;

        TextChannel channel = jda.getTextChannelById(logChannelId);
        if (channel == null) {
            getLogger().warning("Could not find channel!");
            return;
        }

        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = logQueue.poll()) != null) {
            builder.append(line).append("\n");
        }
        String allLogs = builder.toString();
        if (allLogs.isEmpty()) return;
        //max 1900 (Discord text must <2000)
        int maxLen = 1900;
        int idx = 0;
        while (idx < allLogs.length()) {
            int end = Math.min(idx + maxLen, allLogs.length());
            if (end < allLogs.length()) {
                int lastNewline = allLogs.lastIndexOf('\n', end);
                if (lastNewline > idx) {
                    end = lastNewline + 1;
                }
            }
            String chunk = allLogs.substring(idx, end);
            channel.sendMessage("**" + chunk + "**").queue();
            idx = end;
        }
    }

    private static class LogHandler extends Handler {
        private final ConcurrentLinkedQueue<String> queue;
        private final Formatter formatter = new SimpleFormatter();

        public LogHandler(ConcurrentLinkedQueue<String> queue) {
            this.queue = queue;
            setFormatter(formatter);
        }

        @Override
        public void publish(LogRecord record) {
            if (!isLoggable(record)) return;
            StringBuilder sb = new StringBuilder(formatter.format(record));
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(sw));
                sb.append(sw.toString());
            }
            queue.offer(sb.toString());
        }

        @Override public void flush() {}
        @Override public void close() throws SecurityException {}
    }
}
