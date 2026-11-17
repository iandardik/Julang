package julay.program.type

/** Type of a handler reference `Class.ctor ~> Class.action` (only valid as a `serveHttp` argument). */
object HandlerRefType : Type {
    override fun isOfType(obj: Any): Boolean = false
    override fun toString(): String = "Handler"
}
