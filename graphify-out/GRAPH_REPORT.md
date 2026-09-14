# Graph Report - mcdc  (2026-09-14)

## Corpus Check
- 47 files · ~51,479 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 6 file(s) not represented in the graph (top: (none) 2, .properties 2, .jar 1)

## Summary
- 683 nodes · 1641 edges · 35 communities (17 shown, 16 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 167 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `eb9a52b1`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BloodmoonMode
- .onMessageReceived
- org.bukkit.event.EventHandler
- BotManager
- PlayerCacheManager
- org.bukkit.command.CommandSender
- org.bukkit.entity.Player
- FunMode
- AutoRestartManager
- AccountLinkManager
- SpearHungerMode
- 🚀 Key Features
- BloodmoonMode.java
- LeaderboardManager
- GitVersion
- TopEntry
- .reloadPlugin
- gradlew
- AdvancementTranslator
- Future Feature: Multi-Language System (UK/EN/SK) & English Code Comments
- MineCord
- GEMINI.md
- SmartAnvilMode
- ConsoleManager
- .getBotManager
- .onEnable
- .getPlayerCacheManager
- .getAvatarUrl
- ShareCoordsCommand
- AntiSpamManager
- SleepManager
- ReportCommand
- TabManager

## God Nodes (most connected - your core abstractions)
1. `MineCord` - 117 edges
2. `BloodmoonMode` - 79 edges
3. `PlayerCacheManager` - 52 edges
4. `BloodmoonTier` - 32 edges
5. `AfkManager` - 32 edges
6. `RoleSyncManager` - 28 edges
7. `AccountLinkManager` - 23 edges
8. `BotManager` - 21 edges
9. `AutoRestartManager` - 21 edges
10. `SpearHungerMode` - 18 edges

## Surprising Connections (you probably didn't know these)
- `BotManager` --references--> `MineCord`  [EXTRACTED]
  src/main/java/com/example/minecord/bot/BotManager.java → src/main/java/com/example/minecord/MineCord.java
- `DiscordChatListener` --references--> `MineCord`  [EXTRACTED]
  src/main/java/com/example/minecord/bot/DiscordChatListener.java → src/main/java/com/example/minecord/MineCord.java
- `DiscordCommandListener` --references--> `MineCord`  [EXTRACTED]
  src/main/java/com/example/minecord/bot/DiscordCommandListener.java → src/main/java/com/example/minecord/MineCord.java
- `DiscordTicketListener` --references--> `MineCord`  [EXTRACTED]
  src/main/java/com/example/minecord/bot/DiscordTicketListener.java → src/main/java/com/example/minecord/MineCord.java
- `ReportCommand` --references--> `MineCord`  [EXTRACTED]
  src/main/java/com/example/minecord/commands/ReportCommand.java → src/main/java/com/example/minecord/MineCord.java

## Import Cycles
- None detected.

## Communities (35 total, 16 thin omitted)

### Community 0 - "BloodmoonMode"
Cohesion: 0.05
Nodes (15): Creeper, DustOptions, EntityType, Monster, NamespacedKey, org.bukkit.boss.BarColor, Particle, Phantom (+7 more)

### Community 1 - ".onMessageReceived"
Cohesion: 0.36
Nodes (4): net.dv8tion.jda.api.events.message.MessageReceivedEvent, org.bukkit.command.ConsoleCommandSender, DiscordChatListener, Override

### Community 2 - "org.bukkit.event.EventHandler"
Cohesion: 0.07
Nodes (22): org.bukkit.entity.Entity, org.bukkit.entity.TextDisplay, org.bukkit.event.entity.EntityDamageEvent, org.bukkit.event.entity.EntityTargetLivingEntityEvent, org.bukkit.event.EventHandler, org.bukkit.event.player.AsyncPlayerChatEvent, org.bukkit.event.player.PlayerBedEnterEvent, org.bukkit.event.player.PlayerBedLeaveEvent (+14 more)

### Community 3 - "BotManager"
Cohesion: 0.16
Nodes (4): net.dv8tion.jda.api.entities.channel.concrete.TextChannel, net.dv8tion.jda.api.JDA, BotManager, WebhookManager

### Community 4 - "PlayerCacheManager"
Cohesion: 0.10
Nodes (3): org.bukkit.OfflinePlayer, PlayerCacheManager, PlayerXpData

### Community 5 - "org.bukkit.command.CommandSender"
Cohesion: 0.24
Nodes (9): org.bukkit.ChatColor, org.bukkit.command.Command, org.bukkit.command.CommandExecutor, org.bukkit.command.CommandSender, org.bukkit.command.TabCompleter, Override, TopCommand, MineCordCommand (+1 more)

### Community 6 - "org.bukkit.entity.Player"
Cohesion: 0.08
Nodes (11): java.util.regex.Pattern, net.dv8tion.jda.api.entities.Member, org.bukkit.entity.Player, org.bukkit.scheduler.BukkitTask, org.bukkit.scoreboard.Team, DeathTranslator, TranslationRule, PlayerTipManager (+3 more)

### Community 8 - "AutoRestartManager"
Cohesion: 0.07
Nodes (18): Material, MessageEmbed, net.dv8tion.jda.api.entities.Role, net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent, net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent, net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent, net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent, net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent (+10 more)

### Community 10 - "SpearHungerMode"
Cohesion: 0.17
Nodes (6): org.bukkit.event.entity.EntityDamageByEntityEvent, org.bukkit.event.entity.EntityExhaustionEvent, org.bukkit.event.entity.FoodLevelChangeEvent, org.bukkit.inventory.ItemStack, Override, SpearHungerMode

### Community 11 - "🚀 Key Features"
Cohesion: 0.05
Nodes (37): 10. 💻 Remote Discord Console, 10. 💻 Віддалена консоль в Discord, 11. 📊 Monitoring & Polish, 11. 📊 Моніторинг та оптимізація, 12. ⚙️ Vanilla Tech & Farm Exploits Support, 12. ⚙️ Підтримка ванільних механік та ферм (Vanilla Exploits), 1. 💬 Next-Gen Two-Way Cross-Chat (Minecraft ⇄ Discord), 1. 💬 Двосторонній чат нового покоління (Minecraft ⇄ Discord) (+29 more)

### Community 12 - "BloodmoonMode.java"
Cohesion: 0.09
Nodes (17): Entity, org.bukkit.boss.BossBar, org.bukkit.event.entity.AreaEffectCloudApplyEvent, org.bukkit.event.entity.CreatureSpawnEvent, org.bukkit.event.entity.EntityDeathEvent, org.bukkit.event.entity.EntityExplodeEvent, org.bukkit.event.entity.ExplosionPrimeEvent, org.bukkit.event.player.PlayerJoinEvent (+9 more)

### Community 17 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "Future Feature: Multi-Language System (UK/EN/SK) & English Code Comments"
Cohesion: 0.14
Nodes (13): 1. Overview & Goals, 2. Architecture, 3. Implementation Steps, 4. How to Activate When Needed, 5.1 Project Setup on Modrinth, 5.2 Release Assets & Presentation, 5.3 CI/CD Automation (GitHub Actions -> Modrinth), 5. Modrinth Publication Plan / План публікації на Modrinth (+5 more)

### Community 22 - "MineCord"
Cohesion: 0.14
Nodes (9): org.bukkit.event.Listener, org.bukkit.event.player.PlayerLevelChangeEvent, org.bukkit.event.player.PlayerLoginEvent, org.bukkit.Material, org.bukkit.plugin.java.JavaPlugin, ChatListener, PlayerEventListener, MineCord (+1 more)

### Community 24 - "SmartAnvilMode"
Cohesion: 0.17
Nodes (6): org.bukkit.event.inventory.InventoryClickEvent, org.bukkit.event.inventory.InventoryOpenEvent, org.bukkit.event.inventory.PrepareAnvilEvent, org.bukkit.Sound, Override, SmartAnvilMode

### Community 25 - "ConsoleManager"
Cohesion: 0.22
Nodes (3): Handler, java.util.logging.Handler, ConsoleManager

### Community 28 - ".getPlayerCacheManager"
Cohesion: 0.33
Nodes (3): PlayerAdvancementDoneEvent, Override, StatsCommand

## Knowledge Gaps
- **47 isolated node(s):** `CHAOS_AMBUSH`, `SPIDER_SWARM`, `UNDEAD_LEGION`, `SAPPER_SQUAD`, `NETHER_VANGUARD` (+42 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 105 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **16 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MineCord` connect `MineCord` to `BloodmoonMode`, `.onMessageReceived`, `org.bukkit.event.EventHandler`, `BotManager`, `PlayerCacheManager`, `org.bukkit.command.CommandSender`, `org.bukkit.entity.Player`, `FunMode`, `AutoRestartManager`, `AccountLinkManager`, `SpearHungerMode`, `BloodmoonMode.java`, `LeaderboardManager`, `.reloadPlugin`, `SmartAnvilMode`, `ConsoleManager`, `.getBotManager`, `.onEnable`, `.getPlayerCacheManager`, `.getAvatarUrl`, `ShareCoordsCommand`, `AntiSpamManager`, `SleepManager`, `ReportCommand`, `TabManager`?**
  _High betweenness centrality (0.480) - this node is a cross-community bridge._
- **Why does `BloodmoonMode` connect `BloodmoonMode` to `SleepManager`, `org.bukkit.event.EventHandler`, `org.bukkit.entity.Player`, `FunMode`, `BloodmoonMode.java`, `MineCord`?**
  _High betweenness centrality (0.195) - this node is a cross-community bridge._
- **Why does `PlayerCacheManager` connect `PlayerCacheManager` to `.onEnable`, `.getPlayerCacheManager`, `MineCord`?**
  _High betweenness centrality (0.110) - this node is a cross-community bridge._
- **What connects `CHAOS_AMBUSH`, `SPIDER_SWARM`, `UNDEAD_LEGION` to the rest of the system?**
  _47 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BloodmoonMode` be split into smaller, more focused modules?**
  _Cohesion score 0.054199328107502796 - nodes in this community are weakly interconnected._
- **Should `org.bukkit.event.EventHandler` be split into smaller, more focused modules?**
  _Cohesion score 0.06971153846153846 - nodes in this community are weakly interconnected._
- **Should `PlayerCacheManager` be split into smaller, more focused modules?**
  _Cohesion score 0.09954751131221719 - nodes in this community are weakly interconnected._