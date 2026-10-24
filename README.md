# MineCord 🎮💬

<p align="center">
  <img src="https://img.shields.io/badge/Lines_of_Code-14.2k-blue?style=flat-square&logo=java&logoColor=white" alt="Lines of Code">
  <img src="https://img.shields.io/badge/Platform-Paper%20%2F%20Purpur%2026.2%2B-orange?style=flat-square&logo=minecraft&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/License-MIT-green?style=flat-square" alt="License">
</p>

<p align="center">
  <b>Advanced Minecraft & Discord Integration Plugin (Paper / Purpur 26.2+)</b><br>
  <a href="#-українська">🇺🇦 Українська версія</a> • <a href="#-english">🇬🇧 English version</a>
</p>

---

# 🇺🇦 Українська

**MineCord** — високопродуктивний багатофункціональний серверний плагін для Minecraft (Paper / Purpur 26.2+), який забезпечує безшовну двосторонню інтеграцію сервера з Discord, автоматизує адміністрування, покращує взаємодію гравців та розширює стандартні ігрові механіки.

## 🚀 Основний функціонал

Для максимальної гнучкості MineCord розроблено за модульною архітектурою. Усі базові інтеграційні функції готові до роботи одразу після встановлення, а додаткові геймплейні та захисні модулі за замовчуванням вимкнені й легко активуються в `config.yml`.

### 📌 Загальний статус модулів

| Модуль / Функціонал | Статус за замовчуванням | Перемикач у `config.yml` | Призначення |
| :--- | :---: | :--- | :--- |
| **Двосторонній чат (Cross-Chat)** | ✅ **Увімкнено** | `discord.chat-channel-id` | Minecraft ⇄ Discord через Webhooks з 3D скінами та Smart Mentions |
| **Багатомовність та локалізація** | ✅ **Увімкнено** | `language: "en"`, `per-player-language: true` | English (дефолт), Українська, Slovenčina. Персональна мова клієнта, 100+ смертей, здобутки |
| **Офлайн-пошта (Mail System)** | ✅ **Увімкнено** | — (`/mail`) | Особисті повідомлення офлайн гравцям з доставкою в Discord |
| **Прив'язка акаунтів (Account Link)** | ✅ **Увімкнено** | — (`/discord link`) | Генерація кодів верифікації Minecraft ⇄ Discord |
| **Тікети та скарги (/ticket, /report)** | ✅ **Увімкнено** | `discord.moderator-channel-id` | Звернення гравців та скарги з інтерактивними кнопками в Discord |
| **Віддалена консоль (Remote Console)** | ✅ **Увімкнено** | `discord.console-channel-id` | Живий потік консолі та виконання команд прямо з Discord |
| **Статистика, топи та TabList** | ✅ **Увімкнено** | — (`/stats`, `/top`) | Асинхронний таб (TPS, пінг, аптайм), топи, `/sharecoords` з мапою |
| **Діагностика бота** | ✅ **Увімкнено** | — (`/minecord bot`) | Швидка перевірка зв'язку та гаряче перепідключення бота |
| **Авто-оновлення з GitHub** | ⚙️ *Вимкнено* | `auto-update.enabled: false` | Автоматичне стягування свіжих збірок із GitHub Releases у `plugins/update/` |
| **Кривавий Місяць (Bloodmoon Event)** | ⚙️ *Вимкнено* | `fun.modes.bloodmoon.enabled: false` | 4 збалансовані рівні, орди, камікадзе-фантоми, боси, збереження стану |
| **Action Bar HUD (Координати та День)** | ⚙️ *Вимкнено* | `hud.enabled: false` | Координати X Y Z, сторона світу, ігровий день та автоконверсія Незер ➔ Верхній світ (`/hud`) |
| **Тягун без дощу (Riptide without Rain)**| ⚙️ *Вимкнено* | `fun.modes.riptide_no_rain.enabled: false` | Ривки тризубцем на суші без дощу та води із захистом від падіння та PvP-фільтром |
| **Списи без голоду (Spear No Hunger)** | ⚙️ *Вимкнено* | `fun.modes.spear_no_hunger.enabled: false` | Усі види списів не витрачають очки голоду та виснаження |
| **Розумне ковадло (Smart Anvil)** | ⚙️ *Вимкнено* | `fun.modes.smart_anvil.enabled: false` | Зняття штучного ванільного ліміту 40 рівнів («Занадто дорого!») |
| **Розумний пропуск ночі (Sleep Voting)**| ⚙️ *Вимкнено* | `sleep.enabled: false` | Динамічний поріг 50% гравців у ліжках, ігнорування AFK |
| **AFK-система та захист від мобів** | ⚙️ *Вимкнено* | `afk.enabled: false` | Автоперехід через 60с, 3D-таймер, повна невразливість до мобів |
| **Синхронізація ролей (Role Sync)** | ⚙️ *Вимкнено* | `role-sync.enabled: false` | Префікси у табі й чаті, кольори ніків та сортування за рангом |
| **Захист від спаму (AntiSpam)** | ⚙️ *Вимкнено* | `antispam.enabled: false` | Захист від флуду, однакових повідомлень та автомут порушників |
| **AI-модерація чату** | ⚙️ *Вимкнено* | `ai-moderator.enabled: false` | Асинхронний аналіз токсичності через AI (OpenRouter / ChatGPT) |
| **Рольовий Whitelist** | ⚙️ *Вимкнено* | `whitelist.enabled: false` | Доступ на сервер лише верифікованим гравцям або за ролями Discord |
| **Режим технічних робіт** | ⚙️ *Вимкнено* | `maintenance.enabled: false` | Швидке закриття сервера для гравців (`/maintenance`) |
| **Авторестарти за розкладом** | ⚙️ *Вимкнено* | `autorestart.times: []` | Планові рестарти, титри на екрані, черга `/queuerestart` |
| **Автооновлення BlueMap** | ⚙️ *Вимкнено* | `bluemap.auto-reload.enabled: false` | Автоматичне оновлення веб-карти за графіком |
| **Технічний моніторинг та Sentry** | ⚙️ *Вимкнено* | `technical.*`, `sentry.enabled: false`| Сповіщення про падіння TPS, пам'яті RAM та відлов помилок |

