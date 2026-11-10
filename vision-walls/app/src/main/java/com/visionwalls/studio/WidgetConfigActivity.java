package com.visionwalls.studio;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.util.LruCache;

/** Standard Android widget setup: user selects one of 240 per-widget presets. */
public final class WidgetConfigActivity extends Activity {
    private int widgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
    private final LruCache<Integer,Bitmap> cache=new LruCache<Integer,Bitmap>(10*1024*1024){
        @Override protected int sizeOf(Integer key,Bitmap value){return value.getByteCount();}
    };
    @Override public void onCreate(Bundle state){super.onCreate(state);setResult(RESULT_CANCELED);
        widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        if(widgetId==AppWidgetManager.INVALID_APPWIDGET_ID){finish();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(12,16,12,12);root.setBackgroundColor(0xFF071125);
        TextView h=new TextView(this);h.setText("Choose a widget style • 270 designs");h.setTextColor(Color.WHITE);h.setTextSize(20);h.setPadding(8,8,8,18);root.addView(h);
        GridView grid=new GridView(this);grid.setNumColumns(2);grid.setHorizontalSpacing(8);grid.setVerticalSpacing(8);
        grid.setAdapter(new BaseAdapter(){
            public int getCount(){return Art.COUNT;}public Object getItem(int p){return p;}public long getItemId(int p){return p;}
            public View getView(int position,View recycled,ViewGroup parent){
                ImageView img=recycled instanceof ImageView?(ImageView)recycled:new ImageView(WidgetConfigActivity.this);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);img.setLayoutParams(new AbsListView.LayoutParams(-1,155));
                Bitmap b=cache.get(position);if(b==null){b=Art.bitmap(position,200,125,0);cache.put(position,b);}
                img.setImageBitmap(b);return img;
            }
        });root.addView(grid,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        grid.setOnItemClickListener((a,v,pos,id)->{
            getSharedPreferences("vision_walls",MODE_PRIVATE).edit().putInt("widget_"+widgetId,pos).apply();
            AppWidgetManager mgr=AppWidgetManager.getInstance(this);
            android.appwidget.AppWidgetProviderInfo info=mgr.getAppWidgetInfo(widgetId);
            if(info!=null && info.provider!=null && info.provider.getClassName().contains("VisionCompact"))
                VisionCompactWidgetProvider.update(this,mgr,widgetId);
            else VisionWidgetProvider.update(this,mgr,widgetId);
            Intent ok=new Intent();ok.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId);setResult(RESULT_OK,ok);finish();
        });
    }
}
