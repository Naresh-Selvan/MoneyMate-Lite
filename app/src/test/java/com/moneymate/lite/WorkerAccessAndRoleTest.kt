package com.moneymate.lite

import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.model.UserProfile
import com.moneymate.lite.data.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkerAccessAndRoleTest {

    @Test
    fun `worker with accessAllFiles sees all files`() {
        val files = listOf(
            LoanFile(id = 1, name = "File A", sharedId = "shared_1"),
            LoanFile(id = 2, name = "File B", sharedId = "shared_2"),
            LoanFile(id = 3, name = "File C", sharedId = "shared_3")
        )

        val profile = UserProfile(
            uid = "worker_1",
            businessId = "biz_123",
            role = UserRole.WORKER,
            displayName = "John Worker",
            username = "john",
            email = "john@biz_123.moneymate.internal",
            accessAllFiles = true,
            allowedFileIds = emptyList()
        )

        val visibleFiles = if (profile.role == UserRole.WORKER && !profile.accessAllFiles) {
            files.filter { it.sharedId in profile.allowedFileIds }
        } else {
            files
        }

        assertEquals(3, visibleFiles.size)
    }

    @Test
    fun `worker with restricted access sees only allowed files`() {
        val files = listOf(
            LoanFile(id = 1, name = "File A", sharedId = "shared_1"),
            LoanFile(id = 2, name = "File B", sharedId = "shared_2"),
            LoanFile(id = 3, name = "File C", sharedId = "shared_3")
        )

        val profile = UserProfile(
            uid = "worker_2",
            businessId = "biz_123",
            role = UserRole.WORKER,
            displayName = "Jane Worker",
            username = "jane",
            email = "jane@biz_123.moneymate.internal",
            accessAllFiles = false,
            allowedFileIds = listOf("shared_2")
        )

        val visibleFiles = if (profile.role == UserRole.WORKER && !profile.accessAllFiles) {
            files.filter { it.sharedId in profile.allowedFileIds }
        } else {
            files
        }

        assertEquals(1, visibleFiles.size)
        assertEquals("File B", visibleFiles[0].name)
        assertEquals("shared_2", visibleFiles[0].sharedId)
    }

    @Test
    fun `owner always sees all files regardless of allowedFileIds list`() {
        val files = listOf(
            LoanFile(id = 1, name = "File A", sharedId = "shared_1"),
            LoanFile(id = 2, name = "File B", sharedId = "shared_2")
        )

        val ownerProfile = UserProfile(
            uid = "owner_uid",
            businessId = "biz_owner",
            role = UserRole.OWNER,
            displayName = "Business Owner",
            username = "owner",
            email = "owner@gmail.com",
            accessAllFiles = true,
            allowedFileIds = emptyList()
        )

        val visibleFiles = if (ownerProfile.role == UserRole.WORKER && !ownerProfile.accessAllFiles) {
            files.filter { it.sharedId in ownerProfile.allowedFileIds }
        } else {
            files
        }

        assertEquals(2, visibleFiles.size)
    }

    @Test
    fun `disabled worker cannot be considered active`() {
        val disabledWorker = UserProfile(
            uid = "worker_disabled",
            businessId = "biz_123",
            role = UserRole.WORKER,
            displayName = "Disabled Worker",
            username = "badactor",
            email = "badactor@biz_123.moneymate.internal",
            accessAllFiles = true,
            allowedFileIds = emptyList(),
            isActive = false
        )

        assertFalse(disabledWorker.isActive)
    }

    @Test
    fun `new worker requires password change on first login`() {
        val newWorker = UserProfile(
            uid = "worker_new",
            businessId = "biz_123",
            role = UserRole.WORKER,
            displayName = "New Worker",
            username = "newbie",
            email = "newbie@biz_123.moneymate.internal",
            accessAllFiles = false,
            allowedFileIds = listOf("shared_1"),
            mustChangePassword = true
        )

        assertTrue(newWorker.mustChangePassword)
    }
}
