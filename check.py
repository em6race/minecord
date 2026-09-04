
import re, sys
with open("src/main/java/com/example/minecord/utils/DeathTranslator.java", "r", encoding="utf-8") as f:
    for i, line in enumerate(f):
        if "rules.add" in line and "$s" in line:
            sys.stdout.buffer.write(f"Line {i+1} has $s: {line.strip()}\n".encode("utf-8"))
        elif "rules.add" in line:
            # check if groups match replacement
            pattern = re.search(r"\"(.*?)\",", line)
            replacement = re.search(r", \"(.*?)\"\)", line)
            if pattern and replacement:
                p = pattern.group(1)
                r = replacement.group(1)
                num_groups = p.count("(.*?)")
                for j in range(1, 10):
                    if f"${j}" in r and j > num_groups:
                        sys.stdout.buffer.write(f"Line {i+1} has missing group ${j}: {line.strip()}\n".encode("utf-8"))

