package com.mcwiz.redblack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.content.Context;
import android.content.pm.ServiceInfo;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL="redblack_capture";
    private static final int NOTIFICATION_ID=7401;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread workerThread;
    private Handler worker;
    private TextRecognizer recognizer;
    private WindowManager windowManager;
    private TextView bubble;
    private final AtomicBoolean processing=new AtomicBoolean(false);
    private String previousFingerprint="";
    private int stableFrames=0;
    private String savedFingerprint="";
    private long lastSavedAt=0;
    private boolean stopped=false;

    @Override public void onCreate(){
        super.onCreate();
        createNotificationChannel();
        workerThread=new HandlerThread("RedBlackOCR");workerThread.start();worker=new Handler(workerThread.getLooper());
        recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        windowManager=(WindowManager)getSystemService(WINDOW_SERVICE);
        showBubble("RED BLACK OCR\nInaanza…\nGusa kusimamisha");
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        if("STOP".equals(intent.getAction())){stopCapture();return START_NOT_STICKY;}
        int resultCode=intent.getIntExtra("resultCode",0);
        Intent data=intent.getParcelableExtra("data");
        if(data==null){stopCapture();return START_NOT_STICKY;}
        try{
            if(Build.VERSION.SDK_INT>=29)startForeground(NOTIFICATION_ID,buildNotification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            else startForeground(NOTIFICATION_ID,buildNotification());
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection=manager.getMediaProjection(resultCode,data);
            if(projection==null){stopCapture();return START_NOT_STICKY;}
            projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopCapture();}},worker);
            startReader();
            getSharedPreferences("capture_state",MODE_PRIVATE).edit().putBoolean("active",true).apply();
            showBubble("RED BLACK OCR\nInasoma skrini…\nGusa kusimamisha");
        }catch(Exception e){showBubble("OCR imeshindwa kuanza\nFungua app tena");stopCapture();}
        return START_NOT_STICKY;
    }
    private void startReader(){
        DisplayMetrics metrics=new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int width=metrics.widthPixels,height=metrics.heightPixels,density=metrics.densityDpi;
        imageReader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
        imageReader.setOnImageAvailableListener(reader->{
            if(stopped||!processing.compareAndSet(false,true))return;
            Image image=null;
            try{
                image=reader.acquireLatestImage();
                if(image==null){processing.set(false);return;}
                Image.Plane plane=image.getPlanes()[0];ByteBuffer buffer=plane.getBuffer();
                int pixelStride=plane.getPixelStride(),rowStride=plane.getRowStride();
                int rowPadding=rowStride-pixelStride*width;
                Bitmap padded=Bitmap.createBitmap(width+rowPadding/pixelStride,height,Bitmap.Config.ARGB_8888);
                padded.copyPixelsFromBuffer(buffer);
                Bitmap bitmap=Bitmap.createBitmap(padded,0,0,width,height);padded.recycle();
                image.close();image=null;
                InputImage input=InputImage.fromBitmap(bitmap,0);
                recognizer.process(input).addOnSuccessListener(this::processText)
                    .addOnFailureListener(e->showBubble("OCR haijasoma fremu\nWeka matokeo wazi"))
                    .addOnCompleteListener(t->{bitmap.recycle();processing.set(false);});
            }catch(Exception e){if(image!=null)image.close();processing.set(false);}
        },worker);
        virtualDisplay=projection.createVirtualDisplay("RedBlackScreenCapture",width,height,density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,imageReader.getSurface(),null,worker);
    }
    private void processText(Text result){
        StringBuilder all=new StringBuilder();String lastColor="";
        Pattern p=Pattern.compile("\\b(RED|BLACK)\\b",Pattern.CASE_INSENSITIVE);
        for(Text.TextBlock block:result.getTextBlocks())for(Text.Line line:block.getLines()){
            String lineText=line.getText();all.append(lineText).append('\n');
            Matcher m=p.matcher(lineText);while(m.find())lastColor=m.group(1).toUpperCase(Locale.US);
        }
        if(lastColor.isEmpty()){showBubble("OCR inaendelea…\nHaijaona RED/BLACK\nGusa kusimamisha");return;}
        String fingerprint=Integer.toHexString(all.toString().replaceAll("\\s+"," ").trim().hashCode());
        if(fingerprint.equals(previousFingerprint))stableFrames++;else{previousFingerprint=fingerprint;stableFrames=1;}
        if(stableFrames>=2&&!fingerprint.equals(savedFingerprint)&&System.currentTimeMillis()-lastSavedAt>1800){
            appendRound(lastColor.equals("RED")?"R":"B");savedFingerprint=fingerprint;lastSavedAt=System.currentTimeMillis();
            getSharedPreferences("capture_state",MODE_PRIVATE).edit().putString("last_color",lastColor).apply();
            showBubble("IMEONA: "+lastColor+"\nImehifadhiwa • kagua app\nGusa kusimamisha");
        }else showBubble("Imeona: "+lastColor+"\nInathibitisha maandishi…\nGusa kusimamisha");
    }
    private void appendRound(String color){
        SharedPreferences p=getSharedPreferences("redblack_data",MODE_PRIVATE);String old=p.getString("rounds","");
        p.edit().putString("rounds",old.isEmpty()?color:old+","+color).apply();
    }
    private void showBubble(String message){
        if(windowManager==null)return;
        if(bubble==null){
            bubble=new TextView(this);bubble.setTextColor(Color.WHITE);bubble.setTextSize(12);
            bubble.setTypeface(Typeface.DEFAULT,Typeface.BOLD);bubble.setPadding(22,14,22,14);
            bubble.setBackgroundColor(0xee181b25);bubble.setOnClickListener(v->stopCapture());
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
            lp.gravity=Gravity.TOP|Gravity.END;lp.x=18;lp.y=180;
            try{windowManager.addView(bubble,lp);}catch(Exception ignored){}
        }
        if(bubble!=null)bubble.setText(message);
    }
    private Notification buildNotification(){
        Intent stop=new Intent(this,ScreenCaptureService.class);stop.setAction("STOP");
        PendingIntent pi=PendingIntent.getService(this,7402,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        return b.setContentTitle("Red Black Analyzer").setContentText("Inasoma skrini kwa ruhusa yako.")
            .setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true)
            .addAction(new Notification.Action.Builder(null,"SIMAMISHA",pi).build()).build();
    }
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel c=new NotificationChannel(CHANNEL,"Red Black Screen Capture",NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    private void stopCapture(){
        if(stopped)return;stopped=true;
        getSharedPreferences("capture_state",MODE_PRIVATE).edit().putBoolean("active",false).apply();
        try{if(virtualDisplay!=null)virtualDisplay.release();}catch(Exception ignored){}
        try{if(imageReader!=null)imageReader.close();}catch(Exception ignored){}
        try{if(projection!=null)projection.stop();}catch(Exception ignored){}
        try{if(bubble!=null){windowManager.removeView(bubble);bubble=null;}}catch(Exception ignored){}
        try{if(recognizer!=null)recognizer.close();}catch(Exception ignored){}
        if(workerThread!=null)workerThread.quitSafely();
        stopForeground(true);stopSelf();
    }
    @Override public void onDestroy(){stopCapture();super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
