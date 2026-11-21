package com.mcwiz.redblack;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(12,14,20), PANEL=Color.rgb(25,28,38);
    private static final int RED=Color.rgb(235,55,70), BLACK=Color.rgb(80,88,105), WHITE=Color.rgb(245,246,250);
    private static final int REQ_CAPTURE=4102, REQ_OVERLAY=4103;
    private final ArrayList<String> rounds=new ArrayList<>();
    private SharedPreferences prefs;
    private TextView totalView,redView,blackView,streakView,recentView,trendView,captureStatus;
    private boolean waitingForOverlay=false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("redblack_data",MODE_PRIVATE);
        String saved=prefs.getString("rounds","");
        if(!saved.isEmpty()) for(String s:saved.split(",")) if(s.equals("R")||s.equals("B")) rounds.add(s);
        buildUi(); refresh(); updateCaptureStatus();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;
    }
    private LinearLayout card(){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(7),0,dp(7));c.setLayoutParams(p);return c;
    }
    private Button button(String label,int color){
        Button b=new Button(this);b.setText(label);b.setTextColor(WHITE);b.setTextSize(13);b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));return b;
    }
    private void buildUi(){
        ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(22),dp(18),dp(26));root.setBackgroundColor(BG);scroll.addView(root);setContentView(scroll);
        root.addView(text("SPORTYBET",13,RED,true));root.addView(text("RED BLACK ANALYZER",23,WHITE,true));
        root.addView(text("Takwimu za matokeo na usomaji wa skrini kwa ruhusa yako.",13,0xffb7bdca,false));
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);stats.setPadding(0,dp(14),0,dp(4));root.addView(stats);
        LinearLayout rc=statCard("RED",RED),bc=statCard("BLACK",0xffaeb5c4);
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(0,0,dp(6),0);stats.addView(rc,half);
        half=new LinearLayout.LayoutParams(0,-2,1);half.setMargins(dp(6),0,0,0);stats.addView(bc,half);
        LinearLayout total=card();root.addView(total);totalView=text("Jumla ya raundi: 0",16,WHITE,true);total.addView(totalView);
        streakView=text("Mfululizo wa sasa: —",14,0xffc5cad5,false);total.addView(streakView);

        LinearLayout capture=card();root.addView(capture);capture.addView(text("SCREEN CAPTURE + OCR",15,WHITE,true));
        capture.addView(text("Ruhusu kurekodi skrini ya SportyBet. OCR itasoma maneno RED/BLACK yanayoonekana na kuweka matokeo mapya yanapotambua mabadiliko.",12,0xffc5cad5,false));
        captureStatus=text("Hali: haijaanza",13,0xfff0c36a,true);captureStatus.setPadding(0,dp(8),0,dp(4));capture.addView(captureStatus);
        Button start=button("ANZA KUSOMA SKRINI",RED);capture.addView(start,new LinearLayout.LayoutParams(-1,dp(50)));
        Button stop=button("SIMAMISHA KUSOMA",0xff424958);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(46));sp.setMargins(0,dp(7),0,0);capture.addView(stop,sp);
        start.setOnClickListener(v->beginCapture());stop.setOnClickListener(v->{stopService(new Intent(this,ScreenCaptureService.class));updateCaptureStatus();});

        LinearLayout entry=card();root.addView(entry);entry.addView(text("WEKA MATOKEO MWENYEWE",14,WHITE,true));
        entry.addView(text("Unaweza kusahihisha au kuongeza matokeo kwa mkono.",12,0xffb7bdca,false));
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button red=button("+ RED",RED),black=button("+ BLACK",BLACK);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,dp(50),1);bp.setMargins(0,dp(10),dp(5),0);buttons.addView(red,bp);
        bp=new LinearLayout.LayoutParams(0,dp(50),1);bp.setMargins(dp(5),dp(10),0,0);buttons.addView(black,bp);entry.addView(buttons);
        red.setOnClickListener(v->add("R"));black.setOnClickListener(v->add("B"));

        LinearLayout history=card();root.addView(history);history.addView(text("UCHAMBUZI WA HISTORIA",14,WHITE,true));
        redView=text("RED: 0 (0.0%)",14,RED,true);history.addView(redView);
        blackView=text("BLACK: 0 (0.0%)",14,0xffc5cad5,true);history.addView(blackView);
        trendView=text("Bado hakuna data ya kutosha.",13,WHITE,false);trendView.setPadding(0,dp(10),0,0);history.addView(trendView);
        LinearLayout recent=card();root.addView(recent);recent.addView(text("RAUNDI ZA HIVI KARIBUNI",14,WHITE,true));
        recentView=text("Bado hakuna matokeo.",14,0xffc5cad5,false);recentView.setPadding(0,dp(8),0,0);recent.addView(recentView);

        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);root.addView(actions);
        Button undo=button("RUDISHA",0xff424958),clear=button("FUTA HISTORIA",0xff424958);
        bp=new LinearLayout.LayoutParams(0,dp(46),1);bp.setMargins(0,dp(8),dp(5),0);actions.addView(undo,bp);
        bp=new LinearLayout.LayoutParams(0,dp(46),1);bp.setMargins(dp(5),dp(8),0,0);actions.addView(clear,bp);
        undo.setOnClickListener(v->{if(!rounds.isEmpty()){rounds.remove(rounds.size()-1);save();refresh();}});
        clear.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Futa historia?")
          .setMessage("Matokeo yote yaliyohifadhiwa yatafutwa.").setNegativeButton("Ghairi",null)
          .setPositiveButton("Futa",(d,w)->{rounds.clear();save();refresh();}).show());
        TextView note=text("USALAMA: Android itaonyesha ruhusa ya Screen Capture. Unaweza kusimamisha wakati wowote. App haisomi nenosiri, haiingii kwenye akaunti yako na haiweki dau. OCR inaweza kukosea; kagua matokeo. Takwimu za nyuma hazihakikishi raundi ijayo.",12,0xff9da5b5,false);
        note.setPadding(dp(2),dp(14),dp(2),0);root.addView(note);
    }
    private LinearLayout statCard(String label,int color){
        LinearLayout c=card();c.addView(text(label,13,color,true));TextView v=text("0",27,WHITE,true);
        if(label.equals("RED"))redCount=v;else blackCount=v;c.addView(v);return c;
    }
    private TextView redCount,blackCount;
    private void beginCapture(){
        if(!Settings.canDrawOverlays(this)){
            waitingForOverlay=true;
            new AlertDialog.Builder(this).setTitle("Ruhusa ya kitufe kinachoelea")
              .setMessage("Washa 'Allow display over other apps' kwa SPORTYBET RED BLACK ANALYZER, kisha rudi kwenye app na bonyeza ANZA tena.")
              .setPositiveButton("Fungua ruhusa",(d,w)->{Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));startActivityForResult(i,REQ_OVERLAY);})
              .setNegativeButton("Ghairi",null).show();return;
        }
        MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(),REQ_CAPTURE);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_CAPTURE){
            if(resultCode==RESULT_OK&&data!=null){
                Intent service=new Intent(this,ScreenCaptureService.class);
                service.putExtra("resultCode",resultCode);service.putExtra("data",data);
                if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);
                captureStatus.setText("Hali: inaomba/inaanza kusoma skrini…");
            }else captureStatus.setText("Hali: ruhusa ya Screen Capture imekataliwa.");
        } else if(requestCode==REQ_OVERLAY){waitingForOverlay=false;updateCaptureStatus();}
    }
    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null){
            rounds.clear();
            String saved=prefs.getString("rounds","");
            if(!saved.isEmpty())for(String s:saved.split(","))if(s.equals("R")||s.equals("B"))rounds.add(s);
            if(redCount!=null)refresh();
        }
        if(captureStatus!=null)updateCaptureStatus();
    }
    private void updateCaptureStatus(){
        if(captureStatus==null)return;
        boolean active=getSharedPreferences("capture_state",MODE_PRIVATE).getBoolean("active",false);
        captureStatus.setText(active?"Hali: OCR inaendelea • gusa kiputo kinachoelea kusimamisha":"Hali: haijaanza");
    }
    private void add(String value){rounds.add(value);save();refresh();}
    private void save(){
        StringBuilder s=new StringBuilder();for(String r:rounds){if(s.length()>0)s.append(",");s.append(r);}
        prefs.edit().putString("rounds",s.toString()).apply();
    }
    private void refresh(){
        int r=0,b=0;for(String x:rounds)if(x.equals("R"))r++;else b++;int n=rounds.size();
        redCount.setText(String.valueOf(r));blackCount.setText(String.valueOf(b));totalView.setText("Jumla ya raundi: "+n);
        redView.setText(String.format(Locale.US,"RED: %d (%.1f%%)",r,n==0?0:100.0*r/n));
        blackView.setText(String.format(Locale.US,"BLACK: %d (%.1f%%)",b,n==0?0:100.0*b/n));
        if(n==0)streakView.setText("Mfululizo wa sasa: —");else{
            String last=rounds.get(n-1);int count=0;for(int i=n-1;i>=0&&rounds.get(i).equals(last);i--)count++;
            streakView.setText("Mfululizo wa sasa: "+(last.equals("R")?"RED":"BLACK")+" × "+count);
        }
        int sample=Math.min(30,n),sr=0,sb=0;for(int i=n-sample;i<n;i++)if(rounds.get(i).equals("R"))sr++;else sb++;
        if(sample<5)trendView.setText("Ongeza angalau raundi 5 ili kuona muhtasari wa sampuli.");
        else trendView.setText(String.format(Locale.US,"Raundi %d za mwisho: RED %.1f%% • BLACK %.1f%%. Muhtasari wa nyuma tu.",sample,100.0*sr/sample,100.0*sb/sample));
        if(n==0)recentView.setText("Bado hakuna matokeo.");else{
            StringBuilder s=new StringBuilder();for(int i=n-1;i>=Math.max(0,n-19);i--){if(s.length()>0)s.append("   ");s.append(rounds.get(i).equals("R")?"🔴":"⚫");}
            recentView.setText(s.toString());
        }
    }
}
