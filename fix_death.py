
import codecs
import re

with codecs.open("src/main/java/com/example/minecord/utils/DeathTranslator.java", "r", "utf-8") as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "$s" in line:
        count = 1
        while "$s" in line:
            line = line.replace("$s", "$" + str(count), 1)
            count += 1
    new_lines.append(line)

with codecs.open("src/main/java/com/example/minecord/utils/DeathTranslator.java", "w", "utf-8") as f:
    f.writelines(new_lines)

print("Done")

