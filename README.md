# MineCord 🎮💬

<p align="center">
  <img src="https://img.shields.io/badge/Lines_of_Code-7.5k-blue?style=flat-square&logo=java&logoColor=white" alt="Lines of Code">
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

Для максимальної гнучкості MineCord розроблено за модульною архітектурою. Усі базові інтеграційні функції готові до роботи одразу після встановлення, а додаткові геймплейні та захисні модулі за замовчуванням вимкнені й легко активуються в `config.yml`.

### 📌 Загальний статус модулів

| Модуль / Функціонал | Статус за замовчуванням | Перемикач у `config.yml` | Призначення |
| :--- | :---: | :--- | :--- |
| **Двосторонній чат (Cross-Chat)** | ✅ **Увімкнено** | `discord.chat-channel-id` | Minecraft ⇄ Discord через Webhooks з 3D скінами та Smart Mentions |
| **Локалізація подій та смертей** | ✅ **Увімкнено** | `events.death`, `events.join-leave` | Офіційний переклад 100+ смертей укр., BlueMap лінки, здобутки |
| **Офлайн-пошта (Mail System)** | ✅ **Увімкнено** | — (`/mail`) | Особисті повідомлення офлайн гравцям |
| **Прив'язка акаунтів (Account Link)** | ✅ **Увімкнено** | — (`/discord link`) | Генерація кодів верифікації Minecraft ⇄ Discord |
| **Тікети та скарги (/ticket, /report)** | ✅ **Увімкнено** | `discord.moderator-channel-id` | Звернення гравців та скарги з інтерактивними кнопками в Discord |
| **Віддалена консоль (Remote Console)** | ✅ **Увімкнено** | `discord.console-channel-id` | Живий потік консолі та виконання команд прямо з Discord |
| **Статистика, топи та TabList** | ✅ **Увімкнено** | — (`/stats`, `/top`) | Інформативний таб (TPS, пінг), топи, `/sharecoords` з мапою |
| **Діагностика бота** | ✅ **Увімкнено** | — (`/minecord bot`) | Швидка перевірка зв'язку та гаряче перепідключення бота |
| **Кривавий Місяць (Bloodmoon Event)** | ⚙️ *Вимкнено* | `fun.modes.bloodmoon.enabled: false` | 4 хардкорні рівні, орди, камікадзе-фантоми, 4 боси, рідкісний незерит |
| **Тягун без дощу (Riptide without Rain)**| ⚙️ *Вимкнено* | `fun.modes.riptide_no_rain.enabled: false` | Ривки тризубцем на суші без дощу та води із захистом від падіння |
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
| **Підказки та поради (Tips)** | ⚙️ *Вимкнено* | `tips.enabled: false` | Персональні корисні поради гравцям у випадковий час |
| **Автооновлення BlueMap** | ⚙️ *Вимкнено* | `bluemap.auto-reload.enabled: false` | Автоматичне оновлення веб-карти за розкладом |
| **Технічний моніторинг та Sentry** | ⚙️ *Вимкнено* | `technical.*`, `sentry.enabled: false`| Сповіщення про падіння TPS, пам'яті RAM та відлов помилок |

---

### ✅ 1. Базові функції (Увімкнено за замовчуванням)

#### 💬 Двосторонній чат нового покоління (Minecraft ⇄ Discord)
* **Discord Webhooks**: Повідомлення з гри відправляються в Discord від імені конкретного гравця з його реальною 3D-аватаркою.
* **Підтримка текстур та скінів**: Аватарки формуються безпосередньо з текстури гравця, завдяки чому кастомні скіни відображаються коректно для всіх гравців.
* **Smart Mentions (Розумні згадки)**:
  * Ввівши `@Нікнейм` у Minecraft, адресат отримує підсвічування тексту та звуковий сигнал привернення уваги.
  * Якщо гравець прив'язав свій Discord-акаунт, у чаті Discord повідомлення автоматично перетворюється на реальний Discord-пінг (`<@id>`).
* **Discord ➔ Minecraft**: Повідомлення з каналу Discord миттєво транслюються в ігровий чат із підтримкою кольорів ролей та зрозумілим форматуванням.

