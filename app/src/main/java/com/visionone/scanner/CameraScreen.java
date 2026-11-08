package com.visionone.scanner;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.nio.*;
import java.util.concurrent.*;

public class CameraScreen extends ComponentActivity {
 ExecutorService worker=Executors.newSingleThreadExecutor(),analyzer=Executors.newSingleThreadExecutor();
 PreviewView preview;ImageCapture capture;TextView status,modeButton;String mode;volatile boolean busy=false,automatic=false,needMovement=false;
 int stableFrames=0;double lastAvg=-1;long lastShot=0;
 @Override public void onCreate(Bundle s){super.onCreate(s);mode=getIntent().getStringExtra("mode");automatic="auto".equals(mode)||"batch".equals(mode);ui();if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)open();else requestPermissions(new String[]{Manifest.permission.CAMERA},10);}
 void ui(){FrameLayout base=new FrameLayout(this);base.setBackgroundColor(0xff04142a);preview=new PreviewView(this);base.addView(preview,new FrameLayout.LayoutParams(-1,-1));TextView title=new TextView(this);title.setText("VISION ONE    •    "+("question".equals(mode)?"QUESTION PAPER":automatic?"AUTO / BATCH":"SMART SCAN"));title.setPadding(18,38,10,30);title.setTextColor(-1);title.setTextSize(18);title.setBackgroundColor(0xbb07214a);base.addView(title,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
  LinearLayout bottom=new LinearLayout(this);bottom.setPadding(10,16,10,20);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setBackgroundColor(0xee061932);
  status=new TextView(this);status.setText("Point camera at a document");status.setTextColor(-1);status.setGravity(Gravity.CENTER);bottom.addView(status);
  LinearLayout buttons=new LinearLayout(this);bottom.addView(buttons);
  add(buttons,"📷 CAPTURE",()->take());modeButton=add(buttons,automatic?"AUTO ON":"AUTO OFF",()->{automatic=!automatic;needMovement=false;stableFrames=0;modeButton.setText(automatic?"AUTO ON":"AUTO OFF");});add(buttons,"DONE ✓",()->{if(busy){Toast.makeText(this,"Finishing page; tap DONE again",Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(this,MainActivity.class).putExtra("showPages",true));finish();});
  base.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));setContentView(base);
 }
 TextView add(LinearLayout parent,String text,Runnable action){TextView b=new TextView(this);b.setText(text);b.setTextSize(14);b.setTextColor(-1);b.setGravity(Gravity.CENTER);b.setPadding(10,20,10,20);parent.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->action.run());return b;}
 void open(){ListenableFuture<ProcessCameraProvider> f=ProcessCameraProvider.getInstance(this);f.addListener(()->{try{ProcessCameraProvider p=f.get();Preview pv=new Preview.Builder().build();pv.setSurfaceProvider(preview.getSurfaceProvider());capture=new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build();
   ImageAnalysis ia=new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setTargetResolution(new android.util.Size(640,480)).build();ia.setAnalyzer(analyzer,frame->{try{if(!automatic||busy)return;ImageProxy.PlaneProxy pl=frame.getPlanes()[0];ByteBuffer buf=pl.getBuffer();int stride=pl.getRowStride();int w=frame.getWidth(),h=frame.getHeight();int n=0,sum=0,edge=0;
     for(int y=h/3;y<h*2/3;y+=20)for(int x=w/3;x<w*2/3;x+=20){int pos=y*stride+x;if(pos<buf.limit()){sum+=buf.get(pos)&255;n++;}}
     if(n==0)return;double avg=sum/(double)n;int corners=0,cornerN=0;for(int y=15;y<h;y+=h-30)for(int x=15;x<w;x+=w-30){int pos=y*stride+x;if(pos<buf.limit()){corners+=buf.get(pos)&255;cornerN++;}}
     boolean sheet=cornerN>0&&avg>105&&avg-corners/(double)cornerN>8;boolean moved=lastAvg>=0&&Math.abs(avg-lastAvg)>12;
     if(needMovement){if(moved){needMovement=false;stableFrames=0;}}else if(sheet&&!moved){stableFrames++;if(stableFrames>26&&System.currentTimeMillis()-lastShot>4000){stableFrames=0;runOnUiThread(this::take);}}else stableFrames=0;lastAvg=avg;
   }catch(Exception ignored){}finally{frame.close();}});p.unbindAll();p.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,pv,capture,ia);
 }catch(Exception e){status.setText("Camera error: "+e.getMessage());}},ContextCompat.getMainExecutor(this));}
 void take(){if(busy||capture==null)return;busy=true;lastShot=System.currentTimeMillis();status.setText("Capturing high-quality document…");File f=Docs.newPage(this);ImageCapture.OutputFileOptions opt=new ImageCapture.OutputFileOptions.Builder(f).build();capture.takePicture(opt,ContextCompat.getMainExecutor(this),new ImageCapture.OnImageSavedCallback(){
 @Override public void onImageSaved(ImageCapture.OutputFileResults r){worker.execute(()->{try{boolean cropped=Docs.processCapture(CameraScreen.this,f,mode);int n=Docs.session(CameraScreen.this).listFiles((d,name)->name.endsWith(".jpg")).length;runOnUiThread(()->status.setText("Page "+n+(cropped?" • Auto cropped":" • Manual crop available")+" • DONE to review"));}catch(Exception e){runOnUiThread(()->status.setText("Saved image; review crop later: "+e.getMessage()));}finally{busy=false;needMovement=automatic;}});}
 @Override public void onError(ImageCaptureException e){busy=false;status.setText("Capture failed: "+e.getMessage());}
 });}
 @Override public void onRequestPermissionsResult(int request,String[] perms,int[] grant){super.onRequestPermissionsResult(request,perms,grant);if(request==10&&grant.length>0&&grant[0]==PackageManager.PERMISSION_GRANTED)open();else{status.setText("Camera permission denied. Import images from Home instead.");}}
 @Override protected void onDestroy(){super.onDestroy();analyzer.shutdownNow();worker.shutdown();}
}