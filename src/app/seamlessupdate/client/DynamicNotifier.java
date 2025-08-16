package app.seamlessupdate.client;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

class DynamicNotifier {

    private static final String TAG = "DynamicNotifier";
    private static final String URL = "https://grobox.de/tmp/test";
    private final NotificationHandler notificationHandler;

    DynamicNotifier(NotificationHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    void checkAndNotify() {
        Log.d(TAG, "fetching dynamic data from " + URL);
        try (InputStream inputStream = fetchData();
             BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String title = br.readLine();
            String text = br.readLine();
            String url = br.readLine();
            notify(title, text, url);
        } catch (Throwable e) {
            Log.e(TAG, "Error getting dynamic data: ", e);
        }
    }

    private InputStream fetchData() throws IOException {
        final URL url = new URL(URL);
        return url.openConnection().getInputStream();
    }

    private void notify(String title, String text, String url) {
        notificationHandler.showReinstallNotification(title, text, url);
    }
}
