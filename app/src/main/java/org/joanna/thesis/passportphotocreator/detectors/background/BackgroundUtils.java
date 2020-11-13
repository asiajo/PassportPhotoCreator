package org.joanna.thesis.passportphotocreator.detectors.background;

import android.util.Log;

import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

import static org.opencv.core.CvType.CV_8UC1;

public final class BackgroundUtils {

    private static final String TAG = BackgroundUtils.class.getSimpleName();

    private BackgroundUtils() {
    }

    /**
     * Attempts to detect background color and verify if it is uniform across
     * the whole background. To achieve this it picks a start point (one of
     * hardcoded, best possible locations - for performance reasons) and
     * searches for the whole area containing almost the same color. Performance
     * of this method depends on how good person was segmented out from the
     * background.
     *
     * @param bg         Properties of the background to be set
     * @param background image to be processed
     */
    public static void processBackgroundColorBlobDetection(
            final BackgroundProperties bg,
            final Mat background) {
        Point pixelBackground = findNonPersonPixel(background);
        Point pixelPerson = findPersonPixel(background);
        if (pixelPerson == null || pixelBackground == null) {
            Log.w(
                    TAG,
                    "Did not find on an image a point matching or a person or" +
                            " a background. Will not perform background " +
                            "uniformity verification.");
            return;
        }

        ColorBlobDetector bgContourDetector =
                new ColorBlobDetector(background, pixelBackground);
        bg.setBgColor(bgContourDetector.getmBlobColorRgba());
        final double areaBgd = bgContourDetector.getContoursMaxArea();

        ColorBlobDetector personContourDetector =
                new ColorBlobDetector(background, pixelPerson);
        bg.setPersonContourLen(
                (int) personContourDetector.getContoursMaxPerimeter());
        final double areaPerson = personContourDetector.getContoursMaxArea();

        final int imgArea = background.width() * background.height();
        final int epsilon = imgArea / 10;
        if ((imgArea - areaPerson - areaBgd) > epsilon) {
            Log.i(TAG, "Unfortunately background seems not to be uniform!");
            bg.setUniform(false);
        } else {
            Log.i(TAG, "Background seems to be uniform.");
            bg.setUniform(true);
        }
    }

    /**
     * Attempts to detect edges on the background. Performance of this method
     * depends on how good person was segmented out from the background.
     *
     * @param bg         Properties of the background to be set
     * @param background image to be processed
     */
    public static void processEdgeDetection(
            final BackgroundProperties bg, final Mat background) {

        final int cannyTreshold = 30;
        final int allowedLengthOfEdges = 1000;

        Mat grey = new Mat();
        Imgproc.cvtColor(background, grey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(grey, grey, new Size(5, 5));
        Mat edges = new Mat();
        Imgproc.Canny(grey, edges, cannyTreshold, cannyTreshold * 3);
        grey.release();

        int whitePixels = Core.countNonZero(edges);
        int approxLengthOfEdges = whitePixels - bg.getPersonContourLen();
        if (approxLengthOfEdges > allowedLengthOfEdges) {
            Log.i(TAG, "Background seems to contain edges!");
            bg.setEdgesFree(false);
        } else {
            Log.i(TAG, "Background seems to be plain.");
            bg.setEdgesFree(true);
        }
        edges.release();
    }

    /**
     * Calculates median and average of the hue of the background. If median and
     * average are close to each other there is a high probability that the
     * background have one uniform color. Performance of this method depends
     * on how good person was segmented out from the background.
     *
     * @param bg           Properties of the background to be set
     * @param background   image to be processed
     * @param maskedPerson mask to apply to receive only a background
     */
    public static void processColorsDetection(
            final BackgroundProperties bg,
            final Mat background,
            final Mat maskedPerson) {

        Mat hsvSource = new Mat();
        Imgproc.cvtColor(background, hsvSource, Imgproc.COLOR_BGR2HSV);

        int hBins = 60;
        float hUpperRange = 180f;
        MatOfInt histSize = new MatOfInt(hBins);
        MatOfFloat ranges = new MatOfFloat(0f, hUpperRange);
        MatOfInt channels = new MatOfInt(0);

        Mat mask = new Mat();
        maskedPerson.convertTo(mask, CV_8UC1, 255);
        mask = ImageUtils.unpadMatFromSquare(mask, background.width());
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY);

        Mat histSource = new Mat();
        final ArrayList<Mat> histImages = new ArrayList<>();
        histImages.add(hsvSource);
        Imgproc.calcHist(
                histImages,
                channels,
                mask,
                histSource,
                histSize,
                ranges,
                false);

        double avgHueValue = Core.mean(hsvSource, mask).val[0];
        double medianHueValue = -1;
        for (int i = 0; i < hBins; i++) {
            if (histSource.get(i, 0)[0] >= hUpperRange / 2) {
                medianHueValue = histSource.get(i, 0)[0];
                break;
            }
        }
        mask.release();
        hsvSource.release();
        histSource.release();

        final int epsilon = 10;
        if (Math.abs(medianHueValue - avgHueValue) > epsilon) {
            Log.i(TAG, "It seems that the background is colorful!");
            bg.setUncolorful(false);
        } else {
            Log.i(TAG, "It seems that the background color is uniform.");
            bg.setUncolorful(true);
        }
    }

    /**
     * Attempts to find a pixel that is part of a background. The input image
     * should have masked person with black mask. As such pixels that are
     * non-black have high possibility of being background pixels.
     *
     * @param src image to be processed
     * @return point with a non-background pixel or null if it was not found
     */
    public static Point findNonPersonPixel(final Mat src) {
        Point[] likelyPoints = {
                new Point(10, 10),
                new Point(src.size().width - 10, 10),
                new Point(src.size().width / 2, 10)};
        return findPixel(src, likelyPoints, false);
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
}
