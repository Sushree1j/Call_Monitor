package com.callmonitor.data;

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
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class RecordingDao_Impl implements RecordingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RecordingEntity> __insertionAdapterOfRecordingEntity;

  private final EntityDeletionOrUpdateAdapter<RecordingEntity> __updateAdapterOfRecordingEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkAsUploaded;

  private final SharedSQLiteStatement __preparedStmtOfIncrementUploadAttempt;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public RecordingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRecordingEntity = new EntityInsertionAdapter<RecordingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `recordings` (`id`,`fileName`,`filePath`,`phoneNumber`,`isIncoming`,`timestamp`,`duration`,`fileSize`,`isUploaded`,`uploadAttempts`,`lastUploadAttempt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecordingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFileName());
        statement.bindString(3, entity.getFilePath());
        statement.bindString(4, entity.getPhoneNumber());
        final int _tmp = entity.isIncoming() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getTimestamp());
        statement.bindLong(7, entity.getDuration());
        statement.bindLong(8, entity.getFileSize());
        final int _tmp_1 = entity.isUploaded() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getUploadAttempts());
        if (entity.getLastUploadAttempt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getLastUploadAttempt());
        }
      }
    };
    this.__updateAdapterOfRecordingEntity = new EntityDeletionOrUpdateAdapter<RecordingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `recordings` SET `id` = ?,`fileName` = ?,`filePath` = ?,`phoneNumber` = ?,`isIncoming` = ?,`timestamp` = ?,`duration` = ?,`fileSize` = ?,`isUploaded` = ?,`uploadAttempts` = ?,`lastUploadAttempt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RecordingEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFileName());
        statement.bindString(3, entity.getFilePath());
        statement.bindString(4, entity.getPhoneNumber());
        final int _tmp = entity.isIncoming() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getTimestamp());
        statement.bindLong(7, entity.getDuration());
        statement.bindLong(8, entity.getFileSize());
        final int _tmp_1 = entity.isUploaded() ? 1 : 0;
        statement.bindLong(9, _tmp_1);
        statement.bindLong(10, entity.getUploadAttempts());
        if (entity.getLastUploadAttempt() == null) {
          statement.bindNull(11);
        } else {
          statement.bindLong(11, entity.getLastUploadAttempt());
        }
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfMarkAsUploaded = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE recordings SET isUploaded = 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementUploadAttempt = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE recordings SET uploadAttempts = uploadAttempts + 1, lastUploadAttempt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM recordings WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final RecordingEntity recording,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRecordingEntity.insertAndReturnId(recording);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final RecordingEntity recording,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRecordingEntity.handle(recording);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsUploaded(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsUploaded.acquire();
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
          __preparedStmtOfMarkAsUploaded.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementUploadAttempt(final long id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementUploadAttempt.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
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
          __preparedStmtOfIncrementUploadAttempt.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
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
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RecordingEntity>> getAllRecordings() {
    final String _sql = "SELECT * FROM recordings ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recordings"}, new Callable<List<RecordingEntity>>() {
      @Override
      @NonNull
      public List<RecordingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfIsIncoming = CursorUtil.getColumnIndexOrThrow(_cursor, "isIncoming");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfIsUploaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isUploaded");
          final int _cursorIndexOfUploadAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadAttempts");
          final int _cursorIndexOfLastUploadAttempt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUploadAttempt");
          final List<RecordingEntity> _result = new ArrayList<RecordingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecordingEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final boolean _tmpIsIncoming;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIncoming);
            _tmpIsIncoming = _tmp != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final boolean _tmpIsUploaded;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsUploaded);
            _tmpIsUploaded = _tmp_1 != 0;
            final int _tmpUploadAttempts;
            _tmpUploadAttempts = _cursor.getInt(_cursorIndexOfUploadAttempts);
            final Long _tmpLastUploadAttempt;
            if (_cursor.isNull(_cursorIndexOfLastUploadAttempt)) {
              _tmpLastUploadAttempt = null;
            } else {
              _tmpLastUploadAttempt = _cursor.getLong(_cursorIndexOfLastUploadAttempt);
            }
            _item = new RecordingEntity(_tmpId,_tmpFileName,_tmpFilePath,_tmpPhoneNumber,_tmpIsIncoming,_tmpTimestamp,_tmpDuration,_tmpFileSize,_tmpIsUploaded,_tmpUploadAttempts,_tmpLastUploadAttempt);
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
  public Object getPendingUploads(final Continuation<? super List<RecordingEntity>> $completion) {
    final String _sql = "SELECT * FROM recordings WHERE isUploaded = 0 ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<RecordingEntity>>() {
      @Override
      @NonNull
      public List<RecordingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfIsIncoming = CursorUtil.getColumnIndexOrThrow(_cursor, "isIncoming");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfIsUploaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isUploaded");
          final int _cursorIndexOfUploadAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadAttempts");
          final int _cursorIndexOfLastUploadAttempt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUploadAttempt");
          final List<RecordingEntity> _result = new ArrayList<RecordingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RecordingEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final boolean _tmpIsIncoming;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIncoming);
            _tmpIsIncoming = _tmp != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final boolean _tmpIsUploaded;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsUploaded);
            _tmpIsUploaded = _tmp_1 != 0;
            final int _tmpUploadAttempts;
            _tmpUploadAttempts = _cursor.getInt(_cursorIndexOfUploadAttempts);
            final Long _tmpLastUploadAttempt;
            if (_cursor.isNull(_cursorIndexOfLastUploadAttempt)) {
              _tmpLastUploadAttempt = null;
            } else {
              _tmpLastUploadAttempt = _cursor.getLong(_cursorIndexOfLastUploadAttempt);
            }
            _item = new RecordingEntity(_tmpId,_tmpFileName,_tmpFilePath,_tmpPhoneNumber,_tmpIsIncoming,_tmpTimestamp,_tmpDuration,_tmpFileSize,_tmpIsUploaded,_tmpUploadAttempts,_tmpLastUploadAttempt);
            _result.add(_item);
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
  public Flow<Integer> getPendingUploadCount() {
    final String _sql = "SELECT COUNT(*) FROM recordings WHERE isUploaded = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recordings"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<Integer> getTotalRecordingCount() {
    final String _sql = "SELECT COUNT(*) FROM recordings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"recordings"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getRecordingById(final long id,
      final Continuation<? super RecordingEntity> $completion) {
    final String _sql = "SELECT * FROM recordings WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RecordingEntity>() {
      @Override
      @Nullable
      public RecordingEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "fileName");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfPhoneNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneNumber");
          final int _cursorIndexOfIsIncoming = CursorUtil.getColumnIndexOrThrow(_cursor, "isIncoming");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "duration");
          final int _cursorIndexOfFileSize = CursorUtil.getColumnIndexOrThrow(_cursor, "fileSize");
          final int _cursorIndexOfIsUploaded = CursorUtil.getColumnIndexOrThrow(_cursor, "isUploaded");
          final int _cursorIndexOfUploadAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "uploadAttempts");
          final int _cursorIndexOfLastUploadAttempt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastUploadAttempt");
          final RecordingEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFileName;
            _tmpFileName = _cursor.getString(_cursorIndexOfFileName);
            final String _tmpFilePath;
            _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            final String _tmpPhoneNumber;
            _tmpPhoneNumber = _cursor.getString(_cursorIndexOfPhoneNumber);
            final boolean _tmpIsIncoming;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsIncoming);
            _tmpIsIncoming = _tmp != 0;
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDuration;
            _tmpDuration = _cursor.getLong(_cursorIndexOfDuration);
            final long _tmpFileSize;
            _tmpFileSize = _cursor.getLong(_cursorIndexOfFileSize);
            final boolean _tmpIsUploaded;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsUploaded);
            _tmpIsUploaded = _tmp_1 != 0;
            final int _tmpUploadAttempts;
            _tmpUploadAttempts = _cursor.getInt(_cursorIndexOfUploadAttempts);
            final Long _tmpLastUploadAttempt;
            if (_cursor.isNull(_cursorIndexOfLastUploadAttempt)) {
              _tmpLastUploadAttempt = null;
            } else {
              _tmpLastUploadAttempt = _cursor.getLong(_cursorIndexOfLastUploadAttempt);
            }
            _result = new RecordingEntity(_tmpId,_tmpFileName,_tmpFilePath,_tmpPhoneNumber,_tmpIsIncoming,_tmpTimestamp,_tmpDuration,_tmpFileSize,_tmpIsUploaded,_tmpUploadAttempts,_tmpLastUploadAttempt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
