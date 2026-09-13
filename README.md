# MineCord 🎮💬

<p align="center">
  <img src="https://img.shields.io/badge/Lines_of_Code-6.5k-blue?style=flat-square&logo=java&logoColor=white" alt="Lines of Code">
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur%2026.2%2B-orange?style=flat-square&logo=minecraft&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License">
</p>

<p align="center">
  <b>Advanced Minecraft & Discord Integration Plugin (Paper / Purpur 26.2+)</b><br>
  <a href="#-українська">🇺🇦 Українська версія</a> • <a href="#-english">🇬🇧 English version</a>
</p>

---

# 🇺🇦 Українська

**MineCord** — багатофункціональний серверний плагін для Minecraft (Paper / Purpur 26.2+), який забезпечує безшовну двосторонню інтеграцію сервера з Discord, автоматизує адміністрування, покращує взаємодію гравців та розширює стандартні ігрові механіки.

## 🚀 Основний функціонал

### 1. 💬 Двосторонній чат нового покоління (Minecraft ⇄ Discord)
* **Discord Webhooks**: Повідомлення з гри відправляються в Discord від імені конкретного гравця з його реальною 3D-аватаркою.
* **Підтримка текстур та скінів**: Аватарки формуються безпосередньо з текстури гравця, завдяки чому кастомні скіни відображаються коректно для всіх гравців.
* **Smart Mentions (Розумні згадки)**:
  * Ввівши `@Нікнейм` у Minecraft, адресат отримує підсвічування тексту та звуковий сигнал привернення уваги.
  * Якщо гравець прив'язав свій Discord-акаунт, у чаті Discord повідомлення автоматично перетворюється на реальний Discord-пінг (`<@id>`).
* **Discord ➔ Minecraft**: Повідомлення з каналу Discord миттєво транслюються в ігровий чат із підтримкою кольорів ролей та зрозумілим форматуванням.

### 2. 🩸 Хардкорний «Кривавий Місяць» (Bloodmoon) — 4 Рівні Складності
Повномасштабна система нічних катаклізмів з унікальними мобами, окупацією неба та грандіозними босами:
* **Блокування сну**: Сон у ліжках повністю заборонено до настання світанку.
* **4 Хардкорні Рівні (Tiers)**:
  * **Рівень 1 («Кривавий Місяць»)**: Множник HP 2.5x, бафи Швидкості та Сили, 70% діамантової броні, бос **«Кривавий Жнець»** (300 HP).
  * **Рівень 2 («Кривавий Армагеддон»)**: Множник HP 4.5x, 85% діамантової/незеритової броні, заряджені кріпери, бос **«Володар Безодні»** (550 HP).
  * **Рівень 3 («Пекельний Катаклізм»)**: Множник HP 6.5x, 95% зачарованого незериту, бос **«☠ Архідемон Смерті ☠»** (850 HP).
  * **Рівень 4 («☠ Судний День (Раґнарок) ☠»)**: Екстремальний множник HP 10.0x, Сила IV, Опір II, 100% максимальний незерит, орди до 36 монстрів та фінальний супербос **«☠ ТИТАН ХАОСУ ☠»** (1400 HP).
* **✈️ Фантоми-Камікадзе (Phantom Bombers)**:
  * У повітрі з'являються фантоми з кріперами-наїзниками на голові/спині.
  * Пікірують прямо на гравців; на відстані 4.5 м кріпер запалюється зі звуком шипіння та попередженням в Actionbar, а при зіткненні (≤ 2.2 м або ударі фантома) детонує миттєво.
  * Якщо збити фантома стрілою, активований кріпер скидається вниз як падаюча авіабомба.
  * На вищих рівнях розмір фантомів сягає 7 (гіганти), кріпери стають зарядженими/термоядерними з радіусом вибуху до 6.0 та шлейфом вогню душ.
