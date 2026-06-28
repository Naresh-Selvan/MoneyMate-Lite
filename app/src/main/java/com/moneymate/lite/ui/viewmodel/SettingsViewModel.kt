package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.firebase.RestoreHelper
import com.moneymate.lite.data.firebase.UploadHelper
import com.moneymate.lite.data.repository.LoanFileRepository
import com.moneymate.lite.data.repository.LoanRepository
import com.moneymate.lite.data.repository.PaymentRepository
import com.moneymate.lite.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val uploadHelper: UploadHelper,
    private val restoreHelper: RestoreHelper,
    private val loanFileRepository: LoanFileRepository,
    private val personRepository: PersonRepository,
    private val loanRepository: LoanRepository,
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    fun backup(onResult: (Result<Unit>) -> Unit) {
        if (_isBackingUp.value || _isRestoring.value) return
        _isBackingUp.value = true
        viewModelScope.launch {
            val result = uploadHelper.uploadAll(
                loanFileRepository = loanFileRepository,
                personRepository = personRepository,
                loanRepository = loanRepository,
                paymentRepository = paymentRepository
            )
            _isBackingUp.value = false
            onResult(result)
        }
    }

    fun restore(onResult: (Result<String>) -> Unit) {
        if (_isBackingUp.value || _isRestoring.value) return
        _isRestoring.value = true
        viewModelScope.launch {
            val result = restoreHelper.restoreAll(
                loanFileRepository = loanFileRepository,
                personRepository = personRepository,
                loanRepository = loanRepository,
                paymentRepository = paymentRepository
            )
            _isRestoring.value = false
            onResult(result)
        }
    }
}