#### 🌐 Локалізація та ігрові події
* **Повна локалізація смертей**: Вбудований перекладач адаптує понад 100 офіційних причин смерті (з урахуванням мобів, снарядів та падінь) і відправляє їх у Discord українською мовою.
* **Локалізація здобутків**: Сповіщення про виконання досягнень транслюються в Discord з їхніми офіційними назвами.
* **Інтерактивні координати смерті**: Гравець отримує приватне повідомлення з координатами своєї загибелі та прямим клікабельним посиланням на веб-карту (BlueMap).

#### 📬 Офлайн-пошта (Mail System)
* Можливість відправляти приватні повідомлення офлайн-гравцям (`/mail send <нік> <текст>`).
* При наступному вході гравець отримує сповіщення про нові листи та може їх прочитати (`/mail read`).

#### 🔗 Прив'язка акаунтів (Account Link)
* **Верифікація акаунтів**: Генерація одноразових кодів для зв'язування Minecraft-акаунта з профілем Discord (`/discord link` $\rightarrow$ `/link <code>`).
* Дозволяє синхронізувати згадки Smart Mentions та пересилати сповіщення про пошту.

#### 🎫 Система тікетів та скарг
* **Тікети прямо з гри**: Гравці можуть надіслати запит чи пропозицію адміністрації командою `/ticket create <текст>`.
* **Скарги на порушників**: Швидке повідомлення модераторів через `/report <гравець> <причина>`.
* **Інтерактивні картки в Discord**: Кожен тікет чи скарга з'являється в закритому каналі адміністрації з кнопками керування (закрити, взяти в роботу).

#### 💻 Віддалена консоль в Discord
* Закритий канал для адміністраторів із живою трансляцією серверних логів.
* Можливість виконувати будь-які серверні команди прямо з текстового поля Discord.

#### 📊 Статистика, рейтинги та інфо-панелі
* **Стильний TabList**: Інформативний таб із відображенням пінгів, онлайну, стабільності TPS та привітання сервера.
* **Команди статистики та топів**: Перегляд ігрового часу, смертей, добутих блоків та досягнень (`/stats`, `/top`).
* **Поширення координат (`/sharecoords`)**: Можливість поділитися з друзями точкою на місцевості з описом та прямим лінком на веб-мапу BlueMap.
* **Діагностика бота**: Команди `/minecord bot status` та `/minecord bot reconnect` для контролю підключення бота без перезапуску сервера.

---

### ⚙️ 2. Опціональні модулі (Вимкнено за замовчуванням — налаштовуються в config.yml)

#### 🩸 Хардкорний «Кривавий Місяць» (Bloodmoon) — 4 Рівні Складності
*Налаштування: `fun.modes.bloodmoon.enabled: false`*

Повномасштабна система нічних катаклізмів з унікальними мобами, окупацією неба та грандіозними босами:
* **Блокування сну**: Сон у ліжках повністю заборонено до настання світанку.
* **4 Хардкорні Рівні та Збалансований Лут**:
  * **Рівень 1 («Кривавий Місяць»)**: Множник HP 2.5x, бафи Швидкості та Сили, 70% діамантової броні, бос **«Кривавий Жнець»** (300 HP). Дроп: залізо, золото, поодинокі діаманти (незерит відсутній).
  * **Рівень 2 («Кривавий Армагеддон»)**: Множник HP 4.5x, 85% діамантової/незеритової броні, заряджені кріпери, бос **«Володар Безодні»** (550 HP). Суворий ліміт: **максимум 2 незеритові скрапи** за ніч на гравця! Шанс з мобів — 0.3% (~1 на 333 вбивства), з боса — 60% шанс на 1 скрап (в межах ліміту). Отримати 2 скрапи — справжня рідкісна удача.
  * **Рівень 3 («Пекельний Катаклізм»)**: Множник HP 6.5x, 95% зачарованого незериту, бос **«☠ Архідемон Смерті ☠»** (850 HP). Шанс скрапу з мобів — 0.8% (~1–2 за ніч). Бос гарантовано дає 1–2 скрапи, 30% шанс на Зірку Незеру та 75% шанс на Тотем безсмертя.
  * **Рівень 4 («☠ Судний День (Раґнарок) ☠»)**: Екстремальний множник HP 10.0x, Сила IV, Опір II, 100% максимальний незерит, орди до 36 монстрів та фінальний супербос **«☠ ТИТАН ХАОСУ ☠»** (1400 HP). Шанс скрапу з мобів — 1.5% (~2–4 за ніч), 3.5% на алмази, 0.1% на зачароване яблуко Нотча. Бос гарантовано дропає **1 чистий Незеритовий злиток**, Зірку Незеру та 1–2 Тотеми!
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
  * Повна відмова від ефекту підсвічування (`GLOWING`).
  * Жодного примусового вбивства мобів у печерах чи на фермах: з івентових монстрів на світанку просто чисто знімаються бафи та модифікатори, перетворюючи їх на звичайних ванільних мобів.
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

