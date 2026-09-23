package coalescedlittlemods;

/**
 * Optional for a module plugin: work that still has to run while the module is switched off.
 */
public interface WhileDisabled {

    /** Runs on every game load in place of {@code onGameLoad}. */
    default void onGameLoadWhileDisabled(boolean newGame) {
    }
}