---

### ✅ 1. Базові функції (Увімкнено за замовчуванням)

#### 💬 Двосторонній чат нового покоління (Minecraft ⇄ Discord)
* **Discord Webhooks**: Повідомлення з гри відправляються в Discord від імені конкретного гравця з його реальною 3D-аватаркою. Окремий фоновий пул потоків із таймаутами гарантує нульовий вплив на TPS.
* **Безпека згадок**: Автоматичний фільтр `allowed_mentions` запобігає експлойтам з пінгами `@everyone` та `@here`.
* **Підтримка текстур та скінів**: Аватарки формуються безпосередньо з текстури гравця, завдяки чому кастомні скіни відображаються коректно для всіх гравців.
* **Smart Mentions (Розумні згадки)**:
  * Ввівши `@Нікнейм` у Minecraft, адресат отримує підсвічування тексту та звуковий сигнал привернення уваги.
  * Якщо гравець прив'язав свій Discord-акаунт, у чаті Discord повідомлення автоматично перетворюється на реальний Discord-пінг (`<@id>`).
* **Discord ➔ Minecraft**: Повідомлення з каналу Discord миттєво транслюються в ігровий чат із підтримкою кольорів ролей та зрозумілим форматуванням.

#### 🌐 Багатомовність (Multi-Language) та персональна мова клієнта
MineCord має потужну багатомовну систему з можливістю гарячого перемикання мов без рестарту:
* **Мови «з коробки»**:
  * `en` — Англійська (English) — встановлена за замовчуванням у репозиторії;
  * `uk` — Українська (Ukrainian) — повний переклад смертей, досягнень, команд та повідомлень;
  * `sk` — Словацька (Slovenčina) — переклад усіх смертей, досягнень та Discord/чат сповіщень.
* **Персональна мова клієнта (`per-player-language: true`)**:
  Плагін автоматично зчитує локаль гри кожного гравця (`uk_ua` ➔ `uk`, `sk_sk` / `cs_cz` ➔ `sk`, `en_us` ➔ `en`). Кожен гравець бачить ігрові повідомлення, шапку/підвал TabList та попередження своєю рідною мовою, тоді як загальносерверні сповіщення, Discord-ембеди та консоль транслюються глобальною мовою (`language`).
* **Кастомізація та Fallback**:
  Усі тексти містяться у папці `plugins/MineCord/languages/` (`en.yml`, `uk.yml`, `sk.yml`). Якщо певний рядок відсутній у вибраній мові, плагін автоматично візьме оригінал з `en.yml`.
* **Повна локалізація смертей**: Вбудований перекладач адаптує понад 100 офіційних причин смерті (з урахуванням мобів, снарядів та падінь) і відправляє їх у чат та Discord.
* **Локалізація здобутків**: Сповіщення про виконання досягнень транслюються в Discord з їхніми назвами відповідною мовою.
* **Інтерактивні координати смерті**: Гравець отримує приватне повідомлення з координатами своєї загибелі та прямим посиланням на веб-карту (BlueMap).

#### 📬 Офлайн-пошта (Mail System)
* Можливість відправляти приватні повідомлення офлайн-гравцям (`/mail send <нік> <текст>`).
* Повідомлення надсилаються в приватні повідомлення Discord адресата через бота, а при вході гравець отримує додаткове нагадування.

#### 🔗 Прив'язка акаунтів (Account Link)
* **Верифікація акаунтів**: Генерація одноразових кодів із захистом від колізій та таймером дії для зв'язування Minecraft-акаунта з профілем Discord (`/discord link` $\rightarrow$ `/link <code>`).
* Дозволяє синхронізувати згадки Smart Mentions та пересилати сповіщення про пошту.

#### 🎫 Система тікетів та скарг
* **Тікети прямо з гри**: Гравці можуть надіслати запит чи пропозицію адміністрації командою `/ticket create <текст>`.
* **Скарги на порушників**: Швидке повідомлення модераторів через `/report <гравець> <причина>` із захистом від спаму.
* **Інтерактивні картки в Discord**: Кожен тікет чи скарга з'являється в закритому каналі адміністрації з кнопками керування (закрити, взяти в роботу).

#### 💻 Віддалена консоль в Discord
* Закритий канал для адміністраторів із живою асинхронною трансляцією серверних логів.
* Можливість виконувати будь-які серверні команди прямо з текстового поля Discord з екрануванням небезпечних символів.

#### 📊 Статистика, рейтинги та інфо-панелі
* **Стильний TabList**: Інформативний таб із відображенням пінгів, онлайну, стабільності TPS та часу безперервної роботи (Uptime).
* **Команди статистики та топів**: Асинхронний перегляд ігрового часу, смертей, добутих блоків та досягнень (`/stats`, `/top`).
* **Поширення координат (`/sharecoords`)**: Можливість поділитися з друзями точкою на місцевості з описом та прямим лінком на веб-мапу BlueMap.
* **Діагностика бота**: Команди `/minecord bot status` та `/minecord bot reconnect` для контролю підключення бота без перезапуску сервера.

---

### ⚙️ 2. Опціональні модулі (Вимкнено за замовчуванням — налаштовуються в config.yml)

#### 🩸 Хардкорний «Кривавий Місяць» (Bloodmoon) — 4 Рівні Складності
*Налаштування: `fun.modes.bloodmoon.enabled: false`*

