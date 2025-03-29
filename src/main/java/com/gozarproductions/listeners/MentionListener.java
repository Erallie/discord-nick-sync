package com.gozarproductions.listeners;

import com.earth2me.essentials.Essentials;
import com.gozarproductions.DiscordNickSync;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.JDA;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
        String message = event.getMessage();
        if (!message.contains("@")) return;
        // event.setCancelled(true);

        Map<String, String> mentionMap = new HashMap<>();

        Pattern pattern = Pattern.compile("@(\\S+)"); // matches @ followed by word characters (letters, numbers, underscores)
        Matcher matcher = pattern.matcher(message);

        while (matcher.find()) {
            String mention = matcher.group(); // e.g., "@Erika_Gozar"

            String rawMinecraftNick = matcher.group(1);
            Map.Entry<Player, String> data = getPlayerByNickname(rawMinecraftNick);
            if (data == null) continue;

            Player player = data.getKey();
            String minecraftNick = data.getValue();

            //!TODO: add functionality for mentioning player

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


            if (message.contains("@" + minecraftNick)) {
                mentionMap.put(mention, "@" + discordNick);
            }
        }

        for (Map.Entry<String, String> entry : mentionMap.entrySet()) {
            message = message.replace(entry.getKey(), "§e" + entry.getValue() + "§r");
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
            if (input.equalsIgnoreCase(nick)) {
                return new AbstractMap.SimpleEntry<>(player, nick);
            }
        }
        return null;
    }
}
