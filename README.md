![DiscordNickSync](https://raw.githubusercontent.com/Erallie/discord-nick-sync/main/icons/banner/banner-press-12.png)

[![Latest Release](https://img.shields.io/github/release-date/Erallie/discord-nick-sync?display_date=published_at&style=for-the-badge&label=Latest%20Release)](https://modrinth.com/plugin/discord-nick-sync/version/latest)
[![GitHub Downloads](https://img.shields.io/github/downloads/Erallie/discord-nick-sync/total?style=for-the-badge&logo=github&logoColor=ffffff&label=GitHub%20Downloads&color=hsl(0%2C%200%25%2C%2020%25))](https://github.com/Erallie/discord-nick-sync)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/tles3bfw?style=for-the-badge&logo=modrinth&logoColor=00af5c&label=Modrinth%20Downloads&color=00af5c)](https://modrinth.com/plugin/discord-nick-sync)
<br>
[![Our Discord](https://img.shields.io/discord/1102582171207741480?style=for-the-badge&logo=discord&logoColor=ffffff&label=Our%20Discord&color=5865F2)](https://discord.gg/cCCEk7BX4W)
[![Our Other Projects](https://img.shields.io/badge/Our%20Other%20Projects-%E2%9D%A4-563294?style=for-the-badge&logo=data%3Aimage%2Fwebp%3Bbase64%2CUklGRu4DAABXRUJQVlA4WAoAAAAQAAAAHwAAHwAAQUxQSGABAAABgFtbm5volyZTA%2BtibzK2H0w5sDkmhe3GmxrwxGg0839r%2FvkkOogIBW7bKB0c4%2BARYihzIqfd6dfO%2B%2B3XtHsq4jJhlIvcDRcgNB%2FeieQETorBHgghRtUYqwDs%2B4U4IpcvUB%2BVUPSK54uEnTwsUJoar2DeMpzLxQpeG5DH8lxyyfLivVYAwPBbkWdOBg3qFlqiLy679iHy9UDKMZRXmYxpCcusayTHG01K%2FEtatYWuj7oI9hL4BxsxVwhoP2mlAJJ%2BuuAflc6%2BEUCQTCX9EV87xBR2H75NxLZSpWiwzqdIm7ZO7uB3oEgZKbD9Nt3EmHweEPH1t1GNsZUbKeisiwjyTm5fA3SO1yCrADZXrV2PZQJPL1tjN4%2BxUL9ie1mJobzOnDwSx6ILiF%2FW%2BTUR4tcHx0UaV75JXC1a4g6Ky5dLcTSuy9q4HhTieF64Hy1A3GHB8gLLK2e92feuqnbfPK8IVlA4IGgCAACwDQCdASogACAAPk0cjEQioaEb%2BqwAKATEtgBOl7v9V3sHcA2wG4A3gD0APLP9jX9n%2F2jmqv5AZRh7J%2BN2fOx22iE%2F4TUsecFmY%2BSf1r%2BAP%2BTfzT%2FXdIB7KX7MtdIGr1A8H0jmrrfZvqButwOaYcLWYNRq5QgAAP7%2F%2FmIMpiVNn67QXpM1rrDmRS8Nr%2F6dhD%2Bq5e%2BM%2BAtUP1%2FxOj85Ol5y3ebjz%2BpHoOf%2FWW8a%2F2ojUaKVDkVqof%2Bv4f0f6ud8i58wusz%2Fyrj%2F%2BwnM3q0769dvK%2F%2BQe04xL49tkb9t6ylCqqezZtZGuGLJ%2F5iUrPqdYc%2F8VbYZfP%2FOpZP%2F4X4q%2BqS4gPOxzdINOe5PGv%2F0TS%2FJRf4LlFrFkrWtxlS8n40grV%2BKUu%2FiwzdQzImvwH81FxL1bZyTSsrYwMku1Pk9StTtWNjSR8ZWEYBH9eTn%2FvBERii5XaWOPJ%2FFVXtVQGbv%2BFRW5jbo9tfFDu%2BDHHf8LbgUd%2F8W8Id1AehBtRNsLQWbADmvF1QJU8x5tw%2FtTUwIoSaa%2F2jkcvyVHkAsb2qoIh1KF1pPdae%2BZaqjydy6nUa9agjrDk1G4pMhEUhH%2BV%2FIUe49MjhR%2FuxyFmwQ8dDogMyQ%2BdcSBa56Lwt1wyJ%2F22%2F5O98r6q6wiM63HyaYONd36W7br%2F0%2F6y2DZ3irAddj%2FRxntvr%2FbbChSYXAfEbO%2FD0G%2FFbMFqTHypodt9T6dAx%2BUjJYfHzFf%2FM3Ec%2FAtwbjc2gka6urN1MlSLb2VTS9Q5r8fkDzxZz6vu1OYUPUB1UFMIhYGvMATbxxoTmVhvpovzAc%2F8nbOjw3wAAA)](https://github.com/Erallie)
[![Donate](https://img.shields.io/badge/Donate-%24-563294?style=for-the-badge&logo=paypal&color=rgb(0%2C%2048%2C%20135))](https://www.paypal.com/donate/?hosted_button_id=PHHGM83BQZ8MA)

---

DiscordNickSync is a plugin for Minecraft Java Edition that lets your players sync their Discord nickname to their Essentials nickname, and vice versa, via [DiscordSRV](https://modrinth.com/plugin/discordsrv).

I know that DiscordSRV *already* allows syncing from Minecraft to Discord, but it doesn't allow syncing the other way around. So I created this plugin to do that!

## When Nicknames are Synced
DiscordNickSync currently *only* syncs nicknames when a player does any of the following:
- Logs in
- Links their Discord to Minecraft via DiscordSRV
- Changes their Essentials nickname.
- Uses the command `/discordnick sync`

There is currently *no* timer to sync nicknames from Discord to Minecraft. If this is a feature you would want, [create an issue](https://github.com/Erallie/discord-nick-sync/issues), and I will consider adding it.

## Mentions
DiscordNickSync also allows syncing of Discord mentions!
- Type `@<nickname>` in minecraft chat to also ping the linked account on Discord.
- Mentions from Discord also ping online players in Minecraft.
- Optionally configure a sound and a title message for the mentioned player.

Note that the way I've set it up, it changes the message in Minecraft to use Discord's nickname. I do not currently know a way around this and would like to change this. If you know another way to implement it, please let me know on [Our Discord](https://discord.gg/cCCEk7BX4W)!

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

# Type @<minecraft username|essentials nickname|discord nickname> in minecraft chat to also ping the linked account on Discord.
# Mentions also work when pinged on Discord, except for the mention color.
mentions:
  # Set to false to disable mentions.
  enabled: true

  # The sound that a player hears when they are mentioned.
  # You must manually disable MinecraftMentionSound in DiscordSRV's config.yml if you don't want multiple sounds.
  play-sound:
    enabled: true
    # Can be any value listed under "Java Edition values" on this webpage: https://minecraft.wiki/w/Sounds.json#Sound_events
    sound: "block.note_block.bell"
    # Any decimal number between 0 and 1.
    volume: 1.0
    # Any decimal number between 0 and 2.
    pitch: 1.0
  
  # Display a title to the player who is mentioned.
  send-title:
    enabled: true
    # Type {mentioner} for the nickname of the person who mentioned you
    # Color formats are supported
    title: "&eYou have been mentioned"
    # Type {mentioner} for the nickname of the person who mentioned you
    # Color formats are supported
    subtitle: "&eby &6&l{mentioner}"
    duration:
      # The duration in ticks for the title to fade in.
      fade-in: 5
      # The duration in ticks for the title to stay on the screen.
      stay: 70
      # The duration in ticks for the title to fade out.
      fade-out: 5

  # The color of the mention in chat.
  # Set to "" to disable
  color: "&e"

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
    - Allows players to:
        - Set their own nickname sync mode
        - Run `/discordnick sync` for themselves.
    - Default: `true`
- `discordnick.admin`
    - Allows admins to:
        - Run `/discordnick sync` for all players or individual players.
        - Reload the config with `/discordnick reload`
    - Default: `op`
# Support
If you have any issues, or want to request a feature, please [create an issue](https://github.com/Erallie/discord-nick-sync/issues), and I will try my best to help!