Повномасштабна система нічних катаклізмів з унікальними мобами, окупацією неба та грандіозними босами:
* **Захист від порожнього сервера**: Кривавий Місяць не запускається і автоматично зупиняється, коли на сервері 0 гравців (`require_players_online: true`), запобігаючи зайвому навантаженню та спаму в Discord.
* **Блокування сну**: Сон у ліжках повністю заборонено до настання світанку (гравця виштовхує з ліжка зі звуковим ефектом).
* **Збереження стану при рестарті**: Якщо під час івенту відбувся рестарт сервера, активна фаза, таймер, відбиті хвилі та вбиті моби відновлюються з `bloodmoon_state.yml`.
* **Атмосферний візуал**: Червона віньєтка через персональний WorldBorder максимального масштабу створює ефект кривавого неба без фізичних обмежень пересування гравців.
* **4 Хардкорні Рівні та Збалансований Лут**:
  * **Рівень 1 («Кривавий Місяць»)**: Множник HP 1.6x, бафи Швидкості та Сили, 40% залізного спорядження, бос **«Кривавий Жнець»** (120 HP). Дроп: залізо, золото, алмази, 25% шанс на незеритовий скрап.
  * **Рівень 2 («Кривавий Армагеддон»)**: Множник HP 2.5x, 60% діамантового/залізного спорядження, заряджені кріпери, бос **«Володар Безодні»** (240 HP). Суворий ліміт: **максимум 2 незеритові скрапи** за ніч на гравця! Шанс з мобів — 0.3% (~1 на 333 вбивства), з боса — 60% шанс на 1 скрап (в межах ліміту), тотем (40%).
  * **Рівень 3 («Пекельний Катаклізм»)**: Множник HP 3.5x, 80% діамантового спорядження, бос **«☠ Архідемон Смерті ☠»** (380 HP, регенерація II як у Візера). Шанс скрапу з мобів — 0.8% (~1–2 за ніч). Бос дає 1–2 скрапи, 3–5 алмазів, 30% шанс на Зірку Незеру та 75% шанс на Тотем безсмертя.
  * **Рівень 4 («☠ Судний День (Раґнарок) ☠»)**: Множник HP 5.0x, Сила II, Швидкість II, 90% незеритового та діамантового захисту, орди до 24 монстрів та фінальний супербос **«☠ ТИТАН ХАОСУ ☠»** (550 HP, незерит, регенерація III, свита вартових). Шанс скрапу з мобів — 1.5% (~2–4 за ніч), 3.5% на алмази. Бос гарантовано дропає **Зірку Незеру, 2 Незеритові зливки, 2 Тотеми, 2 Зачарованих яблука Нотча та 8–16 Алмазів**!
* **🎲 5 Випадкових Архетипів Орд та Тактика Бою**:
  * **Хаотична засідка (Chaos Ambush)**: Збалансована хвиля зомбі, скелетів, швидких павуків, хасків та кріперів.
  * **Павуче нашестя (Spider Swarm)**: Рої печерних отруйних павуків та спринтерських звичайних павуків із бафами швидкості.
  * **Легіон немертвих (Undead Legion)**: Важкоброньовані зомбі в латах та влучні скелети-лучники з покращеними луками.
  * **Загін саперів (Sapper Squad)**: Група швидких кріперів-підривників під прикриттям стінки зомбі-авангарду.
  * **Пекельний авангард (Nether Vanguard)**: Скелети-висушувачі з мечами та агресивні піґліни.
  * **Кліщова атака (Pincer Flank)**: 50% шанс появи мобів з двох протилежних боків від гравця (взяття в кліщі).
  * **Темна підтримка**: 15% шанс появи бойової Відьми у складі хвилі.
  * **Персональні таймери**: Кожен гравець має індивідуальний кулдаун між хвилями (58–122 с) зі зміщеним першим спавном (20–75 с) — атаки непередбачувані й не б'ють по всіх одночасно.
* **🌅 Чистий Світанок (Clean Dawn)**:
  * Повна відмова від постійного ефекту підсвічування (`GLOWING`) — очищається лише з мобів івенту, не ламаючи інші плагіни.
  * На світанку боси розчиняються в тумані, авіабомби знешкоджуються, а з залишків мобів знімається івентова незеритова/діамантова броня та мечі.
* **✈️ Фантоми-Камікадзе (Phantom Bombers)**:
  * У повітрі з'являються фантоми з кріперами-наїзниками на спині.
  * Пікірують прямо на гравців; на відстані 4.5 м кріпер запалюється зі звуком шипіння та попередженням в Actionbar, а при зіткненні (≤ 2.2 м або ударі фантома) детонує миттєво.
  * Якщо збити фантома стрілою, активований кріпер скидається вниз як падаюча авіабомба.
* **🛡️ Анти-абуз та захист від ферм**:
  * **0% шанс дропу броні й зброї**: Незерит і мечі слугують виключно для бою і не засмічують економіку.
  * **Захист від автоферм**: Спавнери, яйця призову, підкріплення зомбі, а також чешуйниці, ендерміти та слайми виключені з івенту.
  * **Лут тільки за чесний бій**: Цінні ресурси випадають **лише** при вбивстві моба гравцем або його прирученим вовком.
  * **Захист від багів зілля кріперів**: Запобігання виникненню нескінченних хмар ефектів `AreaEffectCloud`.
* **📢 Трансляція в Discord**:
  * Окремі барвисті Embed-сповіщення при сходженні Кривавого Місяця та анонси перемоги над босами.

#### 🧭 Action Bar HUD (Координати, Компас та Ігровий день)
*Налаштування: `hud.enabled: false`*
* **Інформативний рядок над хотбаром**: Відображає поточні координати `X Y Z`, сторону світу за компасом (`[N]`, `[S]`, `[E]`, `[W]`, `[NE]` тощо) та поточний ігровий день світу (`Day 1819`), синхронізований з екраном F3.
* **Автоконверсія координат у Незері (1:8)**: Перебуваючи в Незері, гравець бачить одночасно координати Незеру та автоматично перераховані відповідні координати Верхнього світу (`Nether: -62 70 -37 (Overworld: -496 -296) | Day 1819`), що дозволяє будувати та лінкувати портали без калькулятора.
* **Індивідуальне перемикання (`/hud`)**: Кожен гравець може в будь-який момент увімкнути або вимкнути панель для себе командою `/hud` (вибір зберігається між сесіями).

