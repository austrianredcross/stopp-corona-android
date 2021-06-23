package at.roteskreuz.stopcorona

import android.app.Activity
import android.app.Application
import android.os.Bundle

object ActivityLifeCycleHelper {

    var currentResumedActivity : Activity? = null
        private set

    fun initWithApplication(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                currentResumedActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                currentResumedActivity = null
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle?) {
            }

            override fun onActivityDestroyed(activity: Activity) {

            }
        })
    }
}