#### ⚡ Кастомні розширені механіки (Fun Modes)
*Налаштування: `fun.enabled: false`*
* **Тягун без дощу (Riptide without Rain)** (`fun.modes.riptide_no_rain.enabled: false`): Можливість запускатися тризубцем із чарами Тягун на суші без води й дощу. Включає захист від падіння на перше приземлення та повний захист пасажирів човна/коня від випадкового дружнього вогню. *(Для плавної швидкості провантаження чанків під час польотів на елітрах рекомендується вчасно рестартувати сервер за розкладом)*.
* **Розумне ковадло (Smart Anvil)** (`fun.modes.smart_anvil.enabled: false`): Знімає штучне ванільне обмеження 40 рівнів («Занадто дорого!»), дозволяючи ремонтувати та покращувати предмети будь-яку кількість разів.
* **Списи без голоду (Spear No Hunger)** (`fun.modes.spear_no_hunger.enabled: false`): Усі види списів не витрачають очки голоду та виснаження гравця.

#### 🌙 Розумний пропуск ночі (Sleep Voting)
*Налаштування: `sleep.enabled: false`*
* **Динамічне голосування сном**: Для звичайного пропуску ночі достатньо, щоб у ліжка лягли 50% активних гравців світу.
* **Фільтрація неактивних**: Система автоматично ігнорує гравців у режимі AFK та глядачів (Spectator mode).
* **Інформативні сповіщення**:
  * Повідомлення в ігровий чат про кількість гравців, які сплять.
  * Стильне Embed-повідомлення в Discord ("☀️ Гравець пропустив ніч. Доброго ранку!").
  * Автоматичне скидання шторму та дощу.

#### 💤 AFK-система та захист від мобів
*Налаштування: `afk.enabled: false`*
* **Автоматичний перехід**: Фіксація неактивності гравця через 60 секунд (налаштовується) або вручну командою `/afk [причина]`.
* **Захист від мобів**: Під час AFK гравець стає повністю невразливим до шкоди мобів, монстри скидають з нього агро та не штовхають.
* **Візуальні індикатори**: 3D-таймер неактивності над головою гравця та позначка `[АФК]` у TabList.

#### 🏷️ Синхронізація ролей з Discord (Role Sync)
*Налаштування: `role-sync.enabled: false`*
* **Префікси ролей**: Автоматичне відображення кольорових префіксів ролей Discord у Tab-листі, внутрішньоігровому чаті та над головами персонажів.
* **Ієрархічне сортування**: Гравці у вкладці Tab впорядковуються зверху вниз згідно з пріоритетом посад (Адміністратор ➔ Модератор ➔ VIP ➔ Гравець).

#### 🛡️ Безпека чату та AI-модерація
* **AntiSpam система** (`antispam.enabled: false`):
  * Захист від швидкого флуду та повторюваних однакових повідомлень.
  * Багаторівнева система покарань: попередження, блокування спам-повідомлень, автоматичний тимчасовий мут на 5 хвилин (`/unmute`).
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
  * Можливість для окремих гравців приховати набридливі повідомлення командою `/togglerestart`.
* **Відкладений розумний рестарт (`/queuerestart`)**: Одноразовий рестарт, який ставиться в чергу через Discord і спрацьовує автоматично, щойно на сервері буде 0 онлайну. Працює як перемикач (toggle).

#### 💡 Періодичні підказки та корисні поради (Tips)
*Налаштування: `tips.enabled: false`*
* Індивідуальне надсилання практичних порад та навігаційних підказок кожному онлайн-гравцю у випадковий часовий інтервал.

#### 🗺️ Періодичне автооновлення веб-карти (BlueMap Auto-Reload)
*Налаштування: `bluemap.auto-reload.enabled: false`*
* Запуск команди `bluemap reload` за встановленим часовим графіком або через 2 хвилини після старту сервера.

