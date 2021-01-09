package org.joanna.thesis.passportphotocreator.processing.background;

import android.util.Log;

import org.joanna.thesis.passportphotocreator.processing.background.verification.BackgroundProperties;
import org.joanna.thesis.passportphotocreator.processing.background.verification.ColorBlobDetector;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.opencv.core.CvType.CV_8UC1;

public final class BackgroundUtils {

    private static final String TAG = BackgroundUtils.class.getSimpleName();

    private BackgroundUtils() {
    }

    public static int isUniform(
            final Mat background,
            final BackgroundProperties properties,
            final ImageSegmentor segmentor) {

        processBackgroundColorBlobDetection(background, properties);
        processEdgeDetection(background, properties);
        processColorsDetection(background, properties, segmentor);

        return properties.isUniform() + properties.isEdgesFree() +
                properties.isUncolorful();
    }

    public static Mat getBackground(
            final Mat src,
            final ImageSegmentor segmentor) {
        int imgWidth = (int) Math.ceil(
                ImageSegmentor.PROCESS_IMG_SIZE
                        * ImageUtils.FINAL_IMAGE_W_TO_H_RATIO);
        Mat image = ImageUtils.resizeMat(src, imgWidth);
        image = ImageUtils.padMatToSquare(
                image,
                ImageSegmentor.PROCESS_IMG_SIZE);

        segmentor.segmentImg(image);
        image = segmentor.getBackground();
        image = ImageUtils.unpadMatFromSquare(image, imgWidth);
        return image;
    }

    public static Mat getPersonMask(
            final Mat src,
            final ImageSegmentor segmentor) {
        int imgWidth = (int) Math.ceil(
                ImageSegmentor.PROCESS_IMG_SIZE
                        * ImageUtils.FINAL_IMAGE_W_TO_H_RATIO);
        Mat modelImage = ImageUtils.resizeMat(src, imgWidth);
        modelImage = ImageUtils.padMatToSquare(
                modelImage,
                ImageSegmentor.PROCESS_IMG_SIZE);

        segmentor.segmentImg(modelImage);
        modelImage.release();
        Mat personMask = segmentor.getMaskedBackground();

        personMask = ImageUtils.unpadMatFromSquare(personMask, imgWidth);
        personMask = ImageUtils.resizeMat(personMask, src.width());

        return personMask;
    }

    /**
     * Attempts to detect background color and verify if it is uniform across
     * the whole background. To achieve this it picks a start point (one of
     * hardcoded, best possible locations - for performance reasons) and
     * searches for the whole area containing almost the same color. Performance
     * of this method depends on how good person was segmented out from the
     * background.
     */
    public static void processBackgroundColorBlobDetection(
            final Mat background, final BackgroundProperties properties) {
        Point pixelBackgroundLeft = findNonPersonPixel(background, true);
        Point pixelBackgroundRight = findNonPersonPixel(background, false);
        Point pixelPerson = findPersonPixel(background);
        if (pixelPerson == null ||
                (pixelBackgroundLeft == null && pixelBackgroundRight == null)) {
            Log.w(
                    TAG,
                    "Did not find on an image a point matching or a person or" +
                            " a background. Will not perform background " +
                            "uniformity verification.");
            return;
        }

        // Detect color from right and left side, as slight tone difference
        // may have influence on correct detection
        ColorBlobDetector bgContourDetectorLeft = new ColorBlobDetector();
        bgContourDetectorLeft.process(background, pixelBackgroundLeft);

        ColorBlobDetector bgContourDetectorRight = new ColorBlobDetector();
        bgContourDetectorRight.process(background, pixelBackgroundRight);

        final Scalar rgbAverage = computeColorAverage(
                bgContourDetectorLeft.getBlobColorRgba(),
                bgContourDetectorRight.getBlobColorRgba());

        properties.setBgColorRgba(rgbAverage);
        final double areaBgdLeft = bgContourDetectorLeft.getContoursTotalArea();
        final double areaBgdRight =
                bgContourDetectorRight.getContoursTotalArea();

        ColorBlobDetector personContourDetector = new ColorBlobDetector();
        personContourDetector.process(background, pixelPerson);
        properties.setPersonContourLen(
                (int) personContourDetector.getContoursMaxPerimeter());
        final double areaPerson = personContourDetector.getContoursMaxArea();

        final int imgArea = background.width() * background.height();
        // hard-coded value that seems to do a good job in most cases
        final int epsilon = imgArea / 10;
        if ((imgArea - areaPerson - areaBgdLeft) > epsilon
                && (imgArea - areaPerson - areaBgdRight) > epsilon) {
            Log.i(TAG, "Unfortunately background seems not to be uniform!");
            properties.setUniform(2);
        } else {
            Log.i(TAG, "Background seems to be uniform.");
            properties.setUniform(0);
        }
    }

