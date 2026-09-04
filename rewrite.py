import os, sys

if len(sys.argv) < 3:
    sys.exit(0)

if sys.argv[1] == 'sequence':
    with open(sys.argv[2], 'r', encoding='utf-8') as f:
        lines = f.readlines()
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        for line in lines:
            if line.startswith('pick '):
                f.write(line.replace('pick ', 'reword ', 1))
            else:
                f.write(line)
elif sys.argv[1] == 'msg':
    with open(sys.argv[2], 'r', encoding='utf-8') as f:
        lines = f.readlines()
    with open(sys.argv[2], 'w', encoding='utf-8') as f:
        for line in lines:
            if line.startswith('Feature: '):
                line = line[9:]
                if line: line = line[0].upper() + line[1:]
            elif line.startswith('Fix: '):
                line = line[5:]
                if line: line = line[0].upper() + line[1:]
            elif line.startswith('Enhancement: '):
                line = line[13:]
                if line: line = line[0].upper() + line[1:]
            f.write(line)
