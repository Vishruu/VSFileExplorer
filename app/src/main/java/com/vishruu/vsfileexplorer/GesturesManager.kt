package com.vishruu.vsfileexplorer

import android.content.Context

object GesturesManager {
    private const val PREFS = "vs_gestures_prefs"
    private const val KEY_MASTER = "master"
    private const val KEY_PINCH = "pinch"
    private const val KEY_SWIPE_BACK = "swipe_back"
    private const val KEY_TWO_FINGER_SEARCH = "two_finger_search"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isMasterEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER, true)

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MASTER, enabled).apply()
    }

    fun isPinchEnabled(context: Context): Boolean =
        isMasterEnabled(context) && prefs(context).getBoolean(KEY_PINCH, true)

    fun setPinchEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PINCH, enabled).apply()
    }

    fun isSwipeBackEnabled(context: Context): Boolean =
        isMasterEnabled(context) && prefs(context).getBoolean(KEY_SWIPE_BACK, true)

    fun setSwipeBackEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SWIPE_BACK, enabled).apply()
    }

    fun isTwoFingerSearchEnabled(context: Context): Boolean =
        isMasterEnabled(context) && prefs(context).getBoolean(KEY_TWO_FINGER_SEARCH, true)

    fun setTwoFingerSearchEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TWO_FINGER_SEARCH, enabled).apply()
    }
}