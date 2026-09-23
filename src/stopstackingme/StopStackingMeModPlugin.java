package stopstackingme;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

/**
 * Entry point for StopStackingMe.
 *
 * <p>Reads the LunaSettings config at application load, and on every game load registers the
 * {@code EveryFrameScript} that flattens stacked cargo icons wherever they are on screen.</p>
 */
public class StopStackingMeModPlugin extends BaseModPlugin {

    /** Settings and data now live under the pack's mod id. */
    public static final String MOD_ID = coalescedlittlemods.CoalescedLittleModsPlugin.MOD_ID;

    @Override
    public void onApplicationLoad() throws Exception {
        SssSettings.INSTANCE.init();
        Global.getLogger(StopStackingMeModPlugin.class).info("StopStackingMe: application loaded.");
    }

    @Override
    public void onGameLoad(boolean newGame) {
        // Transient: not saved with the campaign, so it's re-added cleanly on every load.
        Global.getSector().addTransientScript(new StackIconFlattener());
    }
}
