package yue.discordconsole;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
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

        // Handler
        Logger serverLogger = Bukkit.getServer().getLogger();
        serverLogger.setLevel(Level.ALL);
        LogHandler handler = new LogHandler(logQueue);
        handler.setLevel(Level.ALL);
        serverLogger.addHandler(handler);

        // Get discord bot
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

        // Scheduler
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::sendPendingLogs, 600L, 600L);
    }

    @Override
    public void onDisable() {
        sendPendingLogs();
        if (jda != null) jda.shutdown();
    }

    public long getConsoleChannelId() {
        return consoleChannelId;
    }

    // Drains current logQueue into a List and returns
    public List<String> drainLogs() {
        List<String> logs = new ArrayList<>();
        String line;
        while ((line = logQueue.poll()) != null) {
            logs.add(line);
        }
        return logs;
    }

    // Send log
    public void sendPendingLogs() {
        if (logQueue.isEmpty() || jda == null) return;
        TextChannel channel = jda.getTextChannelById(logChannelId);
        if (channel == null) {
            getLogger().warning("Could not find log discord channel!");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (String msg : drainLogs()) {
            builder.append(msg).append("\n");
        }
        sendChunks(channel, builder.toString());
    }

    // Helper gửi chunk nhỏ hơn 1900 ký tự
    private void sendChunks(TextChannel channel, String allLogs) {
        int maxLen = 1900;
        int idx = 0;
        while (idx < allLogs.length()) {
            int end = Math.min(idx + maxLen, allLogs.length());
            if (end < allLogs.length()) {
                int lastNewline = allLogs.lastIndexOf('\n', end);
                if (lastNewline > idx) end = lastNewline + 1;
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

