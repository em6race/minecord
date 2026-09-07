package com.example.minecord.utils;

import java.util.HashMap;
import java.util.Map;

public class AdvancementTranslator {
    private static final Map<String, String> translations = new HashMap<>();

    static {
        // --- story ---
        translations.put("story/root", "Minecraft");
        translations.put("story/mine_stone", "Кам'яна доба");
        translations.put("story/upgrade_tools", "Покращення обладнання");
        translations.put("story/smelt_iron", "Коваль");
        translations.put("story/obtain_armor", "Одягнись");
        translations.put("story/lava_bucket", "Гаряча штучка");
        translations.put("story/iron_tools", "Металеве покращення");
        translations.put("story/deflect_arrow", "Не сьогодні, спасибі");
        translations.put("story/form_obsidian", "Холодно-гаряче");
        translations.put("story/mine_diamond", "Діаманти!");
        translations.put("story/enter_the_nether", "Треба йти глибше");
        translations.put("story/shiny_gear", "Укрий мене діамантами");
        translations.put("story/enchant_item", "Чародій");
        translations.put("story/cure_zombie_villager", "Лікар для зомбі");
        translations.put("story/follow_ender_eye", "Око-шпигун");
        translations.put("story/enter_the_end", "Кінець?");
        // --- nether ---
        translations.put("nether/root", "Незер");
        translations.put("nether/find_fortress", "Страшна фортеця");
        translations.put("nether/get_wither_skull", "Гільйотина");
        translations.put("nether/obtain_blaze_rod", "У вогонь");
        translations.put("nether/brew_potion", "Приватна броварня");
        translations.put("nether/summon_wither", "Змій Горинич");
        translations.put("nether/all_potions", "Шалений коктейль");
        translations.put("nether/all_effects", "Як це трапилось?");
        translations.put("nether/uneasy_alliance", "Зрада");
        translations.put("nether/explore_nether", "Гаряча путівка");
        translations.put("nether/fast_travel", "Короткий шлях");
        translations.put("nether/find_bastion", "Таємниче минуле");
        translations.put("nether/obtain_ancient_debris", "Схована в глибинах");
        translations.put("nether/obtain_crying_obsidian", "Хто нарізає цибулю?");
        translations.put("nether/distract_piglin", "Ох, блищить!");
        translations.put("nether/loot_bastion", "Було ваше — стало наше!");
        translations.put("nether/ride_strider", "Човен, що крокує");
        translations.put("nether/ride_strider_in_overworld_lava", "Як удома");
        translations.put("nether/return_to_sender", "Повернути до відправника");
        translations.put("nether/use_lodestone", "Поверни мене додому");
        translations.put("nether/netherite_armor", "Укрий мене уламками");
        translations.put("nether/charge_respawn_anchor", "Не зовсім «дев'ять» життів");
        translations.put("nether/create_beacon", "Принеси маяк у домівку");
        translations.put("nether/create_full_beacon", "Маячня");
        // --- end ---
        translations.put("end/root", "Енд");
        translations.put("end/kill_dragon", "Звільніть Енд");
        translations.put("end/dragon_egg", "Нове покоління");
        translations.put("end/enter_end_gateway", "Віддалений портал");
        translations.put("end/find_end_city", "Віддалене місто");
        translations.put("end/elytra", "Вище неба");
        translations.put("end/levitate", "Вище за Говерлу");
        translations.put("end/dragon_breath", "Несвіжий подих");
        translations.put("end/respawn_dragon", "І знову… Енд…");
        // --- adventure ---
        translations.put("adventure/root", "Пригоди");
        translations.put("adventure/adventuring_time", "Час пригод");
        translations.put("adventure/sleep_in_bed", "Солодких снів");
        translations.put("adventure/hero_of_the_village", "Герой селища");
        translations.put("adventure/trade", "Заморський купець");
        translations.put("adventure/honey_block_slide", "Прилип, як муха до меду");
        translations.put("adventure/ol_betsy", "Старенька Бетсі");
        translations.put("adventure/whos_the_pillager_now", "І хто тут тепер розбійник?");
        translations.put("adventure/two_birds_one_arrow", "За двома птахами…");
        translations.put("adventure/shoot_arrow", "Цілься");
        translations.put("adventure/kill_a_mob", "Мисливець на монстрів");
        translations.put("adventure/kill_all_mobs", "Полювання на монстрів");
        translations.put("adventure/sniper_duel", "Снайперська дуель");
        translations.put("adventure/throw_trident", "Тризубець не горобець…");
        translations.put("adventure/totem_of_undying", "Це ще не кінець");
        translations.put("adventure/summon_iron_golem", "Скинулися всім селом");
        translations.put("adventure/trade_at_world_height", "Зоряний торговець");
        translations.put("adventure/very_very_frightening", "Лють Зевса");
        translations.put("adventure/lightning_rod_with_villager_no_fire", "Запобіжник");
        translations.put("adventure/fall_from_world_height", "Печери та скелі");
        translations.put("adventure/walk_on_powder_snow_with_leather_boots", "Снігохід");
        translations.put("adventure/avoid_vibration", "Тишком-нишком");
        translations.put("adventure/spyglass_at_parrot", "Це пташка?");
        translations.put("adventure/spyglass_at_ghast", "Це повітряна кулька?");
        translations.put("adventure/spyglass_at_dragon", "Це літак?");
        translations.put("adventure/bullseye", "У яблучко!");
        translations.put("adventure/read_power_from_chiseled_bookshelf", "Знання — сила");
        translations.put("adventure/under_lock_and_key", "Під замком і ключем");
        translations.put("adventure/voluntary_exile", "Добровільне вигнання");
        translations.put("adventure/who_needs_rockets", "Кому потрібні ракети?");
        translations.put("adventure/trim_with_any_armor_pattern", "Стиліст");
        translations.put("adventure/trim_with_all_exclusive_armor_patterns", "Кування зі стилем");
        translations.put("adventure/salvage_sherd", "Спадщина");
        translations.put("adventure/craft_decorated_pot_using_only_sherds", "Трипільська культура");
        translations.put("adventure/arbalistic", "Арбалістика");
        translations.put("adventure/overoverkill", "Перебір");
        translations.put("adventure/spear_many_mobs", "Моб'ячий шашлик");
        translations.put("adventure/kill_mob_near_sculk_catalyst", "Воно живе");
        translations.put("adventure/play_jukebox_in_meadows", "Звуки музики");
        translations.put("adventure/use_lodestone", "Поверни мене додому");
        translations.put("adventure/lighten_up", "Світло кожному з нас");
        translations.put("adventure/heart_transplanter", "Пересадка серця");
        translations.put("adventure/revaulting", "Розкриття");
        translations.put("adventure/minecraft_trials_edition", "Minecraft: Випробне видання");
        translations.put("adventure/crafters_crafting_crafters", "Нескінченний цикл");
        translations.put("adventure/blowback", "Як вітром здуло");
        translations.put("adventure/brush_armadillo", "Який щитрий");
        // --- husbandry ---
        translations.put("husbandry/root", "Сільське господарство");
        translations.put("husbandry/plant_seed", "Магія чорнозему");
        translations.put("husbandry/breed_an_animal", "Романтичний вечір");
        translations.put("husbandry/tame_an_animal", "Друзі назавжди");
        translations.put("husbandry/fishy_business", "На гачку");
        translations.put("husbandry/silk_touch_nest", "Дзижчить у кишенях");
        translations.put("husbandry/safely_harvest_honey", "Бдж-ж-жолиний гість");
        translations.put("husbandry/breed_all_animals", "Велика сім'я");
        translations.put("husbandry/complete_catalogue", "Повний «коталог»");
        translations.put("husbandry/balanced_diet", "Як не з'їм, то понадкушую");
        translations.put("husbandry/netherite_hoe", "Серйозні наміри");
        translations.put("husbandry/wax_on", "Наноси віск");
        translations.put("husbandry/wax_off", "Стирай віск");
        translations.put("husbandry/make_a_sign_glow", "Просвітлення");
        translations.put("husbandry/kill_axolotl_target", "Дружба — це диво!");
        translations.put("husbandry/axolotl_in_a_bucket", "Наймиліший хижак");
        translations.put("husbandry/tadpole_in_a_bucket", "Чудова кумпанія");
        translations.put("husbandry/froglights", "Жаб'ячі вогні, дайте сил мені!");
        translations.put("husbandry/leash_all_frog_variants", "Жаб'яча веселка");
        translations.put("husbandry/ride_a_boat_with_a_goat", "Вовк, коза і капуста");
        translations.put("husbandry/allay_deliver_item_to_player", "Я твій навіки друг");
        translations.put("husbandry/allay_deliver_cake_to_note_block", "Щастя і здоров'я");
        translations.put("husbandry/tactical_fishing", "Рибальська кмітливість");
        translations.put("husbandry/feed_snifflet", "Перші запахи");
        translations.put("husbandry/obtain_sniffer_egg", "А пахне як");
        translations.put("husbandry/plant_any_sniffer_seed", "Садимо минуле");
        translations.put("husbandry/whole_pack", "Повний гавкет");
        translations.put("husbandry/uh_oh", "Йой");
        translations.put("husbandry/repair_wolf_armor", "Як новеньке");
        translations.put("husbandry/remove_wolf_armor", "Філігранно");
        translations.put("husbandry/place_dried_ghast_in_water", "Час освіжитися!");
    }

    public static String translate(String key, String fallbackTitle) {
        return translations.getOrDefault(key, fallbackTitle);
    }
}
