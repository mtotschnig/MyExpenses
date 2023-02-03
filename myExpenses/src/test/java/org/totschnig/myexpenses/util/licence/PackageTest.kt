package org.totschnig.myexpenses.util.licence

import com.google.common.truth.Truth
import org.junit.Assert.*

import org.junit.Test

class PackageTest {

    @Test
    fun getAllAddOnPackages() {
        Truth.assertThat(AddOnPackage.values).containsExactlyElementsIn(
            AddOnPackage::class.sealedSubclasses.map { it.objectInstance }
        )
    }
}