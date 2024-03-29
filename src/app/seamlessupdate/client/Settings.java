package app.seamlessupdate.client;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;

public class Settings extends PreferenceActivity {
    private static final String KEY_CHANNEL = "channel";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_BATTERY_NOT_LOW = "battery_not_low";
    private static final String KEY_IDLE_REBOOT = "idle_reboot";
    private static final String KEY_CHECK_FOR_UDPATES = "check_for_updates";
    private static final String KEY_CHANGELOG = "changelog";
    static final String KEY_WAITING_FOR_REBOOT = "waiting_for_reboot";

    static SharedPreferences getPreferences(final Context context) {
        final Context deviceContext = context.createDeviceProtectedStorageContext();
        return PreferenceManager.getDefaultSharedPreferences(deviceContext);
    }

    static String migrateChannel(final String prefChannel) {
        if (prefChannel.startsWith("security-express")) return "security-express6";
        else if (prefChannel.startsWith("stable")) return "stable6";
        else if (prefChannel.startsWith("beta")) return "beta6";
        else return prefChannel;
    }

    static String getChannel(final Context context) {
        String def = context.getString(R.string.channel_default);
        return migrateChannel(getPreferences(context).getString(KEY_CHANNEL, def));
    }

    static int getNetworkType(final Context context) {
        int def = Integer.valueOf(context.getString(R.string.network_type_default));
        return getPreferences(context).getInt(KEY_NETWORK_TYPE, def);
    }

    static boolean getBatteryNotLow(final Context context) {
        boolean def = Boolean.valueOf(context.getString(R.string.battery_not_low_default));
        return getPreferences(context).getBoolean(KEY_BATTERY_NOT_LOW, def);
    }

    static boolean getIdleReboot(final Context context) {
        boolean def = Boolean.valueOf(context.getString(R.string.idle_reboot_default));
        return getPreferences(context).getBoolean(KEY_IDLE_REBOOT, def);
    }

    void refreshChannelSummary() {
        final Preference channelPref = findPreference(KEY_CHANNEL);
        final String currentChannel = getChannel(this);
        if (currentChannel.startsWith("security-express")) channelPref.setSummary(getString(R.string.channel_security_express));
        else if (currentChannel.startsWith("stable")) channelPref.setSummary(getString(R.string.channel_stable));
        else if (currentChannel.startsWith("beta")) channelPref.setSummary(getString(R.string.channel_beta));
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserManager.get(this).isSystemUser()) {
            throw new SecurityException("system user only");
        }
        getPreferenceManager().setStorageDeviceProtected();
        PreferenceManager.setDefaultValues(createDeviceProtectedStorageContext(), R.xml.settings, false);
        addPreferencesFromResource(R.xml.settings);

        final Preference checkForUpdates = findPreference(KEY_CHECK_FOR_UDPATES);
        checkForUpdates.setOnPreferenceClickListener((final Preference preference) -> {
            if (!getPreferences(this).getBoolean(KEY_WAITING_FOR_REBOOT, false)) {
                PeriodicJob.schedule(this, true);
            }
            return true;
        });

        final Preference changelog = findPreference(KEY_CHANGELOG);
        changelog.setOnPreferenceClickListener((final Preference preference) -> {
            Intent intent = getChangelogIntent(this, "current.html");
            if (intent != null) {
                startActivity(intent);
            } else {
                changelog.setSummary(getString(R.string.changelog_unavailable));
            }
            return true;
        });

        final Preference channel = findPreference(KEY_CHANNEL);
        channel.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            getPreferences(this).edit().putString(KEY_CHANNEL,(String) newValue).apply();
            refreshChannelSummary();
            if (!getPreferences(this).getBoolean(KEY_WAITING_FOR_REBOOT, false)) {
                PeriodicJob.schedule(this);
            }
            return true;
        });

        final Preference networkType = findPreference(KEY_NETWORK_TYPE);
        networkType.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            final int value = Integer.parseInt((String) newValue);
            getPreferences(this).edit().putInt(KEY_NETWORK_TYPE, value).apply();
            if (!getPreferences(this).getBoolean(KEY_WAITING_FOR_REBOOT, false)) {
                PeriodicJob.schedule(this);
            }
            return true;
        });

        final Preference batteryNotLow = findPreference(KEY_BATTERY_NOT_LOW);
        batteryNotLow.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            getPreferences(this).edit().putBoolean(KEY_BATTERY_NOT_LOW, (boolean) newValue).apply();
            if (!getPreferences(this).getBoolean(KEY_WAITING_FOR_REBOOT, false)) {
                PeriodicJob.schedule(this);
            }
            return true;
        });

        final Preference idleReboot = findPreference(KEY_IDLE_REBOOT);
        idleReboot.setOnPreferenceChangeListener((final Preference preference, final Object newValue) -> {
            final boolean value = (Boolean) newValue;
            if (!value) {
                IdleReboot.cancel(this);
            }
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        final ListPreference networkType = (ListPreference) findPreference(KEY_NETWORK_TYPE);
        networkType.setValue(Integer.toString(getNetworkType(this)));
        final Preference changelog = findPreference(KEY_CHANGELOG);
        changelog.setSummary(getString(R.string.changelog_summary));
        refreshChannelSummary();
    }

    static Intent getChangelogIntent(Context context, String filename) {
        // From com.android.settings.SettingsLicenseActivity.showHtmlFromUri()
        // Kick off external viewer due to WebView security restrictions; we
        // carefully point it at HTMLViewer, since it offers to decompress
        // before viewing.
        final File changelogPath = new File(context.getCacheDir(), "changelog");
        final File changelogFile = new File(changelogPath, filename);
        if (!changelogFile.exists()) {
            return null;
        }
        final Uri uri = FileProvider.getUriForFile(context, "app.seamlessupdate.client.fileprovider", changelogFile);
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/html");
        intent.putExtra(Intent.EXTRA_TITLE, context.getString(R.string.changelog_title));
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.htmlviewer");
        return intent;
    }
}
