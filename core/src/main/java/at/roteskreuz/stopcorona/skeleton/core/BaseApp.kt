package at.roteskreuz.stopcorona.skeleton.core

import android.app.Application
import android.os.StrictMode
import at.roteskreuz.stopcorona.skeleton.core.di.scopeModule
import com.jakewharton.threetenabp.AndroidThreeTen
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.Module

/**
 * Core application class.
 */
open class BaseApp : Application() {

    /**
     * If you want to blinking of screen in inappropriate situations you have to set up in your phone:
     * Settings -> Dev -> Strict mode
     */
    open val strictModeEnabled = false
    /**
     * If you want to crash the application when strict mode caught something.
     */
    open val strictModePenaltyDeath = false
    /**
     * Which modules uses the application.
     */
    protected open val koinModules = listOf<Module>()

    override fun onCreate() {
        onPreCreate()
        super.onCreate()
        onPostCreate()
    }

    /**
     * This method is automatically called before creating of Application instance.
     * WARNING: There is no instance of context.
     */
    protected open fun onPreCreate() {
        if (strictModeEnabled) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .let { builder ->
                        if (strictModePenaltyDeath) builder.penaltyDeath()
                        else builder
                    }
                    .build()
            )
        }
    }

    /**
     * This method is automatically called after creating of Application instance.
     */
    protected open fun onPostCreate() {
        // time initializer
        AndroidThreeTen.init(this)

        // DI
        startKoin(
            this,
            listOf(
                scopeModule
            ) + koinModules
        )
    }
}