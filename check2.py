
import re, sys
with open("src/main/java/com/example/minecord/utils/DeathTranslator.java", "r", encoding="utf-8") as f:
    for i, line in enumerate(f):
        if "add" in line:
            replacement = re.search(r", \"(.*?)\"\)", line)
            if replacement:
                r = replacement.group(1)
                refs = re.findall(r"\$.", r)
                for ref in refs:
                    if ref not in ["$1", "$2", "$3", "$4", "$5"]:
                        sys.stdout.buffer.write(f"Line {i+1}: {line.strip()} -> {ref}\n".encode("utf-8"))

