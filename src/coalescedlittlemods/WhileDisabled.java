package coalescedlittlemods;

/**
 * Optional for a module plugin: work that still has to run while the module's toggle is off.
 *
 * <p>Industries are the reason. Turning a module off takes its buildings out of the build menu,
 * but the ones a save already has keep working until the player removes them, so their colony
 * item hookups have to load anyway.</p>
 */
public interface WhileDisabled {

    /** Runs at application load in place of {@code onApplicationLoad}. */
    default void onApplicationLoadWhileDisabled() throws Exception {
    }

    /** Runs on every game load in place of {@code onGameLoad}. */
    default void onGameLoadWhileDisabled(boolean newGame) {
    }
}
