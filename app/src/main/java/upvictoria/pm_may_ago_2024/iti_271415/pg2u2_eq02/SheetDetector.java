package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import android.graphics.Bitmap;
import android.util.Log;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SheetDetector {
    private static final String TAG = "SheetDetector";

    public static class SheetDetectionResult {
        public Mat image;
        public Point[] corners;
        public String errorMessage;

        public SheetDetectionResult(Mat image, Point[] corners, String errorMessage) {
            this.image = image;
            this.corners = corners;
            this.errorMessage = errorMessage;
        }
    }

    public static SheetDetectionResult detectSheet(Bitmap bitmap) {
        Log.d(TAG, "Starting sheet detection");

        Mat srcColor = new Mat();
        Mat srcGray = new Mat();
        Mat edges = new Mat();
        Mat kernel = null;
        Mat hierarchy = new Mat();

        try {
            Utils.bitmapToMat(bitmap, srcColor);
            if (srcColor.empty()) {
                Log.e(TAG, "Source color Mat is empty");
                return new SheetDetectionResult(null, null, "Source color Mat is empty");
            }
            Log.d(TAG, "Bitmap converted to Mat: " + srcColor.size());

            Imgproc.cvtColor(srcColor, srcGray, Imgproc.COLOR_RGBA2GRAY);
            Log.d(TAG, "Converted to grayscale");

            Imgproc.equalizeHist(srcGray, srcGray);
            Log.d(TAG, "Applied histogram equalization");

            Imgproc.GaussianBlur(srcGray, srcGray, new Size(5, 5), 0);
            Log.d(TAG, "Applied Gaussian blur");

            Imgproc.Canny(srcGray, edges, 20, 80);
            Log.d(TAG, "Canny edge detection completed");

            kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
            Imgproc.dilate(edges, edges, kernel);
            Log.d(TAG, "Applied dilation");

            List<MatOfPoint> contours = new ArrayList<>();
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
            Log.d(TAG, "Found " + contours.size() + " contours");

            double imageArea = srcGray.size().area();
            double maxArea = 0;
            MatOfPoint2f bestContour = null;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area < imageArea * 0.005) { // Reducido al 0.5% (4608 píxeles)
                    Log.d(TAG, "Contour skipped, area too small: " + area);
                    continue;
                }

                MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
                double peri = Imgproc.arcLength(contour2f, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(contour2f, approx, 0.05 * peri, true);

                if (approx.total() >= 4 && approx.total() <= 6) { // Permitir 4-6 puntos
                    if (area > maxArea) {
                        maxArea = area;
                        bestContour = approx;
                        Log.d(TAG, "Found valid contour with area: " + area + ", corners: " + approx.total());
                    }
                }
                contour2f.release();
                approx.release();
            }

            if (bestContour != null) {
                Point[] points = sortCorners(bestContour.toArray());
                Mat result = srcColor.clone();
                Imgproc.cvtColor(result, result, Imgproc.COLOR_RGBA2RGB);
                Log.d(TAG, "Sheet detected successfully");
                bestContour.release();
                return new SheetDetectionResult(result, points, null);
            }

            Log.d(TAG, "No sheet detected");
            return new SheetDetectionResult(null, null, "No se detectó la hoja");

        } catch (Exception e) {
            Log.e(TAG, "Error in detectSheet: " + e.getMessage(), e);
            return new SheetDetectionResult(null, null, "Error en detección: " + e.getMessage());
        } finally {
            if (srcColor != null) srcColor.release();
            if (srcGray != null) srcGray.release();
            if (edges != null) edges.release();
            if (kernel != null) kernel.release();
            if (hierarchy != null) hierarchy.release();
        }
    }

    private static Point[] sortCorners(Point[] corners) {
        Point[] sorted = new Point[4];
        double[] sums = new double[4];
        double[] diffs = new double[4];

        for (int i = 0; i < 4; i++) {
            sums[i] = corners[i].x + corners[i].y;
            diffs[i] = corners[i].x - corners[i].y;
        }

        int tlIndex = 0;
        for (int i = 1; i < 4; i++) if (sums[i] < sums[tlIndex]) tlIndex = i;
        sorted[0] = corners[tlIndex];

        int brIndex = 0;
        for (int i = 1; i < 4; i++) if (sums[i] > sums[brIndex]) brIndex = i;
        sorted[2] = corners[brIndex];

        int trIndex = 0;
        for (int i = 1; i < 4; i++) if (diffs[i] > diffs[trIndex]) trIndex = i;
        sorted[1] = corners[trIndex];

        int blIndex = 0;
        for (int i = 1; i < 4; i++) if (diffs[i] < diffs[blIndex]) blIndex = i;
        sorted[3] = corners[blIndex];

        Log.d(TAG, "Sorted corners: " + Arrays.toString(sorted));
        return sorted;
    }
}