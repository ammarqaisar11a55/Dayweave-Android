package io.github.ammarqaisar11a55.dayweave.core

import android.annotation.SuppressLint
import android.content.Context
import io.github.ammarqaisar11a55.dayweave.notify.FocusNotification
import io.github.ammarqaisar11a55.dayweave.notify.Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

class DayweaveException(val code: String, message: String) : Exception(message)

/**
 * The app's only way into its data. Reads return fresh results, and every write bumps [versions], which
 * makes each screen showing data reload it. Against a local database that is cheap, and it means no
 * screen can show stale data. The desktop app works the same way.
 *
 * It holds only the application context, which lives as long as the process, so nothing leaks.
 */
@SuppressLint("StaticFieldLeak")
object Dayweave {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        coerceInputValues = true
    }

    private val version = MutableStateFlow(0L)
    val versions: StateFlow<Long> = version.asStateFlow()

    lateinit var context: Context
        private set

    @Volatile
    private var opened = false

    fun init(context: Context) = synchronized(this) {
        if (opened) return
        this.context = context.applicationContext
        val directory = File(this.context.filesDir, "data").apply { mkdirs() }
        // Android has no writable /tmp. The core and SQLite both read TMPDIR for their scratch files.
        android.system.Os.setenv("TMPDIR", this.context.cacheDir.path, true)
        NativeCore.open(directory.path)?.let { throw IllegalStateException(it) }
        opened = true
    }

    val dataDirectory: File get() = File(context.filesDir, "data")

    private fun unwrap(reply: String): JsonElement {
        val envelope = json.parseToJsonElement(reply).jsonObject
        envelope["error"]?.jsonObject?.let { error ->
            throw DayweaveException(
                error["code"]?.jsonPrimitive?.content ?: "unknown",
                error["message"]?.jsonPrimitive?.content ?: "Something went wrong",
            )
        }
        return envelope["ok"] ?: JsonNull
    }

    fun callBlocking(method: String, params: JsonObject = NoParams): JsonElement =
        unwrap(NativeCore.rpc(method, params.toString()))

    suspend fun call(method: String, params: JsonObject = NoParams): JsonElement =
        withContext(Dispatchers.IO) { callBlocking(method, params) }

    suspend inline fun <reified T> get(method: String, params: JsonObject = NoParams): T =
        json.decodeFromJsonElement(call(method, params))

    /** Runs a write, then refreshes every screen and moves the next reminder alarm if it changed. */
    suspend fun mutate(method: String, params: JsonObject = NoParams): JsonElement {
        val result = call(method, params)
        refresh()
        withContext(Dispatchers.IO) {
            Scheduler.reschedule(context)
            if (method.startsWith("focus.")) FocusNotification.sync(context)
        }
        return result
    }

    fun refresh() {
        version.value++
    }

    fun tickBlocking(): List<Notice> = json.decodeFromJsonElement(unwrap(NativeCore.tick()))
}

val NoParams = JsonObject(emptyMap())

/** Builds request parameters from plain values. */
fun params(vararg pairs: Pair<String, Any?>): JsonObject =
    JsonObject(pairs.associate { (key, value) -> key to value.toJson() })

fun Any?.toJson(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is List<*> -> JsonArray(map { it.toJson() })
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJson() })
    else -> throw IllegalArgumentException("Unsupported parameter: $this")
}

inline fun <reified T> T.toParams(): JsonObject = Dayweave.json.encodeToJsonElement(
    kotlinx.serialization.serializer<T>(), this,
).jsonObject
