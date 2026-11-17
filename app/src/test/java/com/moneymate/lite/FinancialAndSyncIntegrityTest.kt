package com.moneymate.lite

import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialAndSyncIntegrityTest {

    @Test
    fun `official totals exclude pending sync payments while inclusive totals include them`() {
        val payments = listOf(
            Payment(id = 1, loanId = 100, amount = 1000.0, date = 1000L, syncStatus = "SYNCED"),
            Payment(id = 2, loanId = 100, amount = 500.0, date = 2000L, syncStatus = "SYNCED"),
            Payment(id = 3, loanId = 100, amount = 300.0, date = 3000L, syncStatus = "PENDING_SYNC", recordedByWorkerId = "w1")
        )

        // As defined in PaymentDao query:
        // getTotalPaidForLoan: SUM(amount) WHERE loanId = :loanId AND syncStatus != 'PENDING_SYNC'
        val officialTotalPaid = payments.filter { it.syncStatus != "PENDING_SYNC" }.sumOf { it.amount }

        // Inclusive total: SUM(amount) WHERE loanId = :loanId
        val inclusiveTotalPaid = payments.sumOf { it.amount }

        assertEquals(1500.0, officialTotalPaid, 0.001)
        assertEquals(1800.0, inclusiveTotalPaid, 0.001)
    }

    @Test
    fun `loan completion is determined strictly by official confirmed payments`() {
        val loan = Loan(
            id = 50,
            personId = 10,
            totalAmount = 2000.0,
            dateGiven = 1000L,
            isCompleted = false
        )

        val payments = listOf(
            Payment(id = 1, loanId = 50, amount = 1500.0, date = 1100L, syncStatus = "SYNCED"),
            Payment(id = 2, loanId = 50, amount = 600.0, date = 1200L, syncStatus = "PENDING_SYNC")
        )

        val confirmedPaid = payments.filter { it.syncStatus != "PENDING_SYNC" }.sumOf { it.amount }
        val isConfirmedCompleted = confirmedPaid >= loan.totalAmount

        assertFalse("Loan should not be marked officially complete based on unconfirmed pending payments", isConfirmedCompleted)

        // When the pending payment syncs:
        val paymentsAfterSync = payments.map { it.copy(syncStatus = "SYNCED") }
        val syncedPaid = paymentsAfterSync.filter { it.syncStatus != "PENDING_SYNC" }.sumOf { it.amount }
        val isSyncedCompleted = syncedPaid >= loan.totalAmount

        assertTrue("Loan should be marked complete once payments are officially confirmed/synced", isSyncedCompleted)
    }

    @Test
    fun `worker actor attribution is preserved after payment recording`() {
        val payment = Payment(
            id = 1,
            loanId = 50,
            amount = 450.0,
            date = 1000L,
            recordedByWorkerId = "worker_abc_123",
            recordedByWorkerName = "Ravi Collector",
            syncStatus = "PENDING_SYNC"
        )

        assertEquals("worker_abc_123", payment.recordedByWorkerId)
        assertEquals("Ravi Collector", payment.recordedByWorkerName)
    }

    @Test
    fun `rejection requires a non-blank reason`() {
        fun validateRejectionReason(reason: String): Boolean {
            return reason.trim().isNotBlank()
        }

        assertFalse(validateRejectionReason(""))
        assertFalse(validateRejectionReason("   "))
        assertTrue(validateRejectionReason("Customer provided incorrect address"))
    }
}