#### ⚡ Кастомні розширені механіки (Fun Modes)
*Налаштування: `fun.enabled: false`*
* **Тягун без дощу (Riptide without Rain)** (`fun.modes.riptide_no_rain.enabled: false`): Можливість запускатися тризубцем із чарами Тягун на суші без води й дощу. Включає захист від падіння на перше приземлення, захист пасажирів (GSit/човни) та перевірку PvP/AFK при тарані. Дозволяє взаємодіяти зі скринями та дверима без випадкового ривка.
* **Розумне ковадло (Smart Anvil)** (`fun.modes.smart_anvil.enabled: false`): Знімає штучне ванільне обмеження 40 рівнів («Занадто дорого!»), дозволяючи ремонтувати та покращувати предмети будь-яку кількість разів.
* **Списи без голоду (Spear No Hunger)** (`fun.modes.spear_no_hunger.enabled: false`): Усі види списів не витрачають очки голоду та виснаження гравця.

#### 🌙 Розумний пропуск ночі (Sleep Voting)
*Налаштування: `sleep.enabled: false`*
* **Динамічне голосування сном**: Для звичайного пропуску ночі достатньо, щоб у ліжка лягли 50% активних гравців світу.
* **Фільтрація неактивних**: Система автоматично ігнорує гравців у режимі AFK та глядачів (Spectator mode).
* **Інформативні сповіщення**:
  * Повідомлення в ігровий чат про кількість гравців, які сплять.
  * Стильне Embed-повідомлення в Discord ("☀️ Гравець пропустив ніч. Доброго ранку!").
  * Автоматичне скидання шторму та грози.

#### 💤 AFK-система та захист від мобів
*Налаштування: `afk.enabled: false`*
* **Автоматичний перехід**: Фіксація неактивності гравця через 60 секунд (налаштовується) або вручну командою `/afk [причина]`.
* **Захист від мобів**: Під час AFK гравець стає повністю невразливим до шкоди мобів, монстри скидають з нього агро та не штовхають.
* **Візуальні індикатори**: Текстовий дисплей над головою гравця без засмічення чанку та позначка `[АФК]` у TabList.

#### 🏷️ Синхронізація ролей з Discord (Role Sync)
*Налаштування: `role-sync.enabled: false`*
* **Префікси ролей**: Автоматичне відображення кольорових префіксів ролей Discord у Tab-листі, внутрішньоігровому чаті та над головами персонажів.
* **Ієрархічне сортування**: Гравці у вкладці Tab впорядковуються зверху вниз згідно з пріоритетом посад (Адміністратор ➔ Модератор ➔ VIP ➔ Гравець).

#### 🛡️ Безпека чату та AI-модерація
* **AntiSpam система** (`antispam.enabled: false`):
  * Захист від швидкого флуду та повторюваних однакових повідомлень.
  * Багаторівнева система покарань: попередження, блокування спам-повідомлень, автоматичний тимчасовий мут із підтримкою зняття через `/unmute`.
* **Розумна AI-модерація (OpenAI / ChatGPT / OpenRouter)** (`ai-moderator.enabled: false`):
  * Асинхронна перевірка підозрілих повідомлень на токсичність та образливий зміст без затримок ігрового процесу (zero input-lag).
  * Автоматичне сповіщення адміністрації в закритий Discord-канал модераторів із зазначенням порушника та цитатою.

#### 🔒 Рольовий Whitelist & Доступ
*Налаштування: `whitelist.enabled: false`*
* **Рольовий доступ**: Вимога обов'язкової прив'язки Discord або наявності визначеної ролі (`require-discord-role`) для входу на сервер.
* Автоматичне відхилення неавторизованих користувачів із персоналізованим екраном-поясненням.

#### 🛠️ Режим технічних робіт (Maintenance Mode)
*Налаштування: `maintenance.enabled: false`*
* Швидке закриття сервера на обслуговування командою `/maintenance <on/off>` у грі чи Discord.
* Вхід дозволено лише адміністраторам, а звичайні гравці отримують інформативне повідомлення з причиною робіт.

#### 🔄 Розумний планувальник авторестартів
*Налаштування: `autorestart.times: []`*
* **Регулярні рестарти**: Автоматичний перезапуск сервера за заданим розкладом у зручний час.
* **Візуальні та звукові попередження**:
  * Чат-сповіщення зі зворотним відліком.
  * Спливаючі титри на екрані (Titles & Subtitles) перед рестартом.
  * Можливість для окремих гравців приховати повідомлення командою `/togglerestart`.
* **Відкладений розумний рестарт (`/queuerestart`)**: Одноразовий рестарт, який ставиться в чергу через Discord і спрацьовує автоматично, щойно на сервері буде 0 онлайну. Працює як перемикач (toggle).
* **Тихий режим при 0 онлайну (`events.notify-empty-server: false`)**: Якщо на сервері немає гравців, технічні повідомлення про рестарт чи вимкнення не надсилаються в Discord-чат, захищаючи від спаму.

#### 🗺️ Періодичне автооновлення веб-карти (BlueMap Auto-Reload)
*Налаштування: `bluemap.auto-reload.enabled: false`*
* Запуск команди `bluemap reload` за встановленим графіком або через 2 хвилини після старту сервера (із захистом від пропуску хвилин при лагах).

#### 📈 Технічний моніторинг та збір помилок
*Налаштування: `technical.*`, `sentry.enabled: false`*
* **Performance Monitor**: Автоматичне сповіщення адміністрації у Discord при просіданні TPS нижче критичного порогу (15.0) або переповненні RAM (> 95%), з можливістю автоматичного запуску профилювання Spark.
* **Error Catcher & Sentry**: Автоматичний перехват критичних помилок із вивантаженням стектрейсів на paste-сервіс або в хмару Sentry.

---

## 🕹️ Команди плагіна (UA)

