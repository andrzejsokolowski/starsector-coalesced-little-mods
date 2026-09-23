package stopstackingme

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener

/**
 * Reads the mod's LunaSettings (`data/config/LunaSettings.csv`) into plain fields and keeps them in
 * sync while the game runs.
 *
 * Both settings are read live by [StackIconFlattener] every frame, so flipping one in the LunaLib
 * menu shows up on the next screen you look at — no reload, and no restart.
 *
 * Every read falls back to the hardcoded default below, so a missing field, a broken CSV, or an
 * absent LunaLib degrades to "flatten weapons, leave crew alone" instead of failing.
 */
object SssSettings {

    private const val MOD_ID = StopStackingMeModPlugin.MOD_ID

    private const val KEY_FLATTEN_WEAPONS = "sss_flatten_weapons"
    private const val KEY_FLATTEN_PERSONNEL = "sss_flatten_personnel"

    private const val DEFAULT_FLATTEN_WEAPONS = true
    private const val DEFAULT_FLATTEN_PERSONNEL = false

    private val log = Global.getLogger(SssSettings::class.java)

    // --- Live values ----------------------------------------------------------------------------

    var flattenWeapons = DEFAULT_FLATTEN_WEAPONS; private set
    var flattenPersonnel = DEFAULT_FLATTEN_PERSONNEL; private set

    // --- Wiring ---------------------------------------------------------------------------------

    /** Called once from the mod plugin at application load. */
    fun init() {
        if (!isLunaAvailable()) {
            log.warn("StopStackingMe: LunaLib not enabled; using built-in defaults for every setting.")
            return
        }
        reload()
        runCatching {
            if (!LunaSettings.hasSettingsListenerOfClass(Listener::class.java)) {
                LunaSettings.addSettingsListener(Listener())
            }
        }.onFailure { log.error("StopStackingMe: could not register the LunaSettings listener.", it) }
    }

    private fun isLunaAvailable(): Boolean =
        runCatching { Global.getSettings().modManager.isModEnabled("lunalib") }.getOrDefault(false)

    private class Listener : LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            if (modID != MOD_ID) return
            reload()
        }
    }

    // --- Reading --------------------------------------------------------------------------------

    private fun reload() {
        flattenWeapons = bool(KEY_FLATTEN_WEAPONS, DEFAULT_FLATTEN_WEAPONS)
        flattenPersonnel = bool(KEY_FLATTEN_PERSONNEL, DEFAULT_FLATTEN_PERSONNEL)
    }

    private fun bool(key: String, fallback: Boolean): Boolean =
        runCatching { LunaSettings.getBoolean(MOD_ID, key) }.getOrNull() ?: fallback
}