* **🛡️ Анти-абуз та захист від ферм**:
  * **0% шанс дропу броні й зброї**: Незерит і мечі слугують виключно для бою і не засмічують економіку.
  * **Захист від автоферм**: Спавнери, яйця призову, підкріплення зомбі, а також чешуйниці, ендерміти та слайми виключені з івенту.
  * **Лут тільки за чесний бій**: Цінні ресурси (діаманти, незерит, тотеми, золоті блоки, яблука Нотча) випадають **лише** при вбивстві моба гравцем вручну.
  * **Захист від багів зілля кріперів**: Запобігання виникненню нескінченних хмар ефектів `AreaEffectCloud`.
* **📢 Трансляція в Discord**:
  * Окремі барвисті Embed-сповіщення при сходженні Кривавого Місяця та тріумфальні анонси перемоги над босами.

### 3. 🛡️ Безпека та AI-модерація чату
* **AntiSpam система**:
  * Захист від флуду та повторюваних однакових повідомлень.
  * Багаторівнева система покарань: попередження, блокування спам-повідомлень, автоматичний тимчасовий мут на 5 хвилин.
* **Розумна AI-модерація (OpenAI / ChatGPT)**:
  * Асинхронна перевірка підозрілих повідомлень на токсичність та образливий зміст без затримок ігрового процесу (zero input-lag).
  * Автоматичне сповіщення адміністрації в закритий Discord-канал модераторів із зазначенням порушника та цитатою.

### 4. 🌙 Розумний пропуск ночі (Sleep Voting)
* **Динамічне голосування сном**: Для звичайного пропуску ночі достатньо, щоб у ліжка лягли 50% активних гравців світу.
* **Фільтрація неактивних**: Система автоматично ігнорує гравців у режимі AFK та глядачів (Spectator mode).
* **Інформативні сповіщення**:
  * Повідомлення в ігровий чат про кількість гравців, які сплять.
  * Стильне Embed-повідомлення в Discord ("☀️ Гравець пропустив ніч. Доброго ранку!").
  * Автоматичне скидання шторму та дощу.

### 5. 🌐 Локалізація та ігрові події
* **Повна локалізація смертей**: Вбудований перекладач адаптує понад 100 офіційних причин смерті (з урахуванням мобів, снарядів та падінь) і відправляє їх у Discord українською мовою.
* **Локалізація здобутків**: Сповіщення про виконання досягнень транслюються в Discord з їхніми офіційними назвами.
* **Інтерактивні координати смерті**: Гравець отримує приватне повідомлення з координатами своєї загибелі та прямим клікабельним посиланням на веб-карту (BlueMap).

### 6. 🔄 Розумний планувальник авторестартів
* **Регулярні рестарти**: Автоматичний перезапуск сервера за заданим розкладом у зручний час.
* **Візуальні та звукові попередження**:
  * Чат-сповіщення зі зворотним відліком.
  * Спливаючі титри на екрані (Titles & Subtitles) перед рестартом.
  * Можливість для окремих гравців приховати набридливі повідомлення командою `/togglerestart`.
* **Відкладений розумний рестарт (`/queuerestart`)**: Одноразовий рестарт, який ставиться в чергу через Discord і спрацьовує автоматично, щойно на сервері буде 0 онлайну. Працює як перемикач (toggle).

### 7. 🎫 Система тікетів та підтримки
* **Тікети прямо з гри**: Гравці можуть надіслати запит чи скаргу адміністрації командою `/ticket create <текст>`.
* **Інтерактивні картки в Discord**: Кожен тікет з'являється в каналі адміністрації з кнопками керування (закрити, взяти в роботу).

### 8. 📬 Офлайн-пошта (Mail System)
* Можливість відправляти приватні повідомлення офлайн-гравцям (`/mail send <нік> <текст>`).
* При наступному вході гравець отримує сповіщення про нові листи та може їх прочитати (`/mail read`).

### 9. 🔗 Прив'язка акаунтів (Account Link) & Рольовий Whitelist
* **Верифікація акаунтів**: Генерація одноразових кодів для зв'язування Minecraft-акаунта з профілем Discord (`/discord link` $\rightarrow$ `/link <code>`).
* **Рольовий доступ**: Можливість автоматичної видачі ролі на Discord-сервері та обмеження входу на сервер.

