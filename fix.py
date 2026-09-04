with open("src/main/java/com/example/minecord/utils/DeathTranslator.java", "r", encoding="utf-8") as f:
    content = f.read()

# First revert the previous script's mess by replacing the remaining '$s' if it is preceded by '(.*?)'
content = content.replace("(.*?)$s", "(.*?)")

# Also replace '\\$s' just in case
content = content.replace("\\\\$s", "")

with open("src/main/java/com/example/minecord/utils/DeathTranslator.java", "w", encoding="utf-8") as f:
    f.write(content)
