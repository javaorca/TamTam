package org.telegram.messenger;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;

import java.io.File;
import java.util.Locale;
import java.util.Map;

/**
 * @author javaorca
 */

public class LockStorage {

    private static volatile LockStorage Instance = null;
    private DispatchQueue storageQueue = new DispatchQueue("lockStorageQueue");
    private SQLiteDatabase database;
    private File cacheFile;

    public static LockStorage getInstance() {
        LockStorage localInstance = Instance;
        if (localInstance == null) {
            synchronized (LockStorage.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LockStorage();
                }
            }
        }
        return localInstance;
    }

    public LockStorage() {
        storageQueue.setPriority(Thread.MAX_PRIORITY);
        openDatabase();
    }

    public void openDatabase() {
        cacheFile = new File(ApplicationLoader.getFilesDirFixed(), "cache4.1.db");

        boolean createTable = false;
        //cacheFile.delete();
        if (!cacheFile.exists()) {
            createTable = true;
        }
        try {
            database = new SQLiteDatabase(cacheFile.getPath());
            database.executeFast("PRAGMA secure_delete = ON").stepThis().dispose();
            database.executeFast("PRAGMA temp_store = 1").stepThis().dispose();
            if (createTable) {
                database.executeFast("CREATE TABLE IF NOT EXISTS locked_dialogs(dialog_id INTEGER PRIMARY KEY, password TEXT);").stepThis().dispose();
                database.executeFast("PRAGMA user_version = 1").stepThis().dispose();
            } else {
                int version = database.executeInt("PRAGMA user_version");
                if (version < 1) {
//                    updateDbToLastVersion(version);
                }
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    public void loadLockedDialogs(Map<String, Long> ids, Map<Long, String> codes) {
        try {
            SQLiteCursor cursor = database.queryFinalized("SELECT d.dialog_id, d.password FROM locked_dialogs d");
            while (cursor.next()) {
                Long dialog_id = cursor.longValue(0);
                String password = cursor.stringValue(1);

                ids.put(password, dialog_id);
                codes.put(dialog_id, password);
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
    }

    public void cleanup() {
        storageQueue.cleanupQueue();
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (database != null) {
                    database.close();
                    database = null;
                }
                if (cacheFile != null) {
                    cacheFile.delete();
                    cacheFile = null;
                }
                openDatabase();
            }
        });
    }

    public void unlockDialog(final long dialog_id) {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    database.executeFast(String.format(Locale.US, "DELETE FROM locked_dialogs WHERE dialog_id = %d", dialog_id)).stepThis().dispose();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });
    }

    public void lockDialog(final long dialog_id, final String password) {
        storageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    database.executeFast(String.format(Locale.US, "REPLACE INTO locked_dialogs VALUES(%d, %s)", dialog_id, password)).stepThis().dispose();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });
    }

}
