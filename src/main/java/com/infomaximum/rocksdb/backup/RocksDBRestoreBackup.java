package com.infomaximum.rocksdb.backup;

import com.infomaximum.database.exception.DatabaseException;
import com.infomaximum.database.utils.PathUtils;
import org.rocksdb.*;

import java.nio.file.Path;

public class RocksDBRestoreBackup {

    public static void restore(Path backupPath, Path dbPath) throws DatabaseException {
        PathUtils.checkPath(backupPath);
        PathUtils.checkPath(dbPath);

        RocksDB.loadLibrary();

        try (BackupEngineOptions dbOptions = new BackupEngineOptions(backupPath.toString());
             BackupEngine backupEngine = BackupEngine.open(Env.getDefault(), dbOptions);
             RestoreOptions restoreOptions = new RestoreOptions(false)) {

            final String dbDir = dbPath.toString();
            backupEngine.restoreDbFromLatestBackup(dbDir, dbDir, restoreOptions);
        } catch (IllegalArgumentException e) {
            throw new DatabaseException(e.getMessage());
        } catch (RocksDBException e) {
            throw new DatabaseException(e);
        }
    }
}
