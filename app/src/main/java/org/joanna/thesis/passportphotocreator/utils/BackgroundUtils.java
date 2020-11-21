package org.joanna.thesis.passportphotocreator.utils;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public final class BackgroundUtils {

    private static final String TAG = BackgroundUtils.class.getSimpleName();

    private BackgroundUtils() {
    }

    public static int getContoursLengthOnTheImage(final Mat background) {
        final int cannyTreshold = 30;
        Mat grey = new Mat();
        Imgproc.cvtColor(background, grey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(grey, grey, new Size(5, 5));
        Mat edges = new Mat();
        Imgproc.Canny(grey, edges, cannyTreshold, cannyTreshold * 3);
        grey.release();

        int whitePixels = Core.countNonZero(edges);
        edges.release();
        return whitePixels;
    }

    /**
     * Attempts to find a pixel that is part of a background. The input image
     * should have masked person with black mask. As such pixels that are
     * non-black have high possibility of being background pixels.
     *
     * @param src  image to be processed
     * @param left flag from which side of the picture point shall be taken
     * @return point with a non-background pixel or null if it was not found
     */
    public static Point findNonPersonPixel(final Mat src, final boolean left) {
        Point[] likelyLeftPoints = {
                new Point(10, 10),
                new Point(src.size().width / 4, 10)};
        Point[] likelyRightPoints = {
                new Point(src.size().width - 10, 10),
                new Point(src.size().width / 4 * 3, 10)};
        if (left) {
            return findPixel(src, likelyLeftPoints, false);
        } else {
            return findPixel(src, likelyRightPoints, false);
        }
    }

    /**
     * Attempts to find a pixel that is part of a person. The input image
     * should have masked person with black mask. As such pixels that are
     * black have high possibility of being background pixels.
     *
     * @param src image to be processed
     * @return point with a non-background pixel or null if it was not found
     */
    public static Point findPersonPixel(final Mat src) {
        Point[] likelyPoints = {
                new Point(src.size().width / 2, src.height() - 10),
                new Point(src.size().width / 2, src.height() / 2),
                new Point(src.size().width / 2, src.height() * 3 / 4)};
        return findPixel(src, likelyPoints, true);
    }

    private static Point findPixel(
            final Mat src, final Point[] likelyPoints, final boolean black) {

        for (int i = 0; i < likelyPoints.length; i++) {
            int x = (int) likelyPoints[i].x;
            int y = (int) likelyPoints[i].y;
            double brightness = getBrigtness(src.get(y, x));
            if ((brightness == 0) == black) {
                return new Point(x, y);
            }
        }
        return null;
    }

    /**
     * {@link #getBrigtness(double[] p)}
     *
     * @param p element describing RGB(A) value
     * @return brightness value
     */
    public static double getBrigtness(final Scalar p) {
        return getBrigtness(p.val);
    }

    /**
     * Computes normalized brightness of the received RGB value. Uses standard
     * coefficients for the calculation, as they describe better illumination
     * than the average. Value 1 represents white, 0 - black and values in
     * between represent shades of grey.
     *
     * @param p 3 or 4 element array describing RGB(A) value
     * @return brightness value
     */
    public static double getBrigtness(final double[] p) {
        if (p.length < 3) {
            return -1;
        }
        return (p[0] * 0.3 + p[1] * 0.59 + p[2] * 0.11) / 255;
    }

    /**
     * Returns true if color is in the brighter half of a color space.
     *
     * @param color color to be verified
     * @return true if received color is bright and false otherwise
     */
    public static Boolean isBright(final double color) {
        return color > 0.5;
    }

    /**
     * Pastes one image containing alpha channel into the second one. This
     * implementation expects that bot images have exactly the same width
     * and height and rgba and rgb channels respectively.
     *
     * @param foreground image to be pasted in in rgba format
     * @param background image to which image should be pasted in rgb format
     * @return merged both images into one
     */
    public static Mat paste(final Mat foreground, final Mat background) {
        if (!(foreground.cols() == background.cols() &&
                foreground.rows() == background.rows() &&
                foreground.channels() == 4 &&
                background.channels() == 3)) {
            Log.e(
                    TAG,
                    "Paste method expects two Mats of exactly same width and " +
                            "height, where first one has rgba and second one " +
                            "rgb format. Unmodified background will be " +
                            "returned.");
            return background;
        }
        Mat output = background.clone();
        for (int y = 0; y < output.rows(); y++) {
            for (int x = 0; x < output.cols(); x++) {
                final double alpha = foreground.get(y, x)[3];
                if (alpha > 0) {
                    final double[] point = averageTwoColors(
                            foreground.get(y, x),
                            output.get(y, x));
                    output.put(y, x, point);
                }
            }
        }
        return output;
    }

    private static double[] averageTwoColors(
            final double[] rgba, final double[] rgb) {
        if (!(rgba.length == 4 && rgb.length == 3)) {
            Log.e(
                    TAG,
                    "This method expects two color arrays with channels rgba " +
                            "and rgb respectively.");
            return null;
        }
        double[] out = new double[rgb.length];
        final double alpha = rgba[3] / 255;
        for (int i = 0; i < rgb.length; i++) {
            out[i] = rgb[i] * (1 - alpha) + rgba[i] * alpha;
        }
        return out;
    }
}
