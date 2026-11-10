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
    public static void update(Context context,AppWidgetManager manager,int widgetId){
        int id=context.getSharedPreferences("vision_walls",Context.MODE_PRIVATE).getInt("widget_"+widgetId,
            context.getSharedPreferences("vision_walls",Context.MODE_PRIVATE).getInt("widget_default",0));
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.vision_widget);
        Bitmap image=Art.bitmap(id,580,260,0);
        views.setImageViewBitmap(R.id.widget_background,image);
        views.setTextViewText(R.id.widget_name,"VISION WALLS  •  "+Art.CATEGORIES[id/10]);
        views.setTextColor(R.id.widget_name,Color.WHITE);
        views.setTextColor(R.id.widget_clock,Color.WHITE);
        views.setTextColor(R.id.widget_date,Color.WHITE);
        PendingIntent click=PendingIntent.getActivity(context,widgetId,new Intent(context,MainActivity.class),
                 PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root,click);
        manager.updateAppWidget(widgetId,views);
    }
}