    private static Scalar computeColorAverage(
            final Scalar color1, final Scalar color2) {
        final int scalarLength = Math.min(color1.val.length, color2.val.length);
        double[] colorOut = new double[scalarLength];
        for (int i = 0; i < scalarLength; i++) {
            colorOut[i] = (color1.val[i] + color2.val[i]) / 2;
        }
        return new Scalar(colorOut);
    }

    /**
     * Attempts to detect edges on the background. Performance of this method
     * depends on how good person was segmented out from the background.
     */
    public static void processEdgeDetection(
            final Mat background, final BackgroundProperties properties) {

        final int imgArea = background.width() * background.height();
        // hard-coded value that seems to do a good job in most cases
        final int epsilonLittleEdges = imgArea / 200;
        final int epsilonMediumEdges = imgArea / 120;
        final int epsilonLotsOfEdges = imgArea / 80;
        int whitePixels = getContoursLengthOnTheImage(background);
        int approxLengthOfEdges =
                whitePixels - properties.getPersonContourLen();
        if (approxLengthOfEdges > epsilonLotsOfEdges) {
            Log.i(TAG, "Background seems to contain a lot of edges!");
            properties.setEdgesFree(3);
        } else if (approxLengthOfEdges > epsilonMediumEdges) {
            Log.i(TAG, "Background seems to contain quite some edges!");
            properties.setEdgesFree(2);
        } else if (approxLengthOfEdges > epsilonLittleEdges) {
            Log.i(TAG, "Background seems to contain some edges!");
            properties.setEdgesFree(1);
        } else {
            Log.i(TAG, "Background seems to be plain.");
            properties.setEdgesFree(0);
        }
    }

    /**
     * Calculates the average and standard deviation of the hue and the value
     * of the hsv color space of the background. If those standard deviations
     * are relatively small there is a high probability that the
     * background have one uniform color. Performance of this method depends
     * on how good person was segmented out from the background.
     */
    public static void processColorsDetection(
            final Mat background, final BackgroundProperties properties,
            final ImageSegmentor segmentor) {

        Mat hsvSource = new Mat();
        Imgproc.cvtColor(background, hsvSource, Imgproc.COLOR_RGB2HSV);

        Mat mask = new Mat();
        segmentor.getMaskedPerson().convertTo(mask, CV_8UC1, 255);
        mask = ImageUtils.unpadMatFromSquare(mask, background.width());
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY);

        MatOfDouble std = new MatOfDouble();
        Core.meanStdDev(hsvSource, new MatOfDouble(), std, mask);
        final double[] standardDeviation = std.toArray();

        mask.release();
        hsvSource.release();

        // hard-coded value that seems to do a good job in most cases
        final int epsilonHue = 25;
        final int epsilonValue = 50;
        if (standardDeviation[0] > epsilonHue ||
                standardDeviation[2] > epsilonValue) {
            Log.i(TAG, "It seems that the background is colorful!");
            properties.setUncolorful(3);
        } else {
            Log.i(TAG, "It seems that the background color is uniform.");
            properties.setUncolorful(0);
        }
    }

    public static int getContoursLengthOnTheImage(final Mat background) {
        final int cannyThreshold = 30;
        Mat grey = new Mat();
        Imgproc.cvtColor(background, grey, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(grey, grey, new Size(5, 5));
        Mat edges = new Mat();
        Imgproc.Canny(grey, edges, cannyThreshold, cannyThreshold * 3);
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

        for (final Point likelyPoint : likelyPoints) {
            int x = (int) likelyPoint.x;
            int y = (int) likelyPoint.y;
            double brightness = getBrightness(src.get(y, x));
            if ((brightness == 0) == black) {
                return new Point(x, y);
            }
        }
        return null;
    }

    /**
     * {@link #getBrightness(double[] p)}
     *
     * @param p element describing RGB(A) value
     * @return brightness value
     */
    public static double getBrightness(final Scalar p) {
        return getBrightness(p.val);
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
    public static double getBrightness(final double[] p) {
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
        return color > 0.8;
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
