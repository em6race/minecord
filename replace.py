
import codecs
import re

with codecs.open("src/main/java/com/example/minecord/bot/DiscordCommandListener.java", "r", "utf-8") as f:
    content = f.read()

pattern = r"else if \(event\.getName\(\)\.equals\(\"online\"\)\) \{.*?(?=else if \(event\.getName\(\)\.equals\(\"map\"\)\) \{)"

new_code = """else if (event.getName().equals("online")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int onlineCount = plugin.getServer().getOnlinePlayers().size();
                int maxPlayers = plugin.getServer().getMaxPlayers();
                
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder();
                
                if (onlineCount == 0) {
                    embed.setTitle("🔴 Наразі на сервері немає гравців (0/" + maxPlayers + ")");
                    embed.setColor(0xFF0000);
                } else {
                    embed.setTitle("🟢 Онлайн (" + onlineCount + "/" + maxPlayers + "):");
                    embed.setColor(0x00FF00);
                    
                    StringBuilder playersList = new StringBuilder();
                    for (org.bukkit.entity.Player player : plugin.getServer().getOnlinePlayers()) {
                        playersList.append("`").append(player.getName()).append("` ");
                    }
                    embed.setDescription(playersList.toString());
                }
                
                event.replyEmbeds(embed.build()).queue();
            });
        }
        """

content = re.sub(pattern, new_code, content, flags=re.DOTALL)

with codecs.open("src/main/java/com/example/minecord/bot/DiscordCommandListener.java", "w", "utf-8") as f:
    f.write(content)
print("Done.")

