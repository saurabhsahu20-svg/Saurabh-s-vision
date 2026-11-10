package com.visionwalls.studio;

import android.app.Activity;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private final int navy=0xFF071125, card=0xFF10223D, cyan=0xFF63DBFF, muted=0xFFB5C6E1;
    private int mode=0, filter=0, pendingSave=-1;
    private GridView grid;
    private ImageAdapter adapter;
    private final ArrayList<Integer> shown=new ArrayList<>();
    private final LruCache<String,Bitmap> thumbs=new LruCache<String,Bitmap>(12*1024*1024){
        @Override protected int sizeOf(String key,Bitmap b){return b.getByteCount();}
    };

    @Override public void onCreate(Bundle state){super.onCreate(state);renderHome();}
    private int dp(float px){return (int)(getResources().getDisplayMetrics().density*px+.5f);}
    private GradientDrawable background(int fill,int border,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));
        if(border!=0)d.setStroke(dp(1),border);return d;
    }
    private TextView label(String text,int size,int col,boolean bold){
        TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setTextColor(col);v.setGravity(Gravity.CENTER_VERTICAL);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
    }
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private void renderHome(){
        LinearLayout root=column();root.setBackgroundColor(navy);root.setPadding(dp(14),dp(14),dp(14),dp(7));setContentView(root);
        TextView brand=label("✦  VISION WALLS",27,Color.WHITE,true);root.addView(brand);
        TextView subtitle=label("240 Wallpapers  •  240 Live  •  240 Widgets",12,muted,false);
        LinearLayout.LayoutParams sublp=new LinearLayout.LayoutParams(-1,dp(30));root.addView(subtitle,sublp);
        LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setWeightSum(3);
        String[] names={"WALLPAPERS","LIVE","WIDGETS"};
        for(int i=0;i<3;i++){
            final int j=i;TextView tab=label(names[i],12,i==mode?navy:Color.WHITE,true);
            tab.setGravity(Gravity.CENTER);tab.setBackground(background(i==mode?cyan:card,i==mode?0:0xFF274264,14));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(43),1);p.setMargins(dp(3),0,dp(3),0);
            tabs.addView(tab,p);tab.setOnClickListener(v->{mode=j;renderHome();});
        }
        root.addView(tabs);
        Spinner category=new Spinner(this);
        ArrayList<String> categories=new ArrayList<>();categories.add("All 24 categories");
        for(String c:Art.CATEGORIES)categories.add(c);
        ArrayAdapter<String> spinAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,categories);
        category.setAdapter(spinAdapter);category.setSelection(filter);
        LinearLayout.LayoutParams spinlp=new LinearLayout.LayoutParams(-1,dp(51));spinlp.setMargins(0,dp(8),0,dp(4));
        root.addView(category,spinlp);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){filter=pos;refresh();}
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        TextView hint=label(mode==0?"Tap an artwork to preview, apply or save":
                mode==1?"Animated art • tap to preview and set":"Pick a style • add a live clock widget",13,muted,false);
        root.addView(hint,new LinearLayout.LayoutParams(-1,dp(34)));
        grid=new GridView(this);grid.setNumColumns(2);grid.setVerticalSpacing(dp(10));grid.setHorizontalSpacing(dp(10));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);grid.setClipToPadding(false);
        adapter=new ImageAdapter();grid.setAdapter(adapter);
        root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));
        grid.setOnItemClickListener((parent,view,pos,id)->showDetail(shown.get(pos)));
        TextView footer=label("FREE • OFFLINE • NO ADS • ORIGINAL ART",11,0xFF86A9CF,true);
        footer.setGravity(Gravity.CENTER);root.addView(footer,new LinearLayout.LayoutParams(-1,dp(33)));
        refresh();
    }
    private void refresh(){
        if(adapter==null)return;shown.clear();
        for(int i=0;i<Art.COUNT;i++)if(filter==0 || i/10==filter-1)shown.add(i);
        adapter.notifyDataSetChanged();if(grid!=null)grid.setSelection(0);
    }
    private class ImageAdapter extends android.widget.BaseAdapter {
        @Override public int getCount(){return shown.size();}
        @Override public Object getItem(int p){return shown.get(p);}
        @Override public long getItemId(int p){return shown.get(p);}
        @Override public View getView(int position,View convert,ViewGroup parent){
            LinearLayout box;ImageView preview;TextView title;
            if(convert==null){
                box=column();box.setPadding(dp(5),dp(5),dp(5),dp(6));box.setBackground(background(card,0xFF224264,15));
                preview=new ImageView(MainActivity.this);preview.setId(19);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                box.addView(preview,new LinearLayout.LayoutParams(-1,dp(225)));
                title=label("",12,Color.WHITE,true);title.setId(20);title.setPadding(dp(4),dp(7),0,0);
                box.addView(title,new LinearLayout.LayoutParams(-1,dp(30)));
            } else {box=(LinearLayout)convert;preview=box.findViewById(19);title=box.findViewById(20);}
            int id=shown.get(position);String key="s"+id;
            Bitmap b=thumbs.get(key);if(b==null){b=Art.bitmap(id,200,310,0);thumbs.put(key,b);}
            preview.setImageBitmap(b);
            title.setText(Art.CATEGORIES[id/10]+"  #"+(id%10+1));return box;
        }
    }
    private void showDetail(int id){
        Dialog dialog=new Dialog(this);LinearLayout body=column();body.setPadding(dp(14),dp(12),dp(14),dp(14));
        body.setBackground(background(navy,0xFF35517B,20));
        TextView title=label(Art.CATEGORIES[id/10]+"  •  Design "+(id%10+1),18,Color.WHITE,true);
        body.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));
        ImageView sample=new ImageView(this);sample.setScaleType(ImageView.ScaleType.FIT_CENTER);
        sample.setImageBitmap(Art.bitmap(id,540,850,0));body.addView(sample,new LinearLayout.LayoutParams(-1,dp(475)));
        String action=mode==0?"SET AS WALLPAPER":mode==1?"SET LIVE WALLPAPER":"ADD HOME WIDGET";
        Button primary=new Button(this);primary.setText(action);primary.setTextColor(navy);
        primary.setBackground(background(cyan,0,12));
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(52));blp.setMargins(0,dp(10),0,dp(4));body.addView(primary,blp);
        primary.setOnClickListener(v->{dialog.dismiss();if(mode==0)setStatic(id);else if(mode==1)setLive(id);else pinWidget(id);});
        if(mode==0){Button save=new Button(this);save.setText("SAVE ORIGINAL FULL HD IMAGE");
            save.setTextColor(Color.WHITE);save.setBackground(background(card,0xFF35517B,12));
            body.addView(save,new LinearLayout.LayoutParams(-1,dp(50)));
            save.setOnClickListener(v->{dialog.dismiss();saveImage(id);});}
        else if(mode==2){TextView help=label("Or long-press your home screen → Widgets → Vision Walls to choose any of 240 designs.",12,muted,false);
            help.setPadding(dp(2),dp(9),dp(2),0);body.addView(help);}
        dialog.setContentView(body);if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.95f),-2);}dialog.show();
        if(dialog.getWindow()!=null)dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*.95f),-2);
    }
    private void setStatic(int id){
        Toast.makeText(this,"Applying wallpaper…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            try{Bitmap b=Art.bitmap(id,1080,1920,0);WallpaperManager.getInstance(this).setBitmap(b);
                b.recycle();runOnUiThread(()->toast("Wallpaper applied ✓"));}
            catch(Exception e){runOnUiThread(()->toast("Could not apply: "+e.getMessage()));}
        }).start();
    }
    private void saveImage(int id){pendingSave=id;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_TITLE,"VisionWalls_"+(id+1)+".jpg");startActivityForResult(intent,401);
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==401 && result==RESULT_OK && data!=null && data.getData()!=null && pendingSave>=0){
            final int id=pendingSave;final android.net.Uri uri=data.getData();pendingSave=-1;
            new Thread(()->{try(OutputStream stream=getContentResolver().openOutputStream(uri)){
                if(stream==null)throw new IllegalStateException("Save location unavailable");
                Bitmap b=Art.bitmap(id,1080,1920,0);b.compress(Bitmap.CompressFormat.JPEG,94,stream);b.recycle();
                runOnUiThread(()->toast("Saved full-HD image ✓"));
            }catch(Exception ex){runOnUiThread(()->toast("Save failed: "+ex.getMessage()));}}).start();
        }
    }
    private void setLive(int id){
        getSharedPreferences("vision_walls",MODE_PRIVATE).edit().putInt("live_theme",id).apply();
        try{Intent i=new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,new ComponentName(this,VisionLiveService.class));startActivity(i);
        }catch(Exception e){toast("Live wallpapers are not supported by this device");}
    }
    private void pinWidget(int id){
        getSharedPreferences("vision_walls",MODE_PRIVATE).edit().putInt("widget_default",id).apply();
        AppWidgetManager wm=AppWidgetManager.getInstance(this);
        if(Build.VERSION.SDK_INT>=26 && wm.isRequestPinAppWidgetSupported()){
            wm.requestPinAppWidget(new ComponentName(this,VisionWidgetProvider.class),null,null);
            toast("Choose Add; customize when prompted");
        }else toast("Long-press Home screen → Widgets → Vision Walls");
    }
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
}