#### 📈 Технічний моніторинг та збір помилок
*Налаштування: `technical.*`, `sentry.enabled: false`*
* **Performance Monitor**: Автоматичне сповіщення адміністрації у Discord при просіданні TPS нижче критичного порогу (15.0) або переповненні RAM (> 95%), з можливістю автоматичного запуску профилювання Spark.
* **Error Catcher & Sentry**: Автоматичний перехват критичних помилок із вивантаженням стектрейсів на paste-сервіс або в хмару Sentry.

---

### 🏗️ 3. Підтримка ванільних механік та ферм (Vanilla Exploits)
* **Ламання бедроку**: Дозволено класичні способи видалення бедроку та рамок порталу в Енд (`allow-permanent-block-break-exploits: true`).
* **Безголові поршні**: Розблоковано створення headless pistons (`allow-headless-pistons: true`).
* **Дюп падаючих блоків**: Портали в Енд дозволяють класичний дюп піску, гравію та бетону для масштабних будівельних ферм (`allow-unsafe-end-portal-teleportation: true`).
* **Дюп динаміту (TNT) та рейок**: Поршневе копіювання активованого TNT для кар'єрів та тунелебудівників повністю працює (`allow-piston-duplication: true`).

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
| `/afk [причина]` | Ввійти або вийти з режиму AFK (захист від мобів, не блокує сон іншим) | Усі |

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

MineCord features a modular architecture designed for maximum flexibility. All core integration features work out-of-the-box, while specialized gameplay modes, moderation tools, and automation systems are disabled by default and can be effortlessly activated in `config.yml`.

### 📌 Feature Status Overview

| Module / Feature | Default Status | Config Key (`config.yml`) | Description |
| :--- | :---: | :--- | :--- |
| **Two-Way Cross-Chat** | ✅ **Enabled** | `discord.chat-channel-id` | Minecraft ⇄ Discord via Webhooks with 3D skins & Smart Mentions |
| **Event & Death Localization** | ✅ **Enabled** | `events.death`, `events.join-leave` | Official Ukrainian translation of 100+ deaths, BlueMap links, achievements |
| **Offline Mail System** | ✅ **Enabled** | — (`/mail`) | Private messaging system for offline players |
| **Account Linking** | ✅ **Enabled** | — (`/discord link`) | One-time verification codes linking Minecraft ⇄ Discord |
| **Tickets & Reports (/ticket, /report)**| ✅ **Enabled** | `discord.moderator-channel-id` | In-game reports & support tickets with Discord action buttons |
| **Remote Discord Console** | ✅ **Enabled** | `discord.console-channel-id` | Live console stream & interactive command execution |
| **Stats, Leaderboards & TabList** | ✅ **Enabled** | — (`/stats`, `/top`) | Detailed TabList (TPS, ping), leaderboards, `/sharecoords` |
| **Bot Diagnostics** | ✅ **Enabled** | — (`/minecord bot`) | Gateway latency check and hot session reconnection |
| **Bloodmoon Event** | ⚙️ *Disabled* | `fun.modes.bloodmoon.enabled: false` | 4 difficulty tiers, hordes, kamikaze phantoms, 4 bosses, netherite loot |
| **Riptide without Rain** | ⚙️ *Disabled* | `fun.modes.riptide_no_rain.enabled: false`| Trident thrust on dry land with fall-damage protection |
| **Spear No Hunger** | ⚙️ *Disabled* | `fun.modes.spear_no_hunger.enabled: false`| All spear types consume zero hunger/exhaustion points |
| **Smart Anvil** | ⚙️ *Disabled* | `fun.modes.smart_anvil.enabled: false` | Removes the vanilla 40-level limit ("Too Expensive!") |
| **Smart Sleep Voting** | ⚙️ *Disabled* | `sleep.enabled: false` | 50% dynamic sleeper threshold, ignores AFK players |
| **AFK System & Mob Invulnerability** | ⚙️ *Disabled* | `afk.enabled: false` | Auto-AFK timer (60s), 3D overhead countdown, mob damage immunity |
| **Discord Role Synchronization** | ⚙️ *Disabled* | `role-sync.enabled: false` | Tab/chat prefixes, nametag colors, hierarchy sorting |
| **AntiSpam Engine** | ⚙️ *Disabled* | `antispam.enabled: false` | Flood protection, repeat blocking, automated mute ladder |
| **AI Chat Moderation** | ⚙️ *Disabled* | `ai-moderator.enabled: false` | Asynchronous toxicity analysis via AI (OpenRouter / ChatGPT) |
| **Role-Based Whitelist** | ⚙️ *Disabled* | `whitelist.enabled: false` | Restrict server entry to linked players or specific Discord roles |
| **Maintenance Mode** | ⚙️ *Disabled* | `maintenance.enabled: false` | Lockdown server with customizable kick screen (`/maintenance`) |
| **Scheduled Auto-Restart** | ⚙️ *Disabled* | `autorestart.times: []` | Timed reboots, title countdowns, `/queuerestart` empty server queue |
| **Periodic Player Tips** | ⚙️ *Disabled* | `tips.enabled: false` | Scheduled personalized tips delivered to players in chat |
| **BlueMap Auto-Reload** | ⚙️ *Disabled* | `bluemap.auto-reload.enabled: false` | Automatic timed web-map reloads |
| **Technical Monitoring & Sentry** | ⚙️ *Disabled* | `technical.*`, `sentry.enabled: false`| Low TPS / high RAM alerts, automatic Spark profiling, crash tracking |

