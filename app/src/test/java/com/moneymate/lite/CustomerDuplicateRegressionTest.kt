package com.moneymate.lite

import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.sync.acceptsCloudVersion
import com.moneymate.lite.data.sync.findCustomerDuplicates
import com.moneymate.lite.data.sync.isRedundantCopyOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerDuplicateRegressionTest {
    private fun person(id: Long, fileId: Long = 1, sharedId: String = "customer-$id") = Person(
        id = id,
        sharedId = sharedId,
        businessId = "business",
        fileId = fileId,
        fileSharedId = "file-$fileId",
        name = "Customer AA",
        mobileNumber = null,
        place = null,
        notes = null,
        lastModified = 100
    )

    @Test
    fun findsTheEmptyCopyAcrossFilesWithoutTreatingANameAsIdentity() {
        val original = person(81).copy(sortOrder = 149)
        val extra = person(82).copy(sortOrder = 129)
        val otherOriginal = person(91, fileId = 2)
        val otherExtra = person(92, fileId = 2)
        val unrelated = person(93, fileId = 3)

        assertEquals(
            listOf(extra to original, otherExtra to otherOriginal),
            findCustomerDuplicates(
                listOf(extra, original, otherOriginal, otherExtra, unrelated),
                setOf(original.id, otherOriginal.id),
                "business"
            )
        )
        assertFalse(extra.isRedundantCopyOf(original))
    }

    @Test
    fun sameNameWithSeparateLoanHistoriesIsKept() {
        val first = person(81)
        val second = person(82)
        assertTrue(findCustomerDuplicates(listOf(first, second), setOf(first.id, second.id), "business").isEmpty())
    }

    @Test
    fun onlyAnIdenticalSharedIdentityCanBeAutomaticallyRemoved() {
        val original = person(81, sharedId = "known-id").copy(sortOrder = 149)
        val copy = original.copy(id = 82, sortOrder = 129)
        assertTrue(copy.isRedundantCopyOf(original))
        assertFalse(copy.copy(notes = "Unsaved note").isRedundantCopyOf(original))
        assertFalse(copy.copy(lastModified = 101).isRedundantCopyOf(original))
        assertFalse(copy.copy(fileSharedId = "different-file").isRedundantCopyOf(original))
    }

    @Test
    fun repeatSyncCannotReactivateAnArchivedCustomerWithAStaleOrEqualVersion() {
        val archived = person(82).copy(isDeleted = true)
        repeat(2) {
            assertFalse(archived.acceptsCloudVersion(99, deleted = false))
            assertFalse(archived.acceptsCloudVersion(100, deleted = false))
        }
        assertTrue(archived.acceptsCloudVersion(101, deleted = false))
    }
}
