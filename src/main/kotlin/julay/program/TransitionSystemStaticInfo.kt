package julay.program

import julay.program.action.ConcreteAction
import julay.program.action.SymbolicAction

data class TransitionSystemStaticInfo(
    /**
     * A name that uniquely describes the process class (stable across occurrence-specific alphabets).
     */
    val name: String,

    /**
     * The alphabet, i.e. the set of all possible symbolic actions that this TS will engage in.
     */
    val alphabet: Set<SymbolicAction>,

    /**
     * For each constructor action, a function that constructs a new TransitionSystem.
     */
    val constructors: Map<SymbolicAction, suspend (Program, ConcreteAction) -> TransitionSystem>,
) {
    /** Stable class identity for affinity (not affected by channelKey overrides). */
    fun classID() = name.hashCode()

    /**
     * Copy with per-action [channelKey] overrides (by action name). Used for occurrence-specific
     * composition-hidden syncs, including Kotlin library leaves.
     */
    fun withChannelKeys(overrides: Map<String, String>): TransitionSystemStaticInfo {
        if (overrides.isEmpty()) return this
        fun remap(act: SymbolicAction): SymbolicAction {
            val key = overrides[act.name] ?: return act
            return if (act.channelKey == key) act else act.copy(channelKey = key)
        }
        val newAlphabet = alphabet.map { remap(it) }.toSet()
        val newConstructors = constructors.entries.associate { (act, factory) -> remap(act) to factory }
        return copy(alphabet = newAlphabet, constructors = newConstructors)
    }

    /**
     * Resolve a TS-offered action (possibly with a default public channelKey) to the alphabet /
     * constructor entry for this StaticInfo occurrence.
     */
    fun resolveAction(act: SymbolicAction): SymbolicAction {
        fun matches(candidate: SymbolicAction) =
            candidate.name == act.name &&
                candidate.args == act.args &&
                candidate.isInternal == act.isInternal &&
                candidate.isSession == act.isSession
        return alphabet.firstOrNull { matches(it) }
            ?: constructors.keys.firstOrNull { matches(it) }
            ?: act
    }
}

interface StaticInfo {
    fun staticInfo(): TransitionSystemStaticInfo
}
