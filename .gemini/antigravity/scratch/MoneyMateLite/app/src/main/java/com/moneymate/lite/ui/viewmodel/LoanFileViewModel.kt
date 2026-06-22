package com.moneymate.lite.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.repository.LoanFileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LoanFileViewModel @Inject constructor(
    private val repository: LoanFileRepository
) : ViewModel() {

    val files: StateFlow<List<LoanFile>> = repository.getAllFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    suspend fun addFile(name: String) {
        repository.insert(LoanFile(name = name))
    }

    suspend fun renameFile(id: Long, newName: String) {
        val file = repository.getFileById(id)
        if (file != null) {
            repository.update(file.copy(name = newName))
        }
    }

    suspend fun deleteFile(id: Long) {
        repository.softDelete(id)
    }
}
