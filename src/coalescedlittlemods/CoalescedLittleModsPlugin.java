package coalescedlittlemods;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.log4j.Logger;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.DroneLauncherShipSystemAPI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.thoughtworks.xstream.XStream;

/**
 * Entry point for Coalesced Little Mods.
 *
 * <p>Each module keeps the plugin it had as a mod of its own. This class holds one of each and
 * passes every game event on to the modules that are switched on, in {@link ClmModule} order. A
 * module that is off gets nothing, except the save setup in {@link #configureXStream} and whatever
 * it asks for through {@link WhileDisabled}.</p>
 */
public class CoalescedLittleModsPlugin extends BaseModPlugin {

    public static final String MOD_ID = "coalesced_little_mods";

    private static final Logger log = Global.getLogger(CoalescedLittleModsPlugin.class);

    private final Map<ClmModule, ModPlugin> plugins = new EnumMap<>(ClmModule.class);

    public CoalescedLittleModsPlugin() {
        plugins.put(ClmModule.CLONING, new cloning.plugin());
        plugins.put(ClmModule.PRIVATE_ARSENAL, new privatearsenal.PrivateArsenalModPlugin());
        plugins.put(ClmModule.STOP_BLOATING_ME, new stopbloatingme.StopBloatingMeModPlugin());
        plugins.put(ClmModule.STOP_STACKING_ME, new stopstackingme.StopStackingMeModPlugin());
        plugins.put(ClmModule.INTEL_RENEWED, new intelrenewed.IntelRenewedModPlugin());
        plugins.put(ClmModule.HULLMODS_RENEWED, new hullmodsrenewed.HullmodsRenewedModPlugin());
        plugins.put(ClmModule.GAMBLING_DEN, new gamblingden.GamblingDenModPlugin());
        plugins.put(ClmModule.STARTER_PACK, new starterpack.StarterPackModPlugin());
    }

    @Override
    public void onApplicationLoad() throws Exception {
        refuseSeparateCopies();
        SettingsImport.run();

        List<String> on = new ArrayList<>();
        List<String> off = new ArrayList<>();
        for (Map.Entry<ClmModule, ModPlugin> entry : plugins.entrySet()) {
            ClmModule module = entry.getKey();
            ModPlugin plugin = entry.getValue();
            if (module.isEnabled()) {
                plugin.onApplicationLoad();
                on.add(module.displayName);
            } else {
                if (plugin instanceof WhileDisabled) ((WhileDisabled) plugin).onApplicationLoadWhileDisabled();
                off.add(module.displayName);
            }
        }
        log.info("Coalesced Little Mods: on " + on + ", off " + off);
    }

    /**
     * The pack carries the same code as the separate mods, so running both would load every class
     * twice and add every button, building and settings page twice. Stop at startup with a plain
     * explanation instead.
     */
    private static void refuseSeparateCopies() {
        List<String> clashes = new ArrayList<>();
        for (ClmModule module : ClmModule.values()) {
            if (Global.getSettings().getModManager().isModEnabled(module.standaloneModId)) {
                clashes.add(module.displayName);
            }
        }
        if (clashes.isEmpty()) return;
        throw new IllegalStateException("Coalesced Little Mods already includes " + String.join(", ", clashes)
                + ". Disable the separate " + (clashes.size() == 1 ? "mod" : "mods")
                + " in the mod list and start the game again.");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        for (Map.Entry<ClmModule, ModPlugin> entry : plugins.entrySet()) {
            ModPlugin plugin = entry.getValue();
            if (entry.getKey().isEnabled()) {
                plugin.onGameLoad(newGame);
            } else if (plugin instanceof WhileDisabled) {
                ((WhileDisabled) plugin).onGameLoadWhileDisabled(newGame);
            }
        }
    }

    /** Runs for every module, on or off: a save made with a module on must still load with it off. */
    @Override
    public void configureXStream(XStream x) {
        for (ModPlugin plugin : plugins.values()) plugin.configureXStream(x);
    }

    @Override
    public void onEnabled(boolean wasEnabledBefore) {
        forEachEnabled(plugin -> plugin.onEnabled(wasEnabledBefore));
    }

    @Override
    public void onNewGame() {
        forEachEnabled(ModPlugin::onNewGame);
    }

    @Override
    public void onNewGameAfterProcGen() {
        forEachEnabled(ModPlugin::onNewGameAfterProcGen);
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        forEachEnabled(ModPlugin::onNewGameAfterEconomyLoad);
    }

    @Override
    public void onNewGameAfterTimePass() {
        forEachEnabled(ModPlugin::onNewGameAfterTimePass);
    }

    @Override
    public void beforeGameSave() {
        forEachEnabled(ModPlugin::beforeGameSave);
    }

    @Override
    public void afterGameSave() {
        forEachEnabled(ModPlugin::afterGameSave);
    }

    @Override
    public void onGameSaveFailed() {
        forEachEnabled(ModPlugin::onGameSaveFailed);
    }

    @Override
    public void onDevModeF8Reload() {
        forEachEnabled(ModPlugin::onDevModeF8Reload);
    }

    @Override
    public void onAboutToStartGeneratingCodex() {
        forEachEnabled(ModPlugin::onAboutToStartGeneratingCodex);
    }

    @Override
    public void onAboutToLinkCodexEntries() {
        forEachEnabled(ModPlugin::onAboutToLinkCodexEntries);
    }

    @Override
    public void onCodexDataGenerated() {
        forEachEnabled(ModPlugin::onCodexDataGenerated);
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        return bestPick(plugin -> plugin.pickShipAI(member, ship));
    }

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        return bestPick(plugin -> plugin.pickWeaponAutofireAI(weapon));
    }

    @Override
    public PluginPick<ShipAIPlugin> pickDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        return bestPick(plugin -> plugin.pickDroneAI(drone, mothership, system));
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        return bestPick(plugin -> plugin.pickMissileAI(missile, launchingShip));
    }

    private void forEachEnabled(Consumer<ModPlugin> action) {
        for (Map.Entry<ClmModule, ModPlugin> entry : plugins.entrySet()) {
            if (entry.getKey().isEnabled()) action.accept(entry.getValue());
        }
    }

    /** The highest-priority answer among the enabled modules, or null when none of them has one. */
    private <T> PluginPick<T> bestPick(Function<ModPlugin, PluginPick<T>> ask) {
        PluginPick<T> best = null;
        for (Map.Entry<ClmModule, ModPlugin> entry : plugins.entrySet()) {
            if (!entry.getKey().isEnabled()) continue;
            PluginPick<T> pick = ask.apply(entry.getValue());
            if (pick != null && (best == null || pick.priority.ordinal() > best.priority.ordinal())) best = pick;
        }
        return best;
    }
}
