package com.visionone.scanner;

import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.*;
import android.print.*;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import java.nio.file.*;
import java.io.*;
import java.util.*;

public class Docs {
 static File session(Context c){File f=new File(c.getFilesDir(),"pages");f.mkdirs();return f;}
 static File pdfDir(Context c){File f=new File(c.getFilesDir(),"pdfs");f.mkdirs();return f;}
 static File originals(Context c){File f=new File(c.getFilesDir(),"originals");f.mkdirs();return f;}
 static File original(Context c,File page){File orig=new File(originals(c),page.getName());return orig.exists()?orig:page;}
 static void ensureBackup(Context c,File page)throws IOException {File orig=new File(originals(c),page.getName());if(!orig.exists())Files.copy(page.toPath(),orig.toPath());}
 static void replace(File temporary,File destination)throws IOException {
   if(!temporary.exists())throw new IOException("Edited image missing");
   Files.move(temporary.toPath(),destination.toPath(),StandardCopyOption.REPLACE_EXISTING);
 }
 static void clearPages(Context c){File[] files=session(c).listFiles();if(files!=null)for(File f:files)f.delete();files=originals(c).listFiles();if(files!=null)for(File f:files)f.delete();}
 static void importImage(Context ctx,Uri uri,File file)throws Exception {
   android.graphics.BitmapFactory.Options info=new android.graphics.BitmapFactory.Options();info.inJustDecodeBounds=true;
   try(InputStream s=ctx.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(s,null,info);}
   int scale=1;while(Math.max(info.outWidth,info.outHeight)/scale>3500)scale*=2;
   android.graphics.BitmapFactory.Options opt=new android.graphics.BitmapFactory.Options();opt.inSampleSize=scale;
   Bitmap decoded;try(InputStream s=ctx.getContentResolver().openInputStream(uri)){decoded=BitmapFactory.decodeStream(s,null,opt);}
   if(decoded==null)throw new IOException("Could not read image");
   int exif=ExifInterface.ORIENTATION_NORMAL;try(InputStream stream=ctx.getContentResolver().openInputStream(uri)){
     ExifInterface tag=new ExifInterface(stream);exif=tag.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);
   }catch(Exception ignored){}
   Bitmap oriented=orient(decoded,exif);saveBitmap(oriented,file,95);if(oriented!=decoded)oriented.recycle();decoded.recycle();
 }
 static File newPage(Context c){return new File(session(c),"p"+System.nanoTime()+".jpg");}
 static File newPdf(Context c,String name){String safe=name.replaceAll("[^a-zA-Z0-9_ -]","_").trim();if(safe.isEmpty())safe="Scan";File f=new File(pdfDir(c),safe+".pdf");int i=2;while(f.exists())f=new File(pdfDir(c),safe+"_"+(i++)+".pdf");return f;}
 static String base(File f){return f.getName().replaceFirst("(?i)\\.pdf$","");}
 static Uri uri(Context c,File f){return FileProvider.getUriForFile(c,c.getPackageName()+".fileprovider",f);}
 static Bitmap read(File f,int max){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);int sample=1;while(Math.max(o.outWidth,o.outHeight)/sample>max)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;
   Bitmap decoded=BitmapFactory.decodeFile(f.getPath(),o);if(decoded==null)return null;
   int exif=ExifInterface.ORIENTATION_NORMAL;
   try{ExifInterface tag=new ExifInterface(f.getPath());exif=tag.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}catch(Exception ignored){}
   Bitmap oriented=orient(decoded,exif);if(oriented!=decoded)decoded.recycle();return oriented;
 }
 static Bitmap orient(Bitmap b,int exif){
   Matrix matrix=new Matrix();
   if(exif==ExifInterface.ORIENTATION_ROTATE_90)matrix.postRotate(90);
   else if(exif==ExifInterface.ORIENTATION_ROTATE_180)matrix.postRotate(180);
   else if(exif==ExifInterface.ORIENTATION_ROTATE_270)matrix.postRotate(270);
   else if(exif==ExifInterface.ORIENTATION_FLIP_HORIZONTAL)matrix.preScale(-1,1);
   else if(exif==ExifInterface.ORIENTATION_FLIP_VERTICAL)matrix.preScale(1,-1);
   else if(exif==ExifInterface.ORIENTATION_TRANSPOSE){matrix.preScale(-1,1);matrix.postRotate(90);}
   else if(exif==ExifInterface.ORIENTATION_TRANSVERSE){matrix.preScale(-1,1);matrix.postRotate(270);}
   else return b;
   return Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),matrix,true);
 }
 static void saveBitmap(Bitmap b,File f,int quality)throws Exception {try(OutputStream out=new FileOutputStream(f)){if(!b.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new IOException("JPEG encoder error");}}
 static Bitmap rotate(Bitmap src){Matrix m=new Matrix();m.postRotate(90);return Bitmap.createBitmap(src,0,0,src.getWidth(),src.getHeight(),m,true);}
 static Bitmap enhance(Bitmap src){int w=src.getWidth(),h=src.getHeight();Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] px=new int[w];for(int y=0;y<h;y++){src.getPixels(px,0,w,0,y,w,1);for(int x=0;x<w;x++){int p=px[x],r=Color.red(p),g=Color.green(p),b=Color.blue(p);r=Math.min(255,Math.max(0,(int)((r-127)*1.17+141)));g=Math.min(255,Math.max(0,(int)((g-127)*1.17+141)));b=Math.min(255,Math.max(0,(int)((b-127)*1.17+141)));px[x]=Color.rgb(r,g,b);}out.setPixels(px,0,w,0,y,w,1);}return out;}
 static Bitmap bw(Bitmap src){int w=src.getWidth(),h=src.getHeight();Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] px=new int[w];for(int y=0;y<h;y++){src.getPixels(px,0,w,0,y,w,1);for(int x=0;x<w;x++){int p=px[x];int gray=(Color.red(p)*30+Color.green(p)*59+Color.blue(p)*11)/100;int val=gray<158?0:255;px[x]=Color.rgb(val,val,val);}out.setPixels(px,0,w,0,y,w,1);}return out;}
 static void persistOrder(Context c,List<File> files){ArrayList<File> tmp=new ArrayList<>(),orig=new ArrayList<>();
   for(int i=0;i<files.size();i++){File f=files.get(i);File dest=new File(session(c),"working_"+i+".jpg");File backup=new File(originals(c),f.getName());File backupTemp=new File(originals(c),"working_"+i+".jpg");if(f.renameTo(dest)){tmp.add(dest);if(backup.exists()&&backup.renameTo(backupTemp))orig.add(backupTemp);else orig.add(null);}}
   for(int i=0;i<tmp.size();i++){String name=String.format(java.util.Locale.US,"page_%04d.jpg",i);tmp.get(i).renameTo(new File(session(c),name));if(orig.get(i)!=null)orig.get(i).renameTo(new File(originals(c),name));}
 }
 static Bitmap grayscale(Bitmap src){Bitmap out=Bitmap.createBitmap(src.getWidth(),src.getHeight(),Bitmap.Config.ARGB_8888);Canvas c=new Canvas(out);Paint p=new Paint(3);android.graphics.ColorMatrix cm=new android.graphics.ColorMatrix();cm.setSaturation(0f);p.setColorFilter(new android.graphics.ColorMatrixColorFilter(cm));c.drawBitmap(src,0,0,p);return out;}
 static boolean processCapture(Context ctx,File photo,String mode)throws Exception {
   ensureBackup(ctx,photo);Bitmap source=read(original(ctx,photo),3300);if(source==null)throw new IOException("Cannot read camera photo");
   Bitmap output=source;boolean auto=false;
   float[] corners=PaperEdges.suggest(source);
   if(corners!=null){output=PaperEdges.crop(source,corners,"Original proportions");auto=true;}
   if("question".equals(mode)){Bitmap bw=bw(output);if(output!=source)output.recycle();output=bw;}
   File pending=new File(photo.getParentFile(),photo.getName()+".pending");
   saveBitmap(output,pending,95);replace(pending,photo);
   if(output!=source)output.recycle();source.recycle();return auto;
 }
 static void makePdf(Context ctx,List<File> images,File file)throws Exception{
   String paper=ctx.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("paper","A4");makePdf(ctx,images,file,paper);
 }
 static void makePdf(Context ctx,List<File> images,File file,String paper)throws Exception {PdfDocument doc=new PdfDocument();try{
   int number=0;
   for(File f:images){Bitmap bitmap=read(f,3000);if(bitmap==null)continue;
     int pw=595,ph=842;
     if("A5".equals(paper)){pw=420;ph=595;}
     else if("Letter".equals(paper)){pw=612;ph=792;}
     else if("Legal".equals(paper)){pw=612;ph=1008;}
     else if("ID card".equals(paper)){pw=243;ph=153;}
     else if("Original proportions".equals(paper)){pw=595;ph=Math.max(200,Math.min(1200,Math.round(595f*bitmap.getHeight()/bitmap.getWidth())));}
     PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(pw,ph,++number).create();
     PdfDocument.Page page=doc.startPage(pi);Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
     float margin=Math.min(24,Math.min(pw,ph)*0.06f);
     float scale=Math.min((pw-2*margin)/bitmap.getWidth(),(ph-2*margin)/bitmap.getHeight());
     float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;
     RectF target=new RectF((pw-width)/2,(ph-height)/2,(pw+width)/2,(ph+height)/2);
     canvas.drawBitmap(bitmap,null,target,new Paint(3));doc.finishPage(page);bitmap.recycle();
   }
   if(number==0)throw new IOException("No valid pages");
   try(OutputStream out=new FileOutputStream(file)){doc.writeTo(out);}
 }finally{doc.close();}}
 static void print(Context ctx,File f){PrintManager pm=(PrintManager)ctx.getSystemService(Context.PRINT_SERVICE);pm.print("VISION ONE - "+f.getName(),new PrintDocumentAdapter(){
  @Override public void onLayout(PrintAttributes oldA,PrintAttributes newA,android.os.CancellationSignal signal,LayoutResultCallback cb,Bundle extras){PrintDocumentInfo info=new PrintDocumentInfo.Builder(f.getName()).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build();cb.onLayoutFinished(info,true);}
  @Override public void onWrite(PageRange[] pages,ParcelFileDescriptor destination,android.os.CancellationSignal cancellation,WriteResultCallback cb){try(InputStream in=new FileInputStream(f);OutputStream out=new FileOutputStream(destination.getFileDescriptor())){byte[] buffer=new byte[16384];int r;while((r=in.read(buffer))>0)out.write(buffer,0,r);cb.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});}catch(Exception e){cb.onWriteFailed(e.getMessage());}}
 },null);}
 static void combine(Context c,List<File> sources,File out,boolean small)throws Exception {PdfDocument finalDoc=new PdfDocument();int number=0;try{
  for(File source:sources){try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(source,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){
   for(int i=0;i<renderer.getPageCount();i++){try(PdfRenderer.Page input=renderer.openPage(i)){
    int w=small?900:1700,h=Math.max(1,(int)((float)w*input.getHeight()/input.getWidth()));Bitmap image=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);image.eraseColor(Color.WHITE);input.render(image,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
    PdfDocument.Page pg=finalDoc.startPage(new PdfDocument.PageInfo.Builder(595,842,++number).create());Canvas can=pg.getCanvas();can.drawColor(Color.WHITE);can.drawBitmap(image,null,new RectF(0,0,595,842),new Paint(3));finalDoc.finishPage(pg);image.recycle();
   }}
  }}
  try(FileOutputStream stream=new FileOutputStream(out)){finalDoc.writeTo(stream);}
 }finally{finalDoc.close();}}
 static void split(Context c,File input)throws Exception{try(ParcelFileDescriptor fd=ParcelFileDescriptor.open(input,ParcelFileDescriptor.MODE_READ_ONLY);PdfRenderer renderer=new PdfRenderer(fd)){
  for(int i=0;i<renderer.getPageCount();i++){try(PdfRenderer.Page page=renderer.openPage(i)){
   Bitmap bm=Bitmap.createBitmap(1400,Math.max(1,(int)(1400f*page.getHeight()/page.getWidth())),Bitmap.Config.ARGB_8888);bm.eraseColor(Color.WHITE);page.render(bm,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
   PdfDocument doc=new PdfDocument();try{PdfDocument.Page output=doc.startPage(new PdfDocument.PageInfo.Builder(595,842,1).create());output.getCanvas().drawBitmap(bm,null,new RectF(0,0,595,842),new Paint(3));doc.finishPage(output);try(OutputStream os=new FileOutputStream(newPdf(c,base(input)+"_page_"+(i+1)))){doc.writeTo(os);}}finally{doc.close();bm.recycle();}
  }}
 }}
}