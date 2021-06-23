package at.roteskreuz.stopcorona

import com.huawei.agconnect.crash.AGConnectCrash

fun App.onPostCreatePlatformDependent() {
    AGConnectCrash.getInstance().enableCrashCollection(!BuildConfig.DEBUG)
}