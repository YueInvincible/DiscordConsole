package yue.discordconsole;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class DiscordConsolePlugin extends JavaPlugin {
    private static final String DISCORD_TOKEN = "Discord_Bot_Token";
    private static final long INITIAL_INTERVAL = 100L;  // 5s in ticks
    private static final long NORMAL_INTERVAL = 400L;   // 20s in ticks
    private static final int CHUNK_LIMIT = 1900;
    private static final Pattern ANSI_PATTERN = Pattern.compile("(\\u001B\\[[;\\d]*m)|(\\[[0-9;]+m)");

    private final ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private PrintStream originalOut;
    private JDA jda;
    private TextChannel discordLogChannel;
    private long discordCommandChannelId;

    @Override
    public void onLoad() {
        // Redirect System.out and System.err early
        originalOut = System.out;
        TeeStream tee = new TeeStream(originalOut);
        System.setOut(new PrintStream(tee, true));
        System.setErr(new PrintStream(tee, true));
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        long logChannelId = getConfig().getLong("logChannelId");
        discordCommandChannelId = getConfig().getLong("consoleChannelId");

        try {
            jda = JDABuilder.createDefault(DISCORD_TOKEN)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build();
            jda.awaitReady();
            discordLogChannel = jda.getTextChannelById(logChannelId);
            jda.addEventListener(new DiscordCommandListener(this));
        } catch (Exception e) {
            getLogger().severe("Failed to start Discord bot: " + e.getMessage());
        }

        // Initial flush every 5s until empty
        new BukkitRunnable() {
            @Override
            public void run() {
                flushLogs();
                if (logQueue.isEmpty()) cancel();
            }
        }.runTaskTimerAsynchronously(this, 0L, INITIAL_INTERVAL);

        // Ongoing flush every 20s
        new BukkitRunnable() {
            @Override
            public void run() {
                flushLogs();
            }
        }.runTaskTimerAsynchronously(this, INITIAL_INTERVAL, NORMAL_INTERVAL);
    }

    @Override
    public void onDisable() {
        flushLogs();
        System.setOut(originalOut);
        if (jda != null) jda.shutdown();
    }

    void clearLogs() {
        logQueue.clear();
    }

    String drainAllLogs() {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = logQueue.poll()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private void flushLogs() {
        if (discordLogChannel == null) return;
        String all = drainAllLogs();
        if (all.isEmpty()) return;
        // Chunk and send
        for (int i = 0; i < all.length(); i += CHUNK_LIMIT) {
            int end = Math.min(i + CHUNK_LIMIT, all.length());
            String chunk = stripAnsi(all.substring(i, end));
            discordLogChannel.sendMessage("```" + chunk + "```").queue();
        }
    }

    private static String stripAnsi(String input) {
        return ANSI_PATTERN.matcher(input).replaceAll("");
    }

    long getDiscordCommandChannelId() {
        return discordCommandChannelId;
    }

    // TeeStream to capture console output
    private class TeeStream extends OutputStream {
        private final PrintStream original;
        private final StringBuilder buf = new StringBuilder();

        TeeStream(PrintStream original) {
            this.original = original;
        }

        @Override
        public void write(int b) {
            original.write(b);
            buf.append((char) b);
            if (b == '\n') {
                String line = buf.toString();
                buf.setLength(0);
                line = stripAnsi(line);
                if (!line.trim().isEmpty()) {
                    logQueue.offer(line);
                    // Also fire ServerCommandEvent if it's a command prefix
                    if (line.startsWith("CONSOLE issued server command")) {
                        String cmd = line.substring(line.indexOf(':') + 1).trim();
                        Bukkit.getPluginManager().callEvent(new ServerCommandEvent(Bukkit.getServer().getConsoleSender(), cmd));
                    }
                }
            }
        }
    }
}
