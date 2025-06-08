package yue.discordconsole;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.scheduler.BukkitRunnable;

public class DiscordCommandListener extends ListenerAdapter {
    private static final int CHUNK_LIMIT = 1900;
    private final DiscordConsolePlugin plugin;

    public DiscordCommandListener(DiscordConsolePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getChannel().getIdLong() != plugin.getDiscordCommandChannelId()) return;

        String cmd = event.getMessage().getContentRaw();
        plugin.clearLogs();

        // Dispatch command
        Bukkit.getScheduler().runTask(plugin, () -> {
            ConsoleCommandSender cs = Bukkit.getServer().getConsoleSender();
            Bukkit.getServer().dispatchCommand(cs, cmd);
            Bukkit.getPluginManager().callEvent(new org.bukkit.event.server.ServerCommandEvent(cs, cmd));

            // Delay then return logs
            new BukkitRunnable() {
                @Override
                public void run() {
                    String all = plugin.drainAllLogs();
                    if (all.isEmpty()) {
                        event.getChannel().sendMessage("`(no output)`").queue();
                    } else {
                        for (int i = 0; i < all.length(); i += CHUNK_LIMIT) {
                            int end = Math.min(i + CHUNK_LIMIT, all.length());
                            String chunk = all.substring(i, end);
                            event.getChannel().sendMessage("```" + chunk + "```").queue();
                        }
                    }
                }
            }.runTaskLater(plugin, 2L);
        });
    }
}
