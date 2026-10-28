package hu.gordosmiki.naviindito;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * Amíg az app folyamata él, figyeli a gyári rendszer gombnyomás-üzeneteit
 * (com.nwd.action.ACTION_KEY_VALUE). Így tudjuk, melyik gomb miatt indított
 * el minket a rendszer (a RADIO gombra is minket indít, mert a gyári
 * rádióapp le van tiltva).
 */
public class App extends Application {

    static final String ACTION_KEY = "com.nwd.action.ACTION_KEY_VALUE";

    static volatile int lastKey = -1;
    static volatile long lastKeyTime = 0; // elapsedRealtime
    static volatile long lastLaunchTime = 0; // a MainActivity utolsó indítása
    static volatile long lastScreenOnTime = 0; // a kijelző utolsó bekapcsolása (még fel nem használt)

    @Override
    public void onCreate() {
        super.onCreate();
        BroadcastReceiver r = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                int v = -1;
                Bundle ex = i.getExtras();
                if (ex != null) {
                    Object o = ex.get("extra_key_value");
                    if (o instanceof Number) v = ((Number) o).intValue();
                }
                lastKey = v;
                lastKeyTime = SystemClock.elapsedRealtime();
                Store.log(c, "  gombüzenet: extra_key_value = " + v);
                int radioKey = Store.prefs(c).getInt(Store.KEY_RADIO_KEY, Store.DEFAULT_RADIO_KEY);
                if (v == radioKey) launchRadio(c);
            }
        };
        BroadcastReceiver screen = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
                    lastScreenOnTime = SystemClock.elapsedRealtime();
                    Store.log(c, "kijelző BE");
                } else {
                    Store.log(c, "kijelző KI");
                }
            }
        };
        IntentFilter sf = new IntentFilter(Intent.ACTION_SCREEN_ON);
        sf.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screen, sf);

        IntentFilter f = new IntentFilter(ACTION_KEY);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(r, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(r, f);
        }
    }

    /** RADIO gomb: a kiválasztott rádióapp indítása (ehhez a "más appok fölött" engedély kell). */
    static void launchRadio(Context c) {
        String pkg = Store.prefs(c).getString(Store.KEY_RADIO_PKG, null);
        if (pkg == null) { Store.log(c, "  nincs beállított rádióapp"); return; }
        Intent launch = c.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) { Store.log(c, "  HIBA: nem indítható: " + pkg); return; }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(launch);
            boolean overlay = Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(c);
            Store.log(c, "  rádióapp indítva (háttérből, engedély: " + (overlay ? "van" : "NINCS") + ")");
        } catch (Exception e) {
            Store.log(c, "  HIBA a rádióapp indításakor: " + e);
        }
    }

    /**
     * Az utolsó gombnyomás értéke, ha az legfeljebb maxAgeMs-mal ezelőtt jött; különben -1.
     * A gombnyomást "elhasználja", így egy későbbi indítás már nem látja.
     */
    static synchronized int consumeKey(long maxAgeMs) {
        if (lastKeyTime == 0) return -1;
        int v = SystemClock.elapsedRealtime() - lastKeyTime <= maxAgeMs ? lastKey : -1;
        lastKey = -1;
        lastKeyTime = 0;
        return v;
    }

    /** Igaz, ha a kijelző nemrég (maxAgeMs-on belül) kapcsolt be; egyszer használható. */
    static synchronized boolean consumeRecentScreenOn(long maxAgeMs) {
        if (lastScreenOnTime == 0) return false;
        boolean recent = SystemClock.elapsedRealtime() - lastScreenOnTime <= maxAgeMs;
        lastScreenOnTime = 0;
        return recent;
    }
}
