package org.totschnig.myexpenses.testutils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.di.CoroutineModule

object TestCoroutineModule: CoroutineModule() {
    /*val executor: IdlingThreadPoolExecutor = IdlingThreadPoolExecutor("Coroutine Idling Ressource",
            1,
            1,
            0,
            TimeUnit.MICROSECONDS,
            ArrayBlockingQueue(20),
            Executors.defaultThreadFactory())*/
    override fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}