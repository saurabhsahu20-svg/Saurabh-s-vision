package com.visionone.scanner;

import android.graphics.*;
import java.util.*;

/** No network: proposes a paper quadrilateral based on light-paper/dark-background contrast. */
public final class PaperEdges {
 private PaperEdges(){}
 public static final String[] SIZES={"Original proportions","A4","A5","Letter","Legal","ID card"};
 static float[] FULL={0f,0f,1f,0f,1f,1f,0f,1f};
 static int light(int rgb){return (Color.red(rgb)*30+Color.green(rgb)*59+Color.blue(rgb)*11)/100;}
 static int mean(int[] g,int w,int h,int x,int y){
   int sum=0,n=0;for(int dy=-3;dy<=3;dy+=3)for(int dx=-3;dx<=3;dx+=3){int a=Math.max(0,Math.min(w-1,x+dx)),b=Math.max(0,Math.min(h-1,y+dy));sum+=g[b*w+a];n++;}return sum/n;
 }
 static int median(ArrayList<Integer> items){if(items.isEmpty())return 0;Collections.sort(items);return items.get(items.size()/2);}
 static float[] line(ArrayList<float[]> data){if(data.size()<3)return null;double sx=0,sy=0,sxx=0,sxy=0;int n=data.size();for(float[] d:data){sx+=d[0];sy+=d[1];sxx+=d[0]*d[0];sxy+=d[0]*d[1];}double det=n*sxx-sx*sx;if(Math.abs(det)<0.00001)return null;float a=(float)((n*sxy-sx*sy)/det),b=(float)((sy-a*sx)/n);return new float[]{a,b};}
 static int locate(int[] g,int w,int h,int x,int y,int dx,int dy,int limit,int threshold){
  int consecutive=0;int original=x;for(int i=0;i<limit;i+=2){int xx=x+dx*i, yy=y+dy*i;if(xx<2||xx>=w-2||yy<2||yy>=h-2)break;
   if(mean(g,w,h,xx,yy)<threshold){consecutive++;if(consecutive>=3)return dx!=0?xx:yy;}else consecutive=0;
  }return -1;
 }
 public static float[] suggest(Bitmap bitmap){
  int ow=bitmap.getWidth(),oh=bitmap.getHeight();if(ow<120||oh<120)return null;
  int w=Math.max(120,Math.round(ow*(Math.min(500f/Math.max(ow,oh),1f)))),h=Math.max(120,Math.round(oh*(Math.min(500f/Math.max(ow,oh),1f))));
  Bitmap sample=Bitmap.createScaledBitmap(bitmap,w,h,true);int[] colors=new int[w*h];sample.getPixels(colors,0,w,0,0,w,h);if(sample!=bitmap)sample.recycle();
  int[] g=new int[colors.length];for(int i=0;i<colors.length;i++)g[i]=light(colors[i]);
  ArrayList<Integer> center=new ArrayList<>(),edge=new ArrayList<>();
  for(int y=h*28/100;y<=h*72/100;y+=Math.max(3,h/18))for(int x=w*28/100;x<=w*72/100;x+=Math.max(3,w/18))center.add(mean(g,w,h,x,y));
  int[] sides={4,7,10,90,93,96};for(int sy:sides)for(int sx:sides){if(sx>10&&sx<90 && sy>10&&sy<90)continue;edge.add(mean(g,w,h,w*sx/100,h*sy/100));}
  int inside=median(center),outside=median(edge);
  if(inside<112||inside-outside<19)return null;
  int threshold=Math.max(95,Math.min(232,(inside+outside)/2));
  ArrayList<float[]> left=new ArrayList<>(),right=new ArrayList<>(),top=new ArrayList<>(),bottom=new ArrayList<>();
  for(int t=30;t<=70;t+=10){int y=h*t/100;
    int a=locate(g,w,h,w/2,y,-1,0,w/2-3,threshold),b=locate(g,w,h,w/2,y,1,0,w/2-3,threshold);
    if(a>4&&a<w*40/100)left.add(new float[]{y,a});if(b>w*60/100&&b<w-4)right.add(new float[]{y,b});
    int x=w*t/100;
    a=locate(g,w,h,x,h/2,0,-1,h/2-3,threshold);b=locate(g,w,h,x,h/2,0,1,h/2-3,threshold);
    if(a>4&&a<h*40/100)top.add(new float[]{x,a});if(b>h*60/100&&b<h-4)bottom.add(new float[]{x,b});
  }
  float[] l=line(left),r=line(right),t=line(top),b=line(bottom);
  if(l==null||r==null||t==null||b==null)return null;
  float[] tl=intersect(l,t),tr=intersect(r,t),br=intersect(r,b),bl=intersect(l,b);
  if(tl==null||tr==null||br==null||bl==null)return null;
  float[] out={tl[0]/w,tl[1]/h,tr[0]/w,tr[1]/h,br[0]/w,br[1]/h,bl[0]/w,bl[1]/h};
  if(!valid(out))return null;
  for(float p:out)if(p<0.008||p>0.992)return null;
  return out;
 }
 // x = a*y+b and y=c*x+d
 static float[] intersect(float[] vertical,float[] horizontal){
  float denom=1-vertical[0]*horizontal[0];if(Math.abs(denom)<0.03)return null;
  float x=(vertical[0]*horizontal[1]+vertical[1])/denom;
  return new float[]{x,horizontal[0]*x+horizontal[1]};
 }
 static boolean valid(float[] p){
  if(p==null||p.length!=8)return false;
  for(float n:p)if(Float.isNaN(n)||n<0||n>1)return false;
  float area=0;float sign=0;
  for(int i=0;i<4;i++){int j=(i+1)%4,k=(i+2)%4;
   float ax=p[j*2]-p[i*2],ay=p[j*2+1]-p[i*2+1],bx=p[k*2]-p[j*2],by=p[k*2+1]-p[j*2+1];
   float cross=ax*by-ay*bx;if(Math.abs(cross)<0.00001)return false;if(i==0)sign=cross;else if(sign*cross<0)return false;
   area+=p[i*2]*p[j*2+1]-p[j*2]*p[i*2+1];
  }
  return Math.abs(area)*.5>0.06;
 }
 static float dist(float ax,float ay,float bx,float by){return (float)Math.hypot(ax-bx,ay-by);}
 public static Bitmap crop(Bitmap src,float[] corners,String size){
  if(!valid(corners))throw new IllegalArgumentException("Move crop corners to enclose the document");
  int sw=src.getWidth(),sh=src.getHeight();
  float top=dist(corners[0]*sw,corners[1]*sh,corners[2]*sw,corners[3]*sh);
  float bottom=dist(corners[6]*sw,corners[7]*sh,corners[4]*sw,corners[5]*sh);
  float left=dist(corners[0]*sw,corners[1]*sh,corners[6]*sw,corners[7]*sh);
  float right=dist(corners[2]*sw,corners[3]*sh,corners[4]*sw,corners[5]*sh);
  int w=Math.max(200,Math.round(Math.max(top,bottom))),h=Math.max(200,Math.round(Math.max(left,right)));
  if("A4".equals(size)){w=1654;h=2339;}else if("A5".equals(size)){w=1169;h=1654;}
  else if("Letter".equals(size)){w=1700;h=2200;}else if("Legal".equals(size)){w=1700;h=2800;}
  else if("ID card".equals(size)){w=1012;h=638;}else{float scale=Math.min(1f,2600f/Math.max(w,h));w=Math.max(200,(int)(w*scale));h=Math.max(200,(int)(h*scale));}
  float[] sp=new float[8];for(int i=0;i<4;i++){sp[i*2]=corners[i*2]*sw;sp[i*2+1]=corners[i*2+1]*sh;}
  float[] dst={0,0,w,0,w,h,0,h};Matrix matrix=new Matrix();if(!matrix.setPolyToPoly(sp,0,dst,0,4))throw new IllegalArgumentException("Cannot straighten this crop; move the corners");
  Bitmap output=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(output);canvas.drawColor(Color.WHITE);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);canvas.drawBitmap(src,matrix,paint);return output;
 }
}
