package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class UtilsPerspective {
    // Transforma la imagen para enderezar la hoja, escalando a las dimensiones de la vista
    public static Mat fourPointTransform(Mat src, Point[] pts, float viewWidth, float viewHeight) {
        if (pts == null || pts.length != 4) {
            throw new IllegalArgumentException("Se requieren exactamente 4 puntos para la transformación de perspectiva");
        }

        Point tl = pts[0], tr = pts[1], br = pts[2], bl = pts[3];
        double widthA = Math.hypot(br.x - bl.x, br.y - bl.y);
        double widthB = Math.hypot(tr.x - tl.x, tr.y - tl.y);
        double maxWidth = Math.max(widthA, widthB);
        double heightA = Math.hypot(tr.x - br.x, tr.y - br.y);
        double heightB = Math.hypot(tl.x - bl.x, tl.y - bl.y);
        double maxHeight = Math.max(heightA, heightB);

        // Escalar proporcionalmente para coincidir con las dimensiones de la vista
        double aspectRatio = maxWidth / maxHeight;
        double viewAspectRatio = viewWidth / viewHeight;
        if (aspectRatio > viewAspectRatio) {
            maxWidth = viewWidth;
            maxHeight = viewWidth / aspectRatio;
        } else {
            maxHeight = viewHeight;
            maxWidth = viewHeight * aspectRatio;
        }

        Mat srcPts = new Mat(4, 1, CvType.CV_64F);
        Mat dstPts = new Mat(4, 1, CvType.CV_64F);
        srcPts.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dstPts.put(0, 0, 0.0, 0.0, maxWidth - 1, 0.0, maxWidth - 1, maxHeight - 1, 0.0, maxHeight - 1);

        Mat M = Imgproc.getPerspectiveTransform(srcPts, dstPts);
        Mat warped = new Mat();
        Imgproc.warpPerspective(src, warped, M, new Size(maxWidth, maxHeight));
        srcPts.release();
        dstPts.release();
        M.release();
        return warped;
    }
}