### 10. 💻 Віддалена консоль в Discord
* Закритий канал для адміністраторів із живою трансляцією серверних логів.
* Можливість виконувати будь-які серверні команди прямо з текстового поля Discord.

### 11. 📊 Моніторинг та оптимізація
* **AFK-система та захист від мобів**: Відстеження неактивності з відображенням таймера над головою, статусом `[АФК]` у табі та повною невразливістю до атак і снарядів мобів під час AFK.
* **Стильний TabList**: Інформативний таб із відображенням пінгів, онлайну, TPS та привітання сервера.
* **Команди статистики та топів**: Перегляд ігрового часу, смертей, блоків та досягнень (`/stats`, `/top`).
* **Режим технічних робіт (`/maintenance`)**: Швидке закриття сервера на обслуговування з персоналізованим повідомленням про причину робіт.
* **Автооновлення BlueMap**: Періодичне виконання `/bluemap reload` для автоматичної актуалізації веб-карти.
* **Поради та факти при вході**: З невеликим шансом (за замовчуванням 20%) гравець при підключенні отримує персональну корисну пораду.
* **Діагностика бота**: Команди `/minecord bot status` та `/minecord bot reconnect` для контролю підключення бота без перезапуску сервера.

---

## 🕹️ Команди плагіна (UA)

### Команди в Minecraft:
| Команда | Опис | Доступ |
| :--- | :--- | :--- |
| `/minecord bloodmoon start [1-4]` | Примусово запустити Кривавий Місяць обраного рівня (1–4) | `minecord.admin` |
| `/minecord bloodmoon stop` | Зупинити поточний Кривавий Місяць | `minecord.admin` |
| `/minecord bloodmoon status` | Переглянути активний рівень, кількість убитих монстрів, хвиль та час | `minecord.admin` |
| `/minecord bloodmoon cleareffects [гравець\|@a]` | Очистити аномальні ефекти зілля та шкідливі хмари | `minecord.admin` |
| `/minecord bot status` | Діагностика стану підключення Discord-бота (Gateway, пінг, сервери) | `minecord.admin` |
| `/minecord bot reconnect` | Перепідключити з'єднання Discord-бота без перезавантаження сервера | `minecord.admin` |
| `/minecord reload` | Перезавантажити конфігурацію плагіна `config.yml` | `minecord.admin` |
| `/unmute <гравець>` | Зняти мут від антиспаму з гравця | `minecord.admin` |
| `/discord [link]` | Отримати посилання на Discord-сервер або згенерувати код прив'язки | Усі |
| `/map` | Отримати посилання на інтерактивну онлайн-карту сервера | Усі |
| `/stats [гравець]` | Переглянути статистику (час гри, кількість смертей, блоки тощо) | Усі |
| `/top [категорія]` | Топ-10 гравців за статистикою (час, моби, смерті, алмази, блоки) | Усі |
| `/sharecoords [X Y Z] <опис>` | Поділитися координатами з описом та прямим лінком на веб-мапу | Усі |
| `/report <гравець> <причина>` | Надіслати скаргу на гравця модераторам у Discord | Усі |
| `/ticket create <текст>` | Створити тікет до адміністрації сервера | Усі |
| `/mail send <гравець> <текст>` | Надіслати офлайн-повідомлення гравцю | Усі |
| `/mail read` | Прочитати отримані вхідні повідомлення | Усі |
| `/togglerestart` | Увімкнути/вимкнути для себе сповіщення про авторестарт | Усі |

