package com.visionone.scanner;

import android.graphics.Bitmap;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import java.util.*;

/**
 * Offline document boundary detector.
 * Uses several Canny / threshold passes and validates convex, four-sided contours.
 * Returns [TL, TR, BR, BL] normalized image coordinates; null means "manual crop".
 * Never assumes A4 or crops the image without a credible boundary.
 */
public final class OpenCvPaperDetector {
    private static boolean loaded, tried;
    private OpenCvPaperDetector() { }
    private static synchronized boolean ready() {
        if (!tried) {
            tried = true;
            try { loaded = OpenCVLoader.initLocal(); }
            catch (Throwable ignored) { loaded = false; }
        }
        return loaded;
    }

    public static float[] detect(Bitmap photo) {
        if (photo == null || photo.isRecycled() || !ready()) return null;
        int originalW=photo.getWidth(), originalH=photo.getHeight();
        if (originalW<80 || originalH<80) return null;
        final float factor=Math.min(1f,1050f/Math.max(originalW,originalH));
        final int w=Math.max(1,Math.round(originalW*factor));
        final int h=Math.max(1,Math.round(originalH*factor));
        Bitmap small=null;
        Mat rgba=new Mat(),gray=new Mat(),blur=new Mat(),mask=new Mat(),tmp=new Mat(),hierarchy=new Mat();
        try {
            small=(factor<1f)?Bitmap.createScaledBitmap(photo,w,h,true):photo;
            Utils.bitmapToMat(small,rgba);
            Imgproc.cvtColor(rgba,gray,Imgproc.COLOR_RGBA2GRAY);
            Imgproc.GaussianBlur(gray,blur,new Size(5,5),0);

            float[] best=null;
            double score=0;
            double imgArea=(double)w*h;
            Mat kernel=Imgproc.getStructuringElement(Imgproc.MORPH_RECT,new Size(3,3));

            for (int pass=0;pass<5;pass++) {
                mask.release();
                mask=new Mat();
                if(pass==0) Imgproc.Canny(blur,mask,45,120);
                else if(pass==1) Imgproc.Canny(blur,mask,90,220);
                else if(pass==2) {
                    Imgproc.adaptiveThreshold(blur,mask,255,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,Imgproc.THRESH_BINARY,35,10);
                } else if(pass==3) {
                    Imgproc.threshold(blur,mask,0,255,Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);
                } else {
                    Imgproc.threshold(blur,mask,0,255,Imgproc.THRESH_BINARY_INV+Imgproc.THRESH_OTSU);
                }
                Imgproc.morphologyEx(mask,mask,Imgproc.MORPH_CLOSE,kernel,new Point(-1,-1),2);
                Imgproc.dilate(mask,mask,kernel);
                tmp.release();
                tmp=mask.clone();
                List<MatOfPoint> contours=new ArrayList<>();
                try {
                    Imgproc.findContours(tmp,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
                    contours.sort((a,b)->Double.compare(Imgproc.contourArea(b),Imgproc.contourArea(a)));
                    int count=0;
                    for(MatOfPoint contour:contours) {
                        if(count++>=65) break;
                        double contourArea=Imgproc.contourArea(contour);
                        if(contourArea<imgArea*.045 || contourArea>imgArea*.995) continue;
                        MatOfPoint2f curve=new MatOfPoint2f(contour.toArray());
                        double length=Imgproc.arcLength(curve,true);
                        if(length<=0){curve.release();continue;}
                        for(double epsilon:new double[]{.012,.019,.027,.038,.055}) {
                            MatOfPoint2f quad=new MatOfPoint2f();
                            try {
                                Imgproc.approxPolyDP(curve,quad,length*epsilon,true);
                                if(quad.total()!=4) continue;
                                Point[] points=quad.toArray();
                                MatOfPoint convex=new MatOfPoint(points);
                                try {
                                    if(!Imgproc.isContourConvex(convex))continue;
                                } finally { convex.release(); }
                                float[] corners=order(points,w,h);
                                if(!PaperEdges.valid(corners)) continue;
                                double area=polygonArea(corners);
                                if(area<.055 || area>.965)continue;
                                // Exclude edge-of-image rectangular artifacts.
                                int near=0;
                                for(int i=0;i<4;i++) {
                                    float x=corners[i*2],y=corners[i*2+1];
                                    if(x<.009 || x>.991 || y<.009 || y>.991)near++;
                                }
                                if(near>=3)continue;
                                double minSide=minimumSide(corners);
                                if(minSide<.10)continue;
                                double rectangleScore=rectangleQuality(corners);
                                if(rectangleScore<.29)continue;
                                // Contrast across the polygon boundary suppresses internal tables and text.
                                double contrast=boundaryContrast(gray,corners,w,h);
                                double value=area*(.75+.25*rectangleScore)+Math.min(70,contrast)*.0026;
                                if(value>score) {
                                    score=value;
                                    best=corners;
                                }
                            } finally { quad.release(); }
                        }
                        curve.release();
                    }
                } finally {
                    for(MatOfPoint c:contours)c.release();
                }
            }
            kernel.release();
            return best==null?null:growSlightly(best,.0045f);
        } catch(Throwable ignored) {
            // OpenCV image and native library failures should not erase original captures.
            return null;
        } finally {
            rgba.release();gray.release();blur.release();mask.release();tmp.release();hierarchy.release();
            if(small!=null&&small!=photo&&!small.isRecycled())small.recycle();
        }
    }

    private static float[] order(Point[] pts,int w,int h) {
        Point[] p=pts.clone();
        final double[] center={0,0};
        for(Point q:p){center[0]+=q.x/4;center[1]+=q.y/4;}
        Arrays.sort(p,Comparator.comparingDouble(q->Math.atan2(q.y-center[1],q.x-center[0])));
        int start=0;
        for(int i=1;i<4;i++)if(p[i].x+p[i].y<p[start].x+p[start].y)start=i;
        float[] result=new float[8];
        for(int i=0;i<4;i++){Point q=p[(start+i)%4];result[2*i]=(float)(q.x/w);result[2*i+1]=(float)(q.y/h);}
        if(signedArea(result)<0) {
            float x=result[2],y=result[3];
            result[2]=result[6];result[3]=result[7];
            result[6]=x;result[7]=y;
        }
        return result;
    }
    private static double signedArea(float[] p) {
        double v=0;for(int i=0;i<4;i++){int j=(i+1)%4;v+=p[2*i]*p[2*j+1]-p[2*j]*p[2*i+1];}return v*.5;
    }
    private static double polygonArea(float[] p){return Math.abs(signedArea(p));}
    private static double minimumSide(float[] p){double min=2;
        for(int i=0;i<4;i++){int j=(i+1)%4;min=Math.min(min,Math.hypot(p[2*i]-p[2*j],p[2*i+1]-p[2*j+1]));}
        return min;
    }
    private static double rectangleQuality(float[] p) {
        double result=1;
        for(int i=0;i<4;i++) {
            int prev=(i+3)%4, next=(i+1)%4;
            double ax=p[2*prev]-p[2*i],ay=p[2*prev+1]-p[2*i+1];
            double bx=p[2*next]-p[2*i],by=p[2*next+1]-p[2*i+1];
            double la=Math.hypot(ax,ay),lb=Math.hypot(bx,by);
            if(la<.02||lb<.02)return 0;
            double cos=Math.abs((ax*bx+ay*by)/(la*lb));
            if(cos>.91)return 0;
            result=Math.min(result,1-cos);
        } return result;
    }
    private static double boundaryContrast(Mat gray,float[] p,int w,int h) {
        double sum=0;int n=0;
        for(int i=0;i<4;i++){
            int j=(i+1)%4;
            double mx=(p[2*i]+p[2*j])*.5*w,my=(p[2*i+1]+p[2*j+1])*.5*h;
            double dx=(p[2*j]-p[2*i])*w,dy=(p[2*j+1]-p[2*i+1])*h;
            double length=Math.hypot(dx,dy);if(length<10)continue;
            double nx=-dy/length,ny=dx/length;
            int x1=(int)(mx+nx*9),y1=(int)(my+ny*9);
            int x2=(int)(mx-nx*9),y2=(int)(my-ny*9);
            if(x1>=1&&x1<w-1&&y1>=1&&y1<h-1&&x2>=1&&x2<w-1&&y2>=1&&y2<h-1){
                double[] a=gray.get(y1,x1),b=gray.get(y2,x2);
                if(a!=null&&b!=null){sum+=Math.abs(a[0]-b[0]);n++;}
            }
        }
        return n==0?0:sum/n;
    }
    private static float[] growSlightly(float[] corners,float padding){
        float cx=0,cy=0;for(int i=0;i<4;i++){cx+=corners[2*i]/4;cy+=corners[2*i+1]/4;}
        float[] enlarged=corners.clone();
        for(int i=0;i<4;i++){
            enlarged[2*i]=Math.max(0,Math.min(1,cx+(corners[2*i]-cx)*(1+padding)));
            enlarged[2*i+1]=Math.max(0,Math.min(1,cy+(corners[2*i+1]-cy)*(1+padding)));
        }
        return PaperEdges.valid(enlarged)?enlarged:corners;
    }
}
