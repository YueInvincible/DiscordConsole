package yue.discordconsole;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.StringJoiner;

public class DiscordCommandListener extends ListenerAdapter {
    private final DiscordConsolePlugin plugin;

    public DiscordCommandListener(DiscordConsolePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannel().getIdLong() != plugin.getConsoleChannelId()) return;

        String raw = event.getMessage().getContentRaw();
        plugin.getLogger().info("Received discord command: " + raw);

        // Drain old logs
        plugin.drainLogs();

        final String command = raw.startsWith("/") ? raw.substring(1) : raw;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().getConsoleSender(), command
            );
            plugin.getLogger().info("Executed: " + command);

            // Lấy log vừa tạo
            List<String> logs = plugin.drainLogs();
            if (logs.isEmpty()) {
                event.getChannel().sendMessage("`(no output)`").queue();
                return;
            }

            // Gửi trả lời giống console
            StringJoiner joiner = new StringJoiner("\n", "**", "**");
            for (String line : logs) {
                joiner.add(line);
            }
            event.getChannel().sendMessage(joiner.toString()).queue();
        });
    }
}
