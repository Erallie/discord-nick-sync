package com.gozarproductions.listeners;

import com.earth2me.essentials.Essentials;
import com.gozarproductions.DiscordNickSync;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MentionListener implements Listener {

    private final DiscordNickSync plugin;
    private final Essentials essentials;

    public MentionListener(DiscordNickSync plugin) {
        this.plugin = plugin;
        this.essentials = Bukkit.getPluginManager().isPluginEnabled("Essentials")
            ? (Essentials) Bukkit.getPluginManager().getPlugin("Essentials")
            : null;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        FileConfiguration config = plugin.getConfig();

        //#region Color config
        String colorString = config.getString("mentions.color", "WHITE");
        ChatColor chatColor;
        try {
            chatColor = ChatColor.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            chatColor = ChatColor.WHITE; // fallback if the color is invalid
        }
        //#endregion

        //#region Sound config
        boolean sendSound = config.getBoolean("mentions.play-sound.enabled", true);
        String soundName = null;
        Sound sound = null;
        float volume = 1.0f;
        float pitch = 1.0f;
        if (sendSound) {
            soundName = config.getString("mentions.play-sound.sound", "block.note_block.bell");

            try {
                NamespacedKey namespacedKey = NamespacedKey.minecraft(soundName.toLowerCase());
                sound = Registry.SOUNDS.get(namespacedKey);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound key: " + soundName);
                sound = Sound.BLOCK_NOTE_BLOCK_BELL;
            }

            volume = (float) config.getDouble("mentions.play-sound.volume", 1.0f);
            pitch = (float) config.getDouble("mentions.play-sound.pitch", 1.0f);
        }
        //#endregion

        //#region Title config
        boolean sendTitle = config.getBoolean("mentions.send-title.enabled", true);
        String title = null;
        String subtitle = null;

        int fadeIn = 0;
        int stay = 0;
        int fadeOut = 0;

        if (sendTitle) {
            Player mentioner = event.getPlayer();
            String mentionerName = essentials.getUser(mentioner).getNickname();
            if (mentionerName == null || mentionerName.isEmpty()) {
                mentionerName = mentioner.getDisplayName();
            }
            title = ChatColor.translateAlternateColorCodes('&', config.getString("mentions.send-title.title", "You have been mentioned").replaceAll("\\{mentioner\\}", mentionerName));
            subtitle = ChatColor.translateAlternateColorCodes('&', config.getString("mentions.send-title.subtitle", "by {mentioner}").replaceAll("\\{mentioner\\}", mentionerName));
            fadeIn = config.getInt("mentions.send-title.duration.fade-in", 5);
            stay = config.getInt("mentions.send-title.duration.stay", 60);
            fadeOut = config.getInt("mentions.send-title.duration.fade-out", 5);
        }
        //#endregion

        String message = event.getMessage();
        if (!message.contains("@")) return;
        // event.setCancelled(true);

        Map<String, String> mentionMap = new HashMap<>();

        Pattern pattern = Pattern.compile("@((?:(?! @).)+)"); // matches @ followed by word characters (letters, numbers, underscores)
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String rawMention = matcher.group(); // e.g., "@Erika_Gozar"

            String rawMinecraftNick = matcher.group(1);
            Map.Entry<OfflinePlayer, String> data = getPlayerByNickname(rawMinecraftNick);
            if (data == null) continue;

            OfflinePlayer offlinePlayer = data.getKey();
            Player player = offlinePlayer.isOnline() ? Bukkit.getPlayer(offlinePlayer.getUniqueId()) : null;
            String minecraftNick = data.getValue();
            String mention = rawMention.substring(0, minecraftNick.length() + 1);

            //#region Alert Player
            if (player != null) {
                if (sendSound) {
                    player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
                }

                if (sendTitle) {
                    player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                }
            }
            //#endregion

            UUID uuid = offlinePlayer.getUniqueId();
            DiscordSRV discordSRV = DiscordSRV.getPlugin();
            JDA jda = discordSRV.getJda();
            String discordId = discordSRV.getAccountLinkManager().getDiscordId(uuid);
            if (discordId == null) {
                mentionMap.put(mention, "@" + minecraftNick);
                continue;
            };
            
            User discordUser = jda.getUserById(discordId);
            if (discordUser == null) {
                mentionMap.put(mention, "@" + minecraftNick);
                continue;
            };
            
            Member discordMember = discordSRV.getMainGuild().getMember(discordUser);
            if (discordMember == null) {
                mentionMap.put(mention, "@" + minecraftNick);
                continue;
            };
            String discordNick = discordMember.getEffectiveName();

            mentionMap.put(mention, "@" + discordNick);
        }

        for (Map.Entry<String, String> entry : mentionMap.entrySet()) {
            message = message.replace(entry.getKey(), chatColor + entry.getValue() + ChatColor.RESET);
        }

        event.setMessage(message); // This modifies the message DiscordSRV picks up
    }

    public Map.Entry<OfflinePlayer, String> getPlayerByNickname(String input) {
        String lowerCaseInput = input.toLowerCase();

        return Stream.concat(
                DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().values().stream()
                    .map(Bukkit::getOfflinePlayer),
                Bukkit.getOnlinePlayers().stream()
                    .map(p -> (OfflinePlayer) p)
            )
            .distinct()
            .map(player -> new AbstractMap.SimpleEntry<>(player, getStrippedNickname(player)))
            .sorted(Comparator.comparingInt((Map.Entry<OfflinePlayer, String> e) -> e.getValue().length()).reversed())
            .filter(entry -> {
                String nick = entry.getValue();
                String name = entry.getKey().getName();
                return (nick != null && lowerCaseInput.startsWith(nick.toLowerCase())) ||
                    (name != null && lowerCaseInput.startsWith(name.toLowerCase()));
            })
            .findFirst()
            .orElse(null);
    }

    private String getStrippedNickname(OfflinePlayer player) {
        if (player.isOnline()) {
            Player online = (Player) player;
            String nick = essentials.getUser(online).getNickname();
            return nick != null ? ChatColor.stripColor(nick) : online.getDisplayName();
        } else {
            String nick = essentials.getUser(player.getUniqueId()).getNickname();
            return nick != null ? ChatColor.stripColor(nick) : player.getName();
        }
    }
}
