package com.visionwalls.studio;
import android.graphics.*;
import java.util.Random;

/** Education series: original hand-coded poster art. Static and live show the same design. */
public final class PosterArt {
    private static final String[] MATH={
        "MATHS MAGIC","THE WORLD OF MATH","NUMBERS & WONDER","GEOMETRY UNIVERSE","ALGEBRA ADVENTURE",
        "INFINITE IDEAS","MATHEMATICS","SHAPES & PATTERNS","EXPLORE NUMBERS","MATH MAKES SENSE"};
    private static final String[] SCIENCE={
        "SCIENCE WORLD","THE POWER OF ATOMS","DISCOVER SCIENCE","DNA DISCOVERY","CHEMISTRY MAGIC",
        "THE SOLAR SYSTEM","PHYSICS IN ACTION","SPACE EXPLORERS","CURIOUS MINDS","INVENT THE FUTURE"};
    private static final String[] STUDY={
        "LEARN & GROW","DREAM BIG","BUILD YOUR FUTURE","NEVER STOP LEARNING","SMALL STEPS",
        "FOCUS & ACHIEVE","OPEN A NEW WORLD","STUDY SMART","IMAGINE MORE","BRIGHT BEGINNINGS"};
    private static final String[] SUB={"Where numbers meet imagination","Explore, discover and create","Learn today, lead tomorrow"};
    private static Paint p(int col){Paint x=new Paint(3);x.setColor(col);return x;}
    private static int a(int c,int alpha){return (c&0xffffff)|(Math.max(0,Math.min(255,alpha))<<24);}
    private static void glow(Canvas c,float x,float y,float r,int col){
        Paint q=p(col);q.setShader(new RadialGradient(x,y,r,new int[]{a(col,160),a(col,32),Color.TRANSPARENT},null,Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r,q);
    }
    private static void text(Canvas c,String s,float x,float y,float size,int col,boolean bold){
        Paint t=p(col);t.setTextSize(size);t.setTextAlign(Paint.Align.CENTER);
        t.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif-medium",Typeface.NORMAL));
        t.setShadowLayer(bold?9f:3f,1f,2f,Color.BLACK);c.drawText(s,x,y,t);
    }
    private static void stroke(Canvas c,int col,float width,float x1,float y1,float x2,float y2){
        Paint p=p(col);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x1,y1,x2,y2,p);
    }
    private static void book(Canvas c,int glowCol){
        glow(c,540,1455,420,glowCol);
        Path left=new Path();left.moveTo(533,1475);left.cubicTo(438,1401,273,1405,99,1440);
        left.lineTo(122,1637);left.cubicTo(308,1585,453,1594,537,1653);left.close();
        Paint pg=p(0xFFFFF6D3);pg.setShader(new LinearGradient(100,1420,525,1638,
           new int[]{0xFFFFFDF0,0xFFFFE5A0,0xFFB57A35},null,Shader.TileMode.CLAMP));c.drawPath(left,pg);
        Path right=new Path();right.moveTo(533,1475);right.cubicTo(625,1401,790,1405,981,1440);
        right.lineTo(958,1637);right.cubicTo(758,1585,628,1594,537,1653);right.close();
        Paint pr=p(0xFFFFEFC7);pr.setShader(new LinearGradient(535,1450,973,1650,
           new int[]{0xFFFFFDF1,0xFFFFDE93,0xFF8C4A20},null,Shader.TileMode.CLAMP));c.drawPath(right,pr);
        for(int i=0;i<9;i++){float y=1485+i*13;
          stroke(c,a(0xFF9F742A,80),2,175,y,490,y+18);
          stroke(c,a(0xFF9F742A,80),2,591,y+18,906,y);}
        stroke(c,0xFFFFC565,7,92,1651,540,1690);stroke(c,0xFFFFC565,7,540,1690,988,1651);
    }
    private static void maths(Canvas c,int idx,float t){
        book(c,0xFFFFC35A);
        String[] symbols={"π","√","∞","7","8","3","5","×","+","÷","%","∑","9","2"};
        int[] col={0xFFFFD457,0xFF5CF4FF,0xFFFF7DC8,0xFF82CAFF,0xFF8EFFC0,0xFFFA8DAE};
        for(int i=0;i<symbols.length;i++){
            float x=165+((i*227+idx*31)%755),y=580+((i*167+idx*21)%660);
            float wig=(float)Math.sin(t*.53+i)*17;
            glow(c,x,y+wig,72,col[i%col.length]);
            text(c,symbols[i],x,y+wig+35,75+(i%3)*13,col[i%col.length],true);
        }
        Paint grid=p(0x8839D9FF);grid.setStyle(Paint.Style.STROKE);grid.setStrokeWidth(4);
        c.drawOval(220,680,880,1090,grid);
        stroke(c,0xFF97EEFF,4,280,1135,800,1135);
        stroke(c,0xFF97EEFF,4,540,920,540,1300);
        Path wave=new Path();for(int x=180;x<900;x+=9){float y=1160+(float)Math.sin((x-180)*.014+t*.06)*64;
            if(x==180)wave.moveTo(x,y);else wave.lineTo(x,y);}
        Paint neon=p(0xFFB387FF);neon.setStyle(Paint.Style.STROKE);neon.setStrokeWidth(8);c.drawPath(wave,neon);
        text(c,idx%2==0?"a² + b² = c²":"sin²θ + cos²θ = 1",540,1350,45,0xFFFFE59B,false);
    }
    private static void science(Canvas c,int idx,float t){
        book(c,0xFF53E5FF);
        int blue=0xFF51E5FF,pink=0xFFED70FF,gold=0xFFFFD071;
        glow(c,540,964,405,blue);glow(c,540,965,182,pink);
        Paint orbit=p(blue);orbit.setStyle(Paint.Style.STROKE);orbit.setStrokeWidth(8);
        for(int i=0;i<4;i++){
            c.save();c.rotate(i*44+(float)Math.sin(t*.2)*3,540,950);
            c.drawOval(245,804,835,1098,orbit);
            c.restore();
        }
        for(int i=0;i<14;i++){float x=477+(i%4)*42,y=889+(i/4)*48;
           glow(c,x,y,45,i%2==0?blue:pink);
           c.drawCircle(x,y,25,p(i%2==0?blue:pink));}
        for(int i=0;i<9;i++){float y=600+i*62;
            float x=130+(float)Math.sin(i*.72+t*.08)*65;
            stroke(c,a(gold,110),6,x,y,1080-x,y+23);}
        text(c,idx%3==0?"H₂O":idx%3==1?"E = mc²":"F = ma",540,1350,68,gold,true);
    }
    private static void study(Canvas c,int idx,float t){
        book(c,0xFFFFB75D);
        int[] color={0xFFFFD164,0xFF72E2FF,0xFFFF81CD,0xFF91FFBD};
        String[] labels={"READ","LEARN","CREATE","GROW","DISCOVER"};
        for(int i=0;i<5;i++){
            float x=160+((i*199+idx*42)%750),y=642+i*126+(float)Math.sin(t*.45+i)*11;
            glow(c,x,y,95,color[i%4]);text(c,labels[i],x,y,33,color[i%4],true);
        }
        for(int i=0;i<8;i++){
            float x=160+i*111,y=610+(float)Math.sin(i*.9)*140;
            stroke(c,a(color[i%4],115),5,540,1450,x,y);
        }
        text(c,idx%2==0?"KNOWLEDGE IS POWER":"KEEP LEARNING",540,1360,43,0xFFFFD484,true);
    }
    public static void draw(Canvas canvas,int posterId,int w,int h,float t){
        posterId=Math.floorMod(posterId,30);
        float sx=w/1080f,sy=h/1920f;canvas.save();canvas.scale(sx,sy);
        int category=posterId/10,variant=posterId%10;
        int main=category==0?0xFFFFC765:category==1?0xFF69DCFF:0xFFFBC784;
        Paint background=p(Color.BLACK);
        background.setShader(new LinearGradient(0,0,1080,1920,
            category==1?0xFF030C31:0xFF020D23,
            category==0?0xFF171246:category==1?0xFF1C0D44:0xFF1E1236,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,1080,1920,background);
        Random random=new Random(202607L+posterId*9817);
        for(int i=0;i<100;i++){
           float x=random.nextInt(1080),y=random.nextInt(1920),r=1+random.nextInt(4);
           int light=(i%4==0?main:0xFFE8F8FF);
           int brightness=90+(int)(80*(.5+.5*Math.sin(t*.7+i)));
           canvas.drawCircle(x,y,r,p(a(light,brightness)));
        }
        glow(canvas,540,990,610,variant%2==0?0xFF3468E8:0xFF8522C3);
        if(category==0)maths(canvas,variant,t);
        else if(category==1)science(canvas,variant,t);
        else study(canvas,variant,t);
        String title=category==0?MATH[variant]:category==1?SCIENCE[variant]:STUDY[variant];
        float sz=title.length()>18?58:title.length()>14?69:91;
        text(canvas,category==0?"MATHEMATICS":category==1?"SCIENCE":"STUDY & SUCCESS",540,245,38,0xFF95E5FF,true);
        glow(canvas,540,355,300,main);
        text(canvas,title,540,375,sz,main,true);
        text(canvas,SUB[category],540,456,35,Color.WHITE,false);
        text(canvas,"VISION WALLS  •  EDUCATION",540,1826,31,0xFFE2EDFF,true);
        canvas.restore();
    }
}
