# Coalesced Little Mods

A pack of eight of Andrzej's mods. The workspace `AGENTS.md` one folder up still governs releases,
packaging, Git identity and communication.

## Layout

- `src/coalescedlittlemods/` holds the pack itself: `ClmModule` (toggles), `CoalescedLittleModsPlugin`
  (forwards game events to enabled modules), `SettingsImport` (one-time copy of the separate mods'
  LunaLib values) and `WhileDisabled` (hooks a switched-off module still needs).
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
- A switched-off module must not start, add buttons, offer dialogue or allow new buildings, but its
  existing buildings and saved data keep working. `configureXStream` always reaches every module.
- A new module needs a `ClmModule` entry, a toggle on the Modules tab, a plugin in
  `CoalescedLittleModsPlugin` and gated entry points.
- To bring over a fix from a standalone repository, reapply it here by hand. Keep the pack edits
  (toggle checks and the shared `MOD_ID`).

## Checks

`gradlew releaseZip` runs everything: the pack checks (`test/coalescedlittlemods/PackChecks.java`),
Gambling Den's regression checks, the forbidden-API scan, the metadata check and the release-file
check.
