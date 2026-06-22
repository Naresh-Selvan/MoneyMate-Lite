package com.moneymate.lite.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.moneymate.lite.data.dao.LoanDao;
import com.moneymate.lite.data.dao.LoanDao_Impl;
import com.moneymate.lite.data.dao.LoanFileDao;
import com.moneymate.lite.data.dao.LoanFileDao_Impl;
import com.moneymate.lite.data.dao.PaymentDao;
import com.moneymate.lite.data.dao.PaymentDao_Impl;
import com.moneymate.lite.data.dao.PersonDao;
import com.moneymate.lite.data.dao.PersonDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LoanFileDao _loanFileDao;

  private volatile PersonDao _personDao;

  private volatile LoanDao _loanDao;

  private volatile PaymentDao _paymentDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `loan_files` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `persons` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fileId` INTEGER NOT NULL, `name` TEXT NOT NULL, `mobileNumber` TEXT, `place` TEXT, `notes` TEXT, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `loans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `personId` INTEGER NOT NULL, `totalAmount` REAL NOT NULL, `dateGiven` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `loanId` INTEGER NOT NULL, `amount` REAL NOT NULL, `date` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `isDeleted` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'd143732c25a58ff0743f46249dc9081b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `loan_files`");
        db.execSQL("DROP TABLE IF EXISTS `persons`");
        db.execSQL("DROP TABLE IF EXISTS `loans`");
        db.execSQL("DROP TABLE IF EXISTS `payments`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLoanFiles = new HashMap<String, TableInfo.Column>(5);
        _columnsLoanFiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoanFiles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoanFiles.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoanFiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoanFiles.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLoanFiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLoanFiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLoanFiles = new TableInfo("loan_files", _columnsLoanFiles, _foreignKeysLoanFiles, _indicesLoanFiles);
        final TableInfo _existingLoanFiles = TableInfo.read(db, "loan_files");
        if (!_infoLoanFiles.equals(_existingLoanFiles)) {
          return new RoomOpenHelper.ValidationResult(false, "loan_files(com.moneymate.lite.data.entity.LoanFile).\n"
                  + " Expected:\n" + _infoLoanFiles + "\n"
                  + " Found:\n" + _existingLoanFiles);
        }
        final HashMap<String, TableInfo.Column> _columnsPersons = new HashMap<String, TableInfo.Column>(9);
        _columnsPersons.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("fileId", new TableInfo.Column("fileId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("mobileNumber", new TableInfo.Column("mobileNumber", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("place", new TableInfo.Column("place", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPersons.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPersons = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPersons = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPersons = new TableInfo("persons", _columnsPersons, _foreignKeysPersons, _indicesPersons);
        final TableInfo _existingPersons = TableInfo.read(db, "persons");
        if (!_infoPersons.equals(_existingPersons)) {
          return new RoomOpenHelper.ValidationResult(false, "persons(com.moneymate.lite.data.entity.Person).\n"
                  + " Expected:\n" + _infoPersons + "\n"
                  + " Found:\n" + _existingPersons);
        }
        final HashMap<String, TableInfo.Column> _columnsLoans = new HashMap<String, TableInfo.Column>(8);
        _columnsLoans.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("personId", new TableInfo.Column("personId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("totalAmount", new TableInfo.Column("totalAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("dateGiven", new TableInfo.Column("dateGiven", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLoans.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLoans = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLoans = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLoans = new TableInfo("loans", _columnsLoans, _foreignKeysLoans, _indicesLoans);
        final TableInfo _existingLoans = TableInfo.read(db, "loans");
        if (!_infoLoans.equals(_existingLoans)) {
          return new RoomOpenHelper.ValidationResult(false, "loans(com.moneymate.lite.data.entity.Loan).\n"
                  + " Expected:\n" + _infoLoans + "\n"
                  + " Found:\n" + _existingLoans);
        }
        final HashMap<String, TableInfo.Column> _columnsPayments = new HashMap<String, TableInfo.Column>(6);
        _columnsPayments.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("loanId", new TableInfo.Column("loanId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("isDeleted", new TableInfo.Column("isDeleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPayments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPayments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPayments = new TableInfo("payments", _columnsPayments, _foreignKeysPayments, _indicesPayments);
        final TableInfo _existingPayments = TableInfo.read(db, "payments");
        if (!_infoPayments.equals(_existingPayments)) {
          return new RoomOpenHelper.ValidationResult(false, "payments(com.moneymate.lite.data.entity.Payment).\n"
                  + " Expected:\n" + _infoPayments + "\n"
                  + " Found:\n" + _existingPayments);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "d143732c25a58ff0743f46249dc9081b", "ae7fe28c8e225a9624754ddd469b1a74");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "loan_files","persons","loans","payments");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `loan_files`");
      _db.execSQL("DELETE FROM `persons`");
      _db.execSQL("DELETE FROM `loans`");
      _db.execSQL("DELETE FROM `payments`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LoanFileDao.class, LoanFileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PersonDao.class, PersonDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LoanDao.class, LoanDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PaymentDao.class, PaymentDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LoanFileDao loanFileDao() {
    if (_loanFileDao != null) {
      return _loanFileDao;
    } else {
      synchronized(this) {
        if(_loanFileDao == null) {
          _loanFileDao = new LoanFileDao_Impl(this);
        }
        return _loanFileDao;
      }
    }
  }

  @Override
  public PersonDao personDao() {
    if (_personDao != null) {
      return _personDao;
    } else {
      synchronized(this) {
        if(_personDao == null) {
          _personDao = new PersonDao_Impl(this);
        }
        return _personDao;
      }
    }
  }

  @Override
  public LoanDao loanDao() {
    if (_loanDao != null) {
      return _loanDao;
    } else {
      synchronized(this) {
        if(_loanDao == null) {
          _loanDao = new LoanDao_Impl(this);
        }
        return _loanDao;
      }
    }
  }

  @Override
  public PaymentDao paymentDao() {
    if (_paymentDao != null) {
      return _paymentDao;
    } else {
      synchronized(this) {
        if(_paymentDao == null) {
          _paymentDao = new PaymentDao_Impl(this);
        }
        return _paymentDao;
      }
    }
  }
}
