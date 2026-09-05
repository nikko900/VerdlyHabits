package com.saintnico.verdlyhabits.data.local.goals;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.IllegalArgumentException;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GoalsDao_Impl implements GoalsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GoalEntity> __insertionAdapterOfGoalEntity;

  private final EntityInsertionAdapter<MilestoneEntity> __insertionAdapterOfMilestoneEntity;

  private final EntityInsertionAdapter<CheckInEntity> __insertionAdapterOfCheckInEntity;

  private final EntityDeletionOrUpdateAdapter<GoalEntity> __updateAdapterOfGoalEntity;

  private final EntityDeletionOrUpdateAdapter<MilestoneEntity> __updateAdapterOfMilestoneEntity;

  public GoalsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGoalEntity = new EntityInsertionAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `goals` (`id`,`title`,`whyStatement`,`goalType`,`targetValue`,`currentValue`,`unit`,`startDate`,`targetDate`,`completedDate`,`linkedHabitIds`,`colorHex`,`energyLastUpdated`,`isArchived`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getWhyStatement());
        statement.bindString(4, __GoalType_enumToString(entity.getGoalType()));
        if (entity.getTargetValue() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getTargetValue());
        }
        statement.bindDouble(6, entity.getCurrentValue());
        statement.bindString(7, entity.getUnit());
        statement.bindLong(8, entity.getStartDate());
        statement.bindLong(9, entity.getTargetDate());
        if (entity.getCompletedDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getCompletedDate());
        }
        statement.bindString(11, entity.getLinkedHabitIds());
        statement.bindString(12, entity.getColorHex());
        statement.bindLong(13, entity.getEnergyLastUpdated());
        final int _tmp = entity.isArchived() ? 1 : 0;
        statement.bindLong(14, _tmp);
      }
    };
    this.__insertionAdapterOfMilestoneEntity = new EntityInsertionAdapter<MilestoneEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `milestones` (`id`,`goalId`,`title`,`targetPercent`,`completedDate`,`isCompleted`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MilestoneEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getGoalId());
        statement.bindString(3, entity.getTitle());
        statement.bindDouble(4, entity.getTargetPercent());
        if (entity.getCompletedDate() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCompletedDate());
        }
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__insertionAdapterOfCheckInEntity = new EntityInsertionAdapter<CheckInEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `check_ins` (`id`,`goalId`,`date`,`status`,`note`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CheckInEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getGoalId());
        statement.bindLong(3, entity.getDate());
        statement.bindString(4, __CheckInStatus_enumToString(entity.getStatus()));
        statement.bindString(5, entity.getNote());
      }
    };
    this.__updateAdapterOfGoalEntity = new EntityDeletionOrUpdateAdapter<GoalEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `goals` SET `id` = ?,`title` = ?,`whyStatement` = ?,`goalType` = ?,`targetValue` = ?,`currentValue` = ?,`unit` = ?,`startDate` = ?,`targetDate` = ?,`completedDate` = ?,`linkedHabitIds` = ?,`colorHex` = ?,`energyLastUpdated` = ?,`isArchived` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GoalEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getWhyStatement());
        statement.bindString(4, __GoalType_enumToString(entity.getGoalType()));
        if (entity.getTargetValue() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getTargetValue());
        }
        statement.bindDouble(6, entity.getCurrentValue());
        statement.bindString(7, entity.getUnit());
        statement.bindLong(8, entity.getStartDate());
        statement.bindLong(9, entity.getTargetDate());
        if (entity.getCompletedDate() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getCompletedDate());
        }
        statement.bindString(11, entity.getLinkedHabitIds());
        statement.bindString(12, entity.getColorHex());
        statement.bindLong(13, entity.getEnergyLastUpdated());
        final int _tmp = entity.isArchived() ? 1 : 0;
        statement.bindLong(14, _tmp);
        statement.bindString(15, entity.getId());
      }
    };
    this.__updateAdapterOfMilestoneEntity = new EntityDeletionOrUpdateAdapter<MilestoneEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `milestones` SET `id` = ?,`goalId` = ?,`title` = ?,`targetPercent` = ?,`completedDate` = ?,`isCompleted` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MilestoneEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getGoalId());
        statement.bindString(3, entity.getTitle());
        statement.bindDouble(4, entity.getTargetPercent());
        if (entity.getCompletedDate() == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, entity.getCompletedDate());
        }
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindString(7, entity.getId());
      }
    };
  }

  @Override
  public void insertGoal(final GoalEntity goal) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfGoalEntity.insert(goal);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertMilestones(final List<MilestoneEntity> milestones) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfMilestoneEntity.insert(milestones);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void insertCheckIn(final CheckInEntity checkIn) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfCheckInEntity.insert(checkIn);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateGoal(final GoalEntity goal) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfGoalEntity.handle(goal);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void updateMilestone(final MilestoneEntity milestone) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __updateAdapterOfMilestoneEntity.handle(milestone);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public Flow<List<GoalEntity>> getActiveGoals() {
    final String _sql = "SELECT * FROM goals WHERE isArchived = 0 ORDER BY startDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<GoalEntity>>() {
      @Override
      @NonNull
      public List<GoalEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfWhyStatement = CursorUtil.getColumnIndexOrThrow(_cursor, "whyStatement");
          final int _cursorIndexOfGoalType = CursorUtil.getColumnIndexOrThrow(_cursor, "goalType");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfCompletedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completedDate");
          final int _cursorIndexOfLinkedHabitIds = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedHabitIds");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfEnergyLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "energyLastUpdated");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final List<GoalEntity> _result = new ArrayList<GoalEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GoalEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpWhyStatement;
            _tmpWhyStatement = _cursor.getString(_cursorIndexOfWhyStatement);
            final GoalType _tmpGoalType;
            _tmpGoalType = __GoalType_stringToEnum(_cursor.getString(_cursorIndexOfGoalType));
            final Float _tmpTargetValue;
            if (_cursor.isNull(_cursorIndexOfTargetValue)) {
              _tmpTargetValue = null;
            } else {
              _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            }
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpTargetDate;
            _tmpTargetDate = _cursor.getLong(_cursorIndexOfTargetDate);
            final Long _tmpCompletedDate;
            if (_cursor.isNull(_cursorIndexOfCompletedDate)) {
              _tmpCompletedDate = null;
            } else {
              _tmpCompletedDate = _cursor.getLong(_cursorIndexOfCompletedDate);
            }
            final String _tmpLinkedHabitIds;
            _tmpLinkedHabitIds = _cursor.getString(_cursorIndexOfLinkedHabitIds);
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final long _tmpEnergyLastUpdated;
            _tmpEnergyLastUpdated = _cursor.getLong(_cursorIndexOfEnergyLastUpdated);
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            _item = new GoalEntity(_tmpId,_tmpTitle,_tmpWhyStatement,_tmpGoalType,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpStartDate,_tmpTargetDate,_tmpCompletedDate,_tmpLinkedHabitIds,_tmpColorHex,_tmpEnergyLastUpdated,_tmpIsArchived);
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
  public Flow<GoalEntity> getGoalById(final String goalId) {
    final String _sql = "SELECT * FROM goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, goalId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<GoalEntity>() {
      @Override
      @Nullable
      public GoalEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfWhyStatement = CursorUtil.getColumnIndexOrThrow(_cursor, "whyStatement");
          final int _cursorIndexOfGoalType = CursorUtil.getColumnIndexOrThrow(_cursor, "goalType");
          final int _cursorIndexOfTargetValue = CursorUtil.getColumnIndexOrThrow(_cursor, "targetValue");
          final int _cursorIndexOfCurrentValue = CursorUtil.getColumnIndexOrThrow(_cursor, "currentValue");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfTargetDate = CursorUtil.getColumnIndexOrThrow(_cursor, "targetDate");
          final int _cursorIndexOfCompletedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completedDate");
          final int _cursorIndexOfLinkedHabitIds = CursorUtil.getColumnIndexOrThrow(_cursor, "linkedHabitIds");
          final int _cursorIndexOfColorHex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorHex");
          final int _cursorIndexOfEnergyLastUpdated = CursorUtil.getColumnIndexOrThrow(_cursor, "energyLastUpdated");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final GoalEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpWhyStatement;
            _tmpWhyStatement = _cursor.getString(_cursorIndexOfWhyStatement);
            final GoalType _tmpGoalType;
            _tmpGoalType = __GoalType_stringToEnum(_cursor.getString(_cursorIndexOfGoalType));
            final Float _tmpTargetValue;
            if (_cursor.isNull(_cursorIndexOfTargetValue)) {
              _tmpTargetValue = null;
            } else {
              _tmpTargetValue = _cursor.getFloat(_cursorIndexOfTargetValue);
            }
            final float _tmpCurrentValue;
            _tmpCurrentValue = _cursor.getFloat(_cursorIndexOfCurrentValue);
            final String _tmpUnit;
            _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpTargetDate;
            _tmpTargetDate = _cursor.getLong(_cursorIndexOfTargetDate);
            final Long _tmpCompletedDate;
            if (_cursor.isNull(_cursorIndexOfCompletedDate)) {
              _tmpCompletedDate = null;
            } else {
              _tmpCompletedDate = _cursor.getLong(_cursorIndexOfCompletedDate);
            }
            final String _tmpLinkedHabitIds;
            _tmpLinkedHabitIds = _cursor.getString(_cursorIndexOfLinkedHabitIds);
            final String _tmpColorHex;
            _tmpColorHex = _cursor.getString(_cursorIndexOfColorHex);
            final long _tmpEnergyLastUpdated;
            _tmpEnergyLastUpdated = _cursor.getLong(_cursorIndexOfEnergyLastUpdated);
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            _result = new GoalEntity(_tmpId,_tmpTitle,_tmpWhyStatement,_tmpGoalType,_tmpTargetValue,_tmpCurrentValue,_tmpUnit,_tmpStartDate,_tmpTargetDate,_tmpCompletedDate,_tmpLinkedHabitIds,_tmpColorHex,_tmpEnergyLastUpdated,_tmpIsArchived);
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
  public Flow<List<MilestoneEntity>> getMilestonesForGoal(final String goalId) {
    final String _sql = "SELECT * FROM milestones WHERE goalId = ? ORDER BY targetPercent ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, goalId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"milestones"}, new Callable<List<MilestoneEntity>>() {
      @Override
      @NonNull
      public List<MilestoneEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "goalId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfTargetPercent = CursorUtil.getColumnIndexOrThrow(_cursor, "targetPercent");
          final int _cursorIndexOfCompletedDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completedDate");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final List<MilestoneEntity> _result = new ArrayList<MilestoneEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MilestoneEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpGoalId;
            _tmpGoalId = _cursor.getString(_cursorIndexOfGoalId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final float _tmpTargetPercent;
            _tmpTargetPercent = _cursor.getFloat(_cursorIndexOfTargetPercent);
            final Long _tmpCompletedDate;
            if (_cursor.isNull(_cursorIndexOfCompletedDate)) {
              _tmpCompletedDate = null;
            } else {
              _tmpCompletedDate = _cursor.getLong(_cursorIndexOfCompletedDate);
            }
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            _item = new MilestoneEntity(_tmpId,_tmpGoalId,_tmpTitle,_tmpTargetPercent,_tmpCompletedDate,_tmpIsCompleted);
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
  public Flow<List<CheckInEntity>> getCheckInsForGoal(final String goalId) {
    final String _sql = "SELECT * FROM check_ins WHERE goalId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, goalId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"check_ins"}, new Callable<List<CheckInEntity>>() {
      @Override
      @NonNull
      public List<CheckInEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "goalId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<CheckInEntity> _result = new ArrayList<CheckInEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CheckInEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpGoalId;
            _tmpGoalId = _cursor.getString(_cursorIndexOfGoalId);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final CheckInStatus _tmpStatus;
            _tmpStatus = __CheckInStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new CheckInEntity(_tmpId,_tmpGoalId,_tmpDate,_tmpStatus,_tmpNote);
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

  private String __GoalType_enumToString(@NonNull final GoalType _value) {
    switch (_value) {
      case BUILD: return "BUILD";
      case REACH: return "REACH";
      case QUIT: return "QUIT";
      case MAINTAIN: return "MAINTAIN";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private String __CheckInStatus_enumToString(@NonNull final CheckInStatus _value) {
    switch (_value) {
      case ON_TRACK: return "ON_TRACK";
      case FELL_BEHIND: return "FELL_BEHIND";
      case CRUSHED_IT: return "CRUSHED_IT";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private GoalType __GoalType_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "BUILD": return GoalType.BUILD;
      case "REACH": return GoalType.REACH;
      case "QUIT": return GoalType.QUIT;
      case "MAINTAIN": return GoalType.MAINTAIN;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }

  private CheckInStatus __CheckInStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "ON_TRACK": return CheckInStatus.ON_TRACK;
      case "FELL_BEHIND": return CheckInStatus.FELL_BEHIND;
      case "CRUSHED_IT": return CheckInStatus.CRUSHED_IT;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
