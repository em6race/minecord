# MineCord 🎮💬

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

### 2. 🛡️ Безпека та AI-модерація чату
* **AntiSpam система**:
  * Захист від флуду та повторюваних однакових повідомлень.
  * Багаторівнева система покарань: попередження, блокування спам-повідомлень, автоматичний тимчасовий мут на 5 хвилин.
* **Розумна AI-модерація (OpenAI / ChatGPT)**:
  * Асинхронна перевірка підозрілих повідомлень на токсичність та образливий зміст без затримок ігрового процесу (zero input-lag).
  * Автоматичне сповіщення адміністрації в закритий Discord-канал модераторів із зазначенням порушника та цитатою.

### 3. 🌙 Розумний пропуск ночі (Sleep Voting)
* **Динамічне голосування сном**: Для пропуску ночі достатньо, щоб у ліжка лягли 50% активних гравців світу.
* **Фільтрація неактивних**: Система автоматично ігнорує гравців у режимі AFK та глядачів (Spectator mode), тому відсутні на місці гравці не заважають пропускати ніч.
* **Інформативні сповіщення**:
  * Повідомлення в ігровий чат про кількість гравців, які сплять, та скільки ще потрібно.
  * Стильне Embed-повідомлення в Discord ("☀️ Гравець пропустив ніч. Доброго ранку!").
  * Автоматичне очищення погоди (скидання дощу та грози).

### 4. 🌐 Локалізація та ігрові події
* **Локалізація смертей**: Вбудований перекладач адаптує понад 100 офіційних причин смерті (з урахуванням мобів, снарядів та падінь) і відправляє їх у Discord українською мовою.
* **Локалізація здобутків**: Сповіщення про виконання досягнень транслюються в Discord з їхніми офіційними назвами.
* **Інтерактивні координати смерті**: Гравець отримує приватне повідомлення з координатами своєї загибелі та прямим клікабельним посиланням на карту (BlueMap), що відкриває точне місце смерті в браузері.

### 5. 🔄 Розумний планувальник авторестартів
* **Регулярні рестарти**: Автоматичний перезапуск сервера за заданим розкладом у зручний час.
* **Візуальні та звукові попередження**:
  * Чат-сповіщення з зворотним відліком.
  * Спливаючі титри на екрані (Titles & Subtitles) перед рестартом.
  * Можливість для окремих гравців приховати набридливі повідомлення командою `/togglerestart`.
* **Відкладений розумний рестарт (`/queuerestart`)**: Одноразовий рестарт, який ставиться в чергу через Discord і спрацьовує автоматично, щойно на сервері буде 0 онлайну (всі гравці вийдуть). Працює як перемикач: не зберігається в конфіг, виконується лише 1 раз, а повторне введення команди скасовує чергу.

### 6. 🎫 Система тікетів та підтримки
* **Тікети прямо з гри**: Гравці можуть надіслати запит чи скаргу адміністрації командою `/ticket create <текст>`.
* **Інтерактивні картки в Discord**: Кожен тікет з'являється в каналі адміністрації з кнопками керування (закрити, взяти в роботу).

### 7. 📬 Офлайн-пошта (Mail System)
* Можливість відправляти приватні повідомлення офлайн-гравцям (`/mail send <нік> <текст>`).
* При наступному вході гравець отримує сповіщення про нові листи та може їх прочитати.

### 8. 🔗 Прив'язка акаунтів (Account Link) & Рольовий Whitelist
* **Верифікація акаунтів**: Генерація одноразових кодів для зв'язування Minecraft-акаунта з профілем Discord.
* **Рольовий доступ**: Можливість автоматичної видачі ролі на Discord-сервері та обмеження входу на сервер (грати можуть лише учасники з певною роллю в Discord).

### 9. 💻 Віддалена консоль в Discord
* Закритий канал для розробників та адміністраторів із живою трансляцією серверних логів.
* Можливість виконувати будь-які серверні команди прямо з текстового поля Discord без необхідності відкривати панель хостингу.

### 10. 📊 Моніторинг та оптимізація
* **AFK-система та захист від мобів**: Відстеження неактивності з відображенням таймера над головою, статусом `[АФК]` у табі та повною невразливістю до атак і снарядів мобів під час AFK.
* **Стильний TabList**: Інформативний таб із відображенням пінгів, онлайну, TPS та привітання сервера.
* **Команда статистики**: Перегляд ігрового часу, смертей та досягнень (`/stats`).
* **Режим технічних робіт (`/maintenance`)**: Швидке закриття сервера на обслуговування з персоналізованим повідомленням про причину робіт.
* **Автооновлення BlueMap**: Періодичне виконання `/bluemap reload` (за замовчуванням кожні 2 години) для автоматичної актуалізації веб-карти.
* **Поради та факти при вході**: З невеликим шансом (за замовчуванням 20%) гравець при підключенні отримує персональну корисну пораду про можливості сервера (мапа, пошта, тікети, статистика тощо).
* **Sentry Error Tracking**: Вбудоване логування винятків для миттєвого виявлення та налагодження помилок.

---

## 🕹️ Команди плагіна (UA)

### Команди в Minecraft:
| Команда | Опис |
| :--- | :--- |
| `/discord [link]` | Отримати посилання на Discord-сервер або згенерувати код прив'язки |
| `/minecord reload` | Перезавантажити конфігурацію плагіна (`minecord.admin`) |
| `/map` | Отримати посилання на інтерактивну онлайн-карту сервера |
| `/stats [гравець]` | Переглянути статистику (час гри, кількість смертей тощо) |
| `/ticket create <текст>` | Створити тікет до адміністрації сервера |
| `/mail send <гравець> <текст>` | Надіслати офлайн-повідомлення гравцю |
| `/mail read` | Прочитати отримані вхідні повідомлення |
| `/togglerestart` | Увімкнути/вимкнути для себе сповіщення про авторестарт |

