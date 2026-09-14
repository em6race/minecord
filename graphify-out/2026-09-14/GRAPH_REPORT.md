# Graph Report - mcdc  (2026-09-14)

## Corpus Check
- 47 files · ~50,496 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 6 file(s) not represented in the graph (top: (none) 2, .properties 2, .jar 1)

## Summary
- 681 nodes · 1639 edges · 23 communities (15 shown, 6 thin omitted)
- Extraction: 90% EXTRACTED · 10% INFERRED · 0% AMBIGUOUS · INFERRED: 167 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cb6fe481`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- BloodmoonMode
- net.dv8tion.jda.api.hooks.ListenerAdapter
- org.bukkit.event.EventHandler
- BotManager
- PlayerCacheManager
- MineCord
- org.bukkit.entity.Player
- FunMode
- AutoRestartManager
- .onCommand
- SpearHungerMode
- 🚀 Key Features
- RiptideMode
- LeaderboardManager
- GitVersion
- TopEntry
- .reloadPlugin
- gradlew
- AdvancementTranslator
- Future Feature: Multi-Language System (UK/EN/SK) & English Code Comments
- GEMINI.md

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
- `BloodmoonMode` --references--> `MineCord`  [EXTRACTED]
  src/main/java/com/example/minecord/fun/BloodmoonMode.java → src/main/java/com/example/minecord/MineCord.java

## Import Cycles
- None detected.

## Communities (23 total, 6 thin omitted)

### Community 0 - "BloodmoonMode"
Cohesion: 0.05
Nodes (21): Creeper, DustOptions, EntityType, Monster, NamespacedKey, org.bukkit.boss.BarColor, Particle, Phantom (+13 more)

### Community 1 - "net.dv8tion.jda.api.hooks.ListenerAdapter"
Cohesion: 0.12
Nodes (13): net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent, net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent, net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent, net.dv8tion.jda.api.events.interaction.ModalInteractionEvent, net.dv8tion.jda.api.events.message.MessageReceivedEvent, net.dv8tion.jda.api.hooks.ListenerAdapter, org.bukkit.command.ConsoleCommandSender, DiscordChatListener (+5 more)

### Community 2 - "org.bukkit.event.EventHandler"
Cohesion: 0.05
Nodes (39): Entity, org.bukkit.boss.BossBar, org.bukkit.entity.Entity, org.bukkit.entity.TextDisplay, org.bukkit.event.entity.AreaEffectCloudApplyEvent, org.bukkit.event.entity.CreatureSpawnEvent, org.bukkit.event.entity.EntityDamageByEntityEvent, org.bukkit.event.entity.EntityDamageEvent (+31 more)

### Community 3 - "BotManager"
Cohesion: 0.09
Nodes (8): Handler, java.util.logging.Handler, net.dv8tion.jda.api.entities.channel.concrete.TextChannel, net.dv8tion.jda.api.JDA, BotManager, ConsoleManager, SkinHelper, WebhookManager

### Community 4 - "PlayerCacheManager"
Cohesion: 0.10
Nodes (3): org.bukkit.OfflinePlayer, PlayerCacheManager, PlayerXpData

### Community 5 - "MineCord"
Cohesion: 0.07
Nodes (21): org.bukkit.ChatColor, org.bukkit.command.Command, org.bukkit.command.CommandExecutor, org.bukkit.command.CommandSender, org.bukkit.command.TabCompleter, org.bukkit.Material, org.bukkit.plugin.java.JavaPlugin, Override (+13 more)

### Community 6 - "org.bukkit.entity.Player"
Cohesion: 0.10
Nodes (10): java.util.regex.Pattern, net.dv8tion.jda.api.entities.Member, net.dv8tion.jda.api.entities.Role, org.bukkit.entity.Player, org.bukkit.scoreboard.Team, DeathTranslator, TranslationRule, Pattern (+2 more)

### Community 7 - "FunMode"
Cohesion: 0.09
Nodes (4): FunManager, FunMode, Override, SmartAnvilMode

### Community 8 - "AutoRestartManager"
Cohesion: 0.12
Nodes (8): Material, MessageEmbed, net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent, net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent, DiscordCommandListener, ButtonInteractionEvent, Override, AutoRestartManager

### Community 9 - ".onCommand"
Cohesion: 0.08
Nodes (7): org.bukkit.configuration.file.FileConfiguration, org.bukkit.event.entity.PlayerDeathEvent, org.bukkit.event.player.AsyncPlayerPreLoginEvent, PlayerAdvancementDoneEvent, PlayerEventListener, Override, AccountLinkManager

### Community 10 - "SpearHungerMode"
Cohesion: 0.19
Nodes (4): org.bukkit.event.entity.EntityExhaustionEvent, org.bukkit.event.entity.FoodLevelChangeEvent, Override, SpearHungerMode

### Community 11 - "🚀 Key Features"
Cohesion: 0.06
Nodes (35): 10. 💻 Remote Discord Console, 10. 💻 Віддалена консоль в Discord, 11. 📊 Monitoring & Polish, 11. 📊 Моніторинг та оптимізація, 1. 💬 Next-Gen Two-Way Cross-Chat (Minecraft ⇄ Discord), 1. 💬 Двосторонній чат нового покоління (Minecraft ⇄ Discord), 2. 🩸 Hardcore Bloodmoon Event — 4 Difficulty Tiers, 2. 🩸 Хардкорний «Кривавий Місяць» (Bloodmoon) — 4 Рівні Складності (+27 more)

### Community 16 - ".reloadPlugin"
Cohesion: 0.07
Nodes (8): org.bukkit.scheduler.BukkitTask, org.bukkit.World, Override, BlueMapManager, PerformanceMonitor, PlayerTipManager, SleepManager, TabManager

### Community 17 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 21 - "Future Feature: Multi-Language System (UK/EN/SK) & English Code Comments"
Cohesion: 0.14
Nodes (13): 1. Overview & Goals, 2. Architecture, 3. Implementation Steps, 4. How to Activate When Needed, 5.1 Project Setup on Modrinth, 5.2 Release Assets & Presentation, 5.3 CI/CD Automation (GitHub Actions -> Modrinth), 5. Modrinth Publication Plan / План публікації на Modrinth (+5 more)

## Knowledge Gaps
- **45 isolated node(s):** `CHAOS_AMBUSH`, `SPIDER_SWARM`, `UNDEAD_LEGION`, `SAPPER_SQUAD`, `NETHER_VANGUARD` (+40 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 103 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `MineCord` connect `MineCord` to `BloodmoonMode`, `net.dv8tion.jda.api.hooks.ListenerAdapter`, `org.bukkit.event.EventHandler`, `BotManager`, `PlayerCacheManager`, `org.bukkit.entity.Player`, `FunMode`, `AutoRestartManager`, `.onCommand`, `SpearHungerMode`, `RiptideMode`, `LeaderboardManager`, `.reloadPlugin`?**
  _High betweenness centrality (0.483) - this node is a cross-community bridge._
- **Why does `BloodmoonMode` connect `BloodmoonMode` to `org.bukkit.event.EventHandler`, `MineCord`, `FunMode`, `.onCommand`, `.reloadPlugin`?**
  _High betweenness centrality (0.196) - this node is a cross-community bridge._
- **Why does `PlayerCacheManager` connect `PlayerCacheManager` to `.reloadPlugin`, `MineCord`?**
  _High betweenness centrality (0.111) - this node is a cross-community bridge._
- **What connects `CHAOS_AMBUSH`, `SPIDER_SWARM`, `UNDEAD_LEGION` to the rest of the system?**
  _45 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `BloodmoonMode` be split into smaller, more focused modules?**
  _Cohesion score 0.0509020618556701 - nodes in this community are weakly interconnected._
- **Should `net.dv8tion.jda.api.hooks.ListenerAdapter` be split into smaller, more focused modules?**
  _Cohesion score 0.12 - nodes in this community are weakly interconnected._
- **Should `org.bukkit.event.EventHandler` be split into smaller, more focused modules?**
  _Cohesion score 0.05116279069767442 - nodes in this community are weakly interconnected._