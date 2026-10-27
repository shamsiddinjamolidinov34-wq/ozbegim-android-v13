package uz.ekranoquvchi.mvp.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.speech.tts.TextToSpeech
import uz.ekranoquvchi.mvp.ReaderService

object SystemUtil {
    fun isServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val me = ComponentName(context, ReaderService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun openTtsSettings(context: Context) {
        try {
            context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun openInstallVoiceData(context: Context) {
        try {
            context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
        } catch (_: Exception) {
            openTtsSettings(context)
        }
    }

    fun openBatterySettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun isInstalled(context: Context, pkg: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Throwable) {
        false
    }

    /** Do'kon sahifasini ochadi (Google Play yoki boshqa do'kon); bo'lmasa F-Droid sahifasini brauzerda. */
    fun openStorePage(context: Context, pkg: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/$pkg/")))
            } catch (_: Exception) {
            }
        }
    }
}
