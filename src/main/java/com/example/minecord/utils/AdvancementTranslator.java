package com.example.minecord.utils;

import java.util.HashMap;
import java.util.Map;

public class AdvancementTranslator {
    private static final Map<String, String> ukTranslations = new HashMap<>();
    private static final Map<String, String> skTranslations = new HashMap<>();

    static {
        // ==================== UKRAINIAN (uk) ====================
        // --- story ---
        ukTranslations.put("story/root", "Minecraft");
        ukTranslations.put("story/mine_stone", "Кам'яна доба");
        ukTranslations.put("story/upgrade_tools", "Покращення обладнання");
        ukTranslations.put("story/smelt_iron", "Коваль");
        ukTranslations.put("story/obtain_armor", "Одягнись");
        ukTranslations.put("story/lava_bucket", "Гаряча штучка");
        ukTranslations.put("story/iron_tools", "Металеве покращення");
        ukTranslations.put("story/deflect_arrow", "Не сьогодні, спасибі");
        ukTranslations.put("story/form_obsidian", "Холодно-гаряче");
        ukTranslations.put("story/mine_diamond", "Діаманти!");
        ukTranslations.put("story/enter_the_nether", "Треба йти глибше");
        ukTranslations.put("story/shiny_gear", "Укрий мене діамантами");
        ukTranslations.put("story/enchant_item", "Чародій");
        ukTranslations.put("story/cure_zombie_villager", "Лікар для зомбі");
        ukTranslations.put("story/follow_ender_eye", "Око-шпигун");
        ukTranslations.put("story/enter_the_end", "Кінець?");
        // --- nether ---
        ukTranslations.put("nether/root", "Незер");
        ukTranslations.put("nether/find_fortress", "Страшна фортеця");
        ukTranslations.put("nether/get_wither_skull", "Гільйотина");
        ukTranslations.put("nether/obtain_blaze_rod", "У вогонь");
        ukTranslations.put("nether/brew_potion", "Приватна броварня");
        ukTranslations.put("nether/summon_wither", "Змій Горинич");
        ukTranslations.put("nether/all_potions", "Шалений коктейль");
        ukTranslations.put("nether/all_effects", "Як це трапилось?");
        ukTranslations.put("nether/uneasy_alliance", "Зрада");
        ukTranslations.put("nether/explore_nether", "Гаряча путівка");
        ukTranslations.put("nether/fast_travel", "Короткий шлях");
        ukTranslations.put("nether/find_bastion", "Таємниче минуле");
        ukTranslations.put("nether/obtain_ancient_debris", "Схована в глибинах");
        ukTranslations.put("nether/obtain_crying_obsidian", "Хто нарізає цибулю?");
        ukTranslations.put("nether/distract_piglin", "Ох, блищить!");
        ukTranslations.put("nether/loot_bastion", "Було ваше — стало наше!");
        ukTranslations.put("nether/ride_strider", "Човен, що крокує");
        ukTranslations.put("nether/ride_strider_in_overworld_lava", "Як удома");
        ukTranslations.put("nether/return_to_sender", "Повернути до відправника");
        ukTranslations.put("nether/use_lodestone", "Поверни мене додому");
        ukTranslations.put("nether/netherite_armor", "Укрий мене уламками");
        ukTranslations.put("nether/charge_respawn_anchor", "Не зовсім «дев'ять» життів");
        ukTranslations.put("nether/create_beacon", "Принеси маяк у домівку");
        ukTranslations.put("nether/create_full_beacon", "Маячня");
        // --- end ---
        ukTranslations.put("end/root", "Енд");
        ukTranslations.put("end/kill_dragon", "Звільніть Енд");
        ukTranslations.put("end/dragon_egg", "Нове покоління");
        ukTranslations.put("end/enter_end_gateway", "Віддалений портал");
        ukTranslations.put("end/find_end_city", "Віддалене місто");
        ukTranslations.put("end/elytra", "Вище неба");
        ukTranslations.put("end/levitate", "Вище за Говерлу");
        ukTranslations.put("end/dragon_breath", "Несвіжий подих");
        ukTranslations.put("end/respawn_dragon", "І знову… Енд…");
        // --- adventure ---
        ukTranslations.put("adventure/root", "Пригоди");
        ukTranslations.put("adventure/adventuring_time", "Час пригод");
        ukTranslations.put("adventure/sleep_in_bed", "Солодких снів");
        ukTranslations.put("adventure/hero_of_the_village", "Герой селища");
        ukTranslations.put("adventure/trade", "Заморський купець");
        ukTranslations.put("adventure/honey_block_slide", "Прилип, як муха до меду");
        ukTranslations.put("adventure/ol_betsy", "Старенька Бетсі");
        ukTranslations.put("adventure/whos_the_pillager_now", "І хто тут тепер розбійник?");
        ukTranslations.put("adventure/two_birds_one_arrow", "За двома птахами…");
        ukTranslations.put("adventure/shoot_arrow", "Цілься");
        ukTranslations.put("adventure/kill_a_mob", "Мисливець на монстрів");
        ukTranslations.put("adventure/kill_all_mobs", "Полювання на монстрів");
        ukTranslations.put("adventure/sniper_duel", "Снайперська дуель");
        ukTranslations.put("adventure/throw_trident", "Тризубець не горобець…");
        ukTranslations.put("adventure/totem_of_undying", "Це ще не кінець");
        ukTranslations.put("adventure/summon_iron_golem", "Скинулися всім селом");
        ukTranslations.put("adventure/trade_at_world_height", "Зоряний торговець");
        ukTranslations.put("adventure/very_very_frightening", "Лють Зевса");
        ukTranslations.put("adventure/lightning_rod_with_villager_no_fire", "Запобіжник");
        ukTranslations.put("adventure/fall_from_world_height", "Печери та скелі");
        ukTranslations.put("adventure/walk_on_powder_snow_with_leather_boots", "Снігохід");
        ukTranslations.put("adventure/avoid_vibration", "Тишком-нишком");
        ukTranslations.put("adventure/spyglass_at_parrot", "Це пташка?");
        ukTranslations.put("adventure/spyglass_at_ghast", "Це повітряна кулька?");
        ukTranslations.put("adventure/spyglass_at_dragon", "Це літак?");
        ukTranslations.put("adventure/bullseye", "У яблучко!");
        ukTranslations.put("adventure/read_power_from_chiseled_bookshelf", "Знання — сила");
        ukTranslations.put("adventure/under_lock_and_key", "Під замком і ключем");
        ukTranslations.put("adventure/voluntary_exile", "Добровільне вигнання");
        ukTranslations.put("adventure/who_needs_rockets", "Кому потрібні ракети?");
        ukTranslations.put("adventure/trim_with_any_armor_pattern", "Стиліст");
        ukTranslations.put("adventure/trim_with_all_exclusive_armor_patterns", "Кування зі стилем");
        ukTranslations.put("adventure/salvage_sherd", "Спадщина");
        ukTranslations.put("adventure/craft_decorated_pot_using_only_sherds", "Трипільська культура");
        ukTranslations.put("adventure/arbalistic", "Арбалістика");
        ukTranslations.put("adventure/overoverkill", "Перебір");
        ukTranslations.put("adventure/spear_many_mobs", "Моб'ячий шашлик");
        ukTranslations.put("adventure/kill_mob_near_sculk_catalyst", "Воно живе");
        ukTranslations.put("adventure/play_jukebox_in_meadows", "Звуки музики");
        ukTranslations.put("adventure/use_lodestone", "Поверни мене додому");
        ukTranslations.put("adventure/lighten_up", "Світло кожному з нас");
        ukTranslations.put("adventure/heart_transplanter", "Пересадка серця");
        ukTranslations.put("adventure/revaulting", "Розкриття");
        ukTranslations.put("adventure/minecraft_trials_edition", "Minecraft: Випробне видання");
        ukTranslations.put("adventure/crafters_crafting_crafters", "Нескінченний цикл");
        ukTranslations.put("adventure/blowback", "Як вітром здуло");
        ukTranslations.put("adventure/brush_armadillo", "Який щитрий");
        // --- husbandry ---
        ukTranslations.put("husbandry/root", "Сільське господарство");
        ukTranslations.put("husbandry/plant_seed", "Магія чорнозему");
        ukTranslations.put("husbandry/breed_an_animal", "Романтичний вечір");
        ukTranslations.put("husbandry/tame_an_animal", "Друзі назавжди");
        ukTranslations.put("husbandry/fishy_business", "На гачку");
        ukTranslations.put("husbandry/silk_touch_nest", "Дзижчить у кишенях");
        ukTranslations.put("husbandry/safely_harvest_honey", "Бдж-ж-жолиний гість");
        ukTranslations.put("husbandry/breed_all_animals", "Велика сім'я");
        ukTranslations.put("husbandry/complete_catalogue", "Повний «коталог»");
        ukTranslations.put("husbandry/balanced_diet", "Як не з'їм, то понадкушую");
        ukTranslations.put("husbandry/netherite_hoe", "Серйозні наміри");
        ukTranslations.put("husbandry/wax_on", "Наноси віск");
        ukTranslations.put("husbandry/wax_off", "Стирай віск");
        ukTranslations.put("husbandry/make_a_sign_glow", "Просвітлення");
        ukTranslations.put("husbandry/kill_axolotl_target", "Дружба — це диво!");
        ukTranslations.put("husbandry/axolotl_in_a_bucket", "Наймиліший хижак");
        ukTranslations.put("husbandry/tadpole_in_a_bucket", "Чудова кумпанія");
        ukTranslations.put("husbandry/froglights", "Жаб'ячі вогні, дайте сил мені!");
        ukTranslations.put("husbandry/leash_all_frog_variants", "Жаб'яча веселка");
        ukTranslations.put("husbandry/ride_a_boat_with_a_goat", "Вовк, коза і капуста");
        ukTranslations.put("husbandry/allay_deliver_item_to_player", "Я твій навіки друг");
        ukTranslations.put("husbandry/allay_deliver_cake_to_note_block", "Щастя і здоров'я");
        ukTranslations.put("husbandry/tactical_fishing", "Рибальська кмітливість");
        ukTranslations.put("husbandry/feed_snifflet", "Перші запахи");
        ukTranslations.put("husbandry/obtain_sniffer_egg", "А пахне як");
        ukTranslations.put("husbandry/plant_any_sniffer_seed", "Садимо минуле");
        ukTranslations.put("husbandry/whole_pack", "Повний гавкет");
        ukTranslations.put("husbandry/uh_oh", "Йой");
        ukTranslations.put("husbandry/repair_wolf_armor", "Як новеньке");
        ukTranslations.put("husbandry/remove_wolf_armor", "Філігранно");
        ukTranslations.put("husbandry/place_dried_ghast_in_water", "Час освіжитися!");

        // ==================== SLOVAK (sk) ====================
        // --- story ---
        skTranslations.put("story/root", "Minecraft");
        skTranslations.put("story/mine_stone", "Doba kamenná");
        skTranslations.put("story/upgrade_tools", "Lepšie vybavenie");
        skTranslations.put("story/smelt_iron", "Doba železná");
        skTranslations.put("story/obtain_armor", "Obleč sa");
        skTranslations.put("story/lava_bucket", "Horúci tovar");
        skTranslations.put("story/iron_tools", "Nie je to také zlé?");
        skTranslations.put("story/deflect_arrow", "Dnes nie, ďakujem");
        skTranslations.put("story/form_obsidian", "Doba ľadová");
        skTranslations.put("story/mine_diamond", "Diamanty!");
        skTranslations.put("story/enter_the_nether", "Musíme ísť hlbšie");
        skTranslations.put("story/shiny_gear", "Obklop sa diamantmi");
        skTranslations.put("story/enchant_item", "Čarodejník");
        skTranslations.put("story/cure_zombie_villager", "Zombie doktor");
        skTranslations.put("story/follow_ender_eye", "Špionážne oko");
        skTranslations.put("story/enter_the_end", "Koniec?");
        // --- nether ---
        skTranslations.put("nether/root", "Nether");
        skTranslations.put("nether/find_fortress", "Hrozivá pevnosť");
        skTranslations.put("nether/get_wither_skull", "Strašidelný strašidelný kostlivec");
        skTranslations.put("nether/obtain_blaze_rod", "Do ohňa");
        skTranslations.put("nether/brew_potion", "Miestny pivovar");
        skTranslations.put("nether/summon_wither", "Chradnúce výšiny");
        skTranslations.put("nether/all_potions", "Zúrivý koktail");
        skTranslations.put("nether/all_effects", "Ako sme sa sem dostali?");
        skTranslations.put("nether/uneasy_alliance", "Krehké spojenectvo");
        skTranslations.put("nether/explore_nether", "Horúce turistické destinácie");
        skTranslations.put("nether/fast_travel", "Podpriestorová bublina");
        skTranslations.put("nether/find_bastion", "Dávne časy");
        skTranslations.put("nether/obtain_ancient_debris", "Skryté v hlbinách");
        skTranslations.put("nether/obtain_crying_obsidian", "Kto tu krája cibuľu?");
        skTranslations.put("nether/distract_piglin", "Ó, to sa blyští!");
        skTranslations.put("nether/loot_bastion", "Vojnové ošípané");
        skTranslations.put("nether/ride_strider", "Tento čln má nohy");
        skTranslations.put("nether/ride_strider_in_overworld_lava", "Ako doma");
        skTranslations.put("nether/return_to_sender", "Späť odosielateľovi");
        skTranslations.put("nether/use_lodestone", "Vidiecka cesta, zaveď ma domov");
        skTranslations.put("nether/netherite_armor", "Pokry ma troskami");
        skTranslations.put("nether/charge_respawn_anchor", "Nie celkom deväť životov");
        skTranslations.put("nether/create_beacon", "Prines maják domov");
        skTranslations.put("nether/create_full_beacon", "Strážca majáku");
        // --- end ---
        skTranslations.put("end/root", "End");
        skTranslations.put("end/kill_dragon", "Osloboď End");
        skTranslations.put("end/dragon_egg", "Nová generácia");
        skTranslations.put("end/enter_end_gateway", "Vzdialený útek");
        skTranslations.put("end/find_end_city", "Mesto na konci hry");
        skTranslations.put("end/elytra", "Kam až obloha siaha");
        skTranslations.put("end/levitate", "Skvelý výhľad odtiaľto zhora");
        skTranslations.put("end/dragon_breath", "Potrebuješ mentolku");
        skTranslations.put("end/respawn_dragon", "Koniec... Znovu...");
        // --- adventure ---
        skTranslations.put("adventure/root", "Dobrodružstvo");
        skTranslations.put("adventure/adventuring_time", "Čas na dobrodružstvo");
        skTranslations.put("adventure/sleep_in_bed", "Sladké sny");
        skTranslations.put("adventure/hero_of_the_village", "Hrdina dediny");
        skTranslations.put("adventure/trade", "To je ale obchod!");
        skTranslations.put("adventure/honey_block_slide", "Lepkavá situácia");
        skTranslations.put("adventure/ol_betsy", "Stará dobrá Betsy");
        skTranslations.put("adventure/whos_the_pillager_now", "Kto je tu teraz plieniteľ?");
        skTranslations.put("adventure/two_birds_one_arrow", "Dve muchy jednou ranou");
        skTranslations.put("adventure/shoot_arrow", "Zásah");
        skTranslations.put("adventure/kill_a_mob", "Lovec príšer");
        skTranslations.put("adventure/kill_all_mobs", "Monštrá ulovené");
        skTranslations.put("adventure/sniper_duel", "Ostreľovačský súboj");
        skTranslations.put("adventure/throw_trident", "Jednorazový vtip");
        skTranslations.put("adventure/totem_of_undying", "Po smrti");
        skTranslations.put("adventure/summon_iron_golem", "Najal pomoc");
        skTranslations.put("adventure/trade_at_world_height", "Hviezdny kupec");
        skTranslations.put("adventure/very_very_frightening", "Veľmi, veľmi desivé");
        skTranslations.put("adventure/lightning_rod_with_villager_no_fire", "Bleskozvod");
        skTranslations.put("adventure/fall_from_world_height", "Jaskyne a útesy");
        skTranslations.put("adventure/walk_on_powder_snow_with_leather_boots", "Ľahký ako pierko");
        skTranslations.put("adventure/avoid_vibration", "Nenápadnosť 100");
        skTranslations.put("adventure/spyglass_at_parrot", "Je to vták?");
        skTranslations.put("adventure/spyglass_at_ghast", "Je to balón?");
        skTranslations.put("adventure/spyglass_at_dragon", "Je to lietadlo?");
        skTranslations.put("adventure/bullseye", "Zásah do čierneho");
        skTranslations.put("adventure/read_power_from_chiseled_bookshelf", "Sila poznania");
        skTranslations.put("adventure/under_lock_and_key", "Pod zámkom a kľúčom");
        skTranslations.put("adventure/voluntary_exile", "Dobrovoľné vyhnanstvo");
        skTranslations.put("adventure/who_needs_rockets", "Kto potrebuje rakety?");
        skTranslations.put("adventure/trim_with_any_armor_pattern", "Tvorca štýlu");
        skTranslations.put("adventure/trim_with_all_exclusive_armor_patterns", "Kovanie so štýlom");
        skTranslations.put("adventure/salvage_sherd", "Úcta k minulosti");
        skTranslations.put("adventure/craft_decorated_pot_using_only_sherds", "Pozbieral črepy");
        skTranslations.put("adventure/arbalistic", "Arbaletista");
        skTranslations.put("adventure/overoverkill", "Až príliš silný úder");
        skTranslations.put("adventure/spear_many_mobs", "Kebab z príšer");
        skTranslations.put("adventure/kill_mob_near_sculk_catalyst", "Ono to žije");
        skTranslations.put("adventure/play_jukebox_in_meadows", "Zvuky hudby");
        skTranslations.put("adventure/use_lodestone", "Cesta domov");
        skTranslations.put("adventure/lighten_up", "Rozsvieť to");
        skTranslations.put("adventure/heart_transplanter", "Transplantácia jadra");
        skTranslations.put("adventure/revaulting", "Odomknutie trezoru");
        skTranslations.put("adventure/minecraft_trials_edition", "Minecraft: Edícia skúšok");
        skTranslations.put("adventure/crafters_crafting_crafters", "Nekonečný cyklus");
        skTranslations.put("adventure/blowback", "Odfúknutý");
        skTranslations.put("adventure/brush_armadillo", "Očistený pásavec");
        // --- husbandry ---
        skTranslations.put("husbandry/root", "Poľnohospodárstvo");
        skTranslations.put("husbandry/plant_seed", "Semienkové miesto");
        skTranslations.put("husbandry/breed_an_animal", "Papagáje a netopiere");
        skTranslations.put("husbandry/tame_an_animal", "Najlepší priatelia navždy");
        skTranslations.put("husbandry/fishy_business", "Rybárske remeslo");
        skTranslations.put("husbandry/silk_touch_nest", "Úplná včelovosť");
        skTranslations.put("husbandry/safely_harvest_honey", "Buď naším hosťom");
        skTranslations.put("husbandry/breed_all_animals", "Dvaja po dvoch");
        skTranslations.put("husbandry/complete_catalogue", "Kompletný mačkalóg");
        skTranslations.put("husbandry/balanced_diet", "Vyvážená strava");
        skTranslations.put("husbandry/netherite_hoe", "Vážne odhodlanie");
        skTranslations.put("husbandry/wax_on", "Navoskovať");
        skTranslations.put("husbandry/wax_off", "Odvoskovať");
        skTranslations.put("husbandry/make_a_sign_glow", "Rozjasniť");
        skTranslations.put("husbandry/kill_axolotl_target", "Liečivá sila priateľstva!");
        skTranslations.put("husbandry/axolotl_in_a_bucket", "Najroztomilejší predátor");
        skTranslations.put("husbandry/tadpole_in_a_bucket", "Žubrienka vo vedre");
        skTranslations.put("husbandry/froglights", "Naše sily sa spojili!");
        skTranslations.put("husbandry/leash_all_frog_variants", "Keď družina skáče do mesta");
        skTranslations.put("husbandry/ride_a_boat_with_a_goat", "Čln s kozou");
        skTranslations.put("husbandry/allay_deliver_item_to_player", "Máš vo mne priateľa");
        skTranslations.put("husbandry/allay_deliver_cake_to_note_block", "Pieseň k narodeninám");
        skTranslations.put("husbandry/tactical_fishing", "Taktický rybolov");
        skTranslations.put("husbandry/feed_snifflet", "Prvé vône");
        skTranslations.put("husbandry/obtain_sniffer_egg", "Už to cítiš?");
        skTranslations.put("husbandry/plant_any_sniffer_seed", "Zasaď minulosť");
        skTranslations.put("husbandry/whole_pack", "Celá svorka");
        skTranslations.put("husbandry/uh_oh", "Och nie");
        skTranslations.put("husbandry/repair_wolf_armor", "Ako nové");
        skTranslations.put("husbandry/remove_wolf_armor", "Bezpečne odstrojený");
        skTranslations.put("husbandry/place_dried_ghast_in_water", "Čas na osvieženie!");
    }

    public static String translate(String key, String fallbackTitle) {
        return translate(key, fallbackTitle, "uk");
    }

    public static String translate(String key, String fallbackTitle, String lang) {
        if (lang == null || lang.equalsIgnoreCase("en")) {
            return fallbackTitle;
        }
        if (lang.equalsIgnoreCase("sk")) {
            return skTranslations.getOrDefault(key, fallbackTitle);
        }
        return ukTranslations.getOrDefault(key, fallbackTitle);
    }
}
