package org.telegram.messenger;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author javaorca
 */

public class LockController {

    private static volatile LockController Instance = null;

    private ConcurrentHashMap<String, Long> lockDialogIds = new ConcurrentHashMap<>(10, 1.0f, 2);
    private ConcurrentHashMap<Long, String> lockDialogCodes = new ConcurrentHashMap<>(10, 1.0f, 2);

    public static LockController getInstance() {
        LockController localInstance = Instance;
        if (localInstance == null) {
            synchronized (LockController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new LockController();
                }
            }
        }
        return localInstance;
    }

    public LockController() {
        LockStorage.getInstance().loadLockedDialogs(lockDialogIds, lockDialogCodes);
    }

    public void putLockDialog(final long dialog_id, final String password) {
        lockDialogCodes.put(dialog_id, password);
        lockDialogIds.put(password, dialog_id);
    }

    public void lockDialog(final long dialog_id, final String password) {
        LockStorage.getInstance().lockDialog(dialog_id, password);

        String oldPass = lockDialogCodes.get(dialog_id);
        if (oldPass != null) {
            lockDialogIds.remove(oldPass);
        }

        putLockDialog(dialog_id, password);

        TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
        if (dialog != null) {
            MessagesController.getInstance().dialogs.remove(dialog);
        }

        NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatLockSettingsUpdated);
    }

    public void unlockDialog(final long dialog_id) {
        LockStorage.getInstance().unlockDialog(dialog_id);
        String password = lockDialogCodes.get(dialog_id);
        if (password != null) {
            lockDialogIds.remove(password);
            lockDialogCodes.remove(dialog_id);

            TLRPC.TL_dialog dialog = MessagesController.getInstance().dialogs_dict.get(dialog_id);
            if (dialog != null && !MessagesController.getInstance().dialogs.contains(dialog)) {
                MessagesController.getInstance().dialogs.add(0, dialog);
            }

            NotificationCenter.getInstance().postNotificationName(NotificationCenter.chatLockSettingsUpdated);
        }
    }

    public Long getLockedDialogId(String password) {
        return lockDialogIds.get(password);
    }

    public boolean isDialogLocked(long dialog_id) {
        return lockDialogCodes.containsKey(dialog_id);
    }

    public boolean isPasswordExist(String password) {
        return lockDialogIds.containsKey(password);
    }

    public void addNonlockedDialogs(ConcurrentHashMap<Long, TLRPC.TL_dialog> dialogs_dict, ArrayList<TLRPC.TL_dialog> dialogs) {
        if (lockDialogIds.isEmpty()) {
            dialogs.addAll(dialogs_dict.values());
        } else {
            for (Map.Entry<Long, TLRPC.TL_dialog> dialog : dialogs_dict.entrySet()) {
                if (!lockDialogCodes.containsKey(dialog.getKey())) {
                    dialogs.add(dialog.getValue());
                }
            }
        }
    }

    public void cleanup() {
        lockDialogIds.clear();
        lockDialogCodes.clear();
    }
}
