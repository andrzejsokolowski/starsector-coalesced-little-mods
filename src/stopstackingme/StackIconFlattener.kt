package stopstackingme

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CoreUITabId
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.ui.UIComponentAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.campaign.CampaignState
import com.fs.state.AppDriver
import stopstackingme.uiframework.ReflectionUtils
import java.awt.Color

/**
 * Makes cargo tiles draw one sprite instead of a pile.
 *
 * ### What the game does
 * A cargo tile (`CargoStackView`, used by the cargo screen, trading, storage, loot **and** the refit
 * weapon picker) draws its weapon up to six times in a small pyramid — one bright copy in front and
 * up to five more behind it, tinted a flat grey. That grey is held in two per-tile fields, `row2`
 * and `row3`, which nothing else in the tile reads. Crew and marine tiles do the same, one extra
 * portrait per 50 aboard. Everything else — fighter LPCs, blueprints, AI cores, resources, special
 * items — already draws a single sprite.
 *
 * ### What this does
 * Sets those two colours to zero alpha. The extra copies are still drawn, they just put nothing on
 * screen. Nothing else about the tile changes: same sprite size, same red "you can't take this"
 * background, same quantity number, same tooltip, same drag-and-drop.
 *
 * The one visible side effect is the game's own doing: when a tile holds more than one, it slides
 * the front copy down by 8 (out of a 100-wide cell) to leave room for the pile. That drop stays, so
 * a stack of three sits very slightly lower in its cell than a stack of one.
 *
 * ### How it gets there
 * Same approach as Hullmods - Renewed: run while the campaign is paused, grab the core UI (handling
 * the docked-at-a-market case where it lives inside the encounter dialog), and walk the component
 * tree. Campaign only — the refit screen inside a mission or the simulator runs in a different app
 * state and is not covered.
 */
class StackIconFlattener : EveryFrameScript {

    private var loggedFailure = false

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        // Never let a reflection hiccup take the campaign down with it: the worst this mod should
        // ever do is stop working. Logged once so it is still findable in starsector.log.
        try {
            run()
        } catch (t: Throwable) {
            if (!loggedFailure) {
                loggedFailure = true
                Global.getLogger(StackIconFlattener::class.java)
                    .error("StopStackingMe: could not reach the cargo tiles; leaving icons as they are.", t)
            }
        }
    }

    private fun run() {
        val sector = Global.getSector() ?: return
        // Cargo tiles are only on screen while a core tab or an interaction dialog is open, and both
        // of those pause the campaign. This skips the tree walk entirely during normal play.
        if (!sector.isPaused) return

        val state = AppDriver.getInstance()?.currentState
        if (state !is CampaignState) return

        val root = coreUI(state, sector) ?: return
        flatten(root, 0)
    }

    /** The panel to search under, or null if this screen cannot be showing cargo tiles. */
    private fun coreUI(state: CampaignState, sector: SectorAPI): UIPanelAPI? {
        // Docked at a market, or looting a wreck: the core UI sits inside the encounter dialog.
        val dialog = ReflectionUtils.invoke(state, "getEncounterDialog")
        if (dialog != null) return ReflectionUtils.invoke(dialog, "getCoreUI") as? UIPanelAPI

        // Free-flying with a tab open. Only these two can show cargo tiles.
        val tab = sector.campaignUI?.currentCoreTab
        if (tab != CoreUITabId.CARGO && tab != CoreUITabId.REFIT) return null
        return ReflectionUtils.invoke(state, "getCore") as? UIPanelAPI
    }

    private fun flatten(node: UIComponentAPI, depth: Int) {
        if (depth > MAX_DEPTH) return
        if (isStackTile(node.javaClass)) {
            flattenTile(node)
            return // a tile's own children are its quantity and cost labels; nothing to do in there
        }
        if (node is UIPanelAPI) {
            for (child in childrenOf(node)) flatten(child, depth + 1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun childrenOf(panel: UIPanelAPI): List<UIComponentAPI> =
        (ReflectionUtils.invoke(panel, "getChildrenCopy") as? List<UIComponentAPI>) ?: emptyList()

    private fun flattenTile(view: UIComponentAPI) {
        val stack = ReflectionUtils.invoke(view, "getStack") as? CargoStackAPI ?: return
        val flatten = when {
            stack.isWeaponStack -> SssSettings.flattenWeapons
            stack.isCrewStack || stack.isMarineStack -> SssSettings.flattenPersonnel
            else -> return // nothing else stacks its sprite
        }
        // Writing the vanilla grey back when a setting is off makes the toggles live: tiles that are
        // already on screen go back to piling up as soon as the next frame draws.
        val target = if (flatten) INVISIBLE else VANILLA_ROW
        for (field in rowColorFields(view)) {
            if (field.get(view) !== target) field.set(view, target)
        }
    }

    /**
     * The tile's two "extra copy" tint fields. Looked up by name first; if a future game build
     * renames them, falls back to matching on type — but only when that still finds exactly two, so
     * a third colour field appearing one day means we back off rather than paint over it.
     */
    private fun rowColorFields(view: UIComponentAPI): List<ReflectionUtils.ReflectedField> {
        val byName = ReflectionUtils.getFieldsMatching(view, name = "row2", type = Color::class.java) +
            ReflectionUtils.getFieldsMatching(view, name = "row3", type = Color::class.java)
        if (byName.size == 2) return byName

        val byType = ReflectionUtils.getFieldsMatching(view, type = Color::class.java)
        return if (byType.size == 2) byType else emptyList()
    }

    /**
     * Is this component a cargo tile? Decided once per class and cached, since it runs against every
     * component in the tree on every frame.
     */
    private fun isStackTile(cls: Class<*>): Boolean = tileClasses.getOrPut(cls) {
        if (cls.name == STACK_VIEW_CLASS) return@getOrPut true
        // Name-independent fallback for future game builds. These three together are unique to the
        // cargo tile among UI components.
        ReflectionUtils.getMethodsMatching(clazz = cls, name = "getStack").isNotEmpty() &&
            ReflectionUtils.getMethodsMatching(clazz = cls, name = "setScaleMult").isNotEmpty() &&
            ReflectionUtils.getMethodsMatching(clazz = cls, name = "updateQuantity").isNotEmpty()
    }

    companion object {
        private const val STACK_VIEW_CLASS = "com.fs.starfarer.campaign.ui.trade.CargoStackView"

        /** Guards against a cycle in the tree; the real UI nests nowhere near this deep. */
        private const val MAX_DEPTH = 32

        /** The grey the game tints the extra copies with. */
        private val VANILLA_ROW = Color(95, 95, 95, 255)

        /** The same grey at zero alpha — drawn, but invisible. */
        private val INVISIBLE = Color(95, 95, 95, 0)

        /** Shared across script instances so a reload does not start the cache from scratch. */
        private val tileClasses = HashMap<Class<*>, Boolean>()
    }
}
