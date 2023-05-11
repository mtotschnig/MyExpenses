package org.totschnig.myexpenses

import android.content.Context
import org.totschnig.myexpenses.di.AppComponent

val Context.myApplication get() = applicationContext as MyApplication
val Context.injector: AppComponent get() = myApplication.appComponent