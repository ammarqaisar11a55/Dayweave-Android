package io.github.ammarqaisar11a55.dayweave.core

/** The Rust core, shared with the desktop app. Everything crosses as JSON. See native/src/lib.rs. */
internal object NativeCore {
    init {
        System.loadLibrary("dayweave")
    }

    /** Returns null once the database is open, or a message saying why it couldn't be. */
    @JvmStatic external fun open(dataDir: String): String?

    /** Returns `{"ok": result}` or `{"error": {"code", "message"}}`. */
    @JvmStatic external fun rpc(method: String, params: String): String

    /** One scheduler pass. Returns the notices to show, in the same envelope as [rpc]. */
    @JvmStatic external fun tick(): String
}
