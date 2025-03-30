package com.gozarproductions.listeners;

import com.earth2me.essentials.Essentials;
import com.gozarproductions.DiscordNickSync;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;

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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
        String message = event.getMessage();
        String mentionerName = getStrippedNickname(event.getPlayer());
        event.setMessage(processMentions(message, mentionerName, null));
    }
    
    @Subscribe
    public void onDiscordMessage(DiscordGuildMessagePreProcessEvent event) {
        Message message = event.getMessage();
        String messageString = message.getContentDisplay();
        String mentionerName = event.getMember().getEffectiveName();
        List<Player> players = message.getMentionedUsers().stream().map(user -> {
                UUID minecraftUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
                return Bukkit.getPlayer(minecraftUuid);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        processMentions(messageString, mentionerName, players);
        // String updatedMessage = processMentions(messageString, mentionerName, players);
        // !TODO: Actually set updatedMessage into outgoing chat if/when you move to DiscordSRVMessagePreSendEvent
    }

    private String processMentions(String message, String mentionerName, List<Player> players) {
        if (!message.contains("@")) return message;

        //#region Color config
        FileConfiguration config = plugin.getConfig();
        String chatColor = ChatColor.translateAlternateColorCodes('&', config.getString("mentions.color", "&e"));
        String resetColor = "§r";
        if (chatColor == null || chatColor.isEmpty()) {
            resetColor = "";
        }
        //#endregion

        //#region Sound config
        boolean sendSound = config.getBoolean("mentions.play-sound.enabled", true);
        Sound sound = Sound.BLOCK_NOTE_BLOCK_BELL;
        float volume = 1.0f, pitch = 1.0f;
        if (sendSound) {
            try {
                String soundName = config.getString("mentions.play-sound.sound", "block.note_block.bell");
                sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid sound key.");
            }
            volume = (float) config.getDouble("mentions.play-sound.volume", 1.0f);
            pitch = (float) config.getDouble("mentions.play-sound.pitch", 1.0f);
        }
        //#endregion

        //#region Title config
        boolean sendTitle = config.getBoolean("mentions.send-title.enabled", true);
        String title = null, subtitle = null;
        int fadeIn = 5, stay = 60, fadeOut = 5;

        if (sendTitle && mentionerName != null) {
            title = ChatColor.translateAlternateColorCodes('&',
                config.getString("mentions.send-title.title", "&eYou have been mentioned")
                    .replace("{mentioner}", mentionerName));
            subtitle = ChatColor.translateAlternateColorCodes('&',
                config.getString("mentions.send-title.subtitle", "&eby &6&l{mentioner}")
                    .replace("{mentioner}", mentionerName));
            fadeIn = config.getInt("mentions.send-title.duration.fade-in", 5);
            stay = config.getInt("mentions.send-title.duration.stay", 60);
            fadeOut = config.getInt("mentions.send-title.duration.fade-out", 5);
        }
        //#endregion

        if (players != null) {
            for (Player player : players) {
                if (sendSound) {
                    player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
                }
                if (sendTitle && title != null && subtitle != null) {
                    player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                }
            }
            return message;
        }

        Map<String, String> replacements = new HashMap<>();
        Matcher matcher = Pattern.compile("@((?:(?! @).)+)").matcher(message);

        while (matcher.find()) {
            String rawMention = matcher.group();     // e.g. "@Erika"
            String mentionedName = matcher.group(1); // e.g. "Erika"

            Map.Entry<OfflinePlayer, String> playerEntry = getPlayerByNickname(mentionedName);
            if (playerEntry == null) continue;
            String nick = playerEntry.getValue();
            String mention = rawMention.substring(0, nick.length() + 1);

            Player player = Bukkit.getPlayer(playerEntry.getKey().getUniqueId());
            UUID uuid = playerEntry.getKey().getUniqueId();
            String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(uuid);

            String replacementName = playerEntry.getValue(); // default: Minecraft nickname

            if (discordId != null) {
                User user = DiscordSRV.getPlugin().getJda().getUserById(discordId);
                if (user != null) {
                    Member member = DiscordSRV.getPlugin().getMainGuild().getMember(user);
                    if (member != null && member.getEffectiveName() != null) {
                        replacementName = member.getEffectiveName(); // override with Discord name
                    }
                }
            }

            String replacement = chatColor + "@" + replacementName + resetColor;

            replacements.put(mention, replacement);

            // Alert
            if (player != null && player.isOnline()) {
                if (sendSound) {
                    player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
                }
                if (sendTitle && title != null && subtitle != null) {
                    player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
                }
            }
        }

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }

        return message;
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
            .map(player -> {
                String name = player.getName();
                String nickname = getStrippedNickname(player);

                // Try Minecraft name or Essentials nickname
                if (nickname != null && lowerCaseInput.startsWith(nickname.toLowerCase())) {
                    return new AbstractMap.SimpleEntry<>(player, nickname);
                } else if (name != null && lowerCaseInput.startsWith(name.toLowerCase())) {
                    return new AbstractMap.SimpleEntry<>(player, name);
                }

                // Try Discord nickname if linked
                String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
                if (discordId != null) {
                    User user = DiscordSRV.getPlugin().getJda().getUserById(discordId);
                    if (user != null) {
                        Member member = DiscordSRV.getPlugin().getMainGuild().getMember(user);
                        if (member != null) {
                            String discordNick = member.getEffectiveName();
                            if (discordNick != null && lowerCaseInput.startsWith(discordNick.toLowerCase())) {
                                return new AbstractMap.SimpleEntry<>(player, discordNick);
                            }
                        }
                    }
                }
                return null;
            })
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt((Map.Entry<OfflinePlayer, String> e) -> e.getValue().length()).reversed())
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
