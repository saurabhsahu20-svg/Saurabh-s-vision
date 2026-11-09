package com.visionone.scanner;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import java.io.*;

public class CropActivity extends ComponentActivity {
 File page,source;Bitmap image;CornerView cropView;Spinner sizeSpinner,filterSpinner;
 int turns=0;boolean saving=false;int detectionVersion=0;
 TextView edgeStatus;
 String[] FILTERS={"Original","Enhanced Color","Grayscale","Black & White"};
 @Override public void onCreate(Bundle state){super.onCreate(state);String path=getIntent().getStringExtra("page");if(path==null){finish();return;}
  page=new File(path);if(!page.exists()){finish();return;}source=Docs.original(this,page);
  image=Docs.read(source,1700);if(image==null){new AlertDialog.Builder(this).setMessage("Cannot read this page").setPositiveButton("Close",(d,w)->finish()).show();return;}
  build();
 }
 TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setGravity(Gravity.CENTER);t.setPadding(8,10,8,10);return t;}
 TextView action(LinearLayout row,String s,Runnable click){
  TextView t=label(s,14,Color.WHITE);t.setBackgroundColor(0xff14599c);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(3,4,3,4);row.addView(t,p);t.setOnClickListener(v->click.run());return t;
 }
 void build(){LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff07182e);
  root.addView(label("VISION ONE  •  ADJUST DOCUMENT CORNERS",19,Color.WHITE));
  edgeStatus=label("Detecting actual document outline…",13,0xffc9e1ff);root.addView(edgeStatus);
  root.addView(label("Drag the blue dots if any boundary needs correction",12,0xffc9e1ff));
  cropView=new CornerView(this,image);root.addView(cropView,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout one=new LinearLayout(this);root.addView(one);
  action(one,"AUTO EDGES",()->detectEdgesAsync());
  action(one,"FULL PHOTO",()->{cropView.corners=PaperEdges.FULL.clone();cropView.invalidate();});
  action(one,"ROTATE ↻",()->{detectionVersion++;Bitmap next=Docs.rotate(image);image.recycle();image=next;turns=(turns+1)%4;cropView.bitmap=image;cropView.corners=defaultCorners();cropView.touched=false;cropView.invalidate();detectEdgesAsync();});
  LinearLayout two=new LinearLayout(this);two.setOrientation(LinearLayout.HORIZONTAL);root.addView(two);
  sizeSpinner=new Spinner(this);ArrayAdapter<String> sizes=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PaperEdges.SIZES);sizeSpinner.setAdapter(sizes);two.addView(sizeSpinner,new LinearLayout.LayoutParams(0,54,1));
  String old=getSharedPreferences("settings",MODE_PRIVATE).getString("paper","Original proportions");for(int i=0;i<PaperEdges.SIZES.length;i++)if(PaperEdges.SIZES[i].equals(old))sizeSpinner.setSelection(i);
  filterSpinner=new Spinner(this);filterSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,FILTERS));two.addView(filterSpinner,new LinearLayout.LayoutParams(0,54,1));
  if(getIntent().getBooleanExtra("question",false))filterSpinner.setSelection(3);
  LinearLayout bottom=new LinearLayout(this);root.addView(bottom);
  action(bottom,"CANCEL",this::finish);action(bottom,"✓ APPLY CROP",this::apply);setContentView(root);
  cropView.corners=defaultCorners();cropView.invalidate();detectEdgesAsync();
 }
 void detectEdgesAsync(){
  if(image==null||image.isRecycled())return;
  final int id=++detectionVersion;
  cropView.touched=false;
  edgeStatus.setText("Detecting document edges…");
  final Bitmap input;
  try{
    float scale=Math.min(1f,1150f/Math.max(image.getWidth(),image.getHeight()));
    input=Bitmap.createScaledBitmap(image,Math.max(1,Math.round(image.getWidth()*scale)),Math.max(1,Math.round(image.getHeight()*scale)),true);
    if(input==image){ // avoid accessing/recycling an image being rotated during detection
       Bitmap safe=input.copy(Bitmap.Config.ARGB_8888,false);
       if(safe==null)throw new OutOfMemoryError("Preview memory");
       new Thread(()->finishDetect(id,safe)).start();return;
    }
  }catch(Throwable t){edgeStatus.setText("Manual crop available");return;}
  new Thread(()->finishDetect(id,input)).start();
 }
 void finishDetect(int id,Bitmap input){
  float[] corners=null;
  try{corners=PaperEdges.suggest(input);}
  catch(Throwable ignored){}
  finally{input.recycle();}
  final float[] found=corners;
  runOnUiThread(()->{
    if(isFinishing()||isDestroyed()||id!=detectionVersion)return;
    if(found!=null&&!cropView.touched){
      cropView.corners=found;cropView.invalidate();
      edgeStatus.setText("Auto detected • drag corners to fine-tune actual paper size");
    }else if(found==null){
      edgeStatus.setText("No reliable edge found • drag 4 corners manually");
    }else edgeStatus.setText("Manual corners kept");
  });
 }
 static float[] defaultCorners(){return new float[]{.04f,.04f,.96f,.04f,.96f,.96f,.04f,.96f};}
 void apply(){
  if(saving)return;
  final float[] corners=cropView.corners.clone();if(!PaperEdges.valid(corners)){Toast.makeText(this,"Adjust the 4 corners to make a valid page",Toast.LENGTH_LONG).show();return;}
  saving=true;final int rotation=turns;final String paper=(String)sizeSpinner.getSelectedItem();final int filter=filterSpinner.getSelectedItemPosition();
  Toast.makeText(this,"Processing paper crop…",Toast.LENGTH_SHORT).show();
  new Thread(()->{try{
   Docs.ensureBackup(this,page);
   Bitmap src=Docs.read(source,3200);if(src==null)throw new IOException("Image not available");
   for(int i=0;i<rotation;i++){Bitmap next=Docs.rotate(src);src.recycle();src=next;}
   Bitmap cropped=PaperEdges.crop(src,corners,paper);src.recycle();
   Bitmap output=cropped;
   if(filter==1)output=Docs.enhance(cropped);
   else if(filter==2)output=Docs.grayscale(cropped);
   else if(filter==3)output=Docs.bw(cropped);
   if(output!=cropped)cropped.recycle();
   File temp=new File(page.getParentFile(),page.getName()+".pending");
   Docs.saveBitmap(output,temp,95);output.recycle();
   Docs.replace(temp,page);
   getSharedPreferences("settings",MODE_PRIVATE).edit().putString("paper",paper).apply();
   runOnUiThread(()->{setResult(RESULT_OK);finish();});
  }catch(Exception e){runOnUiThread(()->{saving=false;new AlertDialog.Builder(this).setTitle("Crop failed").setMessage(e.getMessage()).setPositiveButton("OK",null).show();});}}).start();
 }
 @Override protected void onDestroy(){super.onDestroy();if(image!=null&&!image.isRecycled())image.recycle();}
 static final class CornerView extends View {
   Bitmap bitmap;float[] corners=defaultCorners();RectF picture=new RectF();Paint paint=new Paint(3);int selected=-1;boolean touched=false;
   CornerView(Context c,Bitmap bmp){super(c);bitmap=bmp;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
   void frame(){float dw=getWidth(),dh=getHeight();float scale=Math.min((dw-24)/bitmap.getWidth(),(dh-24)/bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;picture.set((dw-w)/2,(dh-h)/2,(dw+w)/2,(dh+h)/2);}
   float sx(float x){return picture.left+x*picture.width();}float sy(float y){return picture.top+y*picture.height();}
   @Override protected void onDraw(Canvas c){super.onDraw(c);if(bitmap==null||bitmap.isRecycled())return;frame();c.drawColor(0xff031126);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);c.drawBitmap(bitmap,null,picture,paint);
    Path path=new Path();path.moveTo(sx(corners[0]),sy(corners[1]));for(int i=1;i<4;i++)path.lineTo(sx(corners[i*2]),sy(corners[i*2+1]));path.close();
    paint.setColor(0x66003572);paint.setStyle(Paint.Style.FILL);c.drawPath(path,paint);
    paint.setColor(0xff00c6ff);paint.setStrokeWidth(4);paint.setStyle(Paint.Style.STROKE);c.drawPath(path,paint);
    paint.setStyle(Paint.Style.FILL);for(int i=0;i<4;i++){float x=sx(corners[i*2]),y=sy(corners[i*2+1]);paint.setColor(Color.WHITE);c.drawCircle(x,y,19,paint);paint.setColor(0xff098ce9);c.drawCircle(x,y,14,paint);}
   }
   @Override public boolean onTouchEvent(android.view.MotionEvent ev){int a=ev.getActionMasked();if(a==MotionEvent.ACTION_DOWN){touched=true;frame();float best=Float.MAX_VALUE;for(int i=0;i<4;i++){float dx=ev.getX()-sx(corners[2*i]),dy=ev.getY()-sy(corners[2*i+1]);float dist=dx*dx+dy*dy;if(dist<best){best=dist;selected=i;}}return true;}
    if(a==MotionEvent.ACTION_MOVE&&selected>=0){corners[2*selected]=Math.max(0,Math.min(1,(ev.getX()-picture.left)/picture.width()));corners[2*selected+1]=Math.max(0,Math.min(1,(ev.getY()-picture.top)/picture.height()));invalidate();return true;}
    if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL){selected=-1;return true;}return true;}
 }
}
