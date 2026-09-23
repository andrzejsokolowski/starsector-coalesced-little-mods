package coalescedlittlemods;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.ModPlugin;
import com.fs.starfarer.api.ModSpecAPI;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.CLM_ModuleEnabled;
import com.fs.starfarer.api.util.Misc;
import com.thoughtworks.xstream.XStream;

import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.lunaSettings.LunaSettings;

/**
 * Checks for the pack's own plumbing: module toggles, the one-time settings import, how game
 * events reach the modules, and the guard against running the separate mods alongside.
 *
 * Runs against a mock game: saves/common is an in-memory map and the settings page is the real
 * data/config/LunaSettings.csv. LunaLib itself is the real library.
 */
public final class PackChecks {

    private static final String ID = CoalescedLittleModsPlugin.MOD_ID;

    private static final Map<String, String> common = new HashMap<>();
    private static final Set<String> enabledMods = new HashSet<>();
    private static int checks;

    public static void main(String[] args) throws Exception {
        installMockGame();
        settingsPage();
        importCarriesOldValues();
        importRunsOnce();
        importWithNothingToCarry();
        toggles();
        ruleCondition();
        eventRouting();
        separateCopiesRefused();
        System.out.println("PASS: " + checks + " pack checks.");
    }

    // --- Scenarios ------------------------------------------------------------------------------

