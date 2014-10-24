package com.chitacan.bridge;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.FileBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * Created by chitacan on 14. 10. 24..
 */
public class BridgeBackupAgent extends BackupAgentHelper{

    static final Object sDataLock = new Object();
    static final String FILES_BACKUP_KEY = "bridge";

    @Override
    public void onCreate() {
        FileBackupHelper helper = new FileBackupHelper(this, ServerProvider.DATABASE_NAME);
        addHelper(FILES_BACKUP_KEY, helper);
        super.onCreate();
    }

    @Override
    public File getFilesDir() {
        File path = getDatabasePath(ServerProvider.DATABASE_NAME);
        return path.getParentFile();
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        synchronized (this.sDataLock) {
            super.onBackup(oldState, data, newState);
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        synchronized (this.sDataLock) {
            super.onRestore(data, appVersionCode, newState);
        }
    }
}
