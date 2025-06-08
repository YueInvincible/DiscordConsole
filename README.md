# 🖥️ Discord Console

A Minecraft plugin that lets you control your server console from Discord — with live command execution, output feedback, and full integration.

![Java](https://img.shields.io/badge/Built%20With-Java-blue)
![Spigot](https://img.shields.io/badge/Spigot-1.16--1.20-green)
![License](https://img.shields.io/github/license/yourusername/discord-console)

---

## ✨ Features

- 🧭 Run server commands directly from Discord
- 📥 Console output sent back in real-time
- 🖨️ Full console output logging
- 🔐 Secure command channel filtering
- 🔄 Automatically captures logs from in-game and console
- ✅ Supports command output chunking for long logs

---

## 📦 Installation

1. **Download the plugin** (`.jar`) from [Releases]([https://github.com/YueInvincible/DiscordConsole](https://github.com/YueInvincible/DiscordConsole/releases))
2. **Place it** into your server’s `plugins/` folder
3. Invite your own bot or use mine: **https://discord.com/oauth2/authorize?client_id=1379405418480009236&permissions=8&scope=bot%20applications.commands**
4. (If you use your own bot, make sure to CHANGE THE BOT TOKEN directly in the source code — I did not implement DISCORD_TOKEN in config.yml on purpose.)
5. Copy the log and console channel IDs you want to use. Make sure to properly configure the permissions so the bot has access to those channels — and ensure that not everyone can access them, for your server's security.
6. **Restart your server**

---

## 🔧 Configuration
Plugins\DiscordConsolePlugin\config.yml

consoleChannelId: <ID1>
logChannelId: <ID2>

![image](https://github.com/user-attachments/assets/9ead1253-6883-42bb-815f-52777b339626)

## ▶️ Usage
Run a command in Discord: anycommand should works

** For example: **

*gamemode*
------------------------------------------------------------------------------------------------------------------------------
Command Help: /gamemode
Description: Change player gamemode.
Usage(s);
/gamemode <survival|creative|adventure|spectator> [player] - Sets the gamemode of either you or another player if specified
------------------------------------------------------------------------------------------------------------------------------
**If the command generates a lot of output, it will be automatically chunked to fit Discord’s limits.**

## 🧪 Compatibility
| Server Type | Supported | Notes                       |
| ----------- | --------- | --------------------------- |
| Spigot      | ✅         | Fully tested               |
| Paper       | ✅         | Fully tested               |
| Bukkit      | ⚠️        | Should work, not guaranteed |
| Other forks | ❓         | Not tested                 |

## 🚫 Permissions
This plugin currently filters only one Discord channel for command usage. Only users in that channel can run commands.

For finer control (per-user permissions, command whitelists, etc.), please open a feature request or PR.

## 📜 License
MIT License — free to use, modify, and share.
