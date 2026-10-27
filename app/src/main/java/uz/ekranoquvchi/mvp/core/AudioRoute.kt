package uz.ekranoquvchi.mvp.core

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

object AudioRoute {
    /** Quloqchin (simli, USB, Bluetooth, eshitish apparati) ulanganmi? Maxfiy xabarlarni o'qish qoidasi uchun. */
    fun headsetConnected(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { d ->
            when (d.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_HEARING_AID -> true
                else -> Build.VERSION.SDK_INT >= 31 && d.type == AudioDeviceInfo.TYPE_BLE_HEADSET
            }
        }
    }
}
