package com.visionwalls.studio;

import android.graphics.*;
import java.util.Random;

/** Original, deterministic procedural artwork: 24 subjects × 10 color variants. */
public final class Art {
    public static final int COUNT = 270;
    public static final String[] CATEGORIES = {
      "Galaxies", "Mountains", "Flowers", "Neon", "Ocean", "Planets",
      "Geometry", "Aurora", "Wildlife", "Cyberpunk", "Mandala", "Abstract",
      "Deep Space", "Sunsets", "Botanical", "Laser Art", "Beaches", "Moons",
      "Minimal", "Northern Lights", "Animals", "City Lights", "Lotus", "Textures",
      "Maths Magic", "Science Discoveries", "Study & Success"
    };
    private static final int[] TONES = {
       0xFF53D9FF,0xFF9F80FF,0xFFFFC765,0xFFFF4ED9,0xFF4EF7B8,
       0xFF5A9FFF,0xFFF56D71,0xFF83FFD3,0xFFFFD481,0xFF47E3FF,
       0xFFE7B5FF,0xFFFF855B
    };
    private Art() {}
    public static int accent(int id) { return TONES[Math.floorMod(id/10 + id%10, TONES.length)]; }
    private static int color(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));
        return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));
    }
    private static int alpha(int rgb,int a){return (a<<24)|(rgb&0xFFFFFF);}
    private static Paint p(int c){Paint x=new Paint(3);x.setColor(c);return x;}
    public static Bitmap bitmap(int id,int width,int height,float time) {
        Bitmap b=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);draw(c,id,width,height,time);return b;
    }
    public static void draw(Canvas c,int id,int w,int h,float t) {
        id=Math.floorMod(id,COUNT);
        if(id>=240){PosterArt.draw(c,id-240,w,h,t);return;}
        int family=id/10, variation=id%10, main=accent(id), second=TONES[(family+4+variation)%TONES.length];
        Random r=new Random(id*9959L+703);
        Paint base=p(Color.BLACK);
        base.setShader(new LinearGradient(0,0,w,h, color(0xFF020715,main,.14f),color(0xFF030C20,second,.12f),Shader.TileMode.CLAMP));
        c.drawRect(0,0,w,h,base);base.setShader(null);
        // Deep luminous bokeh layers, consistent across all themes.
        for(int i=0;i<32;i++) {
            float x=r.nextFloat()*w,y=r.nextFloat()*h,rad=(.04f+r.nextFloat()*.22f)*w;
            Paint glow=p(main);glow.setShader(new RadialGradient(x,y,rad,new int[]{alpha(i%2==0?main:second,52),alpha(main,8),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
            c.drawCircle(x,y,rad,glow);
        }
        int kind=family%12;
        if(kind==0) galaxy(c,w,h,t,r,main,second);
        else if(kind==1) mountains(c,w,h,t,r,main,second);
        else if(kind==2) flowers(c,w,h,t,r,main,second);
        else if(kind==3) neon(c,w,h,t,r,main,second);
        else if(kind==4) ocean(c,w,h,t,r,main,second);
        else if(kind==5) planets(c,w,h,t,r,main,second);
        else if(kind==6) geo(c,w,h,t,r,main,second);
        else if(kind==7) aurora(c,w,h,t,r,main,second);
        else if(kind==8) wildlife(c,w,h,t,r,main,second);
        else if(kind==9) city(c,w,h,t,r,main,second);
        else if(kind==10) mandala(c,w,h,t,r,main,second);
        else ribbon(c,w,h,t,r,main,second);
        // Faint atmospheric dust, subtly animated for live mode.
        for(int i=0;i<95;i++) {
            float x=r.nextFloat()*w,y=r.nextFloat()*h;
            float radius=(.5f+r.nextFloat()*1.7f)*w/480f;
            Paint dot=p(alpha(i%3==0?second:Color.WHITE,55+r.nextInt(120)));
            c.drawCircle(x+(float)Math.sin(t+i)*2*w/480f,y,radius,dot);
        }
    }
    private static void halo(Canvas c,float x,float y,float radius,int col){
        Paint g=p(col);g.setShader(new RadialGradient(x,y,radius,new int[]{alpha(col,170),alpha(col,35),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,radius,g);
    }
    private static void galaxy(Canvas c,int w,int h,float t,Random r,int a,int b){
        float cx=w*.52f,cy=h*.47f;
        halo(c,cx,cy,w*.50f,a); halo(c,cx,cy,w*.22f,b);
        for(int arm=0;arm<4;arm++)for(int i=0;i<620;i++){
            float f=r.nextFloat(),theta=f*11.5f+(float)(arm*Math.PI*.5)+t*.14f+r.nextFloat()*.20f;
            float radius=(.04f+f*.56f)*w;float x=cx+(float)Math.cos(theta)*radius;
            float y=cy+(float)Math.sin(theta)*radius*.90f;
            Paint s=p(alpha(arm%2==0?a:b,70+r.nextInt(180)));
            c.drawCircle(x,y, (.5f+r.nextFloat()*2.1f)*w/480f,s);
        }
        halo(c,cx,cy,w*.12f,0xFFFFE0A6);
    }
    private static void mountains(Canvas c,int w,int h,float t,Random r,int a,int b){
        float sunX=w*(.42f+r.nextFloat()*.2f),sunY=h*.33f;
        halo(c,sunX,sunY,w*.34f,0xFFFFB879);c.drawCircle(sunX,sunY,w*.095f,p(0xFFFFDDA0));
        for(int layer=0;layer<6;layer++){
            float y=h*(.50f+layer*.095f); Path path=new Path();path.moveTo(0,h);
            path.lineTo(0,y);
            for(int x=0;x<=w+w/3;x+=w/3){path.lineTo(x+w/7f,y-(.12f+r.nextFloat()*.24f)*h+layer*.008f*h);path.lineTo(x+w/3f,y);}
            path.lineTo(w,h);path.close();
            Paint pp=p(color(0xFF081631,layer%2==0?a:b,.12f+(5-layer)*.023f));
            c.drawPath(path,pp);
        }
        for(int i=0;i<12;i++){float yy=h*(.78f+i*.017f);Paint ln=p(alpha(a,40));ln.setStrokeWidth(1+i%2);c.drawLine(0,yy,w,yy,ln);}
    }
    private static void flowers(Canvas c,int w,int h,float t,Random r,int a,int b){
        Paint stem=p(alpha(0xFF49E7A8,150));stem.setStrokeWidth(w*.008f);
        for(int k=0;k<8;k++){
            float x=w*(.12f+r.nextFloat()*.76f),y=h*(.25f+r.nextFloat()*.60f),rr=w*(.075f+r.nextFloat()*.08f);
            c.drawLine(x,h,x,y,stem);
            for(int i=0;i<9;i++){float th=(float)(i*Math.PI*2/9+t*.05f);float px=x+(float)Math.cos(th)*rr*.8f,py=y+(float)Math.sin(th)*rr*.8f;
                halo(c,px,py,rr*.9f,i%2==0?a:b);c.drawCircle(px,py,rr*.48f,p(alpha(i%2==0?a:b,180)));}
            c.drawCircle(x,y,rr*.48f,p(0xFFFFD878));
        }
    }
    private static void neon(Canvas c,int w,int h,float t,Random r,int a,int b){
        for(int k=0;k<18;k++){
            float cx=w*.5f,cy=h*(.15f+k*.047f);float rad=w*(.19f+k*.018f);
            Paint stroke=p(alpha(k%2==0?a:b,80+k*8));stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeWidth(Math.max(1,w*.006f));
            c.drawOval(cx-rad,cy-rad*.32f,cx+rad,cy+rad*.32f,stroke);
        }
        for(int k=0;k<8;k++){
            float x=w*(k+.5f)/8f;Paint st=p(alpha(a,85));st.setStrokeWidth(w*.005f);
            c.drawLine(x,0,w-x,h,st);
        }
        halo(c,w*.5f,h*.48f,w*.25f,a);
    }
    private static void ocean(Canvas c,int w,int h,float t,Random r,int a,int b){
        halo(c,w*.53f,h*.37f,w*.35f,0xFFFFA55D);
        c.drawCircle(w*.53f,h*.36f,w*.13f,p(0xFFFFCA90));
        Paint sea=p(color(0xFF04192C,a,.2f));c.drawRect(0,h*.59f,w,h,sea);
        for(int n=0;n<70;n++){
            float y=h*(.61f+n*.006f),xx=r.nextFloat()*w;
            Paint line=p(alpha(n%4==0?0xFFFFC28F:a,25+r.nextInt(80)));line.setStrokeWidth((1+n%3)*w/480f);
            c.drawLine(xx,y,Math.min(w,xx+w*(.04f+r.nextFloat()*.36f)),y,line);
        }
        Path shore=new Path();shore.moveTo(0,h);shore.lineTo(0,h*.87f);shore.quadTo(w*.42f,h*.80f,w,h*.92f);shore.lineTo(w,h);shore.close();
        c.drawPath(shore,p(alpha(0xFFDFC3A2,120)));
    }
    private static void planets(Canvas c,int w,int h,float t,Random r,int a,int b){
        halo(c,w*.47f,h*.39f,w*.36f,a);
        for(int i=0;i<8;i++){
            float x=w*(.1f+r.nextFloat()*.8f),y=h*(.12f+r.nextFloat()*.76f),rad=w*(.035f+r.nextFloat()*.17f);
            Paint planet=p(b);planet.setShader(new RadialGradient(x-rad*.4f,y-rad*.4f,rad*2,
                new int[]{0xFFFFFFFF,i%2==0?a:b,0xFF06102B},null,Shader.TileMode.CLAMP));
            c.drawCircle(x,y,rad,planet);
            if(i%3==0){Paint orbit=p(alpha(0xFFBCE9FF,160));orbit.setStyle(Paint.Style.STROKE);orbit.setStrokeWidth(w*.004f);c.drawOval(x-rad*1.65f,y-rad*.3f,x+rad*1.65f,y+rad*.3f,orbit);}
        }
    }
    private static void geo(Canvas c,int w,int h,float t,Random r,int a,int b){
        for(int k=0;k<14;k++){
            float cx=w*r.nextFloat(),cy=h*r.nextFloat(),radius=w*(.09f+r.nextFloat()*.20f);
            int sides=3+r.nextInt(6);Path path=new Path();for(int i=0;i<=sides;i++){
                float th=(float)(2*Math.PI*i/sides+t*.04f),x=cx+(float)Math.cos(th)*radius,y=cy+(float)Math.sin(th)*radius;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            Paint edge=p(alpha(k%2==0?a:b,190));edge.setStyle(Paint.Style.STROKE);edge.setStrokeWidth(w*.0035f);c.drawPath(path,edge);
            halo(c,cx,cy,radius*.35f,k%2==0?a:b);
        }
    }
    private static void aurora(Canvas c,int w,int h,float t,Random r,int a,int b){
        for(int k=0;k<9;k++){
            Path path=new Path();float offset=k*w*.12f-w*.15f;
            for(int y=0;y<=h;y+=h/45){float x=offset+(float)Math.sin(y/(float)h*8+t*.5f+k*.5f)*w*.3f + w*.23f;
                if(y==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            Paint stroke=p(alpha(k%2==0?a:b,90));stroke.setStrokeWidth(w*(.035f+k*.006f));stroke.setStyle(Paint.Style.STROKE);stroke.setStrokeCap(Paint.Cap.ROUND);
            c.drawPath(path,stroke);
        }
        halo(c,w*.5f,h*.45f,w*.4f,b);
    }
    private static void wildlife(Canvas c,int w,int h,float t,Random r,int a,int b){
        // Stylised glowing soaring birds and forest branches.
        for(int i=0;i<24;i++){
            float x=w*r.nextFloat(),y=h*(.1f+r.nextFloat()*.65f),s=w*(.03f+r.nextFloat()*.12f);
            Paint bird=p(alpha(i%2==0?a:b,140+r.nextInt(110)));bird.setStyle(Paint.Style.STROKE);bird.setStrokeWidth(w*.005f);bird.setStrokeCap(Paint.Cap.ROUND);
            Path wings=new Path();wings.moveTo(x-s,y);wings.quadTo(x-s*.4f,y-s*.7f,x,y);wings.quadTo(x+s*.4f,y-s*.7f,x+s,y);c.drawPath(wings,bird);
        }
        for(int i=0;i<10;i++){float x=w*(i+.5f)/10;Paint st=p(alpha(0xFF33B397,80));st.setStrokeWidth(w*.007f);
            c.drawLine(x,h,x+(float)Math.sin(i*2.1)*w*.10f,h*.68f,st);}
        halo(c,w*.5f,h*.32f,w*.24f,a);
    }
    private static void city(Canvas c,int w,int h,float t,Random r,int a,int b){
        halo(c,w*.5f,h*.38f,w*.35f,a);
        Paint road=p(alpha(0xFF070A1E,220));c.drawRect(0,h*.79f,w,h,road);
        for(int i=0,x=0;x<w;i++){
            int bw=w/14+r.nextInt(w/10+1),height=h/8+r.nextInt(h/2);
            int top=(int)(h*.78f)-height;
            c.drawRect(x,top,x+bw,h*.80f,p(0xFF060D22));
            Paint windows=p(alpha(i%2==0?a:b,170));
            for(int yy=top+14;yy<h*.78f;yy+=Math.max(8,w/40))for(int xx=x+8;xx<x+bw-5;xx+=Math.max(8,w/35))if(r.nextFloat()>.42) c.drawRect(xx,yy,xx+2,yy+4,windows);
            x+=bw+2;
        }
        for(int k=0;k<22;k++){float x=r.nextFloat()*w,y=h*(.81f+r.nextFloat()*.19f);Paint line=p(alpha(k%2==0?a:b,40+r.nextInt(120)));line.setStrokeWidth(w*.006f);c.drawLine(x,y,x,y+h*.05f,line);}
    }
    private static void mandala(Canvas c,int w,int h,float t,Random r,int a,int b){
        float cx=w*.5f,cy=h*.48f;halo(c,cx,cy,w*.37f,a);
        for(int ring=0;ring<6;ring++){
            float rr=w*(.10f+ring*.075f);
            Paint line=p(alpha(ring%2==0?a:b,190));line.setStyle(Paint.Style.STROKE);line.setStrokeWidth(w*.004f);
            c.drawCircle(cx,cy,rr,line);
            for(int i=0;i<12+ring*4;i++){
                float th=(float)(Math.PI*2*i/(12+ring*4)+t*.055f*(ring%2==0?1:-1));
                float x=cx+(float)Math.cos(th)*rr,y=cy+(float)Math.sin(th)*rr;
                c.drawCircle(x,y,w*(ring%2==0?.010f:.007f),p(i%2==0?a:b));
            }
        }
    }
    private static void ribbon(Canvas c,int w,int h,float t,Random r,int a,int b){
        for(int j=0;j<14;j++){
            Path path=new Path();float offset=j*w*.06f;
            for(int i=0;i<=110;i++){
                float y=h*i/110f,x=w*.5f+(float)Math.sin(y/(float)h*9+t*.36f+j*.26f)*w*.37f+offset-w*.43f;
                if(i==0)path.moveTo(x,y);else path.lineTo(x,y);
            }
            Paint stroke=p(alpha(j%2==0?a:b,70+j*8));stroke.setStrokeWidth(w*(.012f+j*.001f));stroke.setStyle(Paint.Style.STROKE);c.drawPath(path,stroke);
        }
    }
}
