package app.seamlessupdate.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (context.getSystemService(UserManager.class).isSystemUser()) {
            try {
                if (Settings.shouldShowNewUpdaterNotification(context)) {
                    new NotificationHandler(context).showNewUpdaterNotification();
                }

                // Mark the notification as shown even if we did not need to show it, because
                // users who didn't change any of this before but who visit the settings page now
                // will be informed at that point that these are legacy settings; we don't need
                // to tell them on the next boot if the info was already presented to them.
                Settings.getPreferences(context).edit()
                        .putBoolean(Settings.KEY_LEGACY_UPDATER_NOTIFICATION_SHOWN, true)
                        .apply();
            } catch (Exception e) {
                Log.e(TAG, "Failed to display notification about new updater", e);
            }
            Settings.getPreferences(context).edit().putBoolean(Settings.KEY_WAITING_FOR_REBOOT, false).apply();
            PeriodicJob.schedule(context);
        } else {
            context.getPackageManager().setApplicationEnabledSetting(context.getPackageName(),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
        }
    }
}