### Команди в Minecraft:
| Команда | Опис | Доступ |
| :--- | :--- | :--- |
| `/minecord reload` | Перезавантажити конфігурацію плагіна `config.yml` | `minecord.admin` |
| `/minecord update` | Перевірити та завантажити оновлення плагіна з GitHub Releases | `minecord.admin` |
| `/minecord links` | Переглянути список усіх прив'язаних акаунтів Minecraft ⇄ Discord | `minecord.admin` |
| `/minecord bloodmoon start [1-4]` | Примусово запустити Кривавий Місяць обраного рівня (1–4) | `minecord.admin` |
| `/minecord bloodmoon stop` | Зупинити поточний Кривавий Місяць | `minecord.admin` |
| `/minecord bloodmoon status` | Переглянути активний рівень, кількість убитих монстрів, хвиль та час | `minecord.admin` |
| `/minecord bloodmoon cleareffects [гравець\|@a]` | Очистити аномальні ефекти зілля та шкідливі хмари | `minecord.admin` |
| `/minecord bot status` | Діагностика стану підключення Discord-бота (Gateway, пінг, сервери) | `minecord.admin` |
| `/minecord bot reconnect` | Перепідключити з'єднання Discord-бота без перезавантаження сервера | `minecord.admin` |
| `/unmute <гравець>` | Зняти мут від антиспаму з гравця (підтримує автодоповнення ніків) | `minecord.admin` |
| `/discord [link]` | Отримати посилання на Discord-сервер або згенерувати код прив'язки | Усі |
| `/map` | Отримати посилання на інтерактивну онлайн-карту сервера | Усі |
| `/stats [гравець]` | Переглянути статистику (час гри, кількість смертей, блоки тощо) | Усі |
| `/top [категорія]` | Топ-10 гравців за статистикою (час, моби, смерті, алмази, блоки) | Усі |
| `/sharecoords [X Y Z] <опис>` | Поділитися координатами з описом та прямим лінком на веб-мапу | Усі |
| `/report <гравець> <причина>` | Надіслати скаргу на гравця модераторам у Discord | Усі |
| `/ticket create <текст>` | Створити тікет до адміністрації сервера | Усі |
| `/mail send <гравець> <текст>` | Надіслати офлайн-повідомлення гравцю | Усі |
| `/mail read` | Прочитати інформацію про вхідні повідомлення | Усі |
| `/togglerestart` | Увімкнути/вимкнути для себе сповіщення про авторестарт | Усі |
| `/afk [причина]` | Ввійти або вийти з режиму AFK (захист від мобів, не блокує сон іншим) | Усі |
| `/hud` | Увімкнути/вимкнути для себе панель координат і дня над хотбаром (Action Bar HUD) | Усі |

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

**MineCord** is a high-performance, feature-rich Minecraft server plugin for Paper / Purpur (26.2+) that provides seamless two-way Discord integration, automates server administration, enhances player communication, and extends core gameplay mechanics.

## 🚀 Key Features

MineCord features a modular architecture designed for maximum flexibility. All core integration features work out-of-the-box, while specialized gameplay modes, moderation tools, and automation systems are disabled by default and can be effortlessly activated in `config.yml`.

### 📌 Feature Status Overview

| Module / Feature | Default Status | Config Key (`config.yml`) | Description |
| :--- | :---: | :--- | :--- |
| **Two-Way Cross-Chat** | ✅ **Enabled** | `discord.chat-channel-id` | Minecraft ⇄ Discord via Webhooks with 3D skins & Smart Mentions |
| **Multi-Language & Localization** | ✅ **Enabled** | `language: "en"`, `per-player-language: true` | English (default), Ukrainian, and Slovak. Client-side language detection, 100+ deaths, advancements |
| **Offline Mail System** | ✅ **Enabled** | — (`/mail`) | Private messaging system for offline players with Discord DM forwarding |
| **Account Linking** | ✅ **Enabled** | — (`/discord link`) | Collision-free one-time verification codes linking Minecraft ⇄ Discord |
| **Tickets & Reports (/ticket, /report)**| ✅ **Enabled** | `discord.moderator-channel-id` | In-game reports & support tickets with Discord action buttons |
| **Remote Discord Console** | ✅ **Enabled** | `discord.console-channel-id` | Live async console stream & interactive command execution |
| **Stats, Leaderboards & TabList** | ✅ **Enabled** | — (`/stats`, `/top`) | Asynchronous TabList (TPS, ping, uptime), leaderboards, `/sharecoords` |
| **Bot Diagnostics** | ✅ **Enabled** | — (`/minecord bot`) | Gateway latency check and hot session reconnection |
| **GitHub Auto-Updater** | ⚙️ *Disabled* | `auto-update.enabled: false` | Automatically pulls latest builds from GitHub Releases into `plugins/update/` |
| **Bloodmoon Event** | ⚙️ *Disabled* | `fun.modes.bloodmoon.enabled: false` | 4 balanced difficulty tiers, hordes, kamikaze phantoms, bosses, state persistence |
| **Action Bar HUD (Coords & Day)** | ⚙️ *Disabled* | `hud.enabled: false` | Real-time X Y Z, cardinal compass, world day & Nether ➔ Overworld 1:8 conversion (`/hud`) |
| **Riptide without Rain** | ⚙️ *Disabled* | `fun.modes.riptide_no_rain.enabled: false`| Trident thrust on dry land with fall protection, block interact check & PvP safety |
| **Spear No Hunger** | ⚙️ *Disabled* | `fun.modes.spear_no_hunger.enabled: false`| All spear types consume zero hunger/exhaustion points |
| **Smart Anvil** | ⚙️ *Disabled* | `fun.modes.smart_anvil.enabled: false` | Removes the vanilla 40-level limit ("Too Expensive!") |
| **Smart Sleep Voting** | ⚙️ *Disabled* | `sleep.enabled: false` | 50% dynamic sleeper threshold, ignores AFK players |
| **AFK System & Mob Invulnerability** | ⚙️ *Disabled* | `afk.enabled: false` | Auto-AFK timer (60s), non-persistent text display, mob damage immunity |
| **Discord Role Synchronization** | ⚙️ *Disabled* | `role-sync.enabled: false` | Tab/chat prefixes, nametag colors, hierarchy sorting |
| **AntiSpam Engine** | ⚙️ *Disabled* | `antispam.enabled: false` | Flood protection, repeat blocking, automated mute ladder |
| **AI Chat Moderation** | ⚙️ *Disabled* | `ai-moderator.enabled: false` | Asynchronous toxicity analysis via AI (OpenRouter / ChatGPT) |
| **Role-Based Whitelist** | ⚙️ *Disabled* | `whitelist.enabled: false` | Restrict server entry to linked players or specific Discord roles |
| **Maintenance Mode** | ⚙️ *Disabled* | `maintenance.enabled: false` | Lockdown server with customizable kick screen (`/maintenance`) |
| **Scheduled Auto-Restart** | ⚙️ *Disabled* | `autorestart.times: []` | Timed reboots, title countdowns, `/queuerestart` empty server queue |
| **BlueMap Auto-Reload** | ⚙️ *Disabled* | `bluemap.auto-reload.enabled: false` | Automatic timed web-map reloads with minute-slot protection |
| **Technical Monitoring & Sentry** | ⚙️ *Disabled* | `technical.*`, `sentry.enabled: false`| Low TPS / high RAM alerts, automatic Spark profiling, crash tracking |

