# Coalesced Little Mods

A pack of eight of Andrzej's mods. The workspace `AGENTS.md` one folder up still governs releases,
packaging, Git identity and communication.

## Layout

- `src/coalescedlittlemods/` holds the pack itself: `ClmModule` (module list and switches),
  `CoalescedLittleModsPlugin` (forwards game events to enabled modules), `SettingsImport`
  (one-time copy of the separate mods' LunaLib values) and `WhileDisabled` (hooks a switched-off
  module still needs).
- `src/com/fs/starfarer/api/impl/campaign/rulecmd/CLM_ModuleEnabled.java` is the rules.csv condition
  that hides a module's dialogue options.
- Every other package under `src/` is one module, copied from its standalone repository in the
  sibling folders.

## Rules

- Keep module package names, class names, persistent-data keys and memory keys identical to the
  standalone mods. Saves made with the separate mods depend on them.
- All modules share the mod id `coalesced_little_mods`. Each module's `MOD_ID` points at
  `CoalescedLittleModsPlugin.MOD_ID`.
- All settings live in `data/config/LunaSettings.csv`, one tab per module. Keep each module's field
  ids unchanged so `SettingsImport` can carry values over. Cloning is the one exception: its old
  ids are renamed with a `cloning_` prefix through `SettingsImport.RENAMED`.
- Only modules that add campaign content get a switch: Cloning, Private Arsenal and Gambling Den.
  The others are always on (`toggleId` null), because their own settings already control them.
  Andrzej decided this on 2026-09-23.
- Switches are read at startup, at a new game and at a save load, then held until the next one.
  Game-load and save events must never see a module change state in between.
- Startup work (`onApplicationLoad`) reaches every module, on or off. It must stay harmless for
  a module that is off.
- A switched-off module gets no game events and must not allow new buildings or offer dialogue.
  Its existing buildings and saved data keep working. `configureXStream` always reaches every
  module.
- A new module needs a `ClmModule` entry and a plugin in `CoalescedLittleModsPlugin`. A switchable
  one also needs a switch on the Modules tab and gated entry points.
- To bring over a fix from a standalone repository, reapply it here by hand. Keep the pack edits
  (switch checks and the shared `MOD_ID`).

## Checks

`gradlew releaseZip` runs everything: the pack checks (`test/coalescedlittlemods/PackChecks.java`),
Gambling Den's regression checks, the forbidden-API scan, the metadata check and the release-file
check.
