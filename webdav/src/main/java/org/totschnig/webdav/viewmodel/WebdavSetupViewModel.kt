package org.totschnig.webdav.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.totschnig.myexpenses.MyApplication
import org.totschnig.webdav.sync.client.WebDavClient
import java.security.cert.X509Certificate

class WebdavSetupViewModel(application: Application) : AndroidViewModel(application) {
    private val _result: MutableLiveData<Result<Unit>> = MutableLiveData()

    val result: LiveData<Result<Unit>> = _result

    fun testLogin(
        url: String,
        userName: String,
        passWord: String,
        certificate: X509Certificate?,
        allowUnverifiedHost: Boolean
    ) {
        viewModelScope.launch(context = Dispatchers.IO) {
            _result.postValue(kotlin.runCatching {
                WebDavClient(getApplication<MyApplication>().appComponent, url, userName, passWord, certificate, allowUnverifiedHost).run {
                    testLogin()
                    testClass2Locking()
                }
            })
        }
    }
}