### Slash-команди в Discord:
| Команда | Опис |
| :--- | :--- |
| `/online` | Список гравців, що знаходяться на сервері |
| `/stats <гравець>` | Перегляд ігрової статистики гравця |
| `/tps` | Стан сервера: поточний TPS, використання пам'яті (RAM) та аптайм |
| `/map` | Посилання на інтерактивну веб-карту |
| `/link <code>` | Прив'язати свій Discord-акаунт до ігрового профілю за кодом |
| `/maintenance <on/off>` | Керування режимом технічних робіт (доступно адміністраторам) |
| `/queuerestart` | Додати або скасувати одноразовий рестарт у чергу (коли онлайн = 0) |

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

### 2. 🛡️ Chat Security & AI Moderation
* **AntiSpam Engine**:
  * Protects against rapid flood and duplicate identical messages.
  * Multi-tiered penalty ladder: gentle warnings, message drops, and automated 5-minute mutes for persistent spamming.
* **OpenAI (ChatGPT) Moderation**:
  * Asynchronously evaluates suspicious chat messages for toxicity and harassment with zero server tick lag.
  * Instantly notifies server staff in a designated Discord moderation channel with context and author info.

### 3. 🌙 Smart Sleep Voting
* **Dynamic Percentage Threshold**: Night skips automatically when 50% of active players sleep in beds.
* **AFK & Spectator Exclusion**: Players in AFK status or Spectator mode are ignored, preventing idle players from blocking morning.
* **Rich Broadcasts**:
  * In-game messages showing current sleeper count and needed sleepers.
  * Discord embed notification ("☀️ Player skipped the night. Good morning!").
  * Automatically resets bad weather (rain & thunder).

### 4. 🌐 Event Localization & Navigation
* **Death Messages**: Translates over 100 official death causes (mobs, projectiles, falls, voids) and relays them to Discord in clean format.
* **Advancement Announcements**: Relays earned achievements to Discord with native names.
* **Interactive Death Coordinates**: Players receive a private message with their death coordinates and an interactive clickable BlueMap link that opens the exact death spot in their browser.

### 5. 🔄 Intelligent Auto-Restart Scheduler
* **Scheduled Restarts**: Automatically reboots the server at configured times.
* **Audio-Visual Warnings**:
  * Countdown notices in chat.
  * On-screen Titles & Subtitles before rebooting.
  * Players can toggle countdown visibility for themselves via `/togglerestart`.
* **Queued Restarts (`/queuerestart`)**: Queue a one-time restart via Discord that triggers automatically once the server is empty (0 online players). Operates as a toggle: does not persist across reboots, executes strictly once, and typing the command again cancels the queued restart.

### 6. 🎫 In-Game Ticket Support System
* **Tickets from Minecraft**: Players can submit reports or help requests using `/ticket create <message>`.
* **Interactive Discord Embeds**: Staff receives the ticket with action buttons (claim, close) right inside Discord.

### 7. 📬 Offline Mail System
* Send messages to offline players via `/mail send <player> <message>`.
* Recipients are greeted with unread notifications upon their next login and can review them via `/mail read`.

### 8. 🔗 Account Linking & Role-Based Whitelist
* **Account Verification**: Generates one-time codes in Minecraft for linking accounts via Discord slash command `/link <code>`.
* **Role-Gated Whitelist**: Automatically assigns verified roles on Discord and can require specific roles to enter the Minecraft server.

### 9. 💻 Remote Discord Console
* Dedicated private Discord channel mirroring server console logs in real time.
* Execute any server console command directly by typing it in the Discord channel.

### 10. 📊 Monitoring & Polish
* **AFK Detection & Mob Invulnerability**: Inactivity tracking with 3D overhead timer, `[AFK]` status, and complete invulnerability against hostile mob attacks and projectiles while AFK.
* **Custom TabList**: Clean header and footer showing online count, TPS, ping, and server branding.
* **Stats Command**: Inspect player playtime, death count, and achievements (`/stats`).
* **Maintenance Mode (`/maintenance`)**: Close the server to non-staff players with a custom kick screen.
* **BlueMap Auto-Reload**: Periodic execution of `/bluemap reload` (by default every 2 hours) to keep web maps constantly refreshed.
* **Join Tips & Facts**: With a configurable chance (default 20%), connecting players receive a private, helpful tip or fun fact about server features (/map, /mail, /ticket, /stats, etc.).
* **Sentry Error Tracking**: Built-in exception capture for instant diagnostics.

---

## 🕹️ Plugin Commands (EN)

### In-Game Commands:
| Command | Description |
| :--- | :--- |
| `/discord [link]` | Get Discord invite link or generate account linking code |
| `/minecord reload` | Reload plugin configuration files (`minecord.admin`) |
| `/map` | Get link to the server's interactive web map |
| `/stats [player]` | View player game statistics (playtime, deaths, etc.) |
| `/ticket create <text>` | Submit a support ticket to server administrators |
| `/mail send <player> <text>` | Send an offline message to a player |
| `/mail read` | View received offline mail |
| `/togglerestart` | Toggle auto-restart countdown messages on or off |

### Discord Slash Commands:
| Command | Description |
| :--- | :--- |
| `/online` | Display list of online players |
| `/stats <player>` | Inspect in-game statistics for a player |
| `/tps` | View server health: current TPS, memory (RAM) usage, and uptime |
| `/map` | Post the server web map link |
| `/link <code>` | Link Discord account to Minecraft player profile |
| `/maintenance <on/off>` | Toggle maintenance mode (Admin only) |
| `/queuerestart` | Toggle/queue a one-time restart when online reaches 0 |

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

