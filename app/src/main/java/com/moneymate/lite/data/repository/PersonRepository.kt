package com.moneymate.lite.data.repository

import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonRepository @Inject constructor(
    private val personDao: PersonDao
) {
    fun getPersonsByFile(fileId: Long): Flow<List<Person>> = personDao.getPersonsByFile(fileId)

    suspend fun insert(person: Person): Long = withContext(Dispatchers.IO) {
        personDao.insert(person)
    }

    suspend fun update(person: Person) = withContext(Dispatchers.IO) {
        personDao.update(person)
    }

    suspend fun softDelete(id: Long) = withContext(Dispatchers.IO) {
        personDao.softDelete(id)
    }

    fun getDeletedPersons(): Flow<List<Person>> = personDao.getDeletedPersons()

    suspend fun restorePerson(id: Long) = withContext(Dispatchers.IO) {
        personDao.restorePerson(id)
    }

    suspend fun getPersonById(id: Long): Person? = withContext(Dispatchers.IO) {
        personDao.getPersonById(id)
    }
}
