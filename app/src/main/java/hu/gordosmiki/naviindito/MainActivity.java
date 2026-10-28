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
 *  - gyújtásráadás után (ébredés), és
 *  - a NAVI gomb megnyomásakor.
 * A két indítás Intentje egyforma, ezért a folyamat korából döntünk:
 * gyújtáslevételkor a gyári rendszer leállítja az appokat, így ébredéskor
 * ez a folyamat mindig frissen indul. NAVI gombnál a folyamat már él.
 *
 * Ébredés:  a beállított parancsikon indítása (Automate "Gombfigyelő"),
 *           majd vissza a kezdőképernyőre (AGAMA).
 * NAVI gomb: a beállított navigációs app indítása.
 *
 * A képernyőn semmi nem jelenik meg (átlátszó Activity).
 */
public class MainActivity extends Activity {

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
            Store.log(this, "NAVI GOMB (" + how + ", folyamat kora " + age + ")");
            onNavi();
        }
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
        // Kis várakozás, hogy a parancsikon lefusson, aztán vissza a kezdőképernyőre.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
                goHome();
                finish();
            }
        }, 1500);
    }

    private void onNavi() {
        String pkg = Store.prefs(this).getString(Store.KEY_NAVI_PKG, null);
        if (pkg == null) {
            Store.log(this, "  nincs beállított navigációs app, beállítások megnyitása");
            startActivity(new Intent(this, SettingsActivity.class));
            finish();
            return;
        }
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            Store.log(this, "  HIBA: nem indítható: " + pkg);
            goHome();
        } else {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(launch);
                Store.log(this, "  navi indítva: " + pkg);
            } catch (Exception e) {
                Store.log(this, "  HIBA a navi indításakor: " + e);
            }
        }
        finish();
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