---

### ✅ 1. Core Features (Enabled by Default)

#### 💬 Next-Gen Two-Way Cross-Chat (Minecraft ⇄ Discord)
* **Discord Webhooks**: In-game player messages are sent to Discord via webhooks displaying the sender's actual username and their real 3D head avatar.
* **Skin Texture Resolution**: Avatars are extracted directly from player texture hashes, ensuring custom skins render accurately for both online and offline mode players.
* **Smart Mentions**:
  * Typing `@PlayerName` in Minecraft highlights the name in yellow and plays an audio ping for the recipient.
  * If the player linked their Discord account, the mention automatically transforms into an authentic Discord ping (`<@id>`).
* **Discord ➔ Minecraft**: Messages from Discord channels stream into Minecraft chat with role colors and clean formatting.

#### 🌐 Event Localization & Navigation
* **Death Messages**: Translates over 100 official death causes (mobs, projectiles, falls, voids) and relays them to Discord in clean Ukrainian format.
* **Advancement Announcements**: Relays earned achievements to Discord with native names.
* **Interactive Death Coordinates**: Players receive a private message with their death coordinates and an interactive clickable BlueMap link that opens the exact death spot in their browser.

#### 📬 Offline Mail System
* Send messages to offline players via `/mail send <player> <message>`.
* Recipients are greeted with unread notifications upon their next login and can review them via `/mail read`.

#### 🔗 Account Linking
* **Account Verification**: Generates one-time codes in Minecraft for linking accounts via Discord slash command `/discord link` $\rightarrow$ `/link <code>`.
* Enables synchronized Smart Mentions and allows offline mail notifications to be forwarded to Discord.

#### 🎫 In-Game Ticket & Report System
* **Tickets from Minecraft**: Players can submit reports or help requests using `/ticket create <message>`.
* **Player Reports**: Rapidly report griefers or rule-breakers via `/report <player> <reason>` with anti-spam cooldown protection.
* **Interactive Discord Embeds**: Staff receives tickets and reports in a private channel with action buttons (claim, close) right inside Discord.

#### 💻 Remote Discord Console
* Dedicated private Discord channel mirroring server console logs in real time.
* Execute any server console command directly by typing it in the Discord channel.

#### 📊 Statistics, Leaderboards & Monitoring
* **Custom TabList**: Clean header and footer showing online count, TPS, ping, and server branding.
* **Stats & Leaderboards**: Inspect player playtime, death count, broken blocks, and achievements (`/stats`, `/top`).
* **Share Coordinates (`/sharecoords`)**: Broadcast Points of Interest with descriptions and a direct interactive BlueMap link.
* **Bot Diagnostics**: `/minecord bot status` and `/minecord bot reconnect` commands for managing Discord bot connectivity on the fly.

---

### ⚙️ 2. Optional Modules (Disabled by Default — Configured in config.yml)

#### 🩸 Hardcore Bloodmoon Event — 4 Difficulty Tiers
*Configuration: `fun.modes.bloodmoon.enabled: false`*

