package com.visionwalls.studio;

import android.service.wallpaper.WallpaperService;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;

/** Android live wallpaper with visibility-aware rendering and 240 selectable themes. */
public final class VisionLiveService extends WallpaperService {
    @Override public Engine onCreateEngine(){return new Renderer();}
    private class Renderer extends Engine {
        private final Handler handler=new Handler(Looper.getMainLooper());
        private boolean visible=false;
        private long start=android.os.SystemClock.uptimeMillis();
        private final Runnable frame=new Runnable(){@Override public void run(){paint();}};
        @Override public void onVisibilityChanged(boolean state){visible=state;if(state)paint();else handler.removeCallbacks(frame);}
        @Override public void onSurfaceChanged(SurfaceHolder holder,int format,int width,int height){super.onSurfaceChanged(holder,format,width,height);paint();}
        @Override public void onDestroy(){visible=false;handler.removeCallbacks(frame);super.onDestroy();}
        private void paint(){
            handler.removeCallbacks(frame);if(!visible)return;
            SurfaceHolder h=getSurfaceHolder();Canvas c=null;
            try{c=h.lockCanvas();if(c!=null){int theme=getSharedPreferences("vision_walls",MODE_PRIVATE).getInt("live_theme",0);
                Art.draw(c,theme,c.getWidth(),c.getHeight(),(android.os.SystemClock.uptimeMillis()-start)/1000f);
            }}catch(Exception ignored){}finally{if(c!=null)h.unlockCanvasAndPost(c);}
            if(visible)handler.postDelayed(frame,90); // ~11 FPS; battery-conscious
        }
    }
}
