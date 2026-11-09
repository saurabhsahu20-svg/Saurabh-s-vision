package com.visionone.scanner;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends ComponentActivity {
    private static final int NAVY=0xff071d42, BLUE=0xff0879eb, INK=0xff122b4d, MUTED=0xff5d7291;
    private static final int IMPORT_IMAGES=41, SAVE_PDF_TO_PHONE=42, EDIT_CROP=43;
    private final ExecutorService jobs=Executors.newSingleThreadExecutor();
    private final ArrayList<File> pages=new ArrayList<>();
    private LinearLayout body;
    private File exportPdf;
    private boolean working=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        loadPages();
        if(getIntent().getBooleanExtra("showPages",false))review();else home();
    }
    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);loadPages();
        if(intent.getBooleanExtra("showPages",false))review();else home();
    }
    @Override protected void onDestroy(){jobs.shutdown();super.onDestroy();}
    private int dp(float size){return (int)(getResources().getDisplayMetrics().density*size+.5f);}
    private GradientDrawable bg(int color,int radius){
        GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));return b;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);t.setTextColor(color);t.setTextSize(size);
        t.setFontFeatureSettings("kern");
        if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
        t.setPadding(dp(4),dp(7),dp(4),dp(7));
        return t;
    }
    private TextView button(String caption,int color,int textColor,Runnable click){
        TextView t=text(caption,17,textColor,true);t.setGravity(Gravity.CENTER);
        t.setMinimumHeight(dp(60));t.setBackground(bg(color,16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(6));
        body.addView(t,p);t.setOnClickListener(v->click.run());return t;
    }
    private void shell(String title,String subtitle){
        LinearLayout parent=new LinearLayout(this);parent.setOrientation(LinearLayout.VERTICAL);parent.setBackgroundColor(0xfff3f7fc);
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(22),dp(24),dp(22),dp(20));header.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable color=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{NAVY,0xff1154a6});
        header.setBackground(color);
        TextView brand=text("▣  VISION ONE",26,0xffffffff,true);header.addView(brand);
        TextView tagline=text("Scan Smart. Save Simple.",14,0xffb7ddff,false);header.addView(tagline);
        TextView screen=text(title,21,0xffffffff,true);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(12);header.addView(screen,sp);
        if(subtitle!=null){TextView hint=text(subtitle,13,0xffd5e6fa,false);header.addView(hint);}
        parent.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        body=new LinearLayout(this);body.setPadding(dp(18),dp(16),dp(18),dp(26));body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);parent.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(parent);
    }
    private void loadPages(){
        pages.clear();
        File[] files=Docs.session(this).listFiles((f,n)->n.endsWith(".jpg")&&f.isFile());
        if(files!=null){Arrays.sort(files,Comparator.comparing(File::getName));Collections.addAll(pages,files);}
    }
    private void home(){
        loadPages();
        shell("Scan documents. Create PDFs.","Simple offline scanner • No login • Private documents");
        TextView info=text("ONE TAP DOCUMENT SCANNING",13,MUTED,true);body.addView(info);
        button("📷   SCAN NOW",BLUE,0xffffffff,()->startCamera(true));
        button("▧   IMPORT PHOTOS",0xffe0efff,NAVY,()->chooseImages(true));
        if(!pages.isEmpty()){
            button("✓   CONTINUE SCAN  •  "+pages.size()+" PAGE(S)",0xffd9f8eb,0xff075d46,this::review);
        }
        TextView small=text("Auto document edges • Adjust 4 corners • Multiple pages • High-quality PDF",14,MUTED,false);
        small.setPadding(dp(5),dp(15),dp(5),dp(24));body.addView(small);
        button("📄   MY SAVED PDFs",0xffe8eff8,NAVY,this::library);
        TextView note=text("Your scans stay on your phone. Save or share a PDF when you're finished.",13,MUTED,false);body.addView(note);
    }
    private void confirmStart(Runnable action){
        loadPages();
        if(pages.isEmpty()){action.run();return;}
        new AlertDialog.Builder(this).setTitle("Start new document?")
            .setMessage("Current unsaved scanned pages will be replaced. Saved PDFs will not be deleted.")
            .setPositiveButton("Start new",(d,w)->action.run())
            .setNegativeButton("Keep pages",null).show();
    }
    private void startCamera(boolean fresh){
        Runnable go=()->{if(fresh)Docs.clearPages(this);Intent i=new Intent(this,CameraScreen.class);startActivity(i);};
        if(fresh)confirmStart(go);else go.run();
    }
    private void chooseImages(boolean fresh){
        Runnable go=()->{
            if(fresh)Docs.clearPages(this);
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
            startActivityForResult(i,IMPORT_IMAGES);
        };
        if(fresh)confirmStart(go);else go.run();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==EDIT_CROP){review();return;}
        if(result!=RESULT_OK||data==null)return;
        if(request==IMPORT_IMAGES){
            ArrayList<Uri> list=new ArrayList<>();
            if(data.getClipData()!=null){
                for(int i=0;i<data.getClipData().getItemCount();i++)list.add(data.getClipData().getItemAt(i).getUri());
            }else if(data.getData()!=null)list.add(data.getData());
            if(list.isEmpty()){review();return;}
            busy("Importing "+list.size()+" image(s)…");
            jobs.execute(()->{
                int imported=0;String failure=null;
                for(Uri uri:list){File target=Docs.newPage(this);try{Docs.importImage(this,uri,target);imported++;}
                catch(Exception e){target.delete();failure=e.getMessage();}}
                final int count=imported;final String err=failure;
                runOnUiThread(()->{working=false;review();if(count==0&&err!=null)error(err);else if(err!=null)toast(count+" imported. Some images could not be read.");});
            });
        } else if(request==SAVE_PDF_TO_PHONE && exportPdf!=null && data.getData()!=null){
            File saved=exportPdf;
            Uri dest=data.getData();
            busy("Saving PDF to selected folder…");
            jobs.execute(()->{
                try(InputStream in=new FileInputStream(saved);OutputStream out=getContentResolver().openOutputStream(dest,"w")){
                    if(out==null)throw new IOException("Storage folder unavailable");
                    byte[] bytes=new byte[32768];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
                    runOnUiThread(()->{working=false;new AlertDialog.Builder(this).setTitle("PDF saved to phone")
                        .setMessage("Your PDF is saved in the selected folder.")
                        .setPositiveButton("OK",(d,w)->library()).show();});
                }catch(Exception e){runOnUiThread(()->{working=false;error("Could not save: "+e.getMessage());});}
            });
        }
    }
    private void review(){
        loadPages();
        shell("Review your pages",pages.size()+" page(s) ready • Crop or rotate before PDF");
        if(pages.isEmpty()){
            body.addView(text("No pages yet. Scan a document to begin.",17,INK,false));
            button("📷 SCAN NOW",BLUE,0xffffffff,()->startCamera(false));
            button("HOME",0xffe0efff,INK,this::home);return;
        }
        button("✓   CREATE PDF  ("+pages.size()+" PAGES)",BLUE,0xffffffff,this::saveDialog);
        button("＋   SCAN MORE PAGES",0xffdcefff,NAVY,()->startCamera(false));
        button("＋   ADD PHOTOS",0xffeaf1fa,NAVY,()->chooseImages(false));
        for(int i=0;i<pages.size();i++){
            final int index=i;File file=pages.get(i);
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12),dp(10),dp(12),dp(12));card.setBackground(bg(0xffffffff,14));
            LinearLayout.LayoutParams margin=new LinearLayout.LayoutParams(-1,-2);
            margin.setMargins(0,dp(9),0,dp(9));body.addView(card,margin);
            TextView heading=text("PAGE "+(i+1),16,INK,true);card.addView(heading);
            ImageView thumbnail=new ImageView(this);thumbnail.setAdjustViewBounds(true);
            thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);thumbnail.setBackgroundColor(0xffeef3f8);
            Bitmap picture=Docs.read(file,640);
            if(picture!=null)thumbnail.setImageBitmap(picture);
            card.addView(thumbnail,new LinearLayout.LayoutParams(-1,dp(225)));
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER);card.addView(row);
            smallButton(row,"✂ CROP",()->crop(file));
            smallButton(row,"↻ ROTATE",()->rotate(file));
            smallButton(row,"↑",()->move(index,-1));
            smallButton(row,"↓",()->move(index,1));
            smallButton(row,"✕",()->remove(file));
            thumbnail.setOnClickListener(v->crop(file));
        }
        button("CREATE PDF",BLUE,0xffffffff,this::saveDialog);
        button("HOME",0xffe9f0fa,NAVY,this::home);
    }
    private void smallButton(LinearLayout row,String label,Runnable click){
        TextView view=text(label,12,BLUE,true);view.setGravity(Gravity.CENTER);
        view.setPadding(0,dp(15),0,dp(15));
        row.addView(view,new LinearLayout.LayoutParams(0,-2,1));
        view.setOnClickListener(v->click.run());
    }
    private void crop(File file){
        Intent i=new Intent(this,CropActivity.class);
        i.putExtra("page",file.getAbsolutePath());
        startActivityForResult(i,EDIT_CROP);
    }
    private void move(int index,int offset){
        int to=index+offset;
        if(to<0||to>=pages.size())return;
        Collections.swap(pages,index,to);
        try{Docs.persistOrder(this,pages);}catch(Exception e){error("Unable to reorder: "+e.getMessage());}
        review();
    }
    private void remove(File file){
        new AlertDialog.Builder(this).setTitle("Remove scanned page?")
            .setMessage("The PDF files you already created will remain safe.")
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Remove",(d,w)->{
                file.delete();new File(Docs.originals(this),file.getName()).delete();review();
            }).show();
    }
    private void rotate(File file){
        busy("Rotating page…");
        jobs.execute(()->{
            try{
                // Rotate the currently cropped image; original remains untouched for recropping.
                Bitmap image=Docs.read(file,3200);
                if(image==null)throw new IOException("Page unreadable");
                Bitmap rotated=Docs.rotate(image);
                File pending=new File(file.getParentFile(),file.getName()+".pending");
                Docs.saveBitmap(rotated,pending,93);Docs.replace(pending,file);
                image.recycle();rotated.recycle();
                runOnUiThread(()->{working=false;review();});
            }catch(Exception e){runOnUiThread(()->{working=false;error(e.getMessage());review();});}
        });
    }
    private void saveDialog(){
        loadPages();if(pages.isEmpty()){error("First scan a page.");return;}
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20),dp(5),dp(20),dp(2));
        EditText name=new EditText(this);
        name.setSingleLine(true);name.setText("Vision_Scan_"+new java.text.SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date()));
        name.setSelectAllOnFocus(true);name.setHint("PDF file name");
        box.addView(name);
        box.addView(text("PDF paper size",15,INK,true));
        Spinner size=new Spinner(this);
        size.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,PaperEdges.SIZES));
        String previous=getSharedPreferences("settings",MODE_PRIVATE).getString("paper","Original proportions");
        for(int i=0;i<PaperEdges.SIZES.length;i++)if(previous.equals(PaperEdges.SIZES[i]))size.setSelection(i);
        box.addView(size);
        box.addView(text("Original proportions keeps the document's shape. Choose A4 for standard printing.",13,MUTED,false));
        new AlertDialog.Builder(this).setTitle("Create PDF").setView(box)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("CREATE",(d,w)->createPdf(name.getText().toString(),(String)size.getSelectedItem())).show();
    }
    private void createPdf(String name,String paper){
        if(working)return;
        String safe=name.replaceAll("[^a-zA-Z0-9_ -]","_").trim();
        if(safe.isEmpty())safe="Vision_Document";
        final String savedName=safe;
        final ArrayList<File> snapshot=new ArrayList<>(pages);
        getSharedPreferences("settings",MODE_PRIVATE).edit().putString("paper",paper).apply();
        busy("Creating high-quality PDF…");
        jobs.execute(()->{
            File output=null;
            try{
                output=Docs.newPdf(this,savedName);
                Docs.makePdf(this,snapshot,output,paper);
                File pdf=output;
                runOnUiThread(()->{working=false;created(pdf);});
            }catch(Exception e){
                if(output!=null)output.delete();
                String reason=e.getMessage();
                runOnUiThread(()->{working=false;error("PDF creation failed: "+reason);review();});
            }
        });
    }
    private void created(File pdf){
        exportPdf=pdf;
        new AlertDialog.Builder(this).setTitle("✓ PDF created successfully")
            .setMessage(pdf.getName()+"\n"+pages.size()+" scanned page(s)\n\nSave to a phone folder or share it.")
            .setPositiveButton("SAVE TO PHONE",(d,w)->export(pdf))
            .setNeutralButton("SHARE",(d,w)->share(pdf))
            .setNegativeButton("MY PDFs",(d,w)->library()).show();
    }
    private void export(File pdf){
        exportPdf=pdf;
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_TITLE,pdf.getName());
        startActivityForResult(i,SAVE_PDF_TO_PHONE);
    }
    private void share(File pdf){
        try{
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM,Docs.uri(this,pdf));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent,"Share PDF"));
        }catch(Exception e){error(e.getMessage());}
    }
    private void open(File pdf){
        try{
            Intent intent=new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Docs.uri(this,pdf),"application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent,"Open PDF"));
        }catch(Exception e){error("No PDF viewer found: "+e.getMessage());}
    }
    private void library(){
        shell("My PDFs","Created here and saved privately until you export them");
        File[] list=Docs.pdfDir(this).listFiles((dir,name)->name.toLowerCase(Locale.US).endsWith(".pdf"));
        if(list==null||list.length==0)body.addView(text("No saved PDFs yet. Tap Scan Now to create one.",16,MUTED,false));
        else{
            Arrays.sort(list,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
            for(File file:list)button("📄  "+file.getName(),0xffffffff,INK,()->fileActions(file));
        }
        button("📷 SCAN NOW",BLUE,0xffffffff,()->startCamera(true));
        button("HOME",0xffe0efff,NAVY,this::home);
    }
    private void fileActions(File file){
        String[] actions={"Open PDF","Save to phone folder","Share PDF","Delete PDF"};
        new AlertDialog.Builder(this).setTitle(file.getName())
            .setItems(actions,(d,which)->{
                if(which==0)open(file);
                else if(which==1)export(file);
                else if(which==2)share(file);
                else new AlertDialog.Builder(this).setTitle("Delete saved PDF?")
                    .setNegativeButton("Cancel",null)
                    .setPositiveButton("Delete",(a,b)->{file.delete();library();}).show();
            }).show();
    }
    private void busy(String message){
        working=true;Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private void error(String message){
        new AlertDialog.Builder(this).setTitle("VISION ONE")
            .setMessage(message==null?"Operation failed":message)
            .setPositiveButton("OK",null).show();
    }
}
