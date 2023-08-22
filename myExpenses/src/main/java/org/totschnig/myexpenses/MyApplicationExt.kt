package org.totschnig.myexpenses

import android.content.Context
import androidx.fragment.app.Fragment
import org.totschnig.myexpenses.di.AppComponent

val Context.myApplication get() = applicationContext as MyApplication
val Context.injector: AppComponent get() = myApplication.appComponent

val Fragment.injector get() = requireActivity().injector