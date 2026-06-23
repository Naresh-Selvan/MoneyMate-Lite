package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.dao.LoanWithBalance
import com.moneymate.lite.data.dao.LoanWithBalanceAndFile
import com.moneymate.lite.data.dao.DateTransactionEntity
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val repository: LoanRepository
) : ViewModel() {

    // Caches to prevent creating new StateFlows on every recomposition
    // Use Eagerly so flows never stop and never reset to initial null/emptyList (prevents flickering)
    private val activeLoansInFileCache = mutableMapOf<Long, StateFlow<List<LoanWithBalance>>>()
    private val loansByPersonCache = mutableMapOf<Long, StateFlow<List<Loan>>>()
    private val activeLoanByPersonCache = mutableMapOf<Long, StateFlow<Loan?>>()
    private val paymentsByLoanCache = mutableMapOf<Long, StateFlow<List<Payment>>>()

    fun getActiveLoansInFile(fileId: Long): StateFlow<List<LoanWithBalance>> {
        return activeLoansInFileCache.getOrPut(fileId) {
            repository.getAllActiveLoansInFile(fileId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    val allActiveLoans: StateFlow<List<LoanWithBalanceAndFile>> = repository.getAllActiveLoansAcrossFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getLoansByPerson(personId: Long): StateFlow<List<Loan>> {
        return loansByPersonCache.getOrPut(personId) {
            repository.getLoansByPerson(personId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    fun getActiveLoanFlow(personId: Long): StateFlow<Loan?> {
        return activeLoanByPersonCache.getOrPut(personId) {
            repository.getActiveLoanByPersonFlow(personId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null
                )
        }
    }

    fun getPaymentsByLoan(loanId: Long): StateFlow<List<Payment>> {
        return paymentsByLoanCache.getOrPut(loanId) {
            repository.getPaymentsByLoan(loanId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    suspend fun createLoan(personId: Long, totalAmount: Double, dateGiven: Long): Result<Long> {
        return try {
            val id = repository.createLoan(personId, totalAmount, dateGiven)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deletePayment(id: Long) {
        viewModelScope.launch {
            repository.softDeletePayment(id)
        }
    }

    fun restorePayment(id: Long) {
        viewModelScope.launch {
            repository.restorePayment(id)
        }
    }

    private val deletedPaymentsCache = mutableMapOf<Long, StateFlow<List<com.moneymate.lite.data.dao.DeletedPaymentWithPerson>>>()

    fun getDeletedPaymentsByFile(fileId: Long): StateFlow<List<com.moneymate.lite.data.dao.DeletedPaymentWithPerson>> {
        return deletedPaymentsCache.getOrPut(fileId) {
            repository.getDeletedPaymentsByFile(fileId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    fun getLoansGivenOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>> {
        return repository.getLoansGivenOnDate(fileId, startOfDay, endOfDay)
    }

    fun getPaymentsReceivedOnDate(fileId: Long, startOfDay: Long, endOfDay: Long): Flow<List<DateTransactionEntity>> {
        return repository.getPaymentsReceivedOnDate(fileId, startOfDay, endOfDay)
    }

    suspend fun addGivenTransaction(personId: Long, amount: Double, date: Long): Result<Unit> {
        return try {
            repository.addGivenTransaction(personId, amount, date)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReceivedTransaction(personId: Long, amount: Double, date: Long): Result<Unit> {
        return try {
            repository.addReceivedTransaction(personId, amount, date)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLoanAmount(loanId: Long, newAmount: Double): Result<Unit> {
        return try {
            repository.updateLoanAmount(loanId, newAmount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePaymentAmount(paymentId: Long, newAmount: Double): Result<Unit> {
        return try {
            repository.updatePaymentAmount(paymentId, newAmount)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteLoan(id: Long) {
        viewModelScope.launch {
            repository.softDeleteLoan(id)
        }
    }
}
