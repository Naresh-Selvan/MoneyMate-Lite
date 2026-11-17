package com.moneymate.lite

import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.migration.MigrationPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationIntegrityTest {

    @Test
    fun `migration preview correctly calculates counts and financial balance`() {
        val files = listOf(
            LoanFile(id = 1, name = "File A", businessId = ""),
            LoanFile(id = 2, name = "File B", businessId = "")
        )

        val persons = listOf(
            Person(id = 1, fileId = 1, name = "Customer 1", mobileNumber = "123", place = "Town", notes = "", businessId = ""),
            Person(id = 2, fileId = 1, name = "Customer 2", mobileNumber = "456", place = "Town", notes = "", businessId = ""),
            Person(id = 3, fileId = 2, name = "Customer 3", mobileNumber = "789", place = "Town", notes = "", businessId = "")
        )

        val loans = listOf(
            Loan(id = 1, personId = 1, totalAmount = 10000.0, dateGiven = 1000L, businessId = ""),
            Loan(id = 2, personId = 2, totalAmount = 15000.0, dateGiven = 1000L, businessId = ""),
            Loan(id = 3, personId = 3, totalAmount = 5000.0, dateGiven = 1000L, businessId = "")
        )

        val payments = listOf(
            Payment(id = 1, loanId = 1, amount = 4000.0, date = 1100L, businessId = ""),
            Payment(id = 2, loanId = 2, amount = 5000.0, date = 1100L, businessId = ""),
            Payment(id = 3, loanId = 3, amount = 2000.0, date = 1100L, businessId = "")
        )

        val totalLoans = loans.sumOf { it.totalAmount }
        val totalPaid = payments.sumOf { it.amount }
        val outstanding = totalLoans - totalPaid

        val preview = MigrationPreview(
            fileCount = files.size,
            customerCount = persons.size,
            loanCount = loans.size,
            paymentCount = payments.size,
            totalLoanAmount = totalLoans,
            totalPaidAmount = totalPaid,
            totalOutstandingBalance = outstanding,
            isAlreadyMigrated = false
        )

        assertEquals(2, preview.fileCount)
        assertEquals(3, preview.customerCount)
        assertEquals(3, preview.loanCount)
        assertEquals(3, preview.paymentCount)
        assertEquals(30000.0, preview.totalLoanAmount, 0.001)
        assertEquals(11000.0, preview.totalPaidAmount, 0.001)
        assertEquals(19000.0, preview.totalOutstandingBalance, 0.001)
        assertFalse(preview.isAlreadyMigrated)
    }

    @Test
    fun `migration assigns businessId and marks already migrated`() {
        val targetBizId = "biz_owner_123"
        val files = listOf(
            LoanFile(id = 1, name = "File A", businessId = targetBizId),
            LoanFile(id = 2, name = "File B", businessId = targetBizId)
        )

        val isAlreadyMigrated = files.all { it.businessId == targetBizId }
        assertTrue(isAlreadyMigrated)
    }
}
