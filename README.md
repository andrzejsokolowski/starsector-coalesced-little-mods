# Coalesced Little Mods

Eight of Oddisz's small Starsector mods in one pack. The ones that add campaign content can be
switched off on their own.

| Module | Built from | What it does |
| --- | --- | --- |
| Cloning | [Cloning](https://fractalsoftworks.com/forum/index.php?topic=35130) 0.2.1 | Adds an industry path that massively increases population growth, at the cost of colony stability. |
| Rev. Eng. Private Arsenal | Reverse Engineered Private Arsenal 1.2.4 | Adds an industry that deconstructs your weapons, fighters and ships and then sells them back in a private arsenal that only you can use. |
| StopBloatingMe | [StopBloatingMe](https://fractalsoftworks.com/forum/index.php?topic=35850) 1.0.0 | Browse ships, weapons, fighters, commodities, special items and bar quests from the main menu, and hide the ones you never want to see. |
| StopStackingMe | [StopStackingMe](https://fractalsoftworks.com/forum/index.php?topic=35952) 1.0.2 | Shows one weapon sprite per stack in cargo and the weapon picker instead of a pile. |
| Intel Renewed | [Intel Renewed](https://fractalsoftworks.com/forum/index.php?topic=35978) 0.3.0 | Declutters the intel screen: hide kinds of intel, categories and single entries, and silence their popups. |
| Hullmods - Renewed | [Hullmods - Renewed](https://fractalsoftworks.com/forum/index.php?topic=35619) 1.6.1 | Adds a filter panel to the refit hull-mod picker, with search, favourites, a blacklist and custom groups. |
| Gambling Den | [Gambling Den](https://fractalsoftworks.com/forum/index.php?topic=35993) 1.6.4 | Sell surplus ships for tokens at large ports and play Slots, Pachinko, Pinball, Blackjack and the Relic Jackpot. |
| StarterPack | [StarterPack](https://fractalsoftworks.com/forum/index.php?topic=35856) 1.1.5 | Build a starting fleet, cargo and character from the main menu and apply it to new games. |

## Settings

Open LunaLib's settings (`Shift+F2` in the campaign) and pick
**Coalesced Little Mods**. Each module's own settings are on its own tab.

The **Modules** tab has a switch for Cloning, Rev. Eng. Private Arsenal and Gambling Den, all on
by default. A switch takes effect the next time you start a new game or load a save.

When one of these is switched off:

- Cloning and Private Arsenal buildings can no longer be built. Buildings you already have keep
  working.
- Private Arsenal's faction entries leave the intel screen.
- The Gambling Den no longer appears in bars. Your tokens stay.
- Nothing is removed from your saves, and switching it back on brings everything back.

StopBloatingMe, StopStackingMe, Intel Renewed, Hullmods - Renewed and StarterPack are always on.
They only do what you set up in them, and their own settings control that.

## Coming from the separate mods

Disable the separate versions of these mods before enabling the pack. The game refuses to start
with a plain message if both are enabled.

Your saves keep working. The first time the pack starts, it copies your settings from the separate
mods. It copies only once and leaves the old settings untouched, so you can go back to the
separate mods at any time.

## Requirements

- [LunaLib](https://fractalsoftworks.com/forum/index.php?topic=25658.0)
- [LazyLib](https://fractalsoftworks.com/forum/index.php?topic=5444.0)
- [Console Commands](https://fractalsoftworks.com/forum/index.php?topic=4106.0) (optional, for StarterPack's console command)

## Building

Set `starsectorPath` in `gradle.properties`, then run `gradlew releaseZip`. The build runs every
check and writes `CoalescedLittleMods.zip` next to this file, ready for a mod manager.

Third-party code and art are listed in [THIRD_PARTY_NOTICES.txt](THIRD_PARTY_NOTICES.txt).
