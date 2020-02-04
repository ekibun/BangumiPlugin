package soko.ekibun.bangumi.plugins.util

import android.content.Context
import android.net.ConnectivityManager
import android.content.Context.CONNECTIVITY_SERVICE
import androidx.preference.PreferenceManager


object NetworkUtil {
    @Suppress("DEPRECATION")
    fun isWifiConnected(context: Context): Boolean {
        if(!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("check_network", true)) return true

        val mConnectivityManager = context
            .getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val mWiFiNetworkInfo = mConnectivityManager
            .getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (mWiFiNetworkInfo != null) {
            return mWiFiNetworkInfo.isConnected
        }
        return false
    }
}