---

### ✅ 1. Core Features (Enabled by Default)

#### 💬 Next-Gen Two-Way Cross-Chat (Minecraft ⇄ Discord)
* **Discord Webhooks**: In-game player messages stream to Discord via webhooks displaying the sender's actual username and their real 3D head avatar. Dedicated daemon executor with 5s timeouts guarantees zero impact on TPS.
* **Mention Sanitization**: Enforces `"allowed_mentions": {"parse": ["users"]}` to block `@everyone` and `@here` ping exploits.
* **Skin Texture Resolution**: Avatars are extracted directly from player texture hashes, ensuring custom skins render accurately for both online and offline mode players.
* **Smart Mentions**:
  * Typing `@PlayerName` in Minecraft highlights the name in yellow and plays an audio ping for the recipient.
  * If the player linked their Discord account, the mention automatically transforms into an authentic Discord ping (`<@id>`).
* **Discord ➔ Minecraft**: Messages from Discord channels stream into Minecraft chat with role colors and clean formatting.

#### 🌐 Multi-Language System & Per-Player Client Localization
MineCord features a built-in localization engine with hot-reload capabilities:
* **Built-In Languages**:
  * `en` — English (Default in repository);
  * `uk` — Ukrainian (Українська) — full localization of deaths, advancements, commands, and chat alerts;
  * `sk` — Slovak (Slovenčina) — complete translation of death messages, advancements, and Discord embeds.
* **Per-Player Client Language (`per-player-language: true`)**:
  MineCord automatically detects the player's client language locale (`uk_ua` ➔ `uk`, `sk_sk` / `cs_cz` ➔ `sk`, `en_us` ➔ `en`). Each player receives in-game messages, TabList header/footer, and warnings in their native language, while server-wide Discord embeds and console logs follow the global server language (`language`).
* **Custom Translations & Fallback**:
  Language files are stored in `plugins/MineCord/languages/` (`en.yml`, `uk.yml`, `sk.yml`). If any key is missing from a translation file, MineCord automatically falls back to `en.yml`.
* **Death Message Localization**: Translates over 100 official death causes (mobs, projectiles, falls, voids) and relays them to Discord and chat in the selected language.
* **Advancement Announcements**: Relays earned achievements with localized names.
* **Interactive Death Coordinates**: Players receive a private message with their death coordinates and an interactive clickable BlueMap link that opens the exact death spot in their browser.

#### 📬 Offline Mail System
* Send messages to offline players via `/mail send <player> <message>`.
* Messages are forwarded directly to the recipient's Discord DMs via the bot, and an in-game reminder is shown upon their next login.

#### 🔗 Account Linking
* **Account Verification**: Generates collision-free, expiring one-time codes in Minecraft for linking accounts via Discord slash command `/discord link` $\rightarrow$ `/link <code>`.
* Enables synchronized Smart Mentions and allows offline mail notifications to be forwarded to Discord.

#### 🎫 In-Game Ticket & Report System
* **Tickets from Minecraft**: Players can submit reports or help requests using `/ticket create <message>`.
* **Player Reports**: Rapidly report griefers or rule-breakers via `/report <player> <reason>` with anti-spam cooldown protection.
* **Interactive Discord Embeds**: Staff receives tickets and reports in a private channel with action buttons (claim, close) right inside Discord.

#### 💻 Remote Discord Console
* Dedicated private Discord channel mirroring server console logs in real time.
* Execute any server console command directly by typing it in the Discord channel with backtick escaping.

#### 📊 Statistics, Leaderboards & Monitoring
* **Custom TabList**: Clean header and footer showing online count, TPS, ping, and server uptime.
* **Stats & Leaderboards**: Asynchronously inspect player playtime, death count, broken blocks, and achievements (`/stats`, `/top`).
* **Share Coordinates (`/sharecoords`)**: Broadcast Points of Interest with descriptions and a direct interactive BlueMap link.
* **Bot Diagnostics**: `/minecord bot status` and `/minecord bot reconnect` commands for managing Discord bot connectivity on the fly.

---

### ⚙️ 2. Optional Modules (Disabled by Default — Configured in config.yml)

#### 🩸 Hardcore Bloodmoon Event — 4 Difficulty Tiers
*Configuration: `fun.modes.bloodmoon.enabled: false`*

