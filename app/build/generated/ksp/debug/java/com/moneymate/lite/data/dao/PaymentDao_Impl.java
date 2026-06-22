package com.moneymate.lite.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.moneymate.lite.data.entity.Payment;
import java.lang.Class;
import java.lang.Double;
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
public final class PaymentDao_Impl implements PaymentDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Payment> __insertionAdapterOfPayment;

  private final SharedSQLiteStatement __preparedStmtOfSoftDelete;

  private final SharedSQLiteStatement __preparedStmtOfRestorePayment;

  public PaymentDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPayment = new EntityInsertionAdapter<Payment>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `payments` (`id`,`loanId`,`amount`,`date`,`createdAt`,`isDeleted`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Payment entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getLoanId());
        statement.bindDouble(3, entity.getAmount());
        statement.bindLong(4, entity.getDate());
        statement.bindLong(5, entity.getCreatedAt());
        final int _tmp = entity.isDeleted() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__preparedStmtOfSoftDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE payments SET isDeleted = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestorePayment = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE payments SET isDeleted = 0 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Payment payment, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfPayment.insertAndReturnId(payment);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object softDelete(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSoftDelete.acquire();
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
          __preparedStmtOfSoftDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restorePayment(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestorePayment.acquire();
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
          __preparedStmtOfRestorePayment.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Payment>> getPaymentsByLoan(final long loanId) {
    final String _sql = "SELECT * FROM payments WHERE loanId = ? AND isDeleted = 0 ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, loanId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payments"}, new Callable<List<Payment>>() {
      @Override
      @NonNull
      public List<Payment> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLoanId = CursorUtil.getColumnIndexOrThrow(_cursor, "loanId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final List<Payment> _result = new ArrayList<Payment>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Payment _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpLoanId;
            _tmpLoanId = _cursor.getLong(_cursorIndexOfLoanId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpIsDeleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp != 0;
            _item = new Payment(_tmpId,_tmpLoanId,_tmpAmount,_tmpDate,_tmpCreatedAt,_tmpIsDeleted);
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
  public Object getTotalPaidForLoan(final long loanId,
      final Continuation<? super Double> $completion) {
    final String _sql = "SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, loanId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp;
            _tmp = _cursor.getDouble(0);
            _result = _tmp;
          } else {
            _result = 0.0;
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
  public Object getTodayCollectionTotal(final long todayStart, final long todayEnd,
      final Continuation<? super Double> $completion) {
    final String _sql = "SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE date BETWEEN ? AND ? AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, todayStart);
    _argIndex = 2;
    _statement.bindLong(_argIndex, todayEnd);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp;
            _tmp = _cursor.getDouble(0);
            _result = _tmp;
          } else {
            _result = 0.0;
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
  public Object getTotalOutstandingBalance(final Continuation<? super Double> $completion) {
    final String _sql = "\n"
            + "        SELECT COALESCE(SUM(loans.totalAmount - (SELECT COALESCE(SUM(amount), 0.0) FROM payments WHERE loanId = loans.id AND isDeleted = 0)), 0.0) \n"
            + "        FROM loans \n"
            + "        WHERE loans.isCompleted = 0 AND loans.isDeleted = 0\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Double>() {
      @Override
      @NonNull
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final double _tmp;
            _tmp = _cursor.getDouble(0);
            _result = _tmp;
          } else {
            _result = 0.0;
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
  public Flow<List<DeletedPaymentWithPerson>> getDeletedPaymentsByFile(final long fileId) {
    final String _sql = "\n"
            + "        SELECT p.id, p.amount, p.date, pe.name AS personName, pe.id AS personId, p.loanId\n"
            + "        FROM payments p\n"
            + "        JOIN loans l ON p.loanId = l.id\n"
            + "        JOIN persons pe ON l.personId = pe.id\n"
            + "        WHERE pe.fileId = ? AND p.isDeleted = 1\n"
            + "        ORDER BY p.createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, fileId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"payments", "loans",
        "persons"}, new Callable<List<DeletedPaymentWithPerson>>() {
      @Override
      @NonNull
      public List<DeletedPaymentWithPerson> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = 0;
          final int _cursorIndexOfAmount = 1;
          final int _cursorIndexOfDate = 2;
          final int _cursorIndexOfPersonName = 3;
          final int _cursorIndexOfPersonId = 4;
          final int _cursorIndexOfLoanId = 5;
          final List<DeletedPaymentWithPerson> _result = new ArrayList<DeletedPaymentWithPerson>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DeletedPaymentWithPerson _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final String _tmpPersonName;
            _tmpPersonName = _cursor.getString(_cursorIndexOfPersonName);
            final long _tmpPersonId;
            _tmpPersonId = _cursor.getLong(_cursorIndexOfPersonId);
            final long _tmpLoanId;
            _tmpLoanId = _cursor.getLong(_cursorIndexOfLoanId);
            _item = new DeletedPaymentWithPerson(_tmpId,_tmpAmount,_tmpDate,_tmpPersonName,_tmpPersonId,_tmpLoanId);
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
