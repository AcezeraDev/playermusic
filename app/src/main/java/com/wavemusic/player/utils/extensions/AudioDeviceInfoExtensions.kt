package com.wavemusic.player.utils.extensions

import android.media.AudioDeviceInfo

fun AudioDeviceInfo.isBluetoothOutputDevice(): Boolean {
    if (!isSink) return false
    return when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_HEARING_AID -> true
        else -> false
    }
}
