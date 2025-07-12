package upvictoria.pm_may_ago_2024.iti_271415.pg2u2_eq02;

import org.opencv.core.Mat;
import org.opencv.core.Point3;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import java.util.ArrayList;

public class CircleDetector {

    public static ArrayList<Point3> detect(Mat input, int maxCircles) {
        Mat gray = new Mat();
        Imgproc.cvtColor(input, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(9, 9), 2, 2);

        Mat circles = new Mat();
        Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1,
                20, 100, 30, 10, 100);

        ArrayList<Point3> result = new ArrayList<>();
        for (int i = 0; i < circles.cols() && result.size() < maxCircles; i++) {
            double[] c = circles.get(0, i);
            if (c != null) {
                result.add(new Point3(c[0], c[1], c[2]));
            }
        }
        return result;
    }

}
