package com.visionone.scanner;

import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.*;
import android.print.*;
import androidx.core.content.FileProvider;
import java.io.*;
import java.util.*;

public class Docs {
 static File session(Context c){File f=new File(c.getFilesDir(),"pages");f.mkdirs();return f;}
 static File pdfDir(Context c){File f=new File(c.getFilesDir(),"pdfs");f.mkdirs();return f;}
 static File newPage(Context c){return new File(session(c),"p"+System.nanoTime()+".jpg");}
 static File newPdf(Context c,String name){String safe=name.replaceAll("[^a-zA-Z0-9_ -]","_").trim();if(safe.isEmpty())safe="Scan";File f=new File(pdfDir(c),safe+".pdf");int i=2;while(f.exists())f=new File(pdfDir(c),safe+"_"+(i++)+".pdf");return f;}
 static String base(File f){return f.getName().replaceFirst("(?i)\\.pdf$","");}
 static Uri uri(Context c,File f){return FileProvider.getUriForFile(c,c.getPackageName()+".fileprovider",f);}
 static Bitmap read(File f,int max){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(f.getPath(),o);int sample=1;while(Math.max(o.outWidth,o.outHeight)/sample>max)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;return BitmapFactory.decodeFile(f.getPath(),o);}
 static void saveBitmap(Bitmap b,File f,int quality)throws Exception {try(OutputStream out=new FileOutputStream(f)){if(!b.compress(Bitmap.CompressFormat.JPEG,quality,out))throw new IOException("JPEG encoder error");}}
 static Bitmap rotate(Bitmap src){Matrix m=new Matrix();m.postRotate(90);return Bitmap.createBitmap(src,0,0,src.getWidth(),src.getHeight(),m,true);}
 static Bitmap enhance(Bitmap src){int w=src.getWidth(),h=src.getHeight();Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] px=new int[w];for(int y=0;y<h;y++){src.getPixels(px,0,w,0,y,w,1);for(int x=0;x<w;x++){int p=px[x],r=Color.red(p),g=Color.green(p),b=Color.blue(p);r=Math.min(255,Math.max(0,(int)((r-127)*1.17+141)));g=Math.min(255,Math.max(0,(int)((g-127)*1.17+141)));b=Math.min(255,Math.max(0,(int)((b-127)*1.17+141)));px[x]=Color.rgb(r,g,b);}out.setPixels(px,0,w,0,y,w,1);}return out;}
 static Bitmap bw(Bitmap src){int w=src.getWidth(),h=src.getHeight();Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);int[] px=new int[w];for(int y=0;y<h;y++){src.getPixels(px,0,w,0,y,w,1);for(int x=0;x<w;x++){int p=px[x];int gray=(Color.red(p)*30+Color.green(p)*59+Color.blue(p)*11)/100;int val=gray<158?0:255;px[x]=Color.rgb(val,val,val);}out.setPixels(px,0,w,0,y,w,1);}return out;}
 static void persistOrder(Context c,List<File> files){ArrayList<File> tmp=new ArrayList<>();for(int i=0;i<files.size();i++){File f=files.get(i);File dest=new File(session(c),"tmp_"+i+".jpg");if(f.renameTo(dest))tmp.add(dest);}for(int i=0;i<tmp.size();i++)tmp.get(i).renameTo(new File(session(c),String.format(java.util.Locale.US,"page_%04d.jpg",i)));}
 static void makePdf(Context ctx,List<File> images,File file)throws Exception{PdfDocument doc=new PdfDocument();try{
  for(File f:images){Bitmap bitmap=read(f,3200);if(bitmap==null)continue;PdfDocument.PageInfo pi=new PdfDocument.PageInfo.Builder(595,842,doc.getPages().size()+1).create();PdfDocument.Page page=doc.startPage(pi);Canvas can=page.getCanvas();can.drawColor(Color.WHITE);
   float scale=Math.min(560f/bitmap.getWidth(),807f/bitmap.getHeight());float width=bitmap.getWidth()*scale,height=bitmap.getHeight()*scale;RectF where=new RectF((595-width)/2,(842-height)/2,(595+width)/2,(842+height)/2);
   can.drawBitmap(bitmap,null,where,new Paint(3));doc.finishPage(page);bitmap.recycle();}
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