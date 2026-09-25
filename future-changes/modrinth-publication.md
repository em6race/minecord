# Future Feature: Modrinth Publication Plan / План публікації на Modrinth

> **Status:** Planned  
> **Description:** Повний план підготовки, оформлення та автоматичної публікації плагіна **MineCord** на платформі [Modrinth](https://modrinth.com/).

---

## 1. Налаштування проєкту на Modrinth (Project Setup)

* **Project Type:** Plugin
* **Loaders:** `Paper`, `Purpur`, `Folia` (compatible)
* **Game Versions:** `26.2+` (1.21.4+)
* **Categories:** `chat`, `social`, `utility`, `management`
* **Project Name:** `MineCord`
* **License:** `MIT`
* **Default Language in Config:** `en` (English, із вбудованою підтримкою `uk` та `sk`)
* **Summary:** High-performance, modular Minecraft & Discord bridge with 3D webhook cross-chat, multi-language support (EN/UK/SK), smart sleep voting, leaderboards, coordinates sharing, remote console, and hardcore Bloodmoon events.

---

## 2. Візуальне оформлення та медіа (Release Assets & Presentation)

* **Icon / Logo:** Високоякісне лого (512x512 px) у стилі Minecraft + Discord.
* **Gallery (Скріншоти для галереї):**
  1. Двосторонній чат із Discord Webhooks та 3D-скінами голів гравців + Smart Mentions.
  2. Динамічний статус бота в Discord із відображенням TPS, онлайну та аптайму.
  3. Інтерактивні координати смерті та `/sharecoords` із прямим посиланням на 3D-мапу BlueMap.
  4. Рейтинги гравців `/top` та статистика `/stats` у грі й у вигляді Discord Embeds.
  5. Адмін-консоль у Discord та інтерактивні картки скарг `/report` із кнопками модерації.
  6. Подія «Кривавий Місяць» (Bloodmoon) з 4 рівнями складності та кастомними босами.
* **Description:** Двомовна презентація (English / Українська) з таблицями модулів, командами, правами доступу (permissions) та інструкцією зі зміни мови (`en`, `uk`, `sk`).

---

## 3. Автоматизація CI/CD (GitHub Actions ➔ Modrinth)

Інтеграція Gradle-плагіна `com.modrinth.minotaur` або GitHub Action (`Kirigami/modrinth-publish`) для автоматичного вивантаження `MineCord-*-all.jar` на Modrinth при створенні нового GitHub Release / Tag:

### Варіант A: Конфігурація в `build.gradle` (Minotaur)
```groovy
plugins {
    id "com.modrinth.minotaur" version "2.+"
}

modrinth {
    token = System.getenv("MODRINTH_TOKEN")
    projectId = "minecord"
    versionNumber = project.version
    versionName = "MineCord v${project.version}"
    versionType = "release"
    uploadFile = shadowJar
    gameVersions = ["26.2"]
    loaders = ["paper", "purpur"]
    syncBodyFrom = rootProject.file("README.md").text
}
```

### Варіант B: Крок у `.github/workflows/build.yml`
```yaml
- name: Publish to Modrinth
  if: startsWith(github.ref, 'refs/tags/')
  uses: Kirigami/modrinth-publish@v2
  with:
    token: ${{ secrets.MODRINTH_TOKEN }}
    id: "minecord"
    version-number: ${{ github.ref_name }}
    name: "MineCord ${{ github.ref_name }}"
    files: build/libs/MineCord-*-all.jar
    game-versions: '["26.2"]'
    loaders: '["paper", "purpur"]'
```

---

## 4. Бейдж для `README.md`

Після публікації додати офіційний бейдж у шапку `README.md`:
```markdown
[![Modrinth](https://img.shields.io/badge/Available_on-Modrinth-00AF5C?style=flat-square&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/minecord)
```
