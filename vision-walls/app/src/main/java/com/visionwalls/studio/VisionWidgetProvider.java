package com.visionwalls.studio;

import android.appwidget.AppWidgetManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.widget.RemoteViews;

/** Styleable clock/date widget; different instances can use different styles. */
public final class VisionWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m,int[] ids){for(int id:ids)update(c,m,id);}
    @Override public void onDeleted(Context c,int[] ids){android.content.SharedPreferences.Editor e=c.getSharedPreferences("vision_walls",Context.MODE_PRIVATE).edit();for(int id:ids)e.remove("widget_"+id);e.apply();}
    @Override public void onReceive(Context c,Intent intent) {
        super.onReceive(c,intent);
        String action=intent.getAction();
        if(Intent.ACTION_POWER_CONNECTED.equals(action) || Intent.ACTION_POWER_DISCONNECTED.equals(action)
            || Intent.ACTION_BATTERY_LOW.equals(action) || Intent.ACTION_BATTERY_OKAY.equals(action)) {
            AppWidgetManager manager=AppWidgetManager.getInstance(c);
            int[] ids=manager.getAppWidgetIds(new android.content.ComponentName(c,VisionWidgetProvider.class));
            for(int id:ids)update(c,manager,id);
        }
    }
    static String batteryText(Context c) {
        Intent battery=c.registerReceiver(null,new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(battery==null)return "BATTERY  --";
        int level=battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL,-1);
        int scale=battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE,-1);
        if(level<0||scale<=0)return "BATTERY  --";
        return "BATTERY  "+Math.round(level*100f/scale)+"%";
    }
    public static void update(Context context,AppWidgetManager manager,int widgetId){
        int id=context.getSharedPreferences("vision_walls",Context.MODE_PRIVATE).getInt("widget_"+widgetId,
            context.getSharedPreferences("vision_walls",Context.MODE_PRIVATE).getInt("widget_default",0));
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.vision_widget);
        Bitmap image=Art.bitmap(id,580,260,0);
        views.setImageViewBitmap(R.id.widget_background,image);
        views.setTextViewText(R.id.widget_name," DIGITAL •  "+Art.CATEGORIES[id/Art.PER_CATEGORY]);
        views.setTextColor(R.id.widget_name,Color.WHITE);
        views.setTextColor(R.id.widget_clock,Color.WHITE);
        views.setTextColor(R.id.widget_date,Color.WHITE);
        views.setTextViewText(R.id.widget_battery,batteryText(context));
        PendingIntent click=PendingIntent.getActivity(context,widgetId,new Intent(context,MainActivity.class),
                 PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root,click);
        manager.updateAppWidget(widgetId,views);
    }
}
