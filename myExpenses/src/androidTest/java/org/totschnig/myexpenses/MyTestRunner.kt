package org.totschnig.myexpenses

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import androidx.test.runner.AndroidJUnitRunner

@Suppress("unused")
class MyTestRunner : AndroidJUnitRunner() {

    @Throws(
        ClassNotFoundException::class,
        IllegalAccessException::class,
        InstantiationException::class
    )
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, TestApp::class.java.name, context)
    }

    @SuppressLint("NewApi")
    override fun onStart() {
        if (!ANIMATION_SETTINGS_MANUALLY_CHECKED) {
            val animationSettings = arrayOf(
                Settings.Global.TRANSITION_ANIMATION_SCALE,
                Settings.Global.WINDOW_ANIMATION_SCALE,
                Settings.Global.ANIMATOR_DURATION_SCALE
            )
            for (setting in animationSettings) {
                check(
                    try {
                        settingGlobalFloat(setting)
                    } catch (e: SettingNotFoundException) {
                        settingSystemFloat(setting)
                    } == 0F
                ) { "$setting  must be disabled for reliable Espresso tests" }
            }
            //Espresso cannot work with this setting
            check(
                Settings.Global.getInt(
                    targetContext.contentResolver,
                    Settings.Global.ALWAYS_FINISH_ACTIVITIES,
                    0
                ) == 0
            )  { "${Settings.Global.ALWAYS_FINISH_ACTIVITIES}  must be disabled for reliable Espresso tests" }
        }
        super.onStart()
    }

    @Throws(SettingNotFoundException::class)
    private fun settingGlobalFloat(setting: String?): Float {
        return Settings.Global.getFloat(targetContext.contentResolver, setting)
    }

    @Throws(SettingNotFoundException::class)
    private fun settingSystemFloat(setting: String?): Float {
        return Settings.System.getFloat(targetContext.contentResolver, setting)
    }

    companion object {
        private const val ANIMATION_SETTINGS_MANUALLY_CHECKED = false
    }
}