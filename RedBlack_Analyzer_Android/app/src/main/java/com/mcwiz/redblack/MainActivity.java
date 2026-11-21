package com.mcwiz.redblack;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(12, 14, 20);
    private static final int PANEL = Color.rgb(25, 28, 38);
    private static final int RED = Color.rgb(235, 55, 70);
    private static final int BLACK = Color.rgb(80, 88, 105);
    private static final int WHITE = Color.rgb(245, 246, 250);
    private final ArrayList<String> rounds = new ArrayList<>();
    private SharedPreferences prefs;
    private LinearLayout root;
    private TextView totalView, redView, blackView, streakView, recentView, trendView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("redblack_data", MODE_PRIVATE);
        String saved = prefs.getString("rounds", "");
        if (!saved.isEmpty()) {
            for (String s : saved.split(",")) if (s.equals("R") || s.equals("B")) rounds.add(s);
        }
        buildUi();
        refresh();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(7), 0, dp(7)); c.setLayoutParams(p);
        return c;
    }
    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(26)); root.setBackgroundColor(BG);
        scroll.addView(root); setContentView(scroll);

        TextView title = text("SPORTYBET", 13, RED, true);
        root.addView(title);
        TextView heading = text("RED BLACK ANALYZER", 23, WHITE, true);
        root.addView(heading);
        TextView subtitle = text("Rekodi matokeo na angalia takwimu za historia.", 13, 0xffb7bdca, false);
        root.addView(subtitle);

        LinearLayout stats = new LinearLayout(this); stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, dp(14), 0, dp(4)); root.addView(stats);
        LinearLayout redCard = statCard("RED", RED);
        LinearLayout blackCard = statCard("BLACK", 0xffaeb5c4);
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, -2, 1);
        half.setMargins(0, 0, dp(6), 0); stats.addView(redCard, half);
        half = new LinearLayout.LayoutParams(0, -2, 1); half.setMargins(dp(6), 0, 0, 0); stats.addView(blackCard, half);

        LinearLayout totalCard = card(); root.addView(totalCard);
        totalView = text("Jumla ya raundi: 0", 16, WHITE, true); totalCard.addView(totalView);
        streakView = text("Mfululizo wa sasa: —", 14, 0xffc5cad5, false); totalCard.addView(streakView);

        LinearLayout entry = card(); root.addView(entry);
        entry.addView(text("WEKA MATOKEO MAPYA", 14, WHITE, true));
        entry.addView(text("Bonyeza rangi iliyoonekana kwenye raundi iliyomalizika.", 12, 0xffb7bdca, false));
        LinearLayout buttons = new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button red = button("＋  RED", RED); Button black = button("＋  BLACK", BLACK);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, dp(52), 1);
        bp.setMargins(0, dp(12), dp(5), 0); buttons.addView(red, bp);
        bp = new LinearLayout.LayoutParams(0, dp(52), 1); bp.setMargins(dp(5), dp(12), 0, 0); buttons.addView(black, bp);
        entry.addView(buttons);
        red.setOnClickListener(v -> add("R")); black.setOnClickListener(v -> add("B"));

        LinearLayout history = card(); root.addView(history);
        history.addView(text("UCHAMBUZI WA HISTORIA", 14, WHITE, true));
        redView = text("RED: 0 (0.0%)", 14, RED, true); history.addView(redView);
        blackView = text("BLACK: 0 (0.0%)", 14, 0xffc5cad5, true); history.addView(blackView);
        trendView = text("Mwelekeo wa sampuli: bado hakuna data ya kutosha.", 13, WHITE, false);
        trendView.setPadding(0, dp(10), 0, 0); history.addView(trendView);

        LinearLayout recent = card(); root.addView(recent);
        recent.addView(text("RAUNDI ZA HIVI KARIBUNI", 14, WHITE, true));
        recentView = text("Bado hakuna matokeo yaliyohifadhiwa.", 14, 0xffc5cad5, false);
        recentView.setPadding(0, dp(8), 0, 0); recent.addView(recentView);

        LinearLayout actions = new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions);
        Button undo = button("RUDISHA", 0xff424958);
        Button clear = button("FUTA HISTORIA", 0xff424958);
        bp = new LinearLayout.LayoutParams(0, dp(46), 1); bp.setMargins(0, dp(8), dp(5), 0); actions.addView(undo, bp);
        bp = new LinearLayout.LayoutParams(0, dp(46), 1); bp.setMargins(dp(5), dp(8), 0, 0); actions.addView(clear, bp);
        undo.setOnClickListener(v -> { if (!rounds.isEmpty()) { rounds.remove(rounds.size()-1); save(); refresh(); } });
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Futa historia?")
            .setMessage("Matokeo yote yaliyohifadhiwa kwenye simu yatafutwa.")
            .setNegativeButton("Ghairi", null).setPositiveButton("Futa", (d,w) -> { rounds.clear(); save(); refresh(); }).show());

        TextView note = text("MUHIMU: RED/BLACK ni matokeo ya bahati nasibu. Historia haiwezi kuhakikisha wala kutabiri kwa uhakika raundi inayofuata. Rekodi data kwa uangalifu; hakuna dau linalowekwa na app hii.", 12, 0xff9da5b5, false);
        note.setPadding(dp(2), dp(14), dp(2), 0); root.addView(note);
    }
    private LinearLayout statCard(String label, int color) {
        LinearLayout c = card();
        c.addView(text(label, 13, color, true));
        TextView v = text("0", 27, WHITE, true);
        if (label.equals("RED")) redCount = v; else blackCount = v;
        c.addView(v); return c;
    }
    private TextView redCount, blackCount;
    private Button button(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setTextColor(WHITE);
        b.setTextSize(13); b.setAllCaps(false); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        return b;
    }
    private void add(String value) { rounds.add(value); save(); refresh(); }
    private void save() {
        StringBuilder s = new StringBuilder();
        for (String r : rounds) { if (s.length()>0) s.append(","); s.append(r); }
        prefs.edit().putString("rounds", s.toString()).apply();
    }
    private void refresh() {
        int r=0,b=0; for (String x: rounds) if (x.equals("R")) r++; else b++;
        int n=rounds.size();
        redCount.setText(String.valueOf(r)); blackCount.setText(String.valueOf(b));
        totalView.setText("Jumla ya raundi: " + n);
        redView.setText(String.format(Locale.US, "RED: %d (%.1f%%)", r, n==0?0:100.0*r/n));
        blackView.setText(String.format(Locale.US, "BLACK: %d (%.1f%%)", b, n==0?0:100.0*b/n));
        if (n==0) streakView.setText("Mfululizo wa sasa: —");
        else {
            String last=rounds.get(n-1); int count=0;
            for(int i=n-1;i>=0 && rounds.get(i).equals(last);i--) count++;
            streakView.setText("Mfululizo wa sasa: " + (last.equals("R")?"RED":"BLACK") + " × " + count);
        }
        int sample=Math.min(30,n), sr=0,sb=0;
        for(int i=n-sample;i<n;i++) if(rounds.get(i).equals("R")) sr++; else sb++;
        if(sample<5) trendView.setText("Mwelekeo wa sampuli: ongeza angalau raundi 5 ili kuona muhtasari.");
        else trendView.setText(String.format(Locale.US,"Raundi %d za mwisho: RED %.1f%% • BLACK %.1f%%. Huu ni muhtasari wa nyuma tu.",sample,100.0*sr/sample,100.0*sb/sample));
        if(n==0) recentView.setText("Bado hakuna matokeo yaliyohifadhiwa.");
        else {
            StringBuilder s=new StringBuilder();
            for(int i=n-1;i>=Math.max(0,n-19);i--) {
                if(s.length()>0) s.append("   ");
                s.append(rounds.get(i).equals("R")?"🔴":"⚫");
            }
            recentView.setText(s.toString());
        }
    }
}
