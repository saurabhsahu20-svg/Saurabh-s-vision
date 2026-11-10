package com.visionwalls.studio;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

public final class VisionCompactWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context ctx,AppWidgetManager mgr,int[] ids){
        for(int id:ids)update(ctx,mgr,id);
    }
    @Override public void onDeleted(Context ctx,int[] ids){
        android.content.SharedPreferences.Editor e=ctx.getSharedPreferences("vision_walls",Context.MODE_PRIVATE).edit();
        for(int id:ids)e.remove("widget_"+id);
        e.apply();
    }
    @Override public void onReceive(Context ctx,Intent intent){
        super.onReceive(ctx,intent);
        String a=intent.getAction();
        if(Intent.ACTION_POWER_CONNECTED.equals(a)||Intent.ACTION_POWER_DISCONNECTED.equals(a)
           ||Intent.ACTION_BATTERY_LOW.equals(a)||Intent.ACTION_BATTERY_OKAY.equals(a)){
           AppWidgetManager mgr=AppWidgetManager.getInstance(ctx);
           for(int id:mgr.getAppWidgetIds(new android.content.ComponentName(ctx,VisionCompactWidgetProvider.class)))update(ctx,mgr,id);
        }
    }
    public static void update(Context ctx,AppWidgetManager mgr,int wid){
        android.content.SharedPreferences pref=ctx.getSharedPreferences("vision_walls",Context.MODE_PRIVATE);
        int id=Math.floorMod(pref.getInt("widget_"+wid,pref.getInt("widget_default",0)),Art.COUNT);
        RemoteViews rv=new RemoteViews(ctx.getPackageName(),R.layout.vision_widget_compact);
        Bitmap bitmap=Art.bitmap(id,450,450,0);
        rv.setImageViewBitmap(R.id.widget_background,bitmap);
        rv.setTextViewText(R.id.widget_name,Art.CATEGORIES[id/Art.PER_CATEGORY].toUpperCase());
        rv.setTextViewText(R.id.widget_battery,VisionWidgetProvider.batteryText(ctx));
        PendingIntent click=PendingIntent.getActivity(ctx,wid,new Intent(ctx,MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root,click);
        mgr.updateAppWidget(wid,rv);
    }
}
