# ![DiscordNickSync](https://raw.githubusercontent.com/Erallie/discord-nick-sync/main/icons/banner/banner-press-12.png)
[Our Discord](https://discord.gg/cCCEk7BX4W) | [Our Other Projects](https://github.com/Erallie) | [Modrinth](https://modrinth.com/plugin/discordnicksync)

DiscordNickSync is a plugin for Minecraft Java Edition that lets your players sync their Discord nickname to their Essentials nickname, and vice versa, via [DiscordSRV](https://modrinth.com/plugin/discordsrv).

I know that DiscordSRV *already* allows syncing from Minecraft to Discord, but it doesn't allow syncing the other way around. So I created this plugin to do that!

## When Nicknames are Synced
DiscordNickSync currently *only* syncs nicknames when a player does any of the following:
- Logs in
- Links their Discord to Minecraft via DiscordSRV
- Changes their Essentials nickname.
- Uses the command `/discordnick sync`

There is currently *no* timer to sync nicknames from Discord to Minecraft. If this is a feature you would want, [create an issue](https://github.com/Erallie/discord-nick-sync/issues), and I will consider adding it.

# Default Files
## Config.yml
```yml
# DO NOT CHANGE THIS
internal:
  plugin-version: "X.X.X"
# DO NOT CHANGE THIS

# What name to use by default for both Minecraft and Discord.
# This can be changed by the player as long as they have the permission discordnick.use
# Can either be "minecraft", "discord", or "off"
default-sync: minecraft

# What to replace whitespace characters with when syncing from Discord to Minecraft.
# Keep this as "" to remove whitespace characters.
# Set this to " " to keep as spaces.
# Set this to anything else to replace it with that.
replace-whitespaces-with: ""

# Whether to combine whitespace characters together before replacing them with the value above.
merge-whitespaces-before-replacing: true

updater:
  # Whether to notify admins for new snapshot releases. Admins will still be notified on stable releases.
  notify-on-dev-release: false
```

## Language.yml
```yml
colors:
  default: "&e"
  highlight: "&6"
  error: "&c"
  error_highlight: "&6"
messages:
  # {d} for default color
  # {h} for highlight color
  # {e} for error color
  # {eh} for error highlight
  
  reload_success: "{h}DiscordNickSync {d}configuration reloaded."

  # Additional placeholders: {player} {from} {to}
  sync_success: "{d}Synced {h}{player} {d}({from} → {to})"

  # Additional placeholders: {count}
  sync_all_success: "{d}Synced {h}{count} {d}players."

  # Additional placeholders: {player}
  sync_disabled: "{h}{player}{d} has syncing disabled."

  # Additional placeholders: {nickname} {from} {to}
  nickname_updated: "{d}Your {to} nickname has been updated to {h}{nickname}{d}."

  # Additional placeholders: {from} {to}
  mode_set: "{d}Your {to} nickname will now use your {h}{from} {d}nickname"

  sync_notif: "{d}If you would like to configure how your nick is synced, type {h}/discordnick"

  mode_off: "{d}Your nicknames will no longer be synced between Minecraft and Discord."
errors:
  # {d} for default color
  # {h} for highlight color
  # {e} for error color
  # {eh} for error highlight

  no_permission: "{e}You do not have permission to use this command."

  # Additional placeholders: {player}
  player_not_found: "{eh}{player} {e}not found or offline"

  # Additional placeholders: {player}
  sync_not_linked: "{eh}{player} {e}has not linked their Discord account."

  # The usage will be shown after this.
  invalid_command: "{e}Invalid command usage!"

  only_players: "{e}Only players can use this command."
```
# Commands
- `/discordnick < discord | minecraft | off >`
- `/discordnick sync [ all | <username> ]`
- `/discordnick reload`

# Permissions
- `discordnick.use`
    - Allows players to use `/discordnick`.
    - Default: true
- `discordnick.admin`
    - Allows admins to:
        - Run `/discordnick sync` for all players or individual players.
        - Reload the config with `/discordnick reload`
    - Default: op
# Support
If you have any issues, or want to request a feature, please [create an issue](https://github.com/Erallie/discord-nick-sync/issues), and I will try my best to help!