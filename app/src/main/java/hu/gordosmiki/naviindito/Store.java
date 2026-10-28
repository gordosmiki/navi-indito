package hu.gordosmiki.naviindito;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Beállítások és napló közös kezelése. */
final class Store {

    static final String KEY_NAVI_PKG = "navi_pkg";
    static final String KEY_NAVI_LABEL = "navi_label";
    static final String KEY_WAKE_SHORTCUT = "wake_shortcut";
    static final String KEY_WAKE_SHORTCUT_LABEL = "wake_shortcut_label";
    static final String KEY_WAKE_LIMIT_MS = "wake_limit_ms";

    static final long DEFAULT_WAKE_LIMIT_MS = 3000;

    private static final String LOG_FILE = "naplo.txt";
    private static final int MAX_LOG_CHARS = 15000;

    private Store() { }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("beallitasok", Context.MODE_PRIVATE);
    }

    static Intent wakeShortcut(Context c) {
        String uri = prefs(c).getString(KEY_WAKE_SHORTCUT, null);
        if (uri == null) return null;
        try {
            return Intent.parseUri(uri, 0);
        } catch (Exception e) {
            return null;
        }
    }

    static void log(Context c, String line) {
        String now = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
        String all = now + "  " + line + "\n" + readLog(c);
        if (all.length() > MAX_LOG_CHARS) all = all.substring(0, MAX_LOG_CHARS);
        try {
            FileOutputStream out = new FileOutputStream(new File(c.getFilesDir(), LOG_FILE), false);
            out.write(all.getBytes("UTF-8"));
            out.close();
        } catch (Exception ignored) { }
    }

    static String readLog(Context c) {
        File f = new File(c.getFilesDir(), LOG_FILE);
        if (!f.exists()) return "";
        try {
            FileInputStream in = new FileInputStream(f);
            byte[] buf = new byte[(int) f.length()];
            int n = in.read(buf);
            in.close();
            return n > 0 ? new String(buf, 0, n, "UTF-8") : "";
        } catch (Exception e) {
            return "";
        }
    }

    static void clearLog(Context c) {
        new File(c.getFilesDir(), LOG_FILE).delete();
    }
}
