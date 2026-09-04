import json
import re

with open('en_us.json', 'r', encoding='utf-8') as f:
    en_us = json.load(f)
with open('uk_ua.json', 'r', encoding='utf-8') as f:
    uk_ua = json.load(f)

def escape_java_regex(text):
    # Escape special regex chars but NOT spaces or letters
    special = '.^$*+?()[{\\\|'
    res = ''
    for c in text:
        if c in special:
            res += '\\\\' + c
        else:
            res += c
    return res

with open('src/main/java/com/example/minecord/utils/DeathTranslator.java', 'w', encoding='utf-8') as out:
    def o(s):
        out.write(s + '\n')
    
    o('package com.example.minecord.utils;')
    o('import java.util.regex.Pattern;')
    o('import java.util.regex.Matcher;')
    o('import java.util.ArrayList;')
    o('import java.util.List;')
    o('public class DeathTranslator {')
    o('    private static class TranslationRule {')
    o('        Pattern pattern;')
    o('        String replacement;')
    o('        TranslationRule(String regex, String replacement) {')
    o('            this.pattern = Pattern.compile(regex);')
    o('            this.replacement = replacement;')
    o('        }')
    o('    }')
    o('    private static final List<TranslationRule> rules = new ArrayList<>();')
    o('    private static final List<TranslationRule> mobRules = new ArrayList<>();')
    o('    static {')
    
    for key in en_us:
        if key.startswith('entity.minecraft.'):
            en_mob = en_us[key]
            if key in uk_ua:
                uk_mob = uk_ua[key]
                uk_mob = uk_mob.replace('"', '\\"')
                o(f'        mobRules.add(new TranslationRule("\\\\b" + Pattern.quote("{en_mob}") + "\\\\b", "{uk_mob}"));')
    
    for key in en_us:
        if key.startswith('death.attack.'):
            en_str = en_us[key]
            if key in uk_ua:
                uk_str = uk_ua[key]
                uk_str = uk_str.replace('"', '\\"')
                
                # We need to replace %1, %2 etc. with capture groups
                # First, temporarily replace % placeholders with unique tokens
                regex = en_str
                regex = regex.replace('%1', '___1___')
                regex = regex.replace('%2', '___2___')
                regex = regex.replace('%3', '___3___')
                regex = regex.replace('%s', '___1___')
                
                # Now escape everything else for Java Regex
                regex = escape_java_regex(regex)
                
                # Now replace tokens with regex capture groups
                regex = regex.replace('___1___', '(.*?)')
                regex = regex.replace('___2___', '(.*?)')
                regex = regex.replace('___3___', '(.*?)')
                
                replacement = uk_str
                replacement = replacement.replace('%1', '')
                replacement = replacement.replace('%2', '')
                replacement = replacement.replace('%3', '')
                replacement = replacement.replace('%s', '')
                
                o(f'        rules.add(new TranslationRule("^{regex}$", "{replacement}"));')
    
    o('    }')
    o('    public static String translate(String message) {')
    o('        if (message == null) return null;')
    o('        for (TranslationRule rule : rules) {')
    o('            Matcher m = rule.pattern.matcher(message);')
    o('            if (m.find()) {')
    o('                String translated = m.replaceAll(rule.replacement);')
    o('                for (TranslationRule mobRule : mobRules) {')
    o('                    Matcher mobM = mobRule.pattern.matcher(translated);')
    o('                    if (mobM.find()) {')
    o('                        translated = mobM.replaceAll(mobRule.replacement);')
    o('                    }')
    o('                }')
    o('                return translated;')
    o('            }')
    o('        }')
    o('        return message;')
    o('    }')
    o('}')
