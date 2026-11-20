package io.github.ammarqaisar11a55.dayweave.update

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import io.github.ammarqaisar11a55.dayweave.BuildConfig
import io.github.ammarqaisar11a55.dayweave.util.isNewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class UpdateException(message: String) : Exception(message)

data class Release(val version: String, val notes: String, val apkUrl: String, val apkSize: Long, val sha256: String)

/**
 * The small manifest each release publishes next to its APK. The release workflow writes it, so the app
 * never needs GitHub's API, which allows only 60 anonymous requests an hour per network.
 */
@Serializable
private data class Manifest(
    val version: String,
    val apk: String,
    val size: Long = 0,
    val sha256: String,
    val notes: String = "",
)

/**
 * Updates from the GitHub repository's releases. This is the app's only network use: it reads the latest
 * release's manifest and downloads the new package, sending nothing about you.
 *
 * Every download is checked against the SHA-256 checksum in the manifest and against this app's signing
 * certificate before Android is asked to install it. Android itself also refuses an update signed with a
 * different key.
 */
object Updater {
    private val json = Json { ignoreUnknownKeys = true }
    private const val RELEASES = BuildConfig.UPDATE_RELEASES

    // GitHub sends /latest/ to the newest release that isn't a draft or pre-release.
    private const val MANIFEST = "$RELEASES/latest/download/latest.json"

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("User-Agent", "Dayweave-Android/${BuildConfig.VERSION_NAME}")
    }

    /** The latest published release if it is newer than this app, or null when up to date. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        val connection = open(MANIFEST)
        try {
            when (connection.responseCode) {
                404 -> return@withContext null // Nothing released yet.
                in 200..299 -> Unit
                else -> throw UpdateException("GitHub answered ${connection.responseCode}. Try again later")
            }
            val manifest = try {
                json.decodeFromString<Manifest>(connection.inputStream.bufferedReader().readText())
            } catch (error: kotlinx.serialization.SerializationException) {
                throw UpdateException("The latest release couldn't be read. Try again later")
            }
            if (!isNewer(manifest.version, BuildConfig.VERSION_NAME.removeSuffix("-debug"))) return@withContext null
            Release(
                manifest.version, manifest.notes,
                "$RELEASES/download/v${manifest.version}/${manifest.apk}", manifest.size, manifest.sha256.lowercase(),
            )
        } catch (error: java.io.IOException) {
            throw UpdateException("Couldn't reach GitHub. Check your connection")
        } finally {
            connection.disconnect()
        }
    }

    suspend fun download(context: Context, release: Release, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
        val file = File(directory, "dayweave-${release.version}.apk")
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            val connection = open(release.apkUrl)
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        digest.update(buffer, 0, read)
                        total += read
                        if (release.apkSize > 0) onProgress(total.toFloat() / release.apkSize)
                    }
                }
            }
        } catch (error: java.io.IOException) {
            throw UpdateException("The download stopped. Check your connection and try again")
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != release.sha256) {
            file.delete()
            throw UpdateException("The download didn't match its published checksum, so it wasn't installed")
        }
        if (!signedLikeThisApp(context, file)) {
            file.delete()
            throw UpdateException("This update isn't signed with Dayweave's key, so it wasn't installed")
        }
        file
    }

    @SuppressLint("PackageManagerGetSignatures")
    @Suppress("DEPRECATION")
    private fun signedLikeThisApp(context: Context, apk: File): Boolean {
        val manager = context.packageManager
        fun fingerprints(signatures: Array<android.content.pm.Signature>?) =
            signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).toList() }.toSet()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val flag = PackageManager.GET_SIGNING_CERTIFICATES
            val installed = manager.getPackageInfo(context.packageName, flag).signingInfo?.apkContentsSigners
            val candidate = manager.getPackageArchiveInfo(apk.path, flag)?.signingInfo?.apkContentsSigners
            candidate != null && fingerprints(installed) == fingerprints(candidate)
        } else {
            val flag = PackageManager.GET_SIGNATURES
            val installed = manager.getPackageInfo(context.packageName, flag).signatures
            val candidate = manager.getPackageArchiveInfo(apk.path, flag)?.signatures
            candidate != null && fingerprints(installed) == fingerprints(candidate)
        }
    }

    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    /** Hands the verified package to Android's installer, which asks the person to confirm. */
    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = installer.createSession(sessionParams)
        installer.openSession(sessionId).use { session ->
            session.openWrite("dayweave.apk", 0, apk.length()).use { output -> apk.inputStream().use { it.copyTo(output) } }
            val callback = PendingIntent.getBroadcast(
                context, sessionId, Intent(context, InstallReceiver::class.java),
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            session.commit(callback.intentSender)
        }
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> Unit
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "The update wasn't installed"
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }
}

/** Android-only preferences that the shared core doesn't know about. */
class UpdatePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var autoCheck: Boolean
        get() = preferences.getBoolean("autoCheck", true)
        set(value) = preferences.edit().putBoolean("autoCheck", value).apply()

    var lastCheck: Long
        get() = preferences.getLong("lastCheck", 0)
        set(value) = preferences.edit().putLong("lastCheck", value).apply()

    /** At most once a day, so the app touches the network as little as possible. */
    fun dueForCheck(now: Long = System.currentTimeMillis()) = autoCheck && now - lastCheck > 24 * 60 * 60 * 1000L
}
