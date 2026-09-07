package com.saintnico.verdlyhabits.data.local;

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
import com.saintnico.verdlyhabits.data.local.focus.FocusSessionDao;
import com.saintnico.verdlyhabits.data.local.focus.FocusSessionDao_Impl;
import com.saintnico.verdlyhabits.data.local.goals.GoalsDao;
import com.saintnico.verdlyhabits.data.local.goals.GoalsDao_Impl;
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
  private volatile GoalsDao _goalsDao;

  private volatile FocusSessionDao _focusSessionDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `whyStatement` TEXT NOT NULL, `goalType` TEXT NOT NULL, `targetValue` REAL, `currentValue` REAL NOT NULL, `unit` TEXT NOT NULL, `startDate` INTEGER NOT NULL, `targetDate` INTEGER NOT NULL, `completedDate` INTEGER, `linkedHabitIds` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `energyLastUpdated` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `milestones` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `title` TEXT NOT NULL, `targetPercent` REAL NOT NULL, `completedDate` INTEGER, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `check_ins` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `date` INTEGER NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `habitId` TEXT, `durationMinutes` INTEGER NOT NULL, `completedAt` INTEGER NOT NULL, `xpEarned` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e34c733e2492410c928fb811b9cabae7')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `goals`");
        db.execSQL("DROP TABLE IF EXISTS `milestones`");
        db.execSQL("DROP TABLE IF EXISTS `check_ins`");
        db.execSQL("DROP TABLE IF EXISTS `focus_sessions`");
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
        final HashMap<String, TableInfo.Column> _columnsGoals = new HashMap<String, TableInfo.Column>(14);
        _columnsGoals.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("whyStatement", new TableInfo.Column("whyStatement", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("goalType", new TableInfo.Column("goalType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("targetValue", new TableInfo.Column("targetValue", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("currentValue", new TableInfo.Column("currentValue", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("unit", new TableInfo.Column("unit", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("startDate", new TableInfo.Column("startDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("targetDate", new TableInfo.Column("targetDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("completedDate", new TableInfo.Column("completedDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("linkedHabitIds", new TableInfo.Column("linkedHabitIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("colorHex", new TableInfo.Column("colorHex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("energyLastUpdated", new TableInfo.Column("energyLastUpdated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("isArchived", new TableInfo.Column("isArchived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGoals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGoals = new TableInfo("goals", _columnsGoals, _foreignKeysGoals, _indicesGoals);
        final TableInfo _existingGoals = TableInfo.read(db, "goals");
        if (!_infoGoals.equals(_existingGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "goals(com.saintnico.verdlyhabits.data.local.goals.GoalEntity).\n"
                  + " Expected:\n" + _infoGoals + "\n"
                  + " Found:\n" + _existingGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsMilestones = new HashMap<String, TableInfo.Column>(6);
        _columnsMilestones.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMilestones.put("goalId", new TableInfo.Column("goalId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMilestones.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMilestones.put("targetPercent", new TableInfo.Column("targetPercent", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMilestones.put("completedDate", new TableInfo.Column("completedDate", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMilestones.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMilestones = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMilestones = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMilestones = new TableInfo("milestones", _columnsMilestones, _foreignKeysMilestones, _indicesMilestones);
        final TableInfo _existingMilestones = TableInfo.read(db, "milestones");
        if (!_infoMilestones.equals(_existingMilestones)) {
          return new RoomOpenHelper.ValidationResult(false, "milestones(com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity).\n"
                  + " Expected:\n" + _infoMilestones + "\n"
                  + " Found:\n" + _existingMilestones);
        }
        final HashMap<String, TableInfo.Column> _columnsCheckIns = new HashMap<String, TableInfo.Column>(5);
        _columnsCheckIns.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("goalId", new TableInfo.Column("goalId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCheckIns = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCheckIns = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCheckIns = new TableInfo("check_ins", _columnsCheckIns, _foreignKeysCheckIns, _indicesCheckIns);
        final TableInfo _existingCheckIns = TableInfo.read(db, "check_ins");
        if (!_infoCheckIns.equals(_existingCheckIns)) {
          return new RoomOpenHelper.ValidationResult(false, "check_ins(com.saintnico.verdlyhabits.data.local.goals.CheckInEntity).\n"
                  + " Expected:\n" + _infoCheckIns + "\n"
                  + " Found:\n" + _existingCheckIns);
        }
        final HashMap<String, TableInfo.Column> _columnsFocusSessions = new HashMap<String, TableInfo.Column>(5);
        _columnsFocusSessions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("habitId", new TableInfo.Column("habitId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("durationMinutes", new TableInfo.Column("durationMinutes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFocusSessions.put("xpEarned", new TableInfo.Column("xpEarned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFocusSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFocusSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFocusSessions = new TableInfo("focus_sessions", _columnsFocusSessions, _foreignKeysFocusSessions, _indicesFocusSessions);
        final TableInfo _existingFocusSessions = TableInfo.read(db, "focus_sessions");
        if (!_infoFocusSessions.equals(_existingFocusSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "focus_sessions(com.saintnico.verdlyhabits.data.local.focus.FocusSessionEntity).\n"
                  + " Expected:\n" + _infoFocusSessions + "\n"
                  + " Found:\n" + _existingFocusSessions);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e34c733e2492410c928fb811b9cabae7", "500728b1a4c01791f099cc90b6867ad1");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "goals","milestones","check_ins","focus_sessions");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `goals`");
      _db.execSQL("DELETE FROM `milestones`");
      _db.execSQL("DELETE FROM `check_ins`");
      _db.execSQL("DELETE FROM `focus_sessions`");
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
    _typeConvertersMap.put(GoalsDao.class, GoalsDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FocusSessionDao.class, FocusSessionDao_Impl.getRequiredConverters());
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
  public GoalsDao goalsDao() {
    if (_goalsDao != null) {
      return _goalsDao;
    } else {
      synchronized(this) {
        if(_goalsDao == null) {
          _goalsDao = new GoalsDao_Impl(this);
        }
        return _goalsDao;
      }
    }
  }

  @Override
  public FocusSessionDao focusSessionDao() {
    if (_focusSessionDao != null) {
      return _focusSessionDao;
    } else {
      synchronized(this) {
        if(_focusSessionDao == null) {
          _focusSessionDao = new FocusSessionDao_Impl(this);
        }
        return _focusSessionDao;
      }
    }
  }
}
