package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.repository.LoanFileRepository
import com.moneymate.lite.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentlyDeletedViewModel @Inject constructor(
    private val fileRepository: LoanFileRepository,
    private val personRepository: PersonRepository
) : ViewModel() {

    val deletedFiles: StateFlow<List<LoanFile>> = fileRepository.getDeletedFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedPersons: StateFlow<List<Person>> = personRepository.getDeletedPersons()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun restoreFile(id: Long) {
        viewModelScope.launch {
            fileRepository.restoreFile(id)
        }
    }

    fun restorePerson(id: Long) {
        viewModelScope.launch {
            personRepository.restorePerson(id)
        }
    }
}