A comprehensive nocturnal apocalypse featuring dangerous hordes, aerial bombers, and epic boss battles:
* **Empty Server Protection**: Bloodmoon will not start and automatically halts if online player count drops to 0 (`require_players_online: true`), avoiding idle server lag and Discord spam.
* **Bed Sleep Blocked**: Sleeping in beds is completely locked out until sunrise (players are forcibly ejected with audio feedback).
* **State Persistence Across Restarts**: Active tier, elapsed time, slain mob counts, and horde waves are saved to `bloodmoon_state.yml` and seamlessly resume after server restarts.
* **Atmospheric Visuals without Physical Barriers**: Red sky vignette is projected via a maximum-radius WorldBorder, giving an immersive horror atmosphere without colliding with invisible walls.
* **4 Scaled Tiers & Rebalanced Loot Progression**:
  * **Tier 1 ("Bloodmoon")**: 1.6x mob HP multiplier, Speed & Strength buffs, 40% iron gear, boss **"Blood Reaper"** (120 HP). Drops: iron, gold, diamonds, 25% netherite scrap chance.
  * **Tier 2 ("Blood Armageddon")**: 2.5x mob HP multiplier, 60% diamond/iron gear, charged creepers, boss **"Abyssal Lord"** (240 HP). Hard cap: **maximum 2 netherite scraps** per player per night! Regular mob drop chance is 0.3% (~1 in 333 kills); boss has a 60% chance to drop 1 scrap (within the cap), 40% totem chance.
  * **Tier 3 ("Infernal Cataclysm")**: 3.5x mob HP multiplier, 80% diamond gear, boss **"☠ Archdemon of Death ☠"** (380 HP, Wither-grade Regeneration II). 0.8% mob scrap drop (~1–2 per night). Boss drops 1–2 scraps, 3–5 diamonds, 30% Nether Star chance, and 75% Totem of Undying chance.
  * **Tier 4 ("☠ Doomsday (Ragnarok) ☠")**: 5.0x mob HP multiplier, Strength II, Speed II, 90% netherite & diamond gear, massive hordes up to 24 monsters, and colossal final boss **"☠ TITAN OF CHAOS ☠"** (550 HP, netherite armor, Regeneration III, chaos guard retinue). Mob scrap drop: 1.5% (~2–4 per night), 3.5% diamonds. Boss drops **1 Nether Star, 2 Netherite Ingots, 2 Totems, 2 Enchanted Golden Apples, and 8–16 Diamonds**!
* **🎲 5 Randomized Horde Archetypes & Combat Tactics**:
  * **Chaos Ambush**: Balanced wave of zombies, skeletons, fast spiders, husks, and creepers.
  * **Spider Swarm**: Infestation of venomous cave spiders and sprinting spiders with speed buffs.
  * **Undead Legion**: Heavily armored zombies and dead-eye skeleton snipers with enhanced bows.
  * **Sapper Squad**: Fast creeper breachers shielded by a zombie vanguard frontline.
  * **Nether Vanguard**: Wither skeletons with swords and aggressive piglins.
  * **Pincer Flank**: 50% chance that horde spawns split on opposite sides of the player to surround them.
  * **Dark Witch Support**: 15% chance to spawn a battle Witch providing potion support.
  * **Individual Player Timers**: Every player has an independent horde countdown (58–122s) with staggered initial starts (20–75s) — spawns are personal, dynamic, and desynchronized.
* **🌅 Clean Dawn Mechanics**:
  * Glow outlines are strictly cleared from event mobs without interfering with other plugins.
  * At dawn, bosses dissipate into smoke, aerial bombs are defused, and lingering mobs have their netherite/diamond gear and combat buffs cleanly stripped.
* **✈️ Phantom Kamikaze Bombers**:
  * Phantoms fly into battle carrying Creepers on their backs.
  * Actively dive-bomb survival players; ignites at 4.5m with warning hiss and Actionbar alert, and detonates instantly upon collision (≤ 2.2m or bite strike).
  * Shooting down the Phantom drops the ignited Creeper as a ticking falling aerial bomb.
* **🛡️ Anti-Exploit & Farm Protection**:
  * **0% Mob Equipment Drop Chance**: Netherite armor and enchanted weapons are strictly for mob defense and do not pollute the player economy.
  * **Anti-Mob-Farm Isolation**: Spawners, spawn eggs, zombie reinforcements, silverfish, endermites, and slimes are completely ignored by the event.
  * **Fair Fight Loot**: Custom resource bundles drop **exclusively** when killed manually by a player or tamed wolf.
  * **AreaEffectCloud Cleanup**: Creepers cannot produce permanent lingering potion clouds upon detonation.
* **📢 Discord Broadcasts**:
  * Rich start embeds announcing threats and rewards, with triumphant global defeat announcements when bosses fall.

#### 🧭 Action Bar HUD (Coordinates, Compass & World Day)
*Configuration: `hud.enabled: false`*
* **Clean HUD Above Hotbar**: Displays real-time `X Y Z` coordinates, 8-direction cardinal compass (`[N]`, `[S]`, `[E]`, `[W]`, `[NE]`, etc.), and the current in-game world day (`Day 1819`) matching the F3 debug screen.
* **Nether ➔ Overworld Coordinate Conversion (1:8)**: While in the Nether, the HUD simultaneously displays both current Nether coordinates and the corresponding Overworld coordinates (`Nether: -62 70 -37 (Overworld: -496 -296) | Day 1819`) for effortless portal linking.
* **Per-Player Toggle (`/hud`)**: Every player can toggle the HUD on or off individually at any time via `/hud` (persisted across sessions).

#### ⚡ Extended Custom Mechanics (Fun Modes)
*Configuration: `fun.enabled: false`*
* **Riptide without Rain** (`fun.modes.riptide_no_rain.enabled: false`): Launch yourself with Riptide-enchanted tridents on dry land without rain or water. Features fall-damage protection on initial landing, passenger safety (GSit/boats), and PvP/AFK filters during dash impacts. Normal block interaction (chests, doors) is preserved.
* **Smart Anvil** (`fun.modes.smart_anvil.enabled: false`): Removes the vanilla 40-level limit ("Too Expensive!"), allowing items to be repaired and combined infinitely.
* **Spear No Hunger** (`fun.modes.spear_no_hunger.enabled: false`): All spear types do not consume player hunger or exhaustion points.

#### 🌙 Smart Sleep Voting
*Configuration: `sleep.enabled: false`*
* **Dynamic Percentage Threshold**: Regular night skips automatically when 50% of active players sleep in beds.
* **AFK & Spectator Exclusion**: Players in AFK status or Spectator mode are ignored, preventing idle players from blocking morning.
* **Rich Broadcasts**:
  * In-game messages showing current sleeper count and needed sleepers.
  * Discord embed notification ("☀️ Player skipped the night. Good morning!").
  * Automatically resets bad weather (rain & thunder).

