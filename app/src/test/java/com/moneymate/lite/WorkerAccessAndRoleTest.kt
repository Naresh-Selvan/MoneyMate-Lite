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
            email = "john.biz123@moneymate.com",
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
            email = "jane.biz123@moneymate.com",
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
            email = "badactor.biz123@moneymate.com",
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
            email = "newbie.biz123@moneymate.com",
            accessAllFiles = false,
            allowedFileIds = listOf("shared_1"),
            mustChangePassword = true
        )

        assertTrue(newWorker.mustChangePassword)
    }

    @Test
    fun `default worker can record payments but has destructive privileges locked`() {
        val worker = UserProfile(
            uid = "worker_default",
            businessId = "biz_123",
            role = UserRole.WORKER,
            displayName = "Field Collector",
            username = "collector"
        )

        // Default security policies
        assertTrue("Workers should be allowed to record collections by default", worker.canRecordPayments)
        assertFalse("Workers must NOT be allowed to directly edit loans without approval", worker.canEditLoansDirectly)
        assertFalse("Workers must NOT be allowed to directly create loans without approval", worker.canCreateLoansDirectly)
        assertFalse("Workers must NOT be allowed to delete records", worker.canDeleteRecords)
    }

    @Test
    fun `trusted worker with elevated privileges has direct edit enabled`() {
        val trustedManager = UserProfile(
            uid = "worker_senior",
            businessId = "biz_123",
            role = UserRole.WORKER,
            displayName = "Senior Manager",
            username = "seniormgr",
            canRecordPayments = true,
            canCreateLoansDirectly = true,
            canEditLoansDirectly = true,
            canDeleteRecords = false
        )

        assertTrue(trustedManager.canRecordPayments)
        assertTrue(trustedManager.canCreateLoansDirectly)
        assertTrue(trustedManager.canEditLoansDirectly)
        assertFalse("Delete records remains restricted", trustedManager.canDeleteRecords)
    }
}
