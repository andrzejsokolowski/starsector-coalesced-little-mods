package coalescedlittlemods;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lazywizard.lazylib.JSONUtils;

import com.fs.starfarer.api.Global;

import lunalib.lunaSettings.LunaSettings;

/**
 * Carries the LunaLib settings over from the mods as they were released on their own.
 *
 * <p>LunaLib keeps each mod's settings in {@code saves/common/LunaSettings/<mod id>.json}. The pack
 * has a new mod id, so without this every slider would start back at its default. On the first
 * launch the values in the old files are copied into the pack's file, once. The old files are only
 * read, never changed, so the separate mods still find them if the player goes back.</p>
 */
final class SettingsImport {

    private static final Logger log = Global.getLogger(SettingsImport.class);

    private static final String MOD_ID = CoalescedLittleModsPlugin.MOD_ID;
    private static final String MARKER_FILE = "coalesced_little_mods/settings_import.json";
    private static final String TARGET_FILE = "LunaSettings/" + MOD_ID + ".json";

    /** Cloning's fields now carry a prefix like every other module's. */
    private static final Map<String, String> RENAMED = Map.of(
            "baseGrowthTier1", "cloning_base_growth_tier1",
            "baseGrowthTier2", "cloning_base_growth_tier2",
            "baseGrowthTier3", "cloning_base_growth_tier3",
            "scalingFactor", "cloning_scaling_factor",
            "globalMult", "cloning_global_mult",
            "resetToDefault", "cloning_reset_to_default");

    /** One-shot actions. An old value left on would reset or wipe something at the first launch. */
    private static final Set<String> ACTIONS = Set.of("cloning_reset_to_default", "ir_wipe_prefs", "hmr_wipe_prefs");

    private SettingsImport() {
    }

    static void run() {
        try {
            JSONUtils.CommonDataJSONObject marker = JSONUtils.loadCommonJSON(MARKER_FILE);
            if (marker.optBoolean("done", false)) return;
            int copied = copyOldValues();
            marker.put("done", true);
            marker.put("valuesImported", copied);
            marker.save();
            log.info("Coalesced Little Mods: carried over " + copied + " settings from the separate mods.");
        } catch (Exception e) {
            log.error("Coalesced Little Mods: could not carry over the settings of the separate mods.", e);
        }
    }

    private static int copyOldValues() throws Exception {
        Set<String> fields = ownFields();

        // Reading any value makes LunaLib write the pack's defaults first; the import overrides them.
        LunaSettings.getBoolean(MOD_ID, ClmModule.CLONING.toggleId);
        JSONUtils.CommonDataJSONObject target = JSONUtils.loadCommonJSON(TARGET_FILE);

        int copied = 0;
        for (ClmModule module : ClmModule.values()) {
            JSONObject old = readOldSettings(module.standaloneModId);
            if (old == null) continue;
            for (Iterator<?> keys = old.keys(); keys.hasNext(); ) {
                String key = (String) keys.next();
                String field = RENAMED.getOrDefault(key, key);
                if (!fields.contains(field) || ACTIONS.contains(field)) continue;
                target.put(field, old.get(key));
                copied++;
            }
        }
        if (copied > 0) {
            target.save();
            LunaSettings.SettingsCreator.refresh(MOD_ID);
        }
        return copied;
    }

    /** The old mod's settings, or null when that mod was never run on this installation. */
    private static JSONObject readOldSettings(String oldModId) {
        try {
            String text = Global.getSettings().readTextFileFromCommon("LunaSettings/" + oldModId + ".json");
            if (text == null || text.trim().isEmpty()) return null;
            return new JSONObject(text);
        } catch (Exception e) {
            return null;
        }
    }

    /** Every value field on the pack's settings page; headers and text rows hold nothing to import. */
    private static Set<String> ownFields() throws Exception {
        Set<String> fields = new HashSet<>();
        JSONArray rows = Global.getSettings().loadCSV("data/config/LunaSettings.csv", MOD_ID);
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String type = row.optString("fieldType", "");
            if (type.equals("Header") || type.equals("Text")) continue;
            String id = row.optString("fieldID", "");
            if (!id.isEmpty()) fields.add(id);
        }
        return fields;
    }
}
