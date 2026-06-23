package com.moneymate.lite.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.moneymate.lite.data.entity.Loan;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LoanDao_Impl implements LoanDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Loan> __insertionAdapterOfLoan;

  private final EntityDeletionOrUpdateAdapter<Loan> __updateAdapterOfLoan;

  private final SharedSQLiteStatement __preparedStmtOfMarkCompleted;

  private final SharedSQLiteStatement __preparedStmtOfMarkIncomplete;

  private final SharedSQLiteStatement __preparedStmtOfSoftDeleteLoan;

  public LoanDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLoan = new EntityInsertionAdapter<Loan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `loans` (`id`,`personId`,`totalAmount`,`dateGiven`,`isCompleted`,`completedAt`,`createdAt`,`isDeleted`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Loan entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPersonId());
        statement.bindDouble(3, entity.getTotalAmount());
        statement.bindLong(4, entity.getDateGiven());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getCompletedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getCompletedAt());
        }
        statement.bindLong(7, entity.getCreatedAt());
        final int _tmp_1 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
      }
    };
    this.__updateAdapterOfLoan = new EntityDeletionOrUpdateAdapter<Loan>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `loans` SET `id` = ?,`personId` = ?,`totalAmount` = ?,`dateGiven` = ?,`isCompleted` = ?,`completedAt` = ?,`createdAt` = ?,`isDeleted` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Loan entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getPersonId());
        statement.bindDouble(3, entity.getTotalAmount());
        statement.bindLong(4, entity.getDateGiven());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getCompletedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getCompletedAt());
        }
        statement.bindLong(7, entity.getCreatedAt());
        final int _tmp_1 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(8, _tmp_1);
        statement.bindLong(9, entity.getId());
      }
    };
    this.__preparedStmtOfMarkCompleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE loans SET isCompleted = 1, completedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMarkIncomplete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE loans SET isCompleted = 0, completedAt = null WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSoftDeleteLoan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE loans SET isDeleted = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Loan loan, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfLoan.insertAndReturnId(loan);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Loan loan, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLoan.handle(loan);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markCompleted(final long loanId, final long completedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkCompleted.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, completedAt);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, loanId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkCompleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object markIncomplete(final long loanId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkIncomplete.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, loanId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkIncomplete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object softDeleteLoan(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDeleteLoan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSoftDeleteLoan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Loan>> getLoansByPerson(final long personId) {
    final String _sql = "SELECT * FROM loans WHERE personId = ? AND isDeleted = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, personId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<List<Loan>>() {
      @Override
      @NonNull
      public List<Loan> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPersonId = CursorUtil.getColumnIndexOrThrow(_cursor, "personId");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfDateGiven = CursorUtil.getColumnIndexOrThrow(_cursor, "dateGiven");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final List<Loan> _result = new ArrayList<Loan>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Loan _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final long _tmpDateGiven;
            _tmpDateGiven = _cursor.getLong(_cursorIndexOfDateGiven);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            _item = new Loan(_tmpId,_tmpPersonId,_tmpTotalAmount,_tmpDateGiven,_tmpIsCompleted,_tmpCompletedAt,_tmpCreatedAt,_tmpIsDeleted);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getActiveLoanByPerson(final long personId,
      final Continuation<? super Loan> $completion) {
    final String _sql = "SELECT * FROM loans WHERE personId = ? AND isCompleted = 0 AND isDeleted = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, personId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Loan>() {
      @Override
      @Nullable
      public Loan call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPersonId = CursorUtil.getColumnIndexOrThrow(_cursor, "personId");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfDateGiven = CursorUtil.getColumnIndexOrThrow(_cursor, "dateGiven");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final Loan _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final long _tmpDateGiven;
            _tmpDateGiven = _cursor.getLong(_cursorIndexOfDateGiven);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            _result = new Loan(_tmpId,_tmpPersonId,_tmpTotalAmount,_tmpDateGiven,_tmpIsCompleted,_tmpCompletedAt,_tmpCreatedAt,_tmpIsDeleted);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Loan> getActiveLoanByPersonFlow(final long personId) {
    final String _sql = "SELECT * FROM loans WHERE personId = ? AND isCompleted = 0 AND isDeleted = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, personId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans"}, new Callable<Loan>() {
      @Override
      @Nullable
      public Loan call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPersonId = CursorUtil.getColumnIndexOrThrow(_cursor, "personId");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfDateGiven = CursorUtil.getColumnIndexOrThrow(_cursor, "dateGiven");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final Loan _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final long _tmpDateGiven;
            _tmpDateGiven = _cursor.getLong(_cursorIndexOfDateGiven);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            _result = new Loan(_tmpId,_tmpPersonId,_tmpTotalAmount,_tmpDateGiven,_tmpIsCompleted,_tmpCompletedAt,_tmpCreatedAt,_tmpIsDeleted);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLoanById(final long id, final Continuation<? super Loan> $completion) {
    final String _sql = "SELECT * FROM loans WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Loan>() {
      @Override
      @Nullable
      public Loan call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPersonId = CursorUtil.getColumnIndexOrThrow(_cursor, "personId");
          final int _cursorIndexOfTotalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "totalAmount");
          final int _cursorIndexOfDateGiven = CursorUtil.getColumnIndexOrThrow(_cursor, "dateGiven");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final Loan _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final long _tmpDateGiven;
            _tmpDateGiven = _cursor.getLong(_cursorIndexOfDateGiven);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final Long _tmpCompletedAt;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmpCompletedAt = null;
            } else {
              _tmpCompletedAt = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            _result = new Loan(_tmpId,_tmpPersonId,_tmpTotalAmount,_tmpDateGiven,_tmpIsCompleted,_tmpCompletedAt,_tmpCreatedAt,_tmpIsDeleted);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LoanWithBalance>> getAllActiveLoansInFile(final long fileId) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            loans.id AS loanId, \n"
            + "            loans.personId AS personId, \n"
            + "            persons.name AS personName, \n"
            + "            loans.totalAmount AS totalAmount, \n"
            + "            (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0) AS totalPaid,\n"
            + "            (loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0)) AS balance,\n"
            + "            loans.dateGiven AS dateGiven\n"
            + "        FROM loans\n"
            + "        INNER JOIN persons ON loans.personId = persons.id\n"
            + "        WHERE persons.fileId = ? AND loans.isCompleted = 0 AND loans.isDeleted = 0\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fileId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payments", "loans",
        "persons"}, new Callable<List<LoanWithBalance>>() {
      @Override
      @NonNull
      public List<LoanWithBalance> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = 0;
          final int _cursorIndexOfPersonId = 1;
          final int _cursorIndexOfPersonName = 2;
          final int _cursorIndexOfTotalAmount = 3;
          final int _cursorIndexOfTotalPaid = 4;
          final int _cursorIndexOfBalance = 5;
          final int _cursorIndexOfDateGiven = 6;
          final List<LoanWithBalance> _result = new ArrayList<LoanWithBalance>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LoanWithBalance _item;
            final long _tmpLoanId;
            _tmpLoanId = _cursor.getLong(_cursorIndexOfLoanId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final String _tmpPersonName;
            _tmpPersonName = _cursor.getString(_cursorIndexOfPersonName);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpTotalPaid;
            _tmpTotalPaid = _cursor.getDouble(_cursorIndexOfTotalPaid);
            final double _tmpBalance;
            _tmpBalance = _cursor.getDouble(_cursorIndexOfBalance);
            final long _tmpDateGiven;
            _tmpDateGiven = _cursor.getLong(_cursorIndexOfDateGiven);
            _item = new LoanWithBalance(_tmpLoanId,_tmpPersonId,_tmpPersonName,_tmpTotalAmount,_tmpTotalPaid,_tmpBalance,_tmpDateGiven);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LoanWithBalanceAndFile>> getAllActiveLoansAcrossFiles() {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            loans.id AS loanId, \n"
            + "            loans.personId AS personId, \n"
            + "            persons.name AS personName, \n"
            + "            loans.totalAmount AS totalAmount, \n"
            + "            (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0) AS totalPaid,\n"
            + "            (loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0)) AS balance,\n"
            + "            loans.dateGiven AS dateGiven,\n"
            + "            loan_files.name AS fileName\n"
            + "        FROM loans\n"
            + "        INNER JOIN persons ON loans.personId = persons.id\n"
            + "        INNER JOIN loan_files ON persons.fileId = loan_files.id\n"
            + "        WHERE loans.isCompleted = 0 AND loans.isDeleted = 0\n"
            + "            AND persons.isDeleted = 0 AND loan_files.isDeleted = 0\n"
            + "        ORDER BY balance DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payments", "loans", "persons",
        "loan_files"}, new Callable<List<LoanWithBalanceAndFile>>() {
      @Override
      @NonNull
      public List<LoanWithBalanceAndFile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLoanId = 0;
          final int _cursorIndexOfPersonId = 1;
          final int _cursorIndexOfPersonName = 2;
          final int _cursorIndexOfTotalAmount = 3;
          final int _cursorIndexOfTotalPaid = 4;
          final int _cursorIndexOfBalance = 5;
          final int _cursorIndexOfDateGiven = 6;
          final int _cursorIndexOfFileName = 7;
          final List<LoanWithBalanceAndFile> _result = new ArrayList<LoanWithBalanceAndFile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LoanWithBalanceAndFile _item;
            final long _tmpLoanId;
            _tmpLoanId = _cursor.getLong(_cursorIndexOfLoanId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final String _tmpPersonName;
            _tmpPersonName = _cursor.getString(_cursorIndexOfPersonName);
            final double _tmpTotalAmount;
            _tmpTotalAmount = _cursor.getDouble(_cursorIndexOfTotalAmount);
            final double _tmpTotalPaid;
            _tmpTotalPaid = _cursor.getDouble(_cursorIndexOfTotalPaid);
            final double _tmpBalance;
            _tmpBalance = _cursor.getDouble(_cursorIndexOfBalance);
            final long _tmpDateGiven;
            _tmpDateGiven = _cursor.getLong(_cursorIndexOfDateGiven);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            _item = new LoanWithBalanceAndFile(_tmpLoanId,_tmpPersonId,_tmpPersonName,_tmpTotalAmount,_tmpTotalPaid,_tmpBalance,_tmpDateGiven,_tmpFileName);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<DateTransactionEntity>> getLoansGivenOnDate(final long fileId,
      final long startOfDay, final long endOfDay) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            loans.id AS id, \n"
            + "            loans.personId AS personId, \n"
            + "            persons.name AS personName, \n"
            + "            loans.totalAmount AS amount, \n"
            + "            loans.dateGiven AS date\n"
            + "        FROM loans\n"
            + "        INNER JOIN persons ON loans.personId = persons.id\n"
            + "        WHERE persons.fileId = ? \n"
            + "          AND loans.isDeleted = 0 \n"
            + "          AND loans.dateGiven BETWEEN ? AND ?\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fileId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 3;
    _statement.bindLong(_argIndex, endOfDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"loans",
        "persons"}, new Callable<List<DateTransactionEntity>>() {
      @Override
      @NonNull
      public List<DateTransactionEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfPersonId = 1;
          final int _cursorIndexOfPersonName = 2;
          final int _cursorIndexOfAmount = 3;
          final int _cursorIndexOfDate = 4;
          final List<DateTransactionEntity> _result = new ArrayList<DateTransactionEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DateTransactionEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final String _tmpPersonName;
            _tmpPersonName = _cursor.getString(_cursorIndexOfPersonName);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            _item = new DateTransactionEntity(_tmpId,_tmpPersonId,_tmpPersonName,_tmpAmount,_tmpDate);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