    /**
     * The modules that add campaign content have an on-by-default switch on the Modules tab, the
     * others have none, and field ids never repeat.
     */
    private static void settingsPage() throws Exception {
        JSONArray rows = csv(Path.of("data/config/LunaSettings.csv"));
        Map<String, JSONObject> byId = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String id = row.getString("fieldID");
            if (id.isEmpty()) continue;
            check(byId.put(id, row) == null, "Field id used twice: " + id);
        }
        Set<ClmModule> switched = Set.of(ClmModule.CLONING, ClmModule.PRIVATE_ARSENAL, ClmModule.GAMBLING_DEN);
        for (ClmModule module : ClmModule.values()) {
            check((module.toggleId != null) == switched.contains(module), module + " has the wrong kind of switch");
            if (module.toggleId == null) continue;
            JSONObject row = byId.get(module.toggleId);
            check(row != null, "No toggle for " + module);
            check(row.getString("fieldType").equals("Boolean") && row.getString("defaultValue").equals("true"),
                    "Toggle for " + module + " is not an on-by-default switch");
            check(row.getString("tab").equals("Modules"), "Toggle for " + module + " is not on the Modules tab");
            check(row.getString("fieldName").equals(module.displayName), "Toggle label differs from " + module.displayName);
        }
        long switches = byId.keySet().stream().filter(id -> id.startsWith("clm_module_")).count();
        check(switches == switched.size(), "The Modules tab has " + switches + " switches");
    }

    private static void importCarriesOldValues() throws Exception {
        freshInstall();
        String oldCloning = "{\"baseGrowthTier1\": 67, \"scalingFactor\": 2, \"globalMult\": 1.5, \"resetToDefault\": true}";
        String oldHullmods = "{\"hmr_border_color\": \"#5bc26c\", \"hmr_wipe_prefs\": true, \"hmr_retired_field\": 5}";
        String oldDen = "{\"gd_jackpot_match_4\": 2, \"gd_pachinko_cost_tokens\": 2}";
        common.put("LunaSettings/oddisz_cloning.json", oldCloning);
        common.put("LunaSettings/hullmods_renewed.json", oldHullmods);
        common.put("LunaSettings/gambling_den.json", oldDen);

        SettingsImport.run();

        check(LunaSettings.getInt(ID, "cloning_base_growth_tier1") == 67, "Renamed Cloning value not carried over");
        check(LunaSettings.getDouble(ID, "cloning_scaling_factor") == 2.0, "Cloning scaling factor not carried over");
        check(LunaSettings.getDouble(ID, "cloning_global_mult") == 1.5, "Cloning multiplier not carried over");
        check(LunaSettings.getInt(ID, "cloning_base_growth_tier2") == 125, "Untouched field lost its default");
        check(!LunaSettings.getBoolean(ID, "cloning_reset_to_default"), "A one-shot reset was carried over");
        check(!LunaSettings.getBoolean(ID, "hmr_wipe_prefs"), "A one-shot wipe was carried over");
        check("#5bc26c".equals(LunaSettings.getString(ID, "hmr_border_color")), "Colour not carried over");
        check(LunaSettings.getInt(ID, "gd_jackpot_match_4") == 2, "Gambling Den value not carried over");

        JSONObject stored = new JSONObject(common.get("LunaSettings/" + ID + ".json"));
        check(!stored.has("hmr_retired_field") && !stored.has("gd_pachinko_cost_tokens"),
                "Fields the pack does not have were carried over");
        check(stored.has("clm_module_cloning"), "Pack defaults missing after the import");
        check(common.get("LunaSettings/oddisz_cloning.json").equals(oldCloning)
                        && common.get("LunaSettings/hullmods_renewed.json").equals(oldHullmods)
                        && common.get("LunaSettings/gambling_den.json").equals(oldDen),
                "The separate mods' settings files were changed");
        JSONObject marker = new JSONObject(common.get("coalesced_little_mods/settings_import.json"));
        check(marker.getBoolean("done") && marker.getInt("valuesImported") == 5, "Import marker wrong: " + marker);
    }

    /** A later edit in the pack must survive: the old files are read only on the first launch. */
    private static void importRunsOnce() throws Exception {
        JSONObject stored = new JSONObject(common.get("LunaSettings/" + ID + ".json"));
        stored.put("cloning_base_growth_tier1", 99);
        common.put("LunaSettings/" + ID + ".json", stored.toString());
        LunaSettings.SettingsCreator.refresh(ID);

        SettingsImport.run();
        check(LunaSettings.getInt(ID, "cloning_base_growth_tier1") == 99, "The import ran a second time");
    }

    private static void importWithNothingToCarry() throws Exception {
        freshInstall();
        SettingsImport.run();
        JSONObject marker = new JSONObject(common.get("coalesced_little_mods/settings_import.json"));
        check(marker.getBoolean("done") && marker.getInt("valuesImported") == 0, "Empty import marker wrong");
        check(LunaSettings.getInt(ID, "cloning_base_growth_tier1") == 50, "Default lost on a clean install");
    }

    /** Switches are taken when a game starts or loads, then held until the next one. */
    private static void toggles() throws Exception {
        freshInstall();
        CoalescedLittleModsPlugin pack = new CoalescedLittleModsPlugin();
        installRecorders(pack);
        pack.onApplicationLoad();
        for (ClmModule module : ClmModule.values()) check(module.isEnabled(), module + " is off by default");

        // Switched off at the main menu, then a new game started without a restart.
        setToggle(ClmModule.GAMBLING_DEN, false);
        check(ClmModule.GAMBLING_DEN.isEnabled(), "A switch changed before any game started or loaded");
        pack.onNewGame();
        check(!ClmModule.GAMBLING_DEN.isEnabled(), "Switched-off module still on in the new game");
        check(ClmModule.CLONING.isEnabled(), "Switching one module off affected another");

        // Flipped back during play: held through the rest of that game, saves included.
        setToggle(ClmModule.GAMBLING_DEN, true);
        pack.onGameLoad(true);
        check(!ClmModule.GAMBLING_DEN.isEnabled(), "A switch changed in the middle of starting a game");
        pack.beforeGameSave();
        pack.afterGameSave();
        check(!ClmModule.GAMBLING_DEN.isEnabled(), "A switch changed mid-game");
        pack.onGameLoad(false);
        check(ClmModule.GAMBLING_DEN.isEnabled(), "Loading a save did not pick up the switch");

        // Switches that 0.1.0 had for the always-on modules may still be saved as off.
        JSONObject stored = new JSONObject(common.get("LunaSettings/" + ID + ".json"));
        for (String gone : new String[]{"clm_module_stopbloatingme", "clm_module_stopstackingme",
                "clm_module_intel_renewed", "clm_module_hullmods_renewed", "clm_module_starterpack"}) {
            stored.put(gone, false);
        }
        common.put("LunaSettings/" + ID + ".json", stored.toString());
        LunaSettings.SettingsCreator.refresh(ID);
        pack.onGameLoad(false);
        for (ClmModule module : ClmModule.values()) {
            if (module.toggleId == null) check(module.isEnabled(), module + " was turned off by an old switch");
        }
    }

    private static void ruleCondition() throws Exception {
        freshInstall();
        setToggle(ClmModule.GAMBLING_DEN, false);
        ClmModule.readToggles();
        CLM_ModuleEnabled command = new CLM_ModuleEnabled();
        check(!command.execute("gd_add_option", null, tokens("GAMBLING_DEN"), null), "Den offered with its module off");
        check(command.execute("x", null, tokens("CLONING"), null), "Condition false for a module that is on");
        check(command.execute("x", null, tokens("STARTER_PACK"), null), "Condition false for an always-on module");
        check(!command.execute("x", null, tokens("NO_SUCH_MODULE"), null), "Unknown module name passed the condition");
        check(!command.execute("x", null, new ArrayList<>(), null), "Condition passed with no module named");
    }

    private static void eventRouting() throws Exception {
        freshInstall();
        setToggle(ClmModule.PRIVATE_ARSENAL, false);
        setToggle(ClmModule.GAMBLING_DEN, false);

        CoalescedLittleModsPlugin pack = new CoalescedLittleModsPlugin();
        Map<ClmModule, Recorder> recorders = installRecorders(pack);
        recorders.get(ClmModule.STOP_STACKING_ME).pick = new PluginPick<>(null, PickPriority.MOD_GENERAL);
        recorders.get(ClmModule.STARTER_PACK).pick = new PluginPick<>(null, PickPriority.MOD_SPECIFIC);
        recorders.get(ClmModule.GAMBLING_DEN).pick = new PluginPick<>(null, PickPriority.HIGHEST);

        pack.onApplicationLoad();
        pack.onGameLoad(false);
        pack.onNewGameAfterTimePass();
        pack.configureXStream(null);
        PluginPick<ShipAIPlugin> pick = pack.pickShipAI(null, null);

        for (Map.Entry<ClmModule, Recorder> entry : recorders.entrySet()) {
            ClmModule module = entry.getKey();
            List<String> calls = entry.getValue().calls;
            boolean on = module != ClmModule.PRIVATE_ARSENAL && module != ClmModule.GAMBLING_DEN;
            check(calls.contains("configureXStream"), module + " missed the save setup");
            check(calls.contains("onApplicationLoad"), module + " missed the startup work");
            if (on) {
                check(calls.contains("onGameLoad") && calls.contains("onNewGameAfterTimePass"),
                        module + " is on but missed events: " + calls);
            } else {
                check(!calls.contains("onGameLoad") && !calls.contains("onNewGameAfterTimePass")
                        && !calls.contains("pickShipAI"), module + " is off but still ran: " + calls);
            }
        }
        List<String> arsenal = recorders.get(ClmModule.PRIVATE_ARSENAL).calls;
        check(arsenal.contains("onGameLoadWhileDisabled"),
                "Switched-off module did not get its while-disabled hook: " + arsenal);
        check(pick == recorders.get(ClmModule.STARTER_PACK).pick,
                "AI pick should be the highest priority among modules that are on");
    }

    private static void separateCopiesRefused() throws Exception {
        freshInstall();
        CoalescedLittleModsPlugin pack = new CoalescedLittleModsPlugin();
        Map<ClmModule, Recorder> recorders = installRecorders(pack);
        enabledMods.add("stopstackingme");
        enabledMods.add("gambling_den");
        try {
            pack.onApplicationLoad();
            check(false, "Running next to the separate mods was not refused");
        } catch (IllegalStateException e) {
            check(e.getMessage().contains("StopStackingMe") && e.getMessage().contains("Gambling Den"),
                    "Refusal does not name the clashing mods: " + e.getMessage());
        } finally {
            enabledMods.remove("stopstackingme");
            enabledMods.remove("gambling_den");
        }
        for (Recorder recorder : recorders.values()) {
            check(recorder.calls.isEmpty(), "A module started before the clash was refused");
        }
    }

    // --- Mock game ------------------------------------------------------------------------------

    private static void installMockGame() {
        Logger.getLogger("lunalib").setLevel(Level.INFO);
        ModSpecAPI pack = proxy(ModSpecAPI.class, (m, a) -> switch (m.getName()) {
            case "getId" -> ID;
            case "getName" -> "Coalesced Little Mods";
            default -> null;
        });
        ModManagerAPI modManager = proxy(ModManagerAPI.class, (m, a) -> switch (m.getName()) {
            case "isModEnabled" -> enabledMods.contains((String) a[0]) || a[0].equals(ID) || a[0].equals("lunalib");
            case "getEnabledModsCopy" -> List.of(pack);
            case "getModSpec" -> a[0].equals(ID) ? pack : null;
            default -> null;
        });
        Global.setSettings(proxy(SettingsAPI.class, (m, a) -> switch (m.getName()) {
            case "getModManager" -> modManager;
            case "readTextFileFromCommon" -> {
                String text = common.get((String) a[0]);
                if (text == null) throw new IllegalStateException("No such common file: " + a[0]);
                yield text;
            }
            case "writeTextFileToCommon" -> {
                common.put((String) a[0], (String) a[1]);
                yield null;
            }
            case "fileExistsInCommon" -> common.containsKey((String) a[0]);
            case "loadCSV" -> {
                if (a.length > 1 && !ID.equals(a[1])) throw new IllegalStateException("No such file in " + a[1]);
                yield csv(Path.of((String) a[0]));
            }
            case "loadJSON" -> new JSONObject("{}");
            default -> null;
        }));
    }

    /** A clean saves/common and a LunaLib that has not loaded anything yet. */
    private static void freshInstall() {
        common.clear();
        LunaSettingsLoader.INSTANCE.setHasLoaded(false);
        LunaSettingsLoader.setSettings(new HashMap<>());
        LunaSettingsLoader.getSettingsData().clear();
        resetSwitches();
    }

    /** Flips a switch the way the settings menu does. The pack only sees it when it next reads them. */
    private static void setToggle(ClmModule module, boolean on) throws Exception {
        LunaSettings.getBoolean(ID, module.toggleId);
        JSONObject stored = new JSONObject(common.get("LunaSettings/" + ID + ".json"));
        stored.put(module.toggleId, on);
        common.put("LunaSettings/" + ID + ".json", stored.toString());
        LunaSettings.SettingsCreator.refresh(ID);
    }

    /** A new scenario is a new launch: every module starts out on. */
    private static void resetSwitches() {
        try {
            Field enabled = ClmModule.class.getDeclaredField("enabled");
            enabled.setAccessible(true);
            for (ClmModule module : ClmModule.values()) enabled.setBoolean(module, true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<ClmModule, Recorder> installRecorders(CoalescedLittleModsPlugin pack) throws Exception {
        Field field = CoalescedLittleModsPlugin.class.getDeclaredField("plugins");
        field.setAccessible(true);
        Map<ClmModule, ModPlugin> plugins = (Map<ClmModule, ModPlugin>) field.get(pack);
        Map<ClmModule, Recorder> recorders = new EnumMap<>(ClmModule.class);
        for (ClmModule module : ClmModule.values()) {
            Recorder recorder = new Recorder();
            recorders.put(module, recorder);
            plugins.put(module, recorder);
        }
        return recorders;
    }

    private static final class Recorder extends BaseModPlugin implements WhileDisabled {
        final List<String> calls = new ArrayList<>();
        PluginPick<ShipAIPlugin> pick;

        @Override public void onApplicationLoad() { calls.add("onApplicationLoad"); }
        @Override public void onGameLoad(boolean newGame) { calls.add("onGameLoad"); }
        @Override public void onNewGameAfterTimePass() { calls.add("onNewGameAfterTimePass"); }
        @Override public void configureXStream(XStream x) { calls.add("configureXStream"); }
        @Override public void onGameLoadWhileDisabled(boolean newGame) { calls.add("onGameLoadWhileDisabled"); }

        @Override
        public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
            calls.add("pickShipAI");
            return pick;
        }
    }

    private static List<Misc.Token> tokens(String... values) {
        List<Misc.Token> list = new ArrayList<>();
        for (String value : values) list.add(new Misc.Token(value, Misc.TokenType.LITERAL));
        return list;
    }

    /** Starsector's CSV reading: a header row, quoted fields may hold commas and line breaks. */
    private static JSONArray csv(Path path) throws Exception {
        String text = Files.readString(path);
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else if (c == '"') quoted = false;
                else cell.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                record.add(cell.toString()); cell.setLength(0);
            } else if (c == '\n') {
                record.add(cell.toString()); cell.setLength(0);
                records.add(record); record = new ArrayList<>();
            } else if (c != '\r') {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !record.isEmpty()) { record.add(cell.toString()); records.add(record); }

        List<String> header = records.get(0);
        JSONArray rows = new JSONArray();
        for (List<String> r : records.subList(1, records.size())) {
            JSONObject row = new JSONObject();
            for (int c = 0; c < header.size(); c++) row.put(header.get(c), c < r.size() ? r.get(c) : "");
            rows.put(row);
        }
        return rows;
    }

    private interface Handler {
        Object call(java.lang.reflect.Method method, Object[] args) throws Exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            if (m.getName().equals("toString")) return "mock " + type.getSimpleName();
            if (m.getName().equals("hashCode")) return System.identityHashCode(p);
            if (m.getName().equals("equals")) return p == a[0];
            Object value = handler.call(m, a == null ? new Object[0] : a);
            if (value != null) return value;
            Class<?> r = m.getReturnType();
            if (r == boolean.class) return false;
            if (r == int.class) return 0;
            if (r == float.class) return 0f;
            return null;
        });
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
