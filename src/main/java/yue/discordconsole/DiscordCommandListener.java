package yue.discordconsole;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

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
        plugin.getLogger().info("Received discord: " + raw);

        final String command = raw.startsWith("/") ? raw.substring(1) : raw;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getServer().dispatchCommand(
                Bukkit.getServer().getConsoleSender(), command
            );
            plugin.getLogger().info("Executed: " + command);
            plugin.sendPendingLogs();
        });
    }
}