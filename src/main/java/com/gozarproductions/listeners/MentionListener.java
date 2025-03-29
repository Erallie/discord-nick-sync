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
import org.bukkit.Sound;
import org.bukkit.Registry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        String colorString = config.getString("mentions.color", "WHITE");
        ChatColor chatColor;
        try {
            chatColor = ChatColor.valueOf(colorString.toUpperCase());
        } catch (IllegalArgumentException e) {
            chatColor = ChatColor.WHITE; // fallback if the color is invalid
        }

        String soundName = config.getString("mentions.sound", "BLOCK_NOTE_BLOCK_BELL");
        Sound sound;

        try {
            NamespacedKey namespacedKey = NamespacedKey.minecraft(soundName.toLowerCase());
            sound = Registry.SOUNDS.get(namespacedKey);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound key: " + soundName);
            sound = Sound.BLOCK_NOTE_BLOCK_BELL;
        }

        String message = event.getMessage();
        if (!message.contains("@")) return;
        // event.setCancelled(true);

        Map<String, String> mentionMap = new HashMap<>();

        Pattern pattern = Pattern.compile("@(\\S+)"); // matches @ followed by word characters (letters, numbers, underscores)
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String rawMention = matcher.group(); // e.g., "@Erika_Gozar"

            String rawMinecraftNick = matcher.group(1);
            Map.Entry<Player, String> data = getPlayerByNickname(rawMinecraftNick);
            if (data == null) continue;

            Player player = data.getKey();
            String minecraftNick = data.getValue();
            String mention = rawMention.substring(0, minecraftNick.length() + 1);

            //#region Alert Player
            //!TODO: add functionality for mentioning player
            player.playSound(player.getLocation(), sound, (float) config.getDouble("mentions.sound.volume"), (float) config.getDouble("mentions.sound.pitch"));
            //#endregion

            UUID uuid = player.getUniqueId();
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

    public Map.Entry<Player, String> getPlayerByNickname(String input) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String nick = essentials.getUser(player).getNickname();
            if (nick == null) {
                nick = player.getDisplayName();
            } else {
                nick = ChatColor.stripColor(nick);
            }
            if (input.toLowerCase().startsWith(nick.toLowerCase())) {
                return new AbstractMap.SimpleEntry<>(player, nick);
            }
        }
        return null;
    }
}
