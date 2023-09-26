package org.totschnig.myexpenses.sync

import com.google.common.truth.Truth
import org.junit.Test
import java.io.InputStream

class ResourceStorageTest {

    open class Resource(val name: String)

    class Folder(val members: Set<Resource>, name: String) : Resource(name)

    class Storage(private val storage: Folder) : ResourceStorage<Resource> {
        override fun collectionForShard(shardNumber: Int) =
            if (shardNumber == 0) storage else storage.members.find {
                it.name == folderForShard(
                    shardNumber
                )
            }

        override fun getCollection(collectionName: String, require: Boolean): Resource? {
            TODO("Not yet implemented")
        }

        override fun getInputStream(resource: Resource): InputStream {
            TODO("Not yet implemented")
        }

        override fun childrenForCollection(folder: Resource?): Collection<Resource> =
            ((folder ?: storage) as Folder).members

        override fun nameForResource(resource: Resource) = resource.name

        override fun isCollection(resource: Resource) = resource is Folder

        override val extensionForData: String
            get() = "txt"
    }

    @Test
    fun testEmptyStorage() {
        val storage = Storage(Folder(emptySet(), "ROOT"))
        val start = SequenceNumber(0, 0)
        Truth.assertThat(storage.getLastSequence(start)).isEqualTo(SequenceNumber(0,0))
        Truth.assertThat(storage.shardResolvingFilterStrategy(start)).isEmpty()
    }

    @Test
    fun testStorageWithoutShards() {
        val one = Resource("_1.txt")
        val two = Resource("_2.txt")
        val storage = Storage(Folder(setOf(one, two), "ROOT"))
        val start = SequenceNumber(0, 0)
        Truth.assertThat(storage.getLastSequence(start)).isEqualTo(SequenceNumber(0,2))
        Truth.assertThat(storage.shardResolvingFilterStrategy(start)).containsExactly(0 to one, 0 to two ).inOrder()
    }

    @Test
    fun testStorageWithShards() {
        val one = Resource("_1.txt")
        val two = Resource("_2.txt")
        val storage = Storage(
            Folder(
                setOf(
                    one,
                    two,
                    Folder(setOf(one, two), "_1")
                ),
                "ROOT"
            )
        )
        val start = SequenceNumber(0, 0)
        Truth.assertThat(storage.getLastSequence(start)).isEqualTo(SequenceNumber(1,2))
        Truth.assertThat(storage.shardResolvingFilterStrategy(start))
            .containsExactly(0 to one, 0 to two, 1 to one, 1 to two )
            .inOrder()
    }
}