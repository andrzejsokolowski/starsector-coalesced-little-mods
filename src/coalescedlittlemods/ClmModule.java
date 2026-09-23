package coalescedlittlemods;

import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;

/**
 * The mods this pack is made of.
 *
 * <p>The modules that add campaign content have a switch on the Modules tab of the LunaLib
 * settings, on by default. The others are always on: they only do what the player sets up in
 * them, and their own settings already cover switching that off.</p>
 *
 * <p>The switches are read when the game starts, when a new game starts and when a save is loaded,
 * then held until the next of those. A module is therefore on or off for a whole play session, so
 * its game-load work and its save work never disagree.</p>
 */
public enum ClmModule {
    CLONING("clm_module_cloning", "Cloning", "oddisz_cloning",
            "cloning", "oddisz.industries"),
    PRIVATE_ARSENAL("clm_module_private_arsenal", "Rev. Eng. Private Arsenal", "re_private_arsenal",
            "privatearsenal"),
    STOP_BLOATING_ME(null, "StopBloatingMe", "stopbloatingme",
            "stopbloatingme"),
    STOP_STACKING_ME(null, "StopStackingMe", "stopstackingme",
            "stopstackingme"),
    INTEL_RENEWED(null, "Intel Renewed", "intel_renewed",
            "intelrenewed"),
    HULLMODS_RENEWED(null, "Hullmods - Renewed", "hullmods_renewed",
            "hullmodsrenewed"),
    GAMBLING_DEN("clm_module_gambling_den", "Gambling Den", "gambling_den",
            "gamblingden", "hullmoddispenser"),
    STARTER_PACK(null, "StarterPack", "starterpack",
            "starterpack");

    /** The LunaSettings field that switches this module, or null for a module that is always on. */
    public final String toggleId;
    /** The module's name as the player knows it. */
    public final String displayName;
    /** The id this module had when it was released as a mod of its own. */
    public final String standaloneModId;
    /** Java packages holding this module's code. */
    public final String[] packages;

    private boolean enabled = true;

    ClmModule(String toggleId, String displayName, String standaloneModId, String... packages) {
        this.toggleId = toggleId;
        this.displayName = displayName;
        this.standaloneModId = standaloneModId;
        this.packages = packages;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Takes the current position of every switch. Only the pack's plugin calls this. */
    static void readToggles() {
        for (ClmModule module : values()) {
            module.enabled = module.toggleId == null || module.readToggle();
        }
    }

    /** A missing or unreadable switch counts as on, matching the default. */
    private boolean readToggle() {
        try {
            Boolean value = LunaSettings.getBoolean(CoalescedLittleModsPlugin.MOD_ID, toggleId);
            return value == null || value;
        } catch (Throwable t) {
            Global.getLogger(ClmModule.class).warn(
                    "Coalesced Little Mods: could not read the switch for " + displayName + "; leaving it on.", t);
            return true;
        }
    }
}