### Slash-команди в Discord:
| Команда | Опис | Доступ |
| :--- | :--- | :--- |
| `/help` | Список усіх доступних команд Discord-бота | Усі |
| `/online` | Список гравців, що знаходяться онлайн на сервері | Усі |
| `/stats [гравець]` | Перегляд ігрової статистики вказаного або власного акаунта | Усі |
| `/top [категорія]` | Топ-10 гравців сервера (загальний, час, дистанція, вбивства, смерті, діаманти, блоки) | Усі |
| `/serverinfo` | Стан сервера: поточний TPS, онлайн та використання пам'яті (RAM) | Усі |
| `/map` | Посилання на інтерактивну веб-карту BlueMap | Усі |
| `/link [code]` | Прив'язати свій Discord-акаунт до гри за кодом (або переглянути інструкцію) | Усі |
| `/queuerestart` | Поставити або скасувати одноразовий рестарт у чергу (коли онлайн = 0) | Адміністратори |
| `/autorestart <add\|remove\|list\|clear\|toggle>` | Керування розкладом автоматичних регулярних рестартів | Адміністратори |
| `/maintenance <on/off>` | Увімкнути чи вимкнути режим технічних робіт | Адміністратори |
| `/linkadmin <гравець> <user>` | Примусово зв'язати профіль гравця з користувачем Discord | Адміністратори |
| `/links` | Переглянути список усіх прив'язаних акаунтів сервера | Адміністратори |

---

<br>

# 🇬🇧 English

**MineCord** is a feature-rich, high-performance Minecraft server plugin for Paper / Purpur (26.2+) that provides seamless two-way Discord integration, automates server administration, enhances player communication, and extends core gameplay mechanics.

## 🚀 Key Features

### 1. 💬 Next-Gen Two-Way Cross-Chat (Minecraft ⇄ Discord)
* **Discord Webhooks**: In-game player messages are sent to Discord via webhooks displaying the sender's actual username and their real 3D head avatar.
* **Skin Texture Resolution**: Avatars are extracted directly from player texture hashes, ensuring custom skins render accurately for both online and offline mode players.
* **Smart Mentions**:
  * Typing `@PlayerName` in Minecraft highlights the name in yellow and plays an audio ping for the recipient.
  * If the player linked their Discord account, the mention automatically transforms into an authentic Discord ping (`<@id>`).
* **Discord ➔ Minecraft**: Messages from Discord channels stream into Minecraft chat with role colors and clean formatting.

### 2. 🩸 Hardcore Bloodmoon Event — 4 Difficulty Tiers
A comprehensive nocturnal apocalypse featuring dangerous hordes, aerial bombers, and epic boss battles:
* **Bed Sleep Blocked**: Sleeping in beds is completely locked out until sunrise.
* **4 Scaled Tiers**:
  * **Tier 1 ("Bloodmoon")**: 2.5x mob HP multiplier, Speed & Strength buffs, 70% diamond gear, boss **"Blood Reaper"** (300 HP).
  * **Tier 2 ("Blood Armageddon")**: 4.5x mob HP multiplier, 85% diamond/netherite gear, charged creepers, boss **"Abyssal Lord"** (550 HP).
  * **Tier 3 ("Infernal Cataclysm")**: 6.5x mob HP multiplier, 95% full enchanted netherite, boss **"☠ Archdemon of Death ☠"** (850 HP).
  * **Tier 4 ("☠ Doomsday (Ragnarok) ☠")**: Extreme 10.0x mob HP multiplier, Strength IV, Resistance II, 100% max netherite, massive hordes up to 36 monsters, and the final colossal boss **"☠ TITAN OF CHAOS ☠"** (1400 HP).
* **✈️ Phantom Kamikaze Bombers**:
  * Phantoms fly into battle carrying Creepers on their backs/heads.
  * Actively dive-bomb survival players; ignites at 4.5m with warning hiss and Actionbar alert, and detonates instantly upon collision (≤ 2.2m or bite strike).
  * Shooting down the Phantom drops the ignited Creeper as a ticking falling aerial bomb.
  * Higher tiers feature giant Phantoms (size up to 7), nuclear charged Creepers with explosion radii up to 6.0, and soul flame particle trails.
