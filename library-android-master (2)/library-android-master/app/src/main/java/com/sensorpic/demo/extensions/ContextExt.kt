package com.sensorpic.demo.extensions

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import timber.log.Timber


fun Context.getVersionName() : String {
    return try {
        val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        pInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e)
        "Unknown version"
    }
}
