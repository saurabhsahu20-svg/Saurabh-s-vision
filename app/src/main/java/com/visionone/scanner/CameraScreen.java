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
import java.io.File;
import java.util.concurrent.*;

public class CameraScreen extends ComponentActivity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private PreviewView preview;
    private ImageCapture imageCapture;
    private androidx.camera.core.Camera camera;
    private TextView status,shoot,flashButton;
    private volatile boolean processing=false;
    private boolean torch=false;
    private int count=0;

    private int dp(float dp){return (int)(getResources().getDisplayMetrics().density*dp+.5f);}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        File[] files=Docs.session(this).listFiles((dir,name)->name.endsWith(".jpg"));
        count=files==null?0:files.length;
        buildUi();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)bindCamera();
        else requestPermissions(new String[]{Manifest.permission.CAMERA},15);
    }
    private TextView text(String label,int size){
        TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(0xffffffff);
        t.setGravity(Gravity.CENTER);t.setPadding(dp(8),dp(15),dp(8),dp(15));return t;
    }
    private void buildUi(){
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff020b19);
        preview=new PreviewView(this);root.addView(preview,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.VERTICAL);top.setBackgroundColor(0xbb061b3e);
        TextView brand=text("VISION ONE   •   DOCUMENT SCAN",19);
        top.addView(brand);
        TextView help=text("Place the entire paper inside the camera view",13);help.setTextColor(0xffd8eaff);top.addView(help);
        root.addView(top,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setPadding(dp(12),dp(18),dp(12),dp(25));bottom.setBackgroundColor(0xee07182d);
        status=text("Ready • "+count+" page(s) captured",14);bottom.addView(status);
        LinearLayout buttons=new LinearLayout(this);bottom.addView(buttons);
        flashButton=text("⚡ FLASH",14);buttons.addView(flashButton,new LinearLayout.LayoutParams(0,dp(68),1));
        flashButton.setOnClickListener(v->toggleFlash());
        shoot=text("●  SCAN",19);shoot.setTypeface(null,1);shoot.setBackgroundColor(0xff0879eb);
        LinearLayout.LayoutParams capture=new LinearLayout.LayoutParams(0,dp(68),2);capture.setMargins(dp(3),0,dp(3),0);
        buttons.addView(shoot,capture);shoot.setOnClickListener(v->take());
        TextView done=text("✓ DONE",15);buttons.addView(done,new LinearLayout.LayoutParams(0,dp(68),1));
        done.setOnClickListener(v->done());
        TextView hint=text("Scan another page or tap DONE to crop and create PDF",12);hint.setTextColor(0xffa9c9e8);bottom.addView(hint);
        root.addView(bottom,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));setContentView(root);
    }
    private void bindCamera(){
        ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);
        future.addListener(()->{
            try{
                ProcessCameraProvider provider=future.get();
                Preview p=new Preview.Builder().build();
                p.setSurfaceProvider(preview.getSurfaceProvider());
                imageCapture=new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build();
                provider.unbindAll();
                camera=provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,imageCapture);
                if(camera==null)status.setText("Camera not available");
            }catch(Exception e){status.setText("Camera error: "+e.getMessage());}
        },ContextCompat.getMainExecutor(this));
    }
    private void toggleFlash(){
        if(camera==null)return;
        if(!camera.getCameraInfo().hasFlashUnit()){
            Toast.makeText(this,"Flash not supported",Toast.LENGTH_SHORT).show();return;
        }
        torch=!torch;camera.getCameraControl().enableTorch(torch);
        flashButton.setText(torch?"⚡ ON":"⚡ FLASH");
    }
    private void take(){
        if(processing)return;
        if(imageCapture==null){Toast.makeText(this,"Camera is not ready",Toast.LENGTH_SHORT).show();return;}
        processing=true;shoot.setEnabled(false);
        status.setText("Capturing and finding document edges…");
        File target=Docs.newPage(this);
        ImageCapture.OutputFileOptions opts=new ImageCapture.OutputFileOptions.Builder(target).build();
        imageCapture.takePicture(opts,ContextCompat.getMainExecutor(this),new ImageCapture.OnImageSavedCallback(){
            @Override public void onImageSaved(ImageCapture.OutputFileResults result){
                worker.execute(()->{
                    boolean cropped=false;String error=null;
                    try{cropped=Docs.processCapture(CameraScreen.this,target,"smart");}
                    catch(Throwable e){error=e.getMessage();}
                    File[] files=Docs.session(CameraScreen.this).listFiles((dir,name)->name.endsWith(".jpg"));
                    final int current=files==null?0:files.length;
                    final boolean success=cropped;
                    final String failure=error;
                    runOnUiThread(()->{
                        processing=false;shoot.setEnabled(true);count=current;
                        status.setText(failure==null?
                            count+" page(s) • "+(success?"Edges detected":"Manual crop available") :
                            count+" page(s) • Check crop manually");
                    });
                });
            }
            @Override public void onError(ImageCaptureException exception){
                target.delete();processing=false;shoot.setEnabled(true);
                status.setText("Capture failed: "+exception.getMessage());
            }
        });
    }
    private void done(){
        if(processing){Toast.makeText(this,"Finishing scan; tap DONE again",Toast.LENGTH_SHORT).show();return;}
        Intent next=new Intent(this,MainActivity.class);
        next.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        next.putExtra("showPages",true);
        startActivity(next);finish();
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(code,permissions,grants);
        if(code==15&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)bindCamera();
        else new AlertDialog.Builder(this).setTitle("Camera permission")
                .setMessage("VISION ONE needs camera access to scan documents. You can still import photos without it.")
                .setPositiveButton("Back",(d,w)->finish()).show();
    }
    @Override protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
