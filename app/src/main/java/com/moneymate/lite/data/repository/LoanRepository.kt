package com.moneymate.lite.data.repository

import androidx.room.withTransaction
import com.moneymate.lite.data.AppDatabase
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanWithBalance
import com.moneymate.lite.data.dao.LoanWithBalanceAndFile
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.DateTransactionEntity
import com.moneymate.lite.data.dao.PersonBalanceUpToDate
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val database: AppDatabase,
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao
) {
    suspend fun insertLoan(loan: Loan): Long = withContext(Dispatchers.IO) {
        loanDao.insert(loan)
    }

    suspend fun createLoan(personId: Long, totalAmount: Double, dateGiven: Long): Long = withContext(Dispatchers.IO) {
        val activeLoan = loanDao.getActiveLoanByPerson(personId)
        if (activeLoan != null) {
            throw IllegalStateException("Person already has an active loan")
        }
        val person = database.personDao().getPersonById(personId)
        val bizId = person?.businessId ?: ""
        val pSharedId = person?.sharedId ?: "person_$personId"
        loanDao.insert(
            Loan(
                sharedId = "loan_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}",
                businessId = bizId,
                personId = personId,
                personSharedId = pSharedId,
                totalAmount = totalAmount,
                dateGiven = dateGiven,
                createdAt = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis(),
                syncStatus = "PENDING_SYNC"
            )
        )
    }

    suspend fun recordPayment(loanId: Long, amount: Double, date: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId)
            val bizId = loan?.businessId ?: ""
            val lSharedId = loan?.sharedId ?: "loan_$loanId"
            paymentDao.insert(
                Payment(
                    sharedId = "payment_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}",
                    businessId = bizId,
                    loanId = loanId,
                    loanSharedId = lSharedId,
                    amount = amount,
                    date = date,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    syncStatus = "PENDING_SYNC"
                )
            )
            if (loan != null) {
                val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
                if (totalPaid >= loan.totalAmount) {
                    loanDao.markCompleted(loanId, System.currentTimeMillis())
                }
            }
        }
    }

    suspend fun recordWorkerPayment(
        loanId: Long,
        amount: Double,
        date: Long,
        workerId: String,
        workerName: String,
        syncStatus: String = "PENDING_SYNC"
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId)
            val bizId = loan?.businessId ?: ""
            val lSharedId = loan?.sharedId ?: "loan_$loanId"
            val paymentId = paymentDao.insert(
                Payment(
                    sharedId = "payment_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}",
                    businessId = bizId,
                    loanId = loanId,
                    loanSharedId = lSharedId,
                    amount = amount,
                    date = date,
                    recordedByWorkerId = workerId,
                    recordedByWorkerName = workerName,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    syncStatus = syncStatus
                )
            )
            if (loan != null) {
                val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
                if (totalPaid >= loan.totalAmount) {
                    loanDao.markCompleted(loanId, System.currentTimeMillis())
                }
            }
            paymentId
        }
    }

    fun getLoansByPerson(personId: Long): Flow<List<Loan>> = loanDao.getLoansByPerson(personId)

    suspend fun getActiveLoanByPerson(personId: Long): Loan? = withContext(Dispatchers.IO) {
        loanDao.getActiveLoanByPerson(personId)
    }

    fun getActiveLoanByPersonFlow(personId: Long): Flow<Loan?> = loanDao.getActiveLoanByPersonFlow(personId)

    fun getAllActiveLoansInFile(fileId: Long): Flow<List<LoanWithBalance>> = loanDao.getAllActiveLoansInFile(fileId)

    fun getAllActiveLoansAcrossFiles(): Flow<List<LoanWithBalanceAndFile>> = loanDao.getAllActiveLoansAcrossFiles()

    fun getPaymentsByLoan(loanId: Long): Flow<List<Payment>> = paymentDao.getPaymentsByLoan(loanId)

    suspend fun getTodayCollectionTotal(todayStart: Long, todayEnd: Long): Double = withContext(Dispatchers.IO) {
        paymentDao.getTodayCollectionTotal(todayStart, todayEnd)
    }

    suspend fun getTotalOutstandingBalance(): Double = withContext(Dispatchers.IO) {
        paymentDao.getTotalOutstandingBalance()
    }

    suspend fun softDeletePayment(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val payment = paymentDao.getPaymentById(id) ?: return@withTransaction
            paymentDao.softDelete(id)
            val loanId = payment.loanId
            val loan = loanDao.getLoanById(loanId)
            if (loan != null) {
                val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
                if (totalPaid < loan.totalAmount) {
                    if (loan.isCompleted) {
                        loanDao.markIncomplete(loanId)
                    }
                }
            }
        }
    }

    suspend fun restorePayment(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val payment = paymentDao.getPaymentById(id) ?: return@withTransaction
            paymentDao.restorePayment(id)
            val loanId = payment.loanId
            val loan = loanDao.getLoanById(loanId)
            if (loan != null) {
                val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
                if (totalPaid >= loan.totalAmount) {
                    if (!loan.isCompleted) {
                        loanDao.markCompleted(loanId, System.currentTimeMillis())
                    }
                }
            }
        }
    }

    fun getDeletedPaymentsByFile(fileId: Long): Flow<List<com.moneymate.lite.data.dao.DeletedPaymentWithPerson>> =
        paymentDao.getDeletedPaymentsByFile(fileId)

    fun getLoansGivenOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>> =
        loanDao.getLoansGivenOnDate(fileId, startOfDay, endOfDay)

    fun getPaymentsReceivedOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>> =
        paymentDao.getPaymentsReceivedOnDate(fileId, startOfDay, endOfDay)

    fun getBalancesUpToDate(fileId: Long, upToDate: Long): Flow<List<PersonBalanceUpToDate>> =
        loanDao.getBalancesUpToDate(fileId, upToDate)

    suspend fun addGivenTransaction(personId: Long, amount: Double, date: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val activeLoan = loanDao.getActiveLoanByPerson(personId)
            val person = database.personDao().getPersonById(personId)
            val bizId = person?.businessId ?: ""
            val pSharedId = person?.sharedId ?: "person_$personId"

            if (activeLoan != null) {
                val updatedLoan = activeLoan.copy(
                    totalAmount = activeLoan.totalAmount + amount,
                    dateGiven = date,
                    businessId = if (activeLoan.businessId.isBlank()) bizId else activeLoan.businessId,
                    personSharedId = if (activeLoan.personSharedId.isBlank()) pSharedId else activeLoan.personSharedId,
                    lastModified = System.currentTimeMillis(),
                    syncStatus = "PENDING_SYNC"
                )
                loanDao.update(updatedLoan)
                
                val totalPaid = paymentDao.getTotalPaidForLoan(activeLoan.id)
                if (totalPaid >= updatedLoan.totalAmount) {
                    if (!activeLoan.isCompleted) {
                        loanDao.markCompleted(activeLoan.id, System.currentTimeMillis())
                    }
                } else {
                    if (activeLoan.isCompleted) {
                        loanDao.markIncomplete(activeLoan.id)
                    }
                }
            } else {
                loanDao.insert(
                    Loan(
                        sharedId = "loan_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}",
                        businessId = bizId,
                        personId = personId,
                        personSharedId = pSharedId,
                        totalAmount = amount,
                        dateGiven = date,
                        createdAt = System.currentTimeMillis(),
                        lastModified = System.currentTimeMillis(),
                        syncStatus = "PENDING_SYNC"
                    )
                )
            }
        }
    }

    suspend fun addReceivedTransaction(personId: Long, amount: Double, date: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val activeLoan = loanDao.getActiveLoanByPerson(personId)
                ?: throw IllegalStateException("No active loan found for this customer.")
            val person = database.personDao().getPersonById(personId)
            val bizId = activeLoan.businessId.ifBlank { person?.businessId ?: "" }
            val lSharedId = activeLoan.sharedId.ifBlank { "loan_${activeLoan.id}" }
            
            paymentDao.insert(
                Payment(
                    sharedId = "payment_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}",
                    businessId = bizId,
                    loanId = activeLoan.id,
                    loanSharedId = lSharedId,
                    amount = amount,
                    date = date,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis(),
                    syncStatus = "PENDING_SYNC"
                )
            )
            
            val totalPaid = paymentDao.getTotalPaidForLoan(activeLoan.id)
            if (totalPaid >= activeLoan.totalAmount) {
                loanDao.markCompleted(activeLoan.id, System.currentTimeMillis())
            }
        }
    }

    suspend fun updateLoanAmount(loanId: Long, newAmount: Double) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: return@withTransaction
            val updatedLoan = loan.copy(
                totalAmount = newAmount, 
                lastModified = System.currentTimeMillis(),
                syncStatus = "PENDING_SYNC"
            )
            loanDao.update(updatedLoan)
            
            val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
            if (totalPaid >= newAmount) {
                if (!loan.isCompleted) {
                    loanDao.markCompleted(loanId, System.currentTimeMillis())
                }
            } else {
                if (loan.isCompleted) {
                    loanDao.markIncomplete(loanId)
                }
            }
        }
    }

    suspend fun updateLoan(loanId: Long, newAmount: Double, newDate: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val loan = loanDao.getLoanById(loanId) ?: return@withTransaction
            val updatedLoan = loan.copy(
                totalAmount = newAmount,
                dateGiven = newDate,
                lastModified = System.currentTimeMillis()
            )
            loanDao.update(updatedLoan)
            
            val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
            if (totalPaid >= newAmount) {
                if (!loan.isCompleted) {
                    loanDao.markCompleted(loanId, System.currentTimeMillis())
                }
            } else {
                if (loan.isCompleted) {
                    loanDao.markIncomplete(loanId)
                }
            }
        }
    }

    suspend fun updatePaymentAmount(paymentId: Long, newAmount: Double) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val payment = paymentDao.getPaymentById(paymentId) ?: return@withTransaction
            val updatedPayment = payment.copy(amount = newAmount, lastModified = System.currentTimeMillis())
            paymentDao.update(updatedPayment)
            
            val loanId = payment.loanId
            val loan = loanDao.getLoanById(loanId)
            if (loan != null) {
                val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
                if (totalPaid >= loan.totalAmount) {
                    if (!loan.isCompleted) {
                        loanDao.markCompleted(loanId, System.currentTimeMillis())
                    }
                } else {
                    if (loan.isCompleted) {
                        loanDao.markIncomplete(loanId)
                    }
                }
            }
        }
    }

    suspend fun updatePayment(paymentId: Long, newAmount: Double, newDate: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val payment = paymentDao.getPaymentById(paymentId) ?: return@withTransaction
            val updatedPayment = payment.copy(
                amount = newAmount,
                date = newDate,
                lastModified = System.currentTimeMillis()
            )
            paymentDao.update(updatedPayment)
            
            val loanId = payment.loanId
            val loan = loanDao.getLoanById(loanId)
            if (loan != null) {
                val totalPaid = paymentDao.getTotalPaidForLoan(loanId)
                if (totalPaid >= loan.totalAmount) {
                    if (!loan.isCompleted) {
                        loanDao.markCompleted(loanId, System.currentTimeMillis())
                    }
                } else {
                    if (loan.isCompleted) {
                        loanDao.markIncomplete(loanId)
                    }
                }
            }
        }
    }

    suspend fun softDeleteLoan(id: Long) = withContext(Dispatchers.IO) {
        database.withTransaction {
            loanDao.softDeleteLoan(id)
            paymentDao.softDeletePaymentsForLoan(id)
        }
    }

    suspend fun getLoanById(id: Long): Loan? = withContext(Dispatchers.IO) {
        loanDao.getLoanById(id)
    }

    suspend fun getPaymentById(id: Long): Payment? = withContext(Dispatchers.IO) {
        paymentDao.getPaymentById(id)
    }

    suspend fun getCustomerNameForLoan(loanId: Long): String? = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanById(loanId) ?: return@withContext null
        database.personDao().getPersonById(loan.personId)?.name
    }

    suspend fun getCustomerNameForPayment(paymentId: Long): String? = withContext(Dispatchers.IO) {
        val payment = paymentDao.getPaymentById(paymentId) ?: return@withContext null
        getCustomerNameForLoan(payment.loanId)
    }

    suspend fun getCustomerNameForPerson(personId: Long): String? = withContext(Dispatchers.IO) {
        database.personDao().getPersonById(personId)?.name
    }

    suspend fun getPersonById(personId: Long): com.moneymate.lite.data.entity.Person? = withContext(Dispatchers.IO) {
        database.personDao().getPersonById(personId)
    }

    suspend fun getFileNameForPerson(personId: Long): String? = withContext(Dispatchers.IO) {
        val person = database.personDao().getPersonById(personId) ?: return@withContext null
        database.loanFileDao().getFileById(person.fileId)?.name
    }

    suspend fun getFileNameForLoan(loanId: Long): String? = withContext(Dispatchers.IO) {
        val loan = loanDao.getLoanById(loanId) ?: return@withContext null
        getFileNameForPerson(loan.personId)
    }

    suspend fun getFileNameForPayment(paymentId: Long): String? = withContext(Dispatchers.IO) {
        val payment = paymentDao.getPaymentById(paymentId) ?: return@withContext null
        getFileNameForLoan(payment.loanId)
    }
}
