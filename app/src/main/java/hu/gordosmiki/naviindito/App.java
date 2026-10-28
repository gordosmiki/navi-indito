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
            }
        };
        IntentFilter f = new IntentFilter(ACTION_KEY);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(r, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(r, f);
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
}
