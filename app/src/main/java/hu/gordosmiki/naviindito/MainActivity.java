package hu.gordosmiki.naviindito;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

/**
 * 0.1-diag: diagnosztikai verzió.
 * Minden indításkor feljegyzi, hogyan indította el a rendszer (Intent adatai),
 * és megmutatja az utolsó bejegyzéseket. Ebből derül ki, miben különbözik
 * a NAVI gombos indítás az ébredés utáni indítástól.
 */
public class MainActivity extends Activity {

    private static final String LOG_FILE = "inditasok.txt";
    private static final int MAX_LOG_CHARS = 20000;

    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        record("onCreate", getIntent());
        refresh();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        record("onNewIntent", intent);
        refresh();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(8);
        root.setPadding(pad, pad, pad, pad);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        Button close = new Button(this);
        close.setText("Bezár");
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        Button clear = new Button(this);
        clear.setText("Napló törlése");
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new File(getFilesDir(), LOG_FILE).delete();
                refresh();
            }
        });

        buttons.addView(close);
        buttons.addView(clear);
        root.addView(buttons);

        ScrollView scroll = new ScrollView(this);
        logView = new TextView(this);
        logView.setTextSize(13);
        logView.setTextIsSelectable(true);
        scroll.addView(logView);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void record(String how, Intent intent) {
        StringBuilder sb = new StringBuilder();
        String now = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
        sb.append("=== ").append(now).append("  ").append(how).append(" ===\n");

        if (intent != null) {
            sb.append("action: ").append(intent.getAction()).append('\n');
            sb.append("categories: ").append(intent.getCategories()).append('\n');
            sb.append("flags: 0x").append(Integer.toHexString(intent.getFlags()))
              .append(' ').append(decodeFlags(intent.getFlags())).append('\n');
            Uri data = intent.getData();
            if (data != null) sb.append("data: ").append(data).append('\n');
            if (intent.getComponent() != null) {
                sb.append("component: ").append(intent.getComponent().flattenToShortString()).append('\n');
            }
            Bundle extras = intent.getExtras();
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String k : keys) {
                    Object val = extras.get(k);
                    sb.append("extra ").append(k).append(" = ").append(val);
                    if (val != null) sb.append(" (").append(val.getClass().getSimpleName()).append(')');
                    sb.append('\n');
                }
            } else {
                sb.append("extras: nincs\n");
            }
        }

        if (Build.VERSION.SDK_INT >= 22) {
            Uri ref = getReferrer();
            sb.append("referrer: ").append(ref).append('\n');
        }
        sb.append("callingPackage: ").append(getCallingPackage()).append('\n');

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && Build.VERSION.SDK_INT >= 20) {
            sb.append("képernyő be: ").append(pm.isInteractive()).append('\n');
        }

        long elapsed = SystemClock.elapsedRealtime();
        long uptime = SystemClock.uptimeMillis();
        sb.append("elapsedRealtime: ").append(elapsed / 1000).append(" s, uptime: ")
          .append(uptime / 1000).append(" s (különbség = alvással töltött idő: ")
          .append((elapsed - uptime) / 1000).append(" s)\n");
        if (Build.VERSION.SDK_INT >= 24) {
            long procAge = elapsed - Process.getStartElapsedRealtime();
            sb.append("folyamat kora: ").append(procAge / 1000.0).append(" s\n");
        }
        sb.append('\n');

        appendLog(sb.toString());
    }

    private static String decodeFlags(int f) {
        StringBuilder s = new StringBuilder("[");
        if ((f & Intent.FLAG_ACTIVITY_NEW_TASK) != 0) s.append("NEW_TASK ");
        if ((f & Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0) s.append("CLEAR_TOP ");
        if ((f & Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0) s.append("SINGLE_TOP ");
        if ((f & Intent.FLAG_ACTIVITY_CLEAR_TASK) != 0) s.append("CLEAR_TASK ");
        if ((f & Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) != 0) s.append("REORDER_TO_FRONT ");
        if ((f & Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) != 0) s.append("RESET_TASK ");
        if ((f & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) s.append("BROUGHT_TO_FRONT ");
        if ((f & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) s.append("FROM_HISTORY ");
        if ((f & Intent.FLAG_ACTIVITY_NO_ANIMATION) != 0) s.append("NO_ANIMATION ");
        if ((f & Intent.FLAG_ACTIVITY_NO_HISTORY) != 0) s.append("NO_HISTORY ");
        if ((f & Intent.FLAG_ACTIVITY_MULTIPLE_TASK) != 0) s.append("MULTIPLE_TASK ");
        if ((f & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0) s.append("EXCLUDE_RECENTS ");
        return s.toString().trim() + "]";
    }

    private void appendLog(String entry) {
        String old = readLog();
        String all = entry + old; // legújabb felül
        if (all.length() > MAX_LOG_CHARS) all = all.substring(0, MAX_LOG_CHARS);
        try {
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), LOG_FILE), false);
            out.write(all.getBytes("UTF-8"));
            out.close();
        } catch (Exception ignored) { }
    }

    private String readLog() {
        File f = new File(getFilesDir(), LOG_FILE);
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

    private void refresh() {
        String log = readLog();
        logView.setText(log.isEmpty() ? "(üres napló)" : log);
    }
}
