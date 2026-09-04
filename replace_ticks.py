import codecs
with codecs.open('src/main/java/com/example/minecord/bot/DiscordCommandListener.java', 'r', 'utf-8') as f:
    content = f.read()

content = content.replace('playersList.append("").append(player.getName()).append(" ");', 'playersList.append("\").append(player.getName()).append("\ ");')

with codecs.open('src/main/java/com/example/minecord/bot/DiscordCommandListener.java', 'w', 'utf-8') as f:
    f.write(content)
