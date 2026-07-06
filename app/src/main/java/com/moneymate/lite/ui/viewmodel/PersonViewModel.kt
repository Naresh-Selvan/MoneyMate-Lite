package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.repository.LoanFileRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val repository: PersonRepository,
    private val fileRepository: LoanFileRepository
) : ViewModel() {

    // Cache per fileId to prevent StateFlow recreation on recomposition
    // Eagerly so flow never stops and never resets to empty (prevents blinking)
    private val personsByFileCache = mutableMapOf<Long, StateFlow<List<Person>>>()

    fun getPersonsByFile(fileId: Long): StateFlow<List<Person>> {
        return personsByFileCache.getOrPut(fileId) {
            repository.getPersonsByFile(fileId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }

    suspend fun addPerson(person: Person, position: Int): Long {
        return repository.insertAtPosition(person, position)
    }

    suspend fun updatePerson(person: Person, oldSortOrder: Int, newSortOrder: Int) {
        repository.updateWithPosition(person, oldSortOrder, newSortOrder)
    }

    suspend fun deletePerson(id: Long) {
        repository.deleteWithShift(id)
    }

    suspend fun getPersonById(id: Long): Person? = repository.getPersonById(id)

    suspend fun getFileById(id: Long): LoanFile? = fileRepository.getFileById(id)

    // Reactive file info — Eagerly started so the title is ready immediately (no flicker)
    private val fileByIdCache = mutableMapOf<Long, StateFlow<LoanFile?>>()

    fun getFileByIdFlow(id: Long): StateFlow<LoanFile?> {
        return fileByIdCache.getOrPut(id) {
            fileRepository.getFileByIdFlow(id)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null
                )
        }
    }

    fun restorePerson(id: Long) {
        viewModelScope.launch {
            repository.restorePerson(id)
        }
    }

    private val deletedPersonsByFileCache = mutableMapOf<Long, StateFlow<List<Person>>>()

    fun getDeletedPersonsByFile(fileId: Long): StateFlow<List<Person>> {
        return deletedPersonsByFileCache.getOrPut(fileId) {
            repository.getDeletedPersonsByFile(fileId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = emptyList()
                )
        }
    }
}
