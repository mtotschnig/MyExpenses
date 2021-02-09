package org.totschnig.myexpenses

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import androidx.test.runner.AndroidJUnitRunner
import org.totschnig.myexpenses.util.Utils

@Suppress("unused")
class MyTestRunner : AndroidJUnitRunner() {

    @Throws(ClassNotFoundException::class, IllegalAccessException::class, InstantiationException::class)
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, TestApp::class.java.name, context)
    }

    @SuppressLint("NewApi")
    override fun onStart() {
        if (!ANIMATION_SETTINGS_MANUALLY_CHECKED) {
            check(Utils.hasApiLevel(Build.VERSION_CODES.JELLY_BEAN_MR1))
            val animationSettings = arrayOf(
                    Settings.Global.TRANSITION_ANIMATION_SCALE,
                    Settings.Global.WINDOW_ANIMATION_SCALE,
                    Settings.Global.ANIMATOR_DURATION_SCALE)
            for (setting in animationSettings) {
                check(try {
                    settingGlobalFloat(setting)
                } catch (e: SettingNotFoundException) {
                    settingSystemFloat(setting)
                } == 0F)
            }
            //Espresso cannot work with this setting
            check(Settings.Global.getInt(targetContext.contentResolver, Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 0)
        }
        super.onStart()
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Throws(SettingNotFoundException::class)
    private fun settingGlobalFloat(setting: String?): Float {
        return Settings.Global.getFloat(targetContext.contentResolver, setting)
    }

    @Throws(SettingNotFoundException::class)
    private fun settingSystemFloat(setting: String?): Float {
        return Settings.System.getFloat(targetContext.contentResolver, setting)
    }

    override fun finish(resultCode: Int, results: Bundle) {
        MyApplication.cleanUpAfterTest()
        super.finish(resultCode, results)
    }

    init {
        // Inform the app we are an instrumentation test before the object graph is initialized.
        Log.d("instrumentationTest", "now setting instrumentationTest to true")
        MyApplication.setInstrumentationTest(true)
    }

    companion object {
        private const val ANIMATION_SETTINGS_MANUALLY_CHECKED = false
    }
}