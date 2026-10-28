package hu.gordosmiki.naviindito;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

/**
 * Ezt indítja a gyári rendszer (com.nwd.kernel) navigációs programként:
 *  - gyújtásráadás után (ébredés),
 *  - a NAVI gomb megnyomásakor,
 *  - és a RADIO gombra is (mert a gyári rádióapp le van tiltva).
 *
 * Ébredés: a folyamat frissen indult (a gyári rendszer gyújtáslevételkor
 *          leállítja az appokat) -> parancsikon (Automate), majd kezdőképernyő.
 * Egyébként: a gombüzenetből (App) eldöntjük, melyik gomb volt:
 *          RADIO -> rádióapp, minden más -> navigációs app.
 */
public class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handle(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handle(false);
    }

    private void handle(boolean viaCreate) {
        long ageMs = -1;
        if (Build.VERSION.SDK_INT >= 24) {
            ageMs = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime();
        }
        long limit = Store.prefs(this).getLong(Store.KEY_WAKE_LIMIT_MS, Store.DEFAULT_WAKE_LIMIT_MS);
        boolean wake = viaCreate && (ageMs < 0 || ageMs < limit);

        String how = viaCreate ? "onCreate" : "onNewIntent";
        String age = ageMs < 0 ? "?" : String.format(java.util.Locale.US, "%.1f s", ageMs / 1000.0);

        if (wake) {
            Store.log(this, "ÉBREDÉS (" + how + ", folyamat kora " + age + ")");
            onWake();
        } else {
            Store.log(this, "GOMB indított (" + how + ", folyamat kora " + age + ")");
            // A gombüzenet és az indítás sorrendje nem biztos, ezért várunk egy kicsit.
            handler.postDelayed(new Runnable() {
                @Override public void run() { decideButton(); }
            }, 400);
        }
    }

    private void decideButton() {
        int key = App.consumeKey(1500);
        int radioKey = Store.prefs(this).getInt(Store.KEY_RADIO_KEY, Store.DEFAULT_RADIO_KEY);
        if (key == radioKey) {
            Store.log(this, "  RADIO gomb (" + key + ")");
            launchStored(Store.KEY_RADIO_PKG, "rádióapp");
        } else {
            Store.log(this, "  NAVI gomb (utolsó gombérték: " + key + ")");
            launchStored(Store.KEY_NAVI_PKG, "navi");
        }
        finish();
    }

    private void onWake() {
        Intent sc = Store.wakeShortcut(this);
        if (sc != null) {
            try {
                sc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(sc);
                Store.log(this, "  parancsikon elindítva: "
                        + Store.prefs(this).getString(Store.KEY_WAKE_SHORTCUT_LABEL, "?"));
            } catch (Exception e) {
                Store.log(this, "  HIBA a parancsikon indításakor: " + e);
            }
        } else {
            Store.log(this, "  nincs beállított parancsikon");
        }
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                goHome();
                finish();
            }
        }, 1500);
    }

    private void launchStored(String prefKey, String what) {
        String pkg = Store.prefs(this).getString(prefKey, null);
        if (pkg == null) {
            Store.log(this, "  nincs beállított " + what + ", beállítások megnyitása");
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            Store.log(this, "  HIBA: nem indítható: " + pkg);
            goHome();
            return;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(launch);
            Store.log(this, "  " + what + " indítva: " + pkg);
        } catch (Exception e) {
            Store.log(this, "  HIBA (" + what + "): " + e);
        }
    }

    private void goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(home);
        } catch (Exception e) {
            Store.log(this, "  HIBA a kezdőképernyő indításakor: " + e);
        }
    }
}