#### 💤 AFK System & Mob Invulnerability
*Configuration: `afk.enabled: false`*
* **Inactivity Detection**: Automatic transition after 60 seconds (configurable) or manually via `/afk [reason]`.
* **Mob Invulnerability**: Full immunity to mob damage, projectile targeting, and knockback while in AFK state.
* **Visual Status**: Non-persistent overhead text display above the player's head and `[АФК]` tag in TabList.

#### 🏷️ Discord Role Synchronization (Role Sync)
*Configuration: `role-sync.enabled: false`*
* **Role Prefixes**: Automatic color-coded Discord role prefixes in TabList, in-game chat, and 3D nametags above player heads.
* **Hierarchy Sorting**: Players in the TabList are automatically sorted top-down based on role priority (Admin ➔ Moderator ➔ VIP ➔ Player).

#### 🛡️ Chat Security & AI Moderation
* **AntiSpam Engine** (`antispam.enabled: false`):
  * Protects against rapid flood and duplicate identical messages.
  * Multi-tiered penalty ladder: gentle warnings, message drops, and automated mutes with `/unmute` staff override.
* **OpenAI (ChatGPT / OpenRouter) Moderation** (`ai-moderator.enabled: false`):
  * Asynchronously evaluates suspicious chat messages for toxicity and harassment with zero server tick lag.
  * Instantly notifies server staff in a designated Discord moderation channel with context and author info.

#### 🔒 Role-Based Whitelist
*Configuration: `whitelist.enabled: false`*
* **Role-Gated Access**: Require players to link their Discord account or possess a specific Discord role (`require-discord-role`) to enter the server.
* Rejects unverified logins with an explanatory kick screen.

#### 🛠️ Maintenance Mode
*Configuration: `maintenance.enabled: false`*
* Temporarily locks the server to regular players via `/maintenance <on/off>` in-game or from Discord.
* Shows a customized maintenance message explaining downtime reasons.

#### 🔄 Intelligent Auto-Restart Scheduler
*Configuration: `autorestart.times: []`*
* **Scheduled Restarts**: Automatically reboots the server at configured times.
* **Audio-Visual Warnings**:
  * Countdown notices in chat.
  * On-screen Titles & Subtitles before rebooting.
  * Players can toggle countdown visibility for themselves via `/togglerestart`.
* **Queued Restarts (`/queuerestart`)**: Queue a one-time restart via Discord that triggers automatically once the server is empty (0 online players). Operates as a toggle.
* **Quiet Mode on Empty Server (`events.notify-empty-server: false`)**: Suppresses restart and shutdown Discord embeds whenever 0 players are online, eliminating channel spam.

#### 🗺️ BlueMap Auto-Reload
*Configuration: `bluemap.auto-reload.enabled: false`*
* Automatically executes `bluemap reload` at configured times or 2 minutes after startup (with minute-slot duplicate protection).

#### 📈 Technical Monitoring & Crash Reporting (Sentry)
*Configuration: `technical.*`, `sentry.enabled: false`*
* **Performance Monitor**: Sends Discord alert embeds when TPS drops below threshold (15.0) or memory spikes (> 95%), with optional automated Spark profiler triggers.
* **Error Catcher & Sentry**: Captures uncaught exceptions and uploads stack traces to a paste service or cloud Sentry dashboard.

---

## 🕹️ Plugin Commands (EN)

### In-Game Commands:
| Command | Description | Permission |
| :--- | :--- | :--- |
| `/minecord reload` | Reload plugin configuration files (`config.yml`) | `minecord.admin` |
| `/minecord update` | Check and download plugin updates from GitHub Releases | `minecord.admin` |
| `/minecord links` | View list of all linked Minecraft ⇄ Discord accounts | `minecord.admin` |
| `/minecord bloodmoon start [1-4]` | Force-start Bloodmoon of specified tier (1–4) | `minecord.admin` |
| `/minecord bloodmoon stop` | Force-stop active Bloodmoon event | `minecord.admin` |
| `/minecord bloodmoon status` | View active tier, killed mobs, spawned hordes, and time remaining | `minecord.admin` |
| `/minecord bloodmoon cleareffects [player\|@a]` | Remove anomalous lingering potion clouds and mob potion effects | `minecord.admin` |
| `/minecord bot status` | Check Discord Gateway status, ping, and connected guilds | `minecord.admin` |
| `/minecord bot reconnect` | Reconnect Discord bot session without restarting the server | `minecord.admin` |
| `/unmute <player>` | Unmute player muted by anti-spam (supports player name tab completion) | `minecord.admin` |
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
| `/afk [reason]` | Toggle AFK status manually (mob invulnerable, never blocks night skip) | Everyone |
| `/hud` | Toggle the Action Bar coordinates, compass, and day HUD on or off | Everyone |

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

1. Download the latest `MineCord-1.0-26.2-all.jar` from GitHub **Releases** or GitHub **Actions** (latest successful build).
2. Place the jar file in your server's `plugins/` directory.
3. Start or restart the server to generate `plugins/MineCord/config.yml`.
4. Configure `config.yml`:
   * `discord.token` — Discord Bot Token (from Discord Developer Portal);
   * `discord.chat-channel-id` — Channel ID for cross-chat;
   * `discord.console-channel-id` — Private channel ID for remote console;
   * `discord.guild-id` — Discord Server (Guild) ID;
   *(Note: Discord Webhook for player avatars is created automatically by the bot — just ensure the bot role has the `Manage Webhooks` permission in the chat channel!)*
5. Run `/minecord reload` or restart your server.

---

## 🛠️ Requirements / Вимоги
* **Server Software**: Paper, Purpur, or compatible forks (26.2+).
* **Java Runtime**: Java 21 or newer (fully supports Java 25).
* **Build System**: Gradle 8.x + ShadowJar.