* **🛡️ Anti-Exploit & Farm Protection**:
  * **0% Mob Equipment Drop Chance**: Netherite armor and enchanted weapons are strictly for mob defense and do not pollute the player economy.
  * **Anti-Mob-Farm Isolation**: Spawners, spawn eggs, zombie reinforcements, silverfish, endermites, and slimes are completely ignored by the event.
  * **Fair Fight Loot**: Custom resource bundles (diamonds, netherite ingots/scraps, Notch apples, totems) drop **exclusively** when killed manually by a player.
  * **AreaEffectCloud Cleanup**: Creepers cannot produce permanent lingering potion clouds upon detonation.
* **📢 Discord Broadcasts**:
  * Rich start embeds announcing threats and rewards, with triumphant global defeat announcements when bosses fall.

### 3. 🛡️ Chat Security & AI Moderation
* **AntiSpam Engine**:
  * Protects against rapid flood and duplicate identical messages.
  * Multi-tiered penalty ladder: gentle warnings, message drops, and automated 5-minute mutes for persistent spamming.
* **OpenAI (ChatGPT) Moderation**:
  * Asynchronously evaluates suspicious chat messages for toxicity and harassment with zero server tick lag.
  * Instantly notifies server staff in a designated Discord moderation channel with context and author info.

### 4. 🌙 Smart Sleep Voting
* **Dynamic Percentage Threshold**: Regular night skips automatically when 50% of active players sleep in beds.
* **AFK & Spectator Exclusion**: Players in AFK status or Spectator mode are ignored, preventing idle players from blocking morning.
* **Rich Broadcasts**:
  * In-game messages showing current sleeper count and needed sleepers.
  * Discord embed notification ("☀️ Player skipped the night. Good morning!").
  * Automatically resets bad weather (rain & thunder).

### 5. 🌐 Event Localization & Navigation
* **Death Messages**: Translates over 100 official death causes (mobs, projectiles, falls, voids) and relays them to Discord in clean Ukrainian format.
* **Advancement Announcements**: Relays earned achievements to Discord with native names.
* **Interactive Death Coordinates**: Players receive a private message with their death coordinates and an interactive clickable BlueMap link that opens the exact death spot in their browser.

### 6. 🔄 Intelligent Auto-Restart Scheduler
* **Scheduled Restarts**: Automatically reboots the server at configured times.
* **Audio-Visual Warnings**:
  * Countdown notices in chat.
  * On-screen Titles & Subtitles before rebooting.
  * Players can toggle countdown visibility for themselves via `/togglerestart`.
* **Queued Restarts (`/queuerestart`)**: Queue a one-time restart via Discord that triggers automatically once the server is empty (0 online players). Operates as a toggle.

### 7. 🎫 In-Game Ticket Support System
* **Tickets from Minecraft**: Players can submit reports or help requests using `/ticket create <message>`.
* **Interactive Discord Embeds**: Staff receives the ticket with action buttons (claim, close) right inside Discord.

### 8. 📬 Offline Mail System
* Send messages to offline players via `/mail send <player> <message>`.
* Recipients are greeted with unread notifications upon their next login and can review them via `/mail read`.

### 9. 🔗 Account Linking & Role-Based Whitelist
* **Account Verification**: Generates one-time codes in Minecraft for linking accounts via Discord slash command `/discord link` $\rightarrow$ `/link <code>`.
* **Role-Gated Whitelist**: Automatically assigns verified roles on Discord and can require specific roles to enter the Minecraft server.

### 10. 💻 Remote Discord Console
* Dedicated private Discord channel mirroring server console logs in real time.
* Execute any server console command directly by typing it in the Discord channel.

### 11. 📊 Monitoring & Polish
* **AFK Detection & Mob Invulnerability**: Inactivity tracking with 3D overhead timer, `[AFK]` status, and complete invulnerability against hostile mob attacks and projectiles while AFK.
* **Custom TabList**: Clean header and footer showing online count, TPS, ping, and server branding.
* **Stats Command**: Inspect player playtime, death count, broken blocks, and achievements (`/stats`, `/top`).
* **Maintenance Mode (`/maintenance`)**: Close the server to non-staff players with a custom kick screen.
* **BlueMap Auto-Reload**: Periodic execution of `/bluemap reload` to keep web maps constantly refreshed.
* **Join Tips & Facts**: With a configurable chance (default 20%), connecting players receive a private, helpful tip or fun fact about server features.
* **Bot Diagnostics**: `/minecord bot status` and `/minecord bot reconnect` commands for managing Discord bot connectivity on the fly.

