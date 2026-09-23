package coalescedlittlemods;

import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;

/**
 * The mods this pack is made of, each behind its own toggle on the Modules tab of the LunaLib
 * settings. All of them are on by default.
 *
 * <p>A toggle is read once per launch and then held, so a module is either on or off for the whole
 * session. Flipping one in the settings takes effect the next time the game starts.</p>
 */
public enum ClmModule {
    CLONING("clm_module_cloning", "Cloning", "oddisz_cloning",
            "cloning", "oddisz.industries"),
    PRIVATE_ARSENAL("clm_module_private_arsenal", "Rev. Eng. Private Arsenal", "re_private_arsenal",
            "privatearsenal"),
    STOP_BLOATING_ME("clm_module_stopbloatingme", "StopBloatingMe", "stopbloatingme",
            "stopbloatingme"),
    STOP_STACKING_ME("clm_module_stopstackingme", "StopStackingMe", "stopstackingme",
            "stopstackingme"),
    INTEL_RENEWED("clm_module_intel_renewed", "Intel Renewed", "intel_renewed",
            "intelrenewed"),
    HULLMODS_RENEWED("clm_module_hullmods_renewed", "Hullmods - Renewed", "hullmods_renewed",
            "hullmodsrenewed"),
    GAMBLING_DEN("clm_module_gambling_den", "Gambling Den", "gambling_den",
            "gamblingden", "hullmoddispenser"),
    STARTER_PACK("clm_module_starterpack", "StarterPack", "starterpack",
            "starterpack");

    /** The LunaSettings field that switches this module. */
    public final String toggleId;
    /** The module's name as the player knows it. */
    public final String displayName;
    /** The id this module had when it was released as a mod of its own. */
    public final String standaloneModId;
    /** Java packages holding this module's code. */
    public final String[] packages;

    private Boolean enabled;

    ClmModule(String toggleId, String displayName, String standaloneModId, String... packages) {
        this.toggleId = toggleId;
        this.displayName = displayName;
        this.standaloneModId = standaloneModId;
        this.packages = packages;
    }

    public boolean isEnabled() {
        if (enabled == null) enabled = readToggle();
        return enabled;
    }

    /** A missing or unreadable toggle counts as on, matching the default. */
    private boolean readToggle() {
        try {
            Boolean value = LunaSettings.getBoolean(CoalescedLittleModsPlugin.MOD_ID, toggleId);
            return value == null || value;
        } catch (Throwable t) {
            Global.getLogger(ClmModule.class).warn(
                    "Coalesced Little Mods: could not read the toggle for " + displayName + "; leaving it on.", t);
            return true;
        }
    }
}
