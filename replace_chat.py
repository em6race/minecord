
import codecs

with codecs.open("src/main/java/com/example/minecord/listeners/ChatListener.java", "r", "utf-8") as f:
    content = f.read()

old_code = "event.getPlayer().sendMessage(warnMsg);"
new_code = "event.getPlayer().sendMessage(warnMsg);\n                event.getPlayer().sendMessage(\"§7Твоє повідомлення: §c\" + message);"

content = content.replace(old_code, new_code)

with codecs.open("src/main/java/com/example/minecord/listeners/ChatListener.java", "w", "utf-8") as f:
    f.write(content)
print("Done")