---

## 🕹️ Plugin Commands (EN)

### In-Game Commands:
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/minecord bloodmoon start [1-4]` | Force-start Bloodmoon of specified tier (1–4) | `minecord.admin` |
| `/minecord bloodmoon stop` | Force-stop active Bloodmoon event | `minecord.admin` |
| `/minecord bloodmoon status` | View active tier, killed mobs, spawned hordes, and time remaining | `minecord.admin` |
| `/minecord bloodmoon cleareffects [player\|@a]` | Remove anomalous lingering potion clouds and mob potion effects | `minecord.admin` |
| `/minecord bot status` | Check Discord Gateway status, ping, and connected guilds | `minecord.admin` |
| `/minecord bot reconnect` | Reconnect Discord bot session without restarting the server | `minecord.admin` |
| `/minecord reload` | Reload plugin configuration files (`config.yml`) | `minecord.admin` |
| `/unmute <player>` | Unmute player muted by anti-spam | `minecord.admin` |
| `/discord [link]` | Get Discord invite link or generate account linking code | Everyone |
| `/map` | Get link to the server's interactive web map | Everyone |
| `/stats [player]` | View player game statistics (playtime, deaths, blocks, etc.) | Everyone |
| `/top [category]` | View Top-10 leaderboards (time, mobs, deaths, diamonds, blocks) | Everyone |
| `/sharecoords [X Y Z] <desc>` | Share coordinates with description and direct BlueMap link | Everyone |
| `/report <player> <reason>` | Report a player to staff on Discord | Everyone |
| `/ticket create <text>` | Submit a support ticket to server administrators | Everyone |
| `/mail send <player> <text>` | Send an offline message to a player | Everyone |
| `/mail read` | View received offline mail | Everyone |
| `/togglerestart` | Toggle auto-restart countdown messages on or off | Everyone |

### Discord Slash Commands:
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/help` | Display list of all available Discord bot commands | Everyone |
| `/online` | Display list of online players | Everyone |
| `/stats [player]` | Inspect in-game statistics for yourself or another player | Everyone |
| `/top [category]` | Top-10 leaderboards (overall, time, distance, kills, deaths, diamonds, blocks) | Everyone |
| `/serverinfo` | View server health: current TPS, memory (RAM) usage, and uptime | Everyone |
| `/map` | Post the server BlueMap web map link | Everyone |
| `/link [code]` | Link Discord account to Minecraft player profile | Everyone |
| `/queuerestart` | Toggle/queue a one-time restart when online reaches 0 | Administrators |
| `/autorestart <add\|remove\|list\|clear\|toggle>` | Manage scheduled server restarts | Administrators |
| `/maintenance <on/off>` | Toggle maintenance mode | Administrators |
| `/linkadmin <player> <user>` | Force-link a player's profile to a Discord account | Administrators |
| `/links` | List all linked Minecraft ⇄ Discord accounts | Administrators |

---

## ⚙️ Installation & Setup / Встановлення

1. Download the latest `MineCord-1.0-26.2-all.jar` from GitHub **Actions** (latest successful build).
2. Place the jar file in your server's `plugins/` directory.
3. Start or restart the server to generate `plugins/MineCord/config.yml`.
4. Configure `config.yml`:
   * `discord.token` — Discord Bot Token;
   * `discord.chat-channel-id` — Channel ID for cross-chat;
   * `discord.console-channel-id` — Private channel ID for remote console;
   * `discord.webhook-url` — Discord webhook URL for player avatars;
5. Run `/minecord reload` or restart your server.

---

## 🛠️ Requirements / Вимоги
* **Server Software**: Paper, Purpur, or compatible forks (26.2+).
* **Java Runtime**: Java 21 or newer (fully supports Java 25).
* **Build System**: Gradle 8.x + ShadowJar.
