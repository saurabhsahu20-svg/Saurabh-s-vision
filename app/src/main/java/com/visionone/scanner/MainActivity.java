package com.visionone.scanner;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.os.*;
import android.print.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.core.content.FileProvider;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import java.io.*;
import java.util.*;

public class MainActivity extends ComponentActivity {
    static final int NAVY=0xff07214a, BLUE=0xff0674e4;
    static final ArrayList<File> PAGES=new ArrayList<>();
    final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    LinearLayout root, body;
    TextView count;
    String mode="smart";
    static final int IMPORT=30;
    @Override public void onCreate(Bundle b){super.onCreate(b);loadSession();home();}
    void loadSession(){PAGES.clear();File[] all=Docs.session(this).listFiles((f,n)->n.endsWith(".jpg"));if(all!=null){Arrays.sort(all,Comparator.comparing(File::getName));Collections.addAll(PAGES,all);}}
    static TextView txt(Context c,String s,int sz,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(sz);t.setTextColor(color);t.setPadding(8,12,8,12);return t;}
    TextView button(String text,Runnable fn){TextView t=txt(this,text,17,NAVY);t.setGravity(Gravity.CENTER);t.setBackground(round(0xffe6f3ff,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,5,0,5);body.addView(t,p);t.setOnClickListener(v->fn.run());return t;}
    android.graphics.drawable.GradientDrawable round(int color,int rad){android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(color);g.setCornerRadius(rad);return g;}
    void shell(String title){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xfff5f9ff);TextView head=txt(this,"VISION ONE     •     "+title,20,-1);head.setBackgroundColor(NAVY);head.setPadding(18,30,10,25);root.addView(head);ScrollView sc=new ScrollView(this);body=new LinearLayout(this);body.setPadding(16,14,16,24);body.setOrientation(LinearLayout.VERTICAL);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);}
    void home(){shell("Scan Smart. Save Simple.");TextView welcome=txt(this,"Document scanner • Offline PDF creator",16,NAVY);body.addView(welcome);
      button("📷   SMART SCAN",()->start("smart",true));
      button("⚡   AUTO SCAN",()->start("auto",true));
      button("📝   QUESTION PAPER MODE",()->start("question",true));
      button("📚   BATCH SCAN",()->start("batch",true));
      button("🖼   IMPORT IMAGES",()->importImages(true));
      button("📂   MY PDFs",this::library);
      button("🕘   RECENT DOCUMENTS",this::library);
      button("🛠   PDF TOOLS (Merge / Split / Compress)",this::tools);
      button("✍   REVIEW SCANNED PAGES ("+PAGES.size()+")",this::editor);
      TextView info=txt(this,"Files stay on your phone. No login or cloud upload.",13,0xff66778f);body.addView(info);
    }
    void start(String m,boolean fresh){mode=m;if(fresh)clearSession();Intent i=new Intent(this,CameraScreen.class);i.putExtra("mode",m);startActivity(i);}
    void clearSession(){for(File f:PAGES)f.delete();PAGES.clear();}
    void importImages(boolean fresh){if(fresh)clearSession();Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,IMPORT);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==IMPORT&&result==RESULT_OK&&data!=null){try{
       if(data.getClipData()!=null){for(int j=0;j<data.getClipData().getItemCount();j++)saveImported(data.getClipData().getItemAt(j).getUri());}
       else if(data.getData()!=null)saveImported(data.getData());
       editor();
    }catch(Exception e){error(e);}}}
    void saveImported(Uri uri)throws Exception {Bitmap bitmap=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));if(bitmap==null)throw new IOException("Image could not be decoded");File f=Docs.newPage(this);Docs.saveBitmap(bitmap,f,90);bitmap.recycle();PAGES.add(f);}
    void editor(){loadSession();shell("Review pages");body.addView(txt(this,"Pages: "+PAGES.size()+"  •  A4 PDF",17,NAVY));button("＋ ADD MORE PAGES",()->{Intent i=new Intent(this,CameraScreen.class);i.putExtra("mode",mode);startActivity(i);});button("🖼 IMPORT MORE",()->importImages(false));if(PAGES.isEmpty()){button("GO HOME",this::home);return;}
      button("✓ SAVE AS PDF",this::saveDialog);
      for(int index=0;index<PAGES.size();index++){final int n=index;File f=PAGES.get(index);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(12,12,12,12);LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.setMargins(0,6,0,6);item.setBackground(round(-1,12));body.addView(item,layout);
         item.addView(txt(this,"PAGE "+(index+1),15,NAVY));ImageView preview=new ImageView(this);preview.setAdjustViewBounds(true);preview.setImageBitmap(Docs.read(f,850));item.addView(preview,new LinearLayout.LayoutParams(-1,380));
         LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);item.addView(row);
         mini(row,"↑",()->{if(n>0){Collections.swap(PAGES,n,n-1);Docs.persistOrder(this,PAGES);editor();}});
         mini(row,"↓",()->{if(n<PAGES.size()-1){Collections.swap(PAGES,n,n+1);Docs.persistOrder(this,PAGES);editor();}});
         mini(row,"↻",()->change(n,1));mini(row,"B&W",()->change(n,2));mini(row,"☀",()->change(n,3));
         mini(row,"OCR",()->ocr(f));mini(row,"✕",()->{f.delete();PAGES.remove(n);Docs.persistOrder(this,PAGES);editor();});
      }button("SAVE PDF",this::saveDialog);button("HOME",this::home);
    }
    void mini(LinearLayout row,String caption,Runnable run){TextView b=txt(this,caption,14,BLUE);b.setGravity(Gravity.CENTER);row.addView(b,new LinearLayout.LayoutParams(0,54,1));b.setOnClickListener(v->run.run());}
    void change(int index,int action){File file=PAGES.get(index);new Thread(()->{try{Bitmap img=Docs.read(file,3000);if(img==null)throw new IOException("Invalid page");Bitmap output=action==1?Docs.rotate(img):action==2?Docs.bw(img):Docs.enhance(img);Docs.saveBitmap(output,file,92);if(output!=img)output.recycle();img.recycle();runOnUiThread(this::editor);}catch(Exception e){runOnUiThread(()->error(e));}}).start();}
    void saveDialog(){EditText input=new EditText(this);input.setSingleLine();input.setText("Vision_Scan_"+System.currentTimeMillis());new AlertDialog.Builder(this).setTitle("Save A4 PDF").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Save",(d,w)->{
      String n=input.getText().toString().replaceAll("[^a-zA-Z0-9_ -]","_").trim();if(n.isEmpty())n="Document";final String name=n;
      new Thread(()->{try{File file=Docs.newPdf(this,name);Docs.makePdf(this,new ArrayList<>(PAGES),file);runOnUiThread(()->new AlertDialog.Builder(this).setTitle("PDF saved").setMessage(file.getName()).setPositiveButton("Open",(a,b)->view(file)).setNeutralButton("Share",(a,b)->share(file)).setNegativeButton("My PDFs",(a,b)->library()).show());}catch(Exception e){runOnUiThread(()->error(e));}}).start();
    }).show();}
    void library(){shell("My PDFs");File[] pdfs=Docs.pdfDir(this).listFiles((f,n)->n.endsWith(".pdf"));if(pdfs==null||pdfs.length==0){body.addView(txt(this,"No PDFs saved yet",16,NAVY));}else{Arrays.sort(pdfs,(a,b)->Long.compare(b.lastModified(),a.lastModified()));for(File f:pdfs){button("📄 "+f.getName(),()->fileMenu(f));}}button("NEW SCAN",()->start("smart",true));button("HOME",this::home);}
    void fileMenu(File f){String[] actions={"Open","Share","Print","Rename","Delete","Split into pages","Compress PDF"};new AlertDialog.Builder(this).setTitle(f.getName()).setItems(actions,(d,i)->{switch(i){case 0:view(f);break;case 1:share(f);break;case 2:Docs.print(this,f);break;case 3:rename(f);break;case 4:new AlertDialog.Builder(this).setTitle("Delete PDF?").setPositiveButton("Delete",(di,wi)->{f.delete();library();}).setNegativeButton("Cancel",null).show();break;case 5:pdfOperation(f,false);break;case 6:pdfOperation(f,true);break;}}).show();}
    void rename(File f){EditText t=new EditText(this);t.setSingleLine();t.setText(Docs.base(f));new AlertDialog.Builder(this).setTitle("Rename PDF").setView(t).setNegativeButton("Cancel",null).setPositiveButton("Rename",(d,w)->{String name=t.getText().toString().replaceAll("[^a-zA-Z0-9_ -]","_");File target=new File(f.getParentFile(),name+".pdf");if(target.exists())new AlertDialog.Builder(this).setMessage("File already exists").setPositiveButton("OK",null).show();else {f.renameTo(target);library();}}).show();}
    void view(File f){try{Intent in=new Intent(Intent.ACTION_VIEW);in.setDataAndType(Docs.uri(this,f),"application/pdf");in.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(in,"Open PDF"));}catch(Exception e){error(e);}}
    void share(File f){Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,Docs.uri(this,f));i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Share PDF"));}
    void ocr(File page){Bitmap b=Docs.read(page,1800);if(b==null)return;InputImage image=InputImage.fromBitmap(b,0);StringBuilder result=new StringBuilder();final int[] remaining={2};
      Runnable finish=()->{if(--remaining[0]==0){b.recycle();String text=result.toString();new AlertDialog.Builder(this).setTitle("OCR • English & Hindi").setMessage(text.isEmpty()?"No text detected":text).setPositiveButton("Copy",(d,w)->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Vision OCR",text));Toast.makeText(this,"Copied",Toast.LENGTH_SHORT).show();}).setNegativeButton("Close",null).show();}};
      TextRecognizer english=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),hindi=TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build());
      english.process(image).addOnSuccessListener(t->{result.append(t.getText()).append("\n");}).addOnCompleteListener(t->{english.close();finish.run();});
      hindi.process(image).addOnSuccessListener(t->{if(result.length()==0)result.append(t.getText());else result.append("\n").append(t.getText());}).addOnCompleteListener(t->{hindi.close();finish.run();});
    }
    void tools(){shell("PDF tools");button("MERGE TWO OR MORE SAVED PDFs",this::mergeSelect);button("SPLIT / COMPRESS MY PDFs",this::library);button("MY PDFs",this::library);button("HOME",this::home);}
    void mergeSelect(){File[] all=Docs.pdfDir(this).listFiles((f,n)->n.endsWith(".pdf"));if(all==null||all.length<2){Toast.makeText(this,"Save at least two PDFs first",Toast.LENGTH_LONG).show();return;}String[] choices=new String[all.length];boolean[] checked=new boolean[all.length];for(int i=0;i<all.length;i++)choices[i]=all[i].getName();
      new AlertDialog.Builder(this).setTitle("Select PDFs to merge").setMultiChoiceItems(choices,checked,(d,w,on)->checked[w]=on).setNegativeButton("Cancel",null).setPositiveButton("Merge",(d,w)->{
        ArrayList<File> selected=new ArrayList<>();for(int i=0;i<all.length;i++)if(checked[i])selected.add(all[i]);if(selected.size()<2){Toast.makeText(this,"Select at least 2 PDFs",Toast.LENGTH_SHORT).show();return;}
        new Thread(()->{try{File out=Docs.newPdf(this,"Merged");Docs.combine(this,selected,out,false);runOnUiThread(()->{Toast.makeText(this,"Merged: "+out.getName(),Toast.LENGTH_LONG).show();library();});}catch(Exception e){runOnUiThread(()->error(e));}}).start();
      }).show();
    }
    void pdfOperation(File f,boolean compress){new Thread(()->{try{if(compress){File o=Docs.newPdf(this,Docs.base(f)+"_small");Docs.combine(this,Collections.singletonList(f),o,true);}else Docs.split(this,f);runOnUiThread(()->{Toast.makeText(this,compress?"Compressed PDF saved":"Pages split into PDFs",Toast.LENGTH_LONG).show();library();});}catch(Exception e){runOnUiThread(()->error(e));}}).start();}
    void error(Exception e){new AlertDialog.Builder(this).setTitle("VISION ONE").setMessage(String.valueOf(e.getMessage())).setPositiveButton("OK",null).show();}
    @Override protected void onResume(){super.onResume();if(root!=null&&root.getParent()!=null){} }
}