package com.visionwalls.studio;

import android.graphics.*;
import java.util.Random;

/**
 * VISION DIGITAL 2.0 — 240 original procedural science / mathematics designs.
 * All visuals are drawn with Android Canvas; none of the older landscape/poster
 * artwork is used. timeSeconds drives real animation for WallpaperService.
 */
public final class Art {
    public static final int PER_CATEGORY = 20;
    public static final int COUNT = 240;
    public static final String[] CATEGORIES = {
        "Digital Atom", "Mathematical Graphs", "Molecular Matrix", "DNA Helix",
        "Digital Heart", "Kidney & Nephron", "Cells & Tissues", "Cyber Circuits",
        "Fractal Geometry", "Solar System HUD", "Digital Botany", "Quantum Waves"
    };
    private static final String[][] LABELS = {
        {"ELECTRON CLOUD", "ATOMIC ENERGY", "QUANTUM SHELL", "ORBITAL SYSTEM"},
        {"FUNCTION MATRIX", "TRIGONOMETRY", "CALCULUS", "COORDINATE SPACE"},
        {"MOLECULAR GRID", "CHEMICAL BONDS", "ELEMENT SYSTEM", "COVALENT NETWORK"},
        {"DOUBLE HELIX", "GENETIC CODE", "DNA STRUCTURE", "BIOLOGICAL DATA"},
        {"CARDIAC SYSTEM", "ECG WAVE", "HUMAN HEART", "HEARTBEAT"},
        {"RENAL SYSTEM", "NEPHRON", "KIDNEY MODEL", "FILTRATION PATH"},
        {"CELL UNIVERSE", "TISSUE MATRIX", "MICROBIOLOGY", "CELLULAR STRUCTURE"},
        {"DIGITAL CIRCUIT", "MICROCHIP", "PROCESSOR GRID", "ELECTRONIC NETWORK"},
        {"FRACTAL SPACE", "FIBONACCI", "GOLDEN GEOMETRY", "POLYGON MATRIX"},
        {"ORBITAL MAP", "SOLAR SYSTEM", "PLANETARY DATA", "COSMIC HUD"},
        {"PHOTOSYNTHESIS", "LEAF STRUCTURE", "CHLOROPLAST", "BOTANICAL GRID"},
        {"WAVE FUNCTION", "QUANTUM FIELD", "FREQUENCY", "PARTICLE DYNAMICS"}
    };
    private static final int[] NEON = {
        0xFF37DFFB,0xFFAB7AFF,0xFF4FFFBC,0xFFFF65D1,
        0xFFFFC56A,0xFF6D9EFF,0xFFFE8B8E,0xFF89EDFF
    };
    private Art() {}
    public static int accent(int id) { return NEON[Math.floorMod(id / PER_CATEGORY + id % PER_CATEGORY, NEON.length)]; }
    private static Paint fill(int color) { Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(color); return p; }
    private static Paint edge(int color,float width) {
        Paint p=fill(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(width); p.setStrokeCap(Paint.Cap.ROUND); return p;
    }
    private static int fade(int color,int alpha) { return (Math.max(0,Math.min(alpha,255))<<24)|(color&0xFFFFFF); }
    public static Bitmap bitmap(int id,int w,int h,float seconds) {
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        draw(new Canvas(bitmap),id,w,h,seconds);
        return bitmap;
    }
    public static void draw(Canvas actual,int rawId,int width,int height,float t) {
        if(width<=0||height<=0)return;
        int id=Math.floorMod(rawId,COUNT), family=id/PER_CATEGORY, variant=id%PER_CATEGORY;
        int a=accent(id), b=NEON[(family*3+variant+2)%NEON.length];
        Canvas c=actual;
        c.save();
        c.scale(width/420f,height/820f);
        Paint bg=fill(0xFF030B20);
        bg.setShader(new LinearGradient(0,0,420,820,
             new int[]{0xFF020716,0xFF081531,0xFF170E30},null,Shader.TileMode.CLAMP));
        c.drawRect(0,0,420,820,bg);
        Random random=new Random(91729L+id*7349L);
        // Fine schematic grid and animated diagnostic dots.
        for(int x=0;x<=420;x+=28)c.drawLine(x,0,x,820,edge(fade(a,22),.55f));
        for(int y=0;y<=820;y+=28)c.drawLine(0,y,420,y,edge(fade(b,18),.55f));
        for(int n=0;n<46;n++){
            float px=random.nextFloat()*420f,py=random.nextFloat()*820f;
            float drift=(float)Math.sin(t*.75f+n*1.7f)*2.8f;
            c.drawCircle(px+drift,py,random.nextFloat()*1.6f+0.7f,fill(fade(n%4==0?b:a,70+random.nextInt(85))));
        }
        halo(c,210,370,190,a);
        halo(c,210,420,135,b);
        // Science-specific visual—each family has its own geometry.
        switch(family){
            case 0: atom(c,t,variant,a,b);break;
            case 1: maths(c,t,variant,a,b);break;
            case 2: molecule(c,t,variant,a,b);break;
            case 3: dna(c,t,variant,a,b);break;
            case 4: heart(c,t,variant,a,b);break;
            case 5: nephron(c,t,variant,a,b);break;
            case 6: cells(c,t,variant,a,b);break;
            case 7: circuits(c,t,variant,a,b);break;
            case 8: fractal(c,t,variant,a,b);break;
            case 9: solar(c,t,variant,a,b);break;
            case 10: leaves(c,t,variant,a,b);break;
            default: waves(c,t,variant,a,b);break;
        }
        // HUD accents are different for every variant.
        int spin=variant*11;
        c.drawArc(34,170,386,702,-35+spin+t*3f,48,false,edge(fade(a,165),2.6f));
        c.drawArc(42,178,378,694,125-spin-t*4f,57,false,edge(fade(b,145),1.6f));
        for(int k=0;k<7;k++){
            float theta=(float)(2*Math.PI*k/7+t*.07+variant*.13);
            float x=210+(float)Math.cos(theta)*175,y=410+(float)Math.sin(theta)*245;
            c.drawCircle(x,y,2.1f,fill(a));
        }
        text(c,"VISION  /  DIGITAL SCI-MATH",28,47,13,0xFFE9F5FF,true);
        text(c,String.format(java.util.Locale.US,"%03d  •  %s",id+1,CATEGORIES[family].toUpperCase()),28,74,10,fade(a,245),true);
        c.drawLine(28,88,392,88,edge(fade(a,120),1.2f));
        c.drawLine(28,734,392,734,edge(fade(b,120),1.2f));
        text(c,LABELS[family][variant%4],28,761,18,0xFFEDF8FF,true);
        text(c,"DESIGN "+(variant+1)+"   /   LIVE DATA ART",28,787,10,fade(a,220),false);
        text(c,"∞",375,788,19,fade(b,220),true);
        c.restore();
    }
    private static void halo(Canvas c,float x,float y,float r,int color){
        Paint p=fill(color);
        p.setShader(new RadialGradient(x,y,r,new int[]{fade(color,42),fade(color,11),Color.TRANSPARENT},
            new float[]{0f,.40f,1f},Shader.TileMode.CLAMP));
        c.drawCircle(x,y,r,p);
    }
    private static void text(Canvas c,String s,float x,float y,int size,int color,boolean bold){
        Paint p=fill(color);p.setTypeface(bold?Typeface.create("sans-serif",Typeface.BOLD):Typeface.create("monospace",Typeface.NORMAL));
        p.setTextSize(size);c.drawText(s,x,y,p);
    }
    private static void line(Canvas c,float x,float y,float xx,float yy,int color,float width){
        c.drawLine(x,y,xx,yy,edge(color,width));
    }
    private static void node(Canvas c,float x,float y,float r,int color){
        c.drawCircle(x,y,r*2.8f,fill(fade(color,36)));
        c.drawCircle(x,y,r,fill(color));
        c.drawCircle(x-r*.26f,y-r*.30f,r*.33f,fill(0xFFFFFFFF));
    }
    private static void atom(Canvas c,float t,int v,int a,int b){
        float tilt=(float)Math.sin(t*.3+v)*9;
        for(int i=0;i<5;i++){
            c.save();c.rotate(i*36+tilt,210,409);
            c.drawOval(53,337+i%2*7,367,481-i%2*7,edge(fade(i%2==0?a:b,155),2.8f));
            c.restore();
        }
        halo(c,210,410,64,b);node(c,204,406,20,a);node(c,226,419,15,b);
        for(int i=0;i<5;i++){
            float q=t*(.40f+.06f*i)+v*.38f+i*1.3f;
            node(c,210+(float)Math.cos(q)*150,410+(float)Math.sin(q)*75,5,i%2==0?a:b);
        }
        text(c,"e⁻   p⁺   n⁰",128,634,23,fade(a,225),true);
    }
    private static void maths(Canvas c,float t,int v,int a,int b){
        for(int k=0;k<=8;k++){
            float x=58+k*38f,y=228+k*40f;
            line(c,x,212,x,620,fade(a,43),1);
            line(c,54,226+k*46,366,226+k*46,fade(b,38),1);
        }
        line(c,210,232,210,620,fade(a,150),2);
        line(c,53,428,366,428,fade(a,150),2);
        for(int f=0;f<3;f++){
            Path p=new Path();
            for(int x=54;x<=365;x+=3){
                double n=(x-210.0)/47.0;
                float y=(float)(418-65*Math.sin(n*(1+v%4*.24)+t*.28+f*1.4)/(1+f*.65));
                if(x==54)p.moveTo(x,y);else p.lineTo(x,y);
            }
            c.drawPath(p,edge(fade(f==0?a:b,210-f*37),3-f*.45f));
        }
        text(c,v%2==0?"f(x) = sin(x)":"y = ax² + bx",91,655,22,fade(b,240),true);
    }
    private static void molecule(Canvas c,float t,int v,int a,int b){
        Random r=new Random(45+v*55L);
        float[] x=new float[14],y=new float[14];
        for(int i=0;i<14;i++){
            x[i]=83+r.nextFloat()*254;y[i]=253+r.nextFloat()*321;
        }
        for(int i=0;i<14;i++){
            for(int j=i+1;j<14;j++){
                float dx=x[i]-x[j],dy=y[i]-y[j];
                if(dx*dx+dy*dy<11000)line(c,x[i],y[i],x[j],y[j],fade(i%2==0?a:b,150),3);
            }
        }
        for(int i=0;i<14;i++)
            node(c,x[i]+(float)Math.sin(t*.3+i)*3,y[i]+(float)Math.cos(t*.2+i)*3,
                i%4==0?12:i%3==0?8:5,i%2==0?a:b);
        text(c,v%3==0?"H₂O  •  CO₂":v%3==1?"C — C — H":"MOLECULES",118,662,22,fade(a,245),true);
    }
    private static void dna(Canvas c,float t,int v,int a,int b){
        Path left=new Path(),right=new Path();
        for(int i=0;i<=90;i++){
            float y=224+i*4.3f,angle=y*.035f+t*.45f+v*.3f;
            float x=210+(float)Math.sin(angle)*90;
            float xr=210-(float)Math.sin(angle)*90;
            if(i==0){left.moveTo(x,y);right.moveTo(xr,y);}else{left.lineTo(x,y);right.lineTo(xr,y);}
            if(i%5==0)line(c,x,y,xr,y,fade(i%2==0?a:b,172),3);
        }
        c.drawPath(left,edge(a,5));c.drawPath(right,edge(b,5));
        text(c,"A - T    C - G",109,672,22,fade(a,245),true);
    }
    private static void heart(Canvas c,float t,int v,int a,int b){
        c.save();float beat=1f+(float)Math.sin(t*2.8)*.035f;
        c.scale(beat,beat,210,375);
        Path heart=new Path();heart.moveTo(210,532);
        heart.cubicTo(160,488,85,419,85,345);
        heart.cubicTo(85,270,172,256,210,319);
        heart.cubicTo(252,256,335,270,335,345);
        heart.cubicTo(335,419,260,488,210,532);
        c.drawPath(heart,edge(a,6));
        c.drawPath(heart,fill(fade(b,28)));
        c.restore();
        Path ecg=new Path();ecg.moveTo(34,594);
        float phase=(float)Math.sin(t)*3;
        float[] pts={34,594,116,594,140,577,154,610,179,544,200,634,225,577,243,594,386,594};
        for(int i=2;i<pts.length;i+=2)ecg.lineTo(pts[i]+phase,pts[i+1]);
        c.drawPath(ecg,edge(b,4));
        text(c,"BPM  •  HEART  /  01",98,675,20,fade(a,235),true);
    }
    private static void nephron(Canvas c,float t,int v,int a,int b){
        // Stylized digital renal anatomy diagram, not a clinical illustration.
        Path kidney=new Path();kidney.moveTo(214,245);
        kidney.cubicTo(101,210,72,325,111,413);
        kidney.cubicTo(136,482,102,537,179,570);
        kidney.cubicTo(285,621,348,514,320,445);
        kidney.cubicTo(295,394,357,297,270,254);
        kidney.cubicTo(252,246,230,243,214,245);
        c.drawPath(kidney,edge(a,5));
        c.drawPath(kidney,fill(fade(b,28)));
        Path tube=new Path();tube.moveTo(246,290);
        tube.cubicTo(100,326,277,351,159,396);
        tube.cubicTo(75,430,263,452,178,488);
        tube.cubicTo(120,515,216,552,247,603);
        c.drawPath(tube,edge(b,4));
        c.drawCircle(247,289,19,edge(a,4));
        for(int i=0;i<8;i++){
            float x=170+(float)Math.sin(i*1.5+t*.18)*65;
            float y=295+i*32;
            node(c,x,y,3.2f,i%2==0?a:b);
        }
        text(c,"RENAL • NEPHRON",100,668,21,fade(a,240),true);
    }
    private static void cells(Canvas c,float t,int v,int a,int b){
        Random random=new Random(v*13L+999);
        for(int i=0;i<7;i++){
            float x=98+random.nextFloat()*225,y=263+random.nextFloat()*320;
            float r=35+random.nextFloat()*36;
            c.drawOval(x-r,y-r*.72f,x+r,y+r*.72f,edge(fade(i%2==0?a:b,210),3));
            c.drawCircle(x,y,r*.20f,fill(fade(b,200)));
            for(int j=0;j<7;j++){
                double q=j*Math.PI/3.5+t*.15;
                c.drawCircle(x+(float)Math.cos(q)*r*.52f,y+(float)Math.sin(q)*r*.33f,3,fill(a));
            }
        }
        text(c,"CELLULAR • NETWORK",83,667,21,fade(b,240),true);
    }
    private static void circuits(Canvas c,float t,int v,int a,int b){
        Random r=new Random(9222L+v*49);
        c.drawRoundRect(121,288,299,531,12,12,fill(fade(a,35)));
        c.drawRoundRect(121,288,299,531,12,12,edge(a,4));
        for(int i=0;i<13;i++){
            float y=300+i*18;
            line(c,80,y,120,y,fade(a,160),2);
            line(c,300,y,345,y,fade(b,160),2);
            node(c,80,y,2.5f,i%2==0?a:b);
        }
        for(int i=0;i<35;i++){
            float x=135+(i%7)*23f,y=315+(i/7)*42f;
            c.drawRect(x,y,x+9,y+11,fill(fade((i+v)%2==0?a:b,120+(int)(65*Math.abs(Math.sin(t+i))))));
        }
        text(c,"CORE  /  SCI-MATH",97,659,23,fade(a,240),true);
    }
    private static void fractal(Canvas c,float t,int v,int a,int b){
        for(int ring=0;ring<12;ring++){
            int sides=3+(v%6),n=ring;
            float rad=30+ring*14f;
            Path polygon=new Path();
            for(int k=0;k<=sides;k++){
                double q=2*Math.PI*k/sides+t*.03*(ring%2==0?1:-1)+ring*.065;
                float x=210+(float)Math.cos(q)*rad,y=407+(float)Math.sin(q)*rad;
                if(k==0)polygon.moveTo(x,y);else polygon.lineTo(x,y);
            }
            c.drawPath(polygon,edge(fade(ring%2==0?a:b,105+ring*9),1.7f));
        }
        text(c,"φ = 1.618  •  π",115,665,25,fade(a,245),true);
    }
    private static void solar(Canvas c,float t,int v,int a,int b){
        halo(c,205,409,83,0xFFFFC86B);
        node(c,205,409,32,0xFFFFCD78);
        float[] radius={68,102,142,180};
        for(int j=0;j<radius.length;j++){
            float r=radius[j];
            c.drawOval(205-r,409-r*.8f,205+r,409+r*.8f,edge(fade(j%2==0?a:b,110),1.7f));
            float angle=t*(.13f+.10f*j)+v*.43f+j*2;
            node(c,205+(float)Math.cos(angle)*r,409+(float)Math.sin(angle)*r*.8f,5+j*2,j%2==0?a:b);
        }
        text(c,"PLANETARY • ORBITS",83,676,21,fade(a,250),true);
    }
    private static void leaves(Canvas c,float t,int v,int a,int b){
        Path leaf=new Path();leaf.moveTo(210,562);
        leaf.cubicTo(75,504,103,307,309,265);
        leaf.cubicTo(352,412,295,517,210,562);
        c.drawPath(leaf,fill(fade(a,37)));c.drawPath(leaf,edge(a,5));
        Path stem=new Path();stem.moveTo(210,563);stem.quadTo(238,439,309,266);
        c.drawPath(stem,edge(b,3));
        for(int i=0;i<9;i++){
            float y=523-i*27,x=227+i*7.2f;
            line(c,x,y,x-72,y-29,fade(b,145),2);
            line(c,x,y,x+42,y-44,fade(a,145),2);
        }
        for(int i=0;i<15;i++){
            float x=85+i*17,y=600+(float)Math.sin(i*.9+t*.2)*12;
            node(c,x,y,2.1f,i%2==0?a:b);
        }
        text(c,"CO₂  +  H₂O  →  O₂",90,673,19,fade(a,245),true);
    }
    private static void waves(Canvas c,float t,int v,int a,int b){
        for(int f=0;f<8;f++){
            Path wave=new Path();
            for(int x=40;x<382;x+=4){
                float xx=(x-40)/35f,phase=xx*(.8f+v*.025f)+t*.35f+f*.42f;
                float y=410+(f-3.5f)*15+(float)Math.sin(phase)*90f/(1+f*.18f);
                if(x==40)wave.moveTo(x,y);else wave.lineTo(x,y);
            }
            c.drawPath(wave,edge(fade(f%2==0?a:b,115+f*13),2.1f));
        }
        for(int i=0;i<18;i++){
            float x=53+i*17,y=413+(float)Math.sin(i*.6+t*.6)*98;
            node(c,x,y,2.3f,i%2==0?a:b);
        }
        text(c,"Ψ(x,t)   /   λ",118,668,24,fade(b,235),true);
    }
}