A comprehensive nocturnal apocalypse featuring dangerous hordes, aerial bombers, and epic boss battles:
* **Bed Sleep Blocked**: Sleeping in beds is completely locked out until sunrise.
* **4 Scaled Tiers & Rebalanced Loot Progression**:
  * **Tier 1 ("Bloodmoon")**: 2.5x mob HP multiplier, Speed & Strength buffs, 70% diamond gear, boss **"Blood Reaper"** (300 HP). Drops: iron, gold, occasional diamonds (no netherite).
  * **Tier 2 ("Blood Armageddon")**: 4.5x mob HP multiplier, 85% diamond/netherite gear, charged creepers, boss **"Abyssal Lord"** (550 HP). Hard cap: **maximum 2 netherite scraps** per player per night! Regular mob drop chance is 0.3% (~1 in 333 kills); boss has a 60% chance to drop 1 scrap (within the cap). Getting 2 scraps is a true feat of luck.
  * **Tier 3 ("Infernal Cataclysm")**: 6.5x mob HP multiplier, 95% full enchanted netherite, boss **"☠ Archdemon of Death ☠"** (850 HP). 0.8% mob scrap drop (~1–2 per night). Boss drops 1–2 scraps, 30% Nether Star chance, and 75% Totem of Undying chance.
  * **Tier 4 ("☠ Doomsday (Ragnarok) ☠")**: Extreme 10.0x mob HP multiplier, Strength IV, Resistance II, 100% max netherite, massive hordes up to 36 monsters, and colossal final boss **"☠ TITAN OF CHAOS ☠"** (1400 HP). Mob scrap drop: 1.5% (~2–4 per night), 3.5% diamonds, 0.1% Notch apples. Boss drops **1 pure Netherite Ingot**, 1 Nether Star, and 1–2 Totems!
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
  * Complete removal of artificial `GLOWING` outlines.
  * No despawning of cave mobs or mob farms: event mobs simply have their buffs cleanly stripped and revert to peaceful vanilla daylight state.
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

#### ⚡ Extended Custom Mechanics (Fun Modes)
*Configuration: `fun.enabled: false`*
* **Riptide without Rain** (`fun.modes.riptide_no_rain.enabled: false`): Launch yourself with Riptide-enchanted tridents on dry land without rain or water. Features fall-damage protection on initial landing and friendly-fire protection for boat/horse passengers. *(Regular server restarts are recommended to ensure seamless chunk loading during high-speed elytra flights)*.
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
* **Visual Status**: 3D overhead timer above the player's head and `[AFK]` tag in TabList.

#### 🏷️ Discord Role Synchronization (Role Sync)
*Configuration: `role-sync.enabled: false`*
* **Role Prefixes**: Automatic color-coded Discord role prefixes in TabList, in-game chat, and 3D nametags above player heads.
* **Hierarchy Sorting**: Players in the TabList are automatically sorted top-down based on role priority (Admin ➔ Moderator ➔ VIP ➔ Player).

#### 🛡️ Chat Security & AI Moderation
* **AntiSpam Engine** (`antispam.enabled: false`):
  * Protects against rapid flood and duplicate identical messages.
  * Multi-tiered penalty ladder: gentle warnings, message drops, and automated 5-minute mutes (`/unmute`).
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

#### 💡 Periodic Player Tips
*Configuration: `tips.enabled: false`*
* Personalized, scheduled in-game tips and advice sent individually to active online players.

#### 🗺️ BlueMap Auto-Reload
*Configuration: `bluemap.auto-reload.enabled: false`*
* Automatically executes `bluemap reload` at configured times or 2 minutes after startup.

#### 📈 Technical Monitoring & Crash Reporting (Sentry)
*Configuration: `technical.*`, `sentry.enabled: false`*
* **Performance Monitor**: Sends Discord alert embeds when TPS drops below threshold (15.0) or memory spikes (> 95%), with optional automated Spark profiler triggers.
* **Error Catcher & Sentry**: Captures uncaught exceptions and uploads stack traces to a paste service or cloud Sentry dashboard.

---

### 🏗️ 3. Vanilla Tech & Farm Exploits Support
* **Bedrock Breaking**: Vanilla methods to break bedrock and End portal frames are enabled (`allow-permanent-block-break-exploits: true`).
* **Headless Pistons**: Retaining headless pistons enabled for tech machinery (`allow-headless-pistons: true`).
* **Falling Block Duplication**: End portals allow vanilla sand, gravel, and concrete powder duping for large-scale farms (`allow-unsafe-end-portal-teleportation: true`).
* **TNT & Rail Duplication**: Piston duplication for primed TNT and rails is fully supported for tunnel bores and world eaters (`allow-piston-duplication: true`).

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
| `/afk [reason]` | Toggle AFK status manually (mob invulnerable, never blocks night skip) | Everyone |

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
