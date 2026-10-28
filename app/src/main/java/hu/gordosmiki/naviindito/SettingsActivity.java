package hu.gordosmiki.naviindito;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Beállítóképernyő: navigációs app, ébredési parancsikon, napló, próbagombok. */
public class SettingsActivity extends Activity {

    private static final int REQ_SHORTCUT = 1;

    private TextView status;
    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        root.setPadding(pad, pad, pad, pad);

        status = new TextView(this);
        status.setTextSize(15);
        root.addView(status);

        LinearLayout row1 = new LinearLayout(this);
        row1.addView(button("Navi app választása", new View.OnClickListener() {
            @Override public void onClick(View v) { pickNaviApp(); }
        }));
        row1.addView(button("Ébredési parancsikon", new View.OnClickListener() {
            @Override public void onClick(View v) { pickShortcutProvider(); }
        }));
        root.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.addView(button("Navi próba", new View.OnClickListener() {
            @Override public void onClick(View v) { testNavi(); }
        }));
        row2.addView(button("Parancsikon próba", new View.OnClickListener() {
            @Override public void onClick(View v) { testShortcut(); }
        }));
        row2.addView(button("Napló törlése", new View.OnClickListener() {
            @Override public void onClick(View v) { Store.clearLog(SettingsActivity.this); refresh(); }
        }));
        root.addView(row2);

        ScrollView scroll = new ScrollView(this);
        logView = new TextView(this);
        logView.setTextSize(13);
        logView.setTextIsSelectable(true);
        scroll.addView(logView);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void refresh() {
        String navi = Store.prefs(this).getString(Store.KEY_NAVI_LABEL, null);
        String sc = Store.prefs(this).getString(Store.KEY_WAKE_SHORTCUT_LABEL, null);
        status.setText("Navigációs app: " + (navi == null ? "(nincs kiválasztva)" : navi)
                + "\nÉbredési parancsikon: " + (sc == null ? "(nincs kiválasztva)" : sc));
        String log = Store.readLog(this);
        logView.setText(log.isEmpty() ? "(üres napló)" : log);
    }

    // ---- Navigációs app kiválasztása ----

    private void pickNaviApp() {
        final PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = new ArrayList<>(pm.queryIntentActivities(main, 0));
        // saját magunkat kihagyjuk
        for (int i = apps.size() - 1; i >= 0; i--) {
            if (getPackageName().equals(apps.get(i).activityInfo.packageName)) apps.remove(i);
        }
        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString().compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });
        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) names[i] = apps.get(i).loadLabel(pm).toString();

        new AlertDialog.Builder(this)
                .setTitle("Melyik legyen a navigációs app?")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        ResolveInfo ri = apps.get(which);
                        Store.prefs(SettingsActivity.this).edit()
                                .putString(Store.KEY_NAVI_PKG, ri.activityInfo.packageName)
                                .putString(Store.KEY_NAVI_LABEL, ri.loadLabel(pm).toString())
                                .apply();
                        Store.log(SettingsActivity.this, "beállítva navi app: " + ri.activityInfo.packageName);
                        refresh();
                    }
                })
                .show();
    }

    // ---- Ébredési parancsikon kiválasztása (pl. Automate folyamat) ----

    private void pickShortcutProvider() {
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> providers =
                new ArrayList<>(pm.queryIntentActivities(new Intent(Intent.ACTION_CREATE_SHORTCUT), 0));
        if (providers.isEmpty()) {
            new AlertDialog.Builder(this).setMessage("Nem található parancsikont kínáló app.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        String[] names = new String[providers.size()];
        for (int i = 0; i < providers.size(); i++) {
            ResolveInfo ri = providers.get(i);
            names[i] = ri.loadLabel(pm) + "  (" + ri.activityInfo.packageName + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Parancsikon típusa (Automate: folyamat)")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        ResolveInfo ri = providers.get(which);
                        Intent i = new Intent(Intent.ACTION_CREATE_SHORTCUT);
                        i.setComponent(new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name));
                        try {
                            startActivityForResult(i, REQ_SHORTCUT);
                        } catch (Exception e) {
                            Store.log(SettingsActivity.this, "HIBA a parancsikon-választónál: " + e);
                            refresh();
                        }
                    }
                })
                .show();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_SHORTCUT) return;
        if (resultCode != RESULT_OK || data == null) {
            Store.log(this, "parancsikon választás megszakítva");
            refresh();
            return;
        }
        Intent shortcut = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        if (shortcut == null) {
            Store.log(this, "HIBA: a választott app nem adott vissza parancsikont");
            refresh();
            return;
        }
        Store.prefs(this).edit()
                .putString(Store.KEY_WAKE_SHORTCUT, shortcut.toUri(0))
                .putString(Store.KEY_WAKE_SHORTCUT_LABEL, name == null ? "(névtelen)" : name)
                .apply();
        Store.log(this, "beállítva ébredési parancsikon: " + name);
        refresh();
    }

    // ---- Próbák ----

    private void testNavi() {
        String pkg = Store.prefs(this).getString(Store.KEY_NAVI_PKG, null);
        if (pkg == null) { pickNaviApp(); return; }
        Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) { Store.log(this, "HIBA: nem indítható: " + pkg); refresh(); return; }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(launch);
    }

    private void testShortcut() {
        Intent sc = Store.wakeShortcut(this);
        if (sc == null) { pickShortcutProvider(); return; }
        try {
            sc.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(sc);
            Store.log(this, "parancsikon próba elindítva");
        } catch (Exception e) {
            Store.log(this, "HIBA a parancsikon próbánál: " + e);
        }
        refresh();
    }
}
