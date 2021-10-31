package org.totschnig.myexpenses.test.room

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.room.ExchangeRate
import org.totschnig.myexpenses.room.ExchangeRateDao
import org.totschnig.myexpenses.room.ExchangeRateDatabase
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test the implementation of [ExchangeRateDao]
 */
@RunWith(AndroidJUnit4::class)
class ExchangeRateDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    lateinit var mDatabase: ExchangeRateDatabase

    lateinit var exchangeRateDao: ExchangeRateDao

    lateinit var localDate: LocalDate

    @Before
    @Throws(Exception::class)
    fun initDb() {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        mDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
                ExchangeRateDatabase::class.java)
                // allowing main thread queries, just for testing
                .allowMainThreadQueries()
                .build()

        exchangeRateDao = mDatabase.exchangeRateDao()

        localDate = LocalDate.now()
    }

    @After
    @Throws(Exception::class)
    fun closeDb() {
        mDatabase.close()
    }

    @Test
    @Throws(InterruptedException::class)
    fun getProductsWhenNoProductInserted() {
        runBlocking {
            assertNull(exchangeRateDao.getRate("EUR", "USD", localDate, "EXCHANGE_RATE_HOST"))
        }
    }

    @Test
    @Throws(InterruptedException::class)
    fun getProductsAfterInserted() {
        val exchangeRate = ExchangeRate("EUR", "USD", localDate, 1.444F, "EXCHANGE_RATE_HOST")
        runBlocking {
            exchangeRateDao.insert(exchangeRate)
            assertEquals(1.444F, exchangeRateDao.getRate("EUR", "USD", localDate, "EXCHANGE_RATE_HOST"))
        }
    }
}
