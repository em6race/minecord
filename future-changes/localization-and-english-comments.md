# Future Feature: Multi-Language System (UK/EN/SK) & English Code Comments

> **Status:** Planned / In Progress  
> **Description:** Architectural plan for implementing multi-language support (Ukrainian, English, Slovak) and standardizing codebase comments in English.

---

## 1. Overview & Goals

1. **Multi-Language Support (UK, EN, SK)**:
   - Provide clean localization for all plugin messages, in-game broadcasts, titles, action bars, BlueMap links, Discord embeds, and slash commands.
   - Store messages in human-editable YAML files in `plugins/MineCord/languages/`:
     - `languages/uk.yml` (Ukrainian)
     - `languages/en.yml` (English)
     - `languages/sk.yml` (Slovak / Slovenčina)
   - Allow server administrators to switch the entire plugin's language with a single config option:
     ```yaml
     # Options: 'uk' (Ukrainian), 'en' (English), or 'sk' (Slovak)
     language: "uk"
     ```

2. **Smart Dictionary / Asset Handling**:
   - For all non-English languages, death messages, entity names, and advancement titles are generated/mapped from the official Minecraft assets repository:
     - Repository: [InventivetalentDev/minecraft-assets: Extracted Minecraft Assets](https://github.com/InventivetalentDev/minecraft-assets/)
   - In Ukrainian mode (`uk`):
     - Uses `DeathTranslator` and `AdvancementTranslator` built from [`assets/minecraft/lang/uk_ua.json`](https://github.com/InventivetalentDev/minecraft-assets/blob/master/assets/minecraft/lang/uk_ua.json).
   - In Slovak mode (`sk`):
     - Uses corresponding translation rules/dictionaries mapped from [`assets/minecraft/lang/sk_sk.json`](https://github.com/InventivetalentDev/minecraft-assets/blob/master/assets/minecraft/lang/sk_sk.json).
   - In English mode (`en`):
     - Bypasses regex pattern matching and dictionary lookup entirely.
     - Uses native Minecraft English messages (e.g. `"<Player> was slain by Zombie"`) and native advancement titles directly, reducing CPU overhead.

3. **Standardize Code Comments in English** *(Completed)*:
   - Refactored all inline comments, javadocs, and log explanations across all Java files to professional, concise English.

---

## 2. Architecture

```mermaid
graph TD
    Config["config.yml (language: 'uk' | 'en' | 'sk')"] --> LangMgr["LanguageManager"]
    LangMgr --> LangFiles["languages/uk.yml, en.yml, sk.yml"]
    
    LangMgr --> Listeners["Chat, Event, Sleep, Command Listeners"]
    
    PlayerDeathEvent --> CheckLang{"language == 'en'?"}
    CheckLang -->|Yes| NativeEN["Native Minecraft Death Message (Bypass Translator)"]
    CheckLang -->|No (uk)| DictUK["DeathTranslator (uk_ua.json)"]
    CheckLang -->|No (sk)| DictSK["DeathTranslator (sk_sk.json)"]

    PlayerAdvancementEvent --> CheckLangAdv{"language == 'en'?"}
    CheckLangAdv -->|Yes| NativeTitle["Native Display Title"]
    CheckLangAdv -->|No (uk)| DictAdvUK["AdvancementTranslator (uk_ua.json)"]
    CheckLangAdv -->|No (sk)| DictAdvSK["AdvancementTranslator (sk_sk.json)"]
```

---

## 3. Implementation Steps

### Step 1: Create `LanguageManager.java`
* Path: `src/main/java/com/example/minecord/utils/LanguageManager.java`
* Responsibilities:
  * Extract default `languages/uk.yml`, `languages/en.yml`, and `languages/sk.yml` from jar resources if missing.
  * Load the active language file using Bukkit `YamlConfiguration`.
  * Provide helper methods:
    * `get(String key, Object... placeholders)` - returns colored string (`&` translated).
    * `getRaw(String key, Object... placeholders)` - returns plain string (for Discord embeds).
    * `getLanguage()` - returns active language code (`uk`, `en`, `sk`).
  * Fallback to default values (English or Ukrainian) if a custom key is missing.

### Step 2: Language YAML Files
* Paths:
  * `src/main/resources/languages/uk.yml`
  * `src/main/resources/languages/en.yml`
  * `src/main/resources/languages/sk.yml`
* Sections:
  * `chat`: anti-spam warnings, mutes, mentions.
  * `events`: join, quit, first join, death notifications, coordinates format, BlueMap link text, dimension names.
  * `sleep`: in-game night skip broadcasts, sleeper count announcements, Discord embeds.
  * `discord`: slash command responses (`/online`, `/stats`, `/tps`, `/map`, `/link`, `/maintenance`, `/autorestart`, `/queuerestart`).
  * `tickets`: ticket creation responses and Discord embed format.
  * `mail`: sent, read, and inbox notifications.
  * `tab`: header and footer format.

### Step 3: Integrate with Listeners & Managers
* `MineCord.java`: Register `LanguageManager` during `onEnable()`, reload in `reloadPlugin()`.
* `PlayerEventListener.java`:
  * Use `cleanMessage` directly if English, `DeathTranslator` if Ukrainian or Slovak.
  * Use native title if English, `AdvancementTranslator` if Ukrainian or Slovak.
  * Localize coordinates messages and dimension names according to active language.
* Note: For non-English languages, translations are derived from [InventivetalentDev/minecraft-assets](https://github.com/InventivetalentDev/minecraft-assets/) (e.g. `uk_ua.json`, `sk_sk.json`).
* `SleepManager.java`: Replace hardcoded strings with `languageManager.get(...)`.
* `ChatListener.java`, `MineCordCommand.java`, `DiscordCommandListener.java`, `DiscordTicketListener.java`: Replace hardcoded strings with language keys.

### Step 4: Translate Comments to English
* Status: **Done** (All Java source files have had their comments translated to English).

---

## 4. How to Activate When Needed
To implement this feature in the future, follow the steps outlined above, populate `uk.yml`, `en.yml`, and `sk.yml`, test locally with `./gradlew build`, and verify language switching between `uk`, `en`, and `sk` in `config.yml`.

