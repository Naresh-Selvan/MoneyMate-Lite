package com.moneymate.lite.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneymate.lite.data.dao.ActivityLogDao
import com.moneymate.lite.data.dao.ApprovalRequestDao
import com.moneymate.lite.data.dao.LoanDao
import com.moneymate.lite.data.dao.LoanFileDao
import com.moneymate.lite.data.dao.PaymentDao
import com.moneymate.lite.data.dao.PersonDao
import com.moneymate.lite.data.dao.WorkerProfileDao
import com.moneymate.lite.data.entity.ActivityLogEntity
import com.moneymate.lite.data.entity.ApprovalRequestEntity
import com.moneymate.lite.data.entity.Loan
import com.moneymate.lite.data.entity.LoanFile
import com.moneymate.lite.data.entity.Payment
import com.moneymate.lite.data.entity.Person
import com.moneymate.lite.data.entity.WorkerProfileEntity

@Database(
    entities = [
        LoanFile::class,
        Person::class,
        Loan::class,
        Payment::class,
        ApprovalRequestEntity::class,
        ActivityLogEntity::class,
        WorkerProfileEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loanFileDao(): LoanFileDao
    abstract fun personDao(): PersonDao
    abstract fun loanDao(): LoanDao
    abstract fun paymentDao(): PaymentDao
    abstract fun approvalRequestDao(): ApprovalRequestDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun workerProfileDao(): WorkerProfileDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add columns to loan_files
                db.execSQL("ALTER TABLE loan_files ADD COLUMN sharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loan_files ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loan_files ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loan_files ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("UPDATE loan_files SET sharedId = 'file_' || id WHERE sharedId = ''")

                // 2. Add columns to persons
                db.execSQL("ALTER TABLE persons ADD COLUMN sharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN fileSharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE persons ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE persons ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("UPDATE persons SET sharedId = 'person_' || id WHERE sharedId = ''")
                db.execSQL("UPDATE persons SET fileSharedId = 'file_' || fileId WHERE fileSharedId = ''")

                // 3. Add columns to loans
                db.execSQL("ALTER TABLE loans ADD COLUMN sharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loans ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loans ADD COLUMN personSharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE loans ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE loans ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("UPDATE loans SET sharedId = 'loan_' || id WHERE sharedId = ''")
                db.execSQL("UPDATE loans SET personSharedId = 'person_' || personId WHERE personSharedId = ''")

                // 4. Add columns to payments
                db.execSQL("ALTER TABLE payments ADD COLUMN sharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN businessId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN loanSharedId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE payments ADD COLUMN recordedByWorkerId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE payments ADD COLUMN recordedByWorkerName TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE payments ADD COLUMN lastModified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE payments ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
                db.execSQL("ALTER TABLE payments ADD COLUMN syncError TEXT DEFAULT NULL")
                db.execSQL("UPDATE payments SET sharedId = 'payment_' || id WHERE sharedId = ''")
                db.execSQL("UPDATE payments SET loanSharedId = 'loan_' || loanId WHERE loanSharedId = ''")

                // 5. Create approval_requests table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS approval_requests (
                        id TEXT PRIMARY KEY NOT NULL,
                        businessId TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        supersededBy TEXT,
                        workerId TEXT NOT NULL,
                        workerName TEXT NOT NULL,
                        fileSharedId TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        recordType TEXT NOT NULL,
                        recordSharedId TEXT,
                        actionType TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        requestedAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        rejectionReason TEXT,
                        reviewedBy TEXT,
                        reviewedAt INTEGER,
                        baseSnapshotJson TEXT,
                        proposedPayloadJson TEXT NOT NULL,
                        syncStatus TEXT NOT NULL,
                        syncError TEXT,
                        clientOpId TEXT NOT NULL
                    )
                """.trimIndent())

                // 6. Create activity_logs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_logs (
                        id TEXT PRIMARY KEY NOT NULL,
                        businessId TEXT NOT NULL,
                        actorId TEXT NOT NULL,
                        actorName TEXT NOT NULL,
                        actorRole TEXT NOT NULL,
                        action TEXT NOT NULL,
                        fileSharedId TEXT,
                        fileName TEXT,
                        recordType TEXT,
                        recordSharedId TEXT,
                        timestamp INTEGER NOT NULL,
                        reason TEXT,
                        details TEXT,
                        beforeSnapshotJson TEXT,
                        afterSnapshotJson TEXT,
                        requestId TEXT,
                        outcome TEXT NOT NULL,
                        syncStatus TEXT NOT NULL
                    )
                """.trimIndent())

                // 7. Create worker_profiles table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS worker_profiles (
                        uid TEXT PRIMARY KEY NOT NULL,
                        businessId TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        username TEXT NOT NULL,
                        email TEXT NOT NULL,
                        accessAllFiles INTEGER NOT NULL,
                        allowedFileIdsJson TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        mustChangePassword INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
