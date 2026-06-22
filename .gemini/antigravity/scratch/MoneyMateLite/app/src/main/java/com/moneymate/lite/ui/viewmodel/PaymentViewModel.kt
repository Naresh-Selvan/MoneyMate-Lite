package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _todayCollectionTotal = MutableStateFlow(0.0)
    val todayCollectionTotal: StateFlow<Double> = _todayCollectionTotal.asStateFlow()

    private val _totalOutstandingBalance = MutableStateFlow(0.0)
    val totalOutstandingBalance: StateFlow<Double> = _totalOutstandingBalance.asStateFlow()

    init {
        refreshTotals()
    }

    suspend fun recordPayment(loanId: Long, amount: Double, date: Long) {
        loanRepository.recordPayment(loanId, amount, date)
        refreshTotals()
    }

    fun refresh() {
        refreshTotals()
    }

    private fun refreshTotals() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            val calendar = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayStart = calendar.timeInMillis
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val todayEnd = calendar.timeInMillis

            _todayCollectionTotal.value = loanRepository.getTodayCollectionTotal(todayStart, todayEnd)
            _totalOutstandingBalance.value = loanRepository.getTotalOutstandingBalance()
        }
    }
}
