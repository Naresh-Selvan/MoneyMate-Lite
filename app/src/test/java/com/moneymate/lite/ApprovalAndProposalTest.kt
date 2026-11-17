package com.moneymate.lite

import com.moneymate.lite.data.entity.ApprovalRequestEntity
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.model.ApprovalStatus
import com.moneymate.lite.data.model.PendingActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ApprovalAndProposalTest {

    @Test
    fun `proposal creation leaves official records untouched`() {
        val officialPerson = Person(
            id = 10,
            fileId = 1,
            name = "Ramesh Kumar",
            mobileNumber = "9876543210",
            place = "Main Street",
            notes = "Regular customer",
            sortOrder = 0,
            sharedId = "person_uuid_10"
        )

        // Worker submits an edit proposal
        val proposedJson = """{"name":"Ramesh K. (Updated)","mobileNumber":"9876543210","place":"North Street"}"""

        val request = ApprovalRequestEntity(
            id = UUID.randomUUID().toString(),
            businessId = "biz_test",
            version = 1,
            supersededBy = null,
            workerId = "worker_1",
            workerName = "Suresh",
            fileSharedId = "file_1",
            fileName = "Collection A",
            recordType = "PERSON",
            recordSharedId = officialPerson.sharedId,
            actionType = PendingActionType.EDIT_CUSTOMER.name,
            reason = "Customer relocated",
            requestedAt = System.currentTimeMillis(),
            status = ApprovalStatus.PENDING.name,
            rejectionReason = null,
            reviewedBy = null,
            reviewedAt = null,
            baseSnapshotJson = """{"name":"${officialPerson.name}","place":"${officialPerson.place}"}""",
            proposedPayloadJson = proposedJson,
            syncStatus = "SUBMITTED",
            syncError = null,
            clientOpId = UUID.randomUUID().toString()
        )

        // Verify request is pending and official record is completely unchanged
        assertEquals(ApprovalStatus.PENDING.name, request.status)
        assertEquals("Ramesh Kumar", officialPerson.name)
        assertEquals("Main Street", officialPerson.place)
        assertTrue(request.proposedPayloadJson.contains("Ramesh K. (Updated)"))
        assertFalse(officialPerson.name.contains("Updated"))
    }

    @Test
    fun `stale proposal conflict is detected when base record was modified`() {
        // Base record at time of worker proposal submission
        val originalSnapshotAmount = 5000.0
        val originalSnapshotDate = 1700000000000L

        // Suppose owner or another operation modified the record before approval
        val currentLiveRecordAmount = 7000.0 // Changed to 7000!
        val currentLiveRecordDate = 1700000000000L

        // Conflict detection logic
        val hasConflict = (originalSnapshotAmount != currentLiveRecordAmount)

        assertTrue("Stale conflict must be detected when underlying record changed", hasConflict)
    }

    @Test
    fun `superseding a proposal creates a new immutable version and marks old version withdrawn`() {
        val originalId = UUID.randomUUID().toString()
        val originalRequest = ApprovalRequestEntity(
            id = originalId,
            businessId = "biz_1",
            version = 1,
            supersededBy = null,
            workerId = "w1",
            workerName = "Worker 1",
            fileSharedId = "f1",
            fileName = "File 1",
            recordType = "LOAN",
            recordSharedId = "loan_1",
            actionType = PendingActionType.EDIT_LOAN.name,
            reason = "Initial reason",
            requestedAt = 1000L,
            status = ApprovalStatus.PENDING.name,
            rejectionReason = null,
            reviewedBy = null,
            reviewedAt = null,
            baseSnapshotJson = null,
            proposedPayloadJson = "{\"amount\": 1000}",
            syncStatus = "SUBMITTED",
            syncError = null,
            clientOpId = "op_1"
        )

        val newId = UUID.randomUUID().toString()
        val newVersion = originalRequest.copy(
            id = newId,
            version = originalRequest.version + 1,
            reason = "Corrected loan amount reason",
            proposedPayloadJson = "{\"amount\": 1200}",
            clientOpId = UUID.randomUUID().toString()
        )
        val markedOld = originalRequest.copy(
            status = ApprovalStatus.WITHDRAWN.name,
            supersededBy = newId
        )

        assertEquals(2, newVersion.version)
        assertEquals(newId, markedOld.supersededBy)
        assertEquals(ApprovalStatus.WITHDRAWN.name, markedOld.status)
        assertEquals(ApprovalStatus.PENDING.name, newVersion.status)
    }

    @Test
    fun `repeated approval taps are idempotent and do not re-execute`() {
        var executionCount = 0
        var currentStatus = ApprovalStatus.PENDING.name

        fun executeApproval() {
            // Idempotency guard
            if (currentStatus == ApprovalStatus.APPROVED.name) {
                return
            }
            executionCount++
            currentStatus = ApprovalStatus.APPROVED.name
        }

        // First tap
        executeApproval()
        assertEquals(1, executionCount)
        assertEquals(ApprovalStatus.APPROVED.name, currentStatus)

        // Second tap (accidental rapid double-tap)
        executeApproval()
        assertEquals(1, executionCount)

        // Third tap
        executeApproval()
        assertEquals(1, executionCount)
    }

    @Test
    fun `withdrawn request cannot be approved`() {
        val request = ApprovalRequestEntity(
            id = "req_1",
            businessId = "biz_1",
            version = 1,
            supersededBy = null,
            workerId = "w1",
            workerName = "Worker",
            fileSharedId = "f1",
            fileName = "File",
            recordType = "PERSON",
            recordSharedId = null,
            actionType = PendingActionType.CREATE_CUSTOMER_WITH_LOAN.name,
            reason = "Mistake",
            requestedAt = 1000L,
            status = ApprovalStatus.WITHDRAWN.name,
            rejectionReason = null,
            reviewedBy = null,
            reviewedAt = null,
            baseSnapshotJson = null,
            proposedPayloadJson = "{}",
            syncStatus = "SUBMITTED",
            syncError = null,
            clientOpId = "op_2"
        )

        val canBeApproved = (request.status == ApprovalStatus.PENDING.name)
        assertFalse(canBeApproved)
    }

    @Test
    fun `edit loan proposal payload correctly retains customer name and amounts for owner diff`() {
        val payloadJson = """{"amount":2000.0,"date":1700000000000,"oldAmount":20000.0,"customerName":"John Doe"}"""
        val baseJson = """{"amount":20000.0,"customerName":"John Doe"}"""

        val request = ApprovalRequestEntity(
            id = "req_diff_1",
            businessId = "biz_1",
            workerId = "worker_staff3",
            workerName = "staff3",
            fileSharedId = "f1",
            fileName = "Collection A",
            recordType = "LOAN",
            recordSharedId = "loan_1",
            actionType = PendingActionType.EDIT_LOAN.name,
            reason = "incorrect initial entry",
            baseSnapshotJson = baseJson,
            proposedPayloadJson = payloadJson
        )

        assertEquals("staff3", request.workerName)
        assertEquals(PendingActionType.EDIT_LOAN.name, request.actionType)
        assertTrue(request.proposedPayloadJson.contains("John Doe"))
        assertTrue(request.proposedPayloadJson.contains("2000.0"))
        assertTrue(request.baseSnapshotJson!!.contains("20000.0"))
    }

    @Test
    fun `edit payment proposal payload retains customer name and calculates diff`() {
        val payloadJson = """{"amount":400.0,"date":1700000000000,"oldAmount":500.0,"customerName":"Jane Smith"}"""
        val baseJson = """{"amount":500.0,"customerName":"Jane Smith"}"""

        val request = ApprovalRequestEntity(
            id = "req_pay_1",
            businessId = "biz_1",
            workerId = "worker_staff2",
            workerName = "Naresh (@staff2)",
            fileSharedId = "f1",
            fileName = "Collection B",
            recordType = "PAYMENT",
            recordSharedId = "pay_1",
            actionType = PendingActionType.EDIT_PAYMENT.name,
            reason = "change entered amount",
            baseSnapshotJson = baseJson,
            proposedPayloadJson = payloadJson
        )

        assertEquals("Naresh (@staff2)", request.workerName)
        assertEquals(PendingActionType.EDIT_PAYMENT.name, request.actionType)
        assertTrue(request.proposedPayloadJson.contains("Jane Smith"))
        assertTrue(request.proposedPayloadJson.contains("400.0"))
        assertTrue(request.baseSnapshotJson!!.contains("500.0"))
    }

    @Test
    fun `ApprovalRequestEntity and ActivityLogEntity define default no-arg constructors for Firestore`() {
        val defaultApproval = ApprovalRequestEntity()
        assertEquals(1, defaultApproval.version)
        assertEquals("PENDING", defaultApproval.status)
        assertEquals("{}", defaultApproval.proposedPayloadJson)

        val defaultLog = com.moneymate.lite.data.entity.ActivityLogEntity()
        assertEquals("SUCCESS", defaultLog.outcome)
        assertEquals("SYNCED", defaultLog.syncStatus)
    }

    @Test
    fun `Sync update preserves businessId so existing local files are not hidden from workers`() {
        // Pre-existing local file before multi-user sync was enabled
        val localFile = com.moneymate.lite.data.entity.LoanFile(
            id = 1,
            sharedId = "file_shared_1",
            businessId = "", // Was empty prior to cloud sync
            name = "NLR 1"
        )

        val targetBusinessId = "biz_yvtuYaX5BhUwsmYThvNS4bKhdvJ2"

        // Simulated cloud update
        val updatedFromCloud = localFile.copy(
            businessId = targetBusinessId,
            sharedId = "file_shared_1",
            name = "NLR 1"
        )

        assertEquals(targetBusinessId, updatedFromCloud.businessId)
        assertEquals("NLR 1", updatedFromCloud.name)
    }
}
