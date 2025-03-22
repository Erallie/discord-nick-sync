# DiscordNickSync
DiscordNickSync is a plugin that lets your players sync their Discord nickname to their Essentials nickname, and vice versa, via [DiscordSRV](https://modrinth.com/plugin/discordsrv).

I know that DiscordSRV *already* allows syncing from Minecraft to Discord, but it doesn't allow syncing the other way around. So I created this plugin to do that!

# Default Files
## Config.yml
```yml
## DO NOT CHANGE THIS
internal:
  plugin-version: "X.X.X"
## DO NOT CHANGE THIS

# What name to use by default for both Minecraft and Discord
# This can be changed by the player as long as they have the permission discordnick.use
# Can either be "minecraft", "discord", or "off"
default-sync: discord
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