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

    public static boolean isUniform(
            final Mat background,
            final BackgroundProperties properties,
            final ImageSegmentor segmentor) {

        processBackgroundColorBlobDetection(background, properties);
        processEdgeDetection(background, properties);
        processColorsDetection(background, properties, segmentor);

        // Those detections are not fully exact, so it is enough that 2 out of 3
        // state that background is uniform to classify it as uniform.
        return properties.isUniform() ?
                (properties.isEdgesFree() ||
                        properties.isUncolorful()) :
                (properties.isEdgesFree() &&
                        properties.isUncolorful());
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

        Imgproc.cvtColor(personMask, personMask, Imgproc.COLOR_RGB2GRAY);
        personMask.convertTo(personMask, CV_8UC1, 255);
        Imgproc.blur(personMask, personMask, new Size(15, 15));
        Imgproc.threshold(
                personMask, personMask, 127, 255, Imgproc.THRESH_BINARY);
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
                bgContourDetectorLeft.getmBlobColorRgba(),
                bgContourDetectorRight.getmBlobColorRgba());

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
            properties.setUniform(false);
        } else {
            Log.i(TAG, "Background seems to be uniform.");
            properties.setUniform(true);
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
        final int epsilon = imgArea / 150;
        int whitePixels = getContoursLengthOnTheImage(background);
        int approxLengthOfEdges =
                whitePixels - properties.getPersonContourLen();
        if (approxLengthOfEdges > epsilon) {
            Log.i(TAG, "Background seems to contain edges!");
            properties.setEdgesFree(false);
        } else {
            Log.i(TAG, "Background seems to be plain.");
            properties.setEdgesFree(true);
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
            properties.setUncolorful(false);
        } else {
            Log.i(TAG, "It seems that the background color is uniform.");
            properties.setUncolorful(true);
        }
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
