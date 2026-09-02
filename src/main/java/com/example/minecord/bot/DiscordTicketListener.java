package com.example.minecord.bot;

import com.example.minecord.MineCord;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DiscordTicketListener extends ListenerAdapter {

    private final MineCord plugin;

    public DiscordTicketListener(MineCord plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        // Кнопка створення заявки
        if (event.getComponentId().equals("ticket_create")) {
            TextInput nicknameInput = TextInput.create("mc_nickname", "Ваш нікнейм у Minecraft", TextInputStyle.SHORT)
                    .setPlaceholder("Наприклад: Steve")
                    .setMinLength(3)
                    .setMaxLength(16)
                    .setRequired(true)
                    .build();

            Modal modal = Modal.create("ticket_modal", "Заявка на сервер")
                    .addComponents(ActionRow.of(nicknameInput))
                    .build();

            event.replyModal(modal).queue();
            return;
        }

        // Обробка прийняття/відхилення модератором
        if (event.getComponentId().startsWith("ticket_accept_") || event.getComponentId().startsWith("ticket_reject_")) {
            // Перевіряємо чи має право (чи це адмін/модератор)
            // В даному випадку ми довіряємо каналу, але краще перевірити права
            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES)) {
                event.reply("❌ У вас немає прав керувати заявками (потрібне право MANAGE_ROLES) !").setEphemeral(true).queue();
                return;
            }

            String[] parts = event.getComponentId().split("_", 4);
            if (parts.length < 3) return;

            String action = parts[1];
            String targetDiscordId = parts[2];
            String nickname = parts.length > 3 ? parts[3] : "Unknown";

            Guild guild = event.getGuild();
            if (guild == null) return;

            if (action.equals("accept")) {
                // Генерація Offline UUID (як на піратських серверах)
                UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
                
                // Зберігаємо прив'язку
                plugin.getLinkManager().linkAccountDirectly(offlineUuid, targetDiscordId);

                // Отримуємо роль
                String roleId = plugin.getConfig().getString("whitelist.require-discord-role", "");
                if (!roleId.isEmpty()) {
                    Role role = guild.getRoleById(roleId);
                    if (role != null) {
                        guild.retrieveMemberById(targetDiscordId).queue(member -> {
                            guild.addRoleToMember(member, role).queue();
                            // Надсилаємо ПП
                            member.getUser().openPrivateChannel().queue(pc -> {
                                pc.sendMessage("✅ **Вашу заявку прийнято!** Ви додані до білого списку та можете заходити на сервер під ніком `" + nickname + "`.").queue(null, e -> {});
                            });
                        }, err -> {});
                    }
                }

                // Оновлюємо повідомлення
                EmbedBuilder embed = new EmbedBuilder(event.getMessage().getEmbeds().get(0));
                embed.setColor(0x00FF00);
                embed.addField("Статус", "✅ Прийнято модератором " + event.getUser().getAsMention(), false);
                event.editMessageEmbeds(embed.build()).setComponents().queue();
                
            } else if (action.equals("reject")) {
                guild.retrieveMemberById(targetDiscordId).queue(member -> {
                    member.getUser().openPrivateChannel().queue(pc -> {
                        pc.sendMessage("❌ **Вашу заявку відхилено.** На жаль, ви не можете грати на нашому сервері.").queue(null, e -> {});
                    });
                }, err -> {});

                EmbedBuilder embed = new EmbedBuilder(event.getMessage().getEmbeds().get(0));
                embed.setColor(0xFF0000);
                embed.addField("Статус", "❌ Відхилено модератором " + event.getUser().getAsMention(), false);
                event.editMessageEmbeds(embed.build()).setComponents().queue();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("ticket_modal")) {
            String nickname = event.getValue("mc_nickname").getAsString();
            String discordId = event.getUser().getId();
            
            String modChannelId = plugin.getConfig().getString("discord.moderator-channel-id", "");
            if (modChannelId.isEmpty() || modChannelId.equals("000000000000000000")) {
                event.reply("❌ Помилка налаштування бота: не вказано канал модераторів!").setEphemeral(true).queue();
                return;
            }

            TextChannel modChannel = event.getJDA().getTextChannelById(modChannelId);
            if (modChannel == null) {
                event.reply("❌ Помилка налаштування бота: канал модераторів не знайдено!").setEphemeral(true).queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("📩 Нова заявка на вхід");
            embed.setColor(0xFFFF00);
            embed.addField("Discord", event.getUser().getAsMention() + " (`" + event.getUser().getName() + "`)", false);
            embed.addField("Minecraft Нік", "`" + nickname + "`", false);
            embed.setThumbnail(event.getUser().getAvatarUrl());

            Button acceptBtn = Button.success("ticket_accept_" + discordId + "_" + nickname, "✅ Прийняти");
            Button rejectBtn = Button.danger("ticket_reject_" + discordId + "_" + nickname, "❌ Відхилити");

            modChannel.sendMessageEmbeds(embed.build())
                    .addActionRow(acceptBtn, rejectBtn)
                    .queue();

            event.reply("✅ **Ваша заявка успішно надіслана!** Модератори переглянуть її найближчим часом. Очікуйте повідомлення.").setEphemeral(true).queue();
        }
    }
}