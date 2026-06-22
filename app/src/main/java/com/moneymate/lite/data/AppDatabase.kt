package com.moneymate.lite.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person

@Database(
    entities = [LoanFile::class, Person::class, Loan::class, Payment::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanFileDao(): LoanFileDao
    abstract fun personDao(): PersonDao
    abstract fun loanDao(): LoanDao
    abstract fun paymentDao(): PaymentDao
}
