package org.joanna.thesis.passportphotocreator.validators.background;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.joanna.thesis.passportphotocreator.PhotoMakerActivity;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.joanna.thesis.passportphotocreator.validators.Action;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.utils.BackgroundUtils.findNonPersonPixel;
import static org.joanna.thesis.passportphotocreator.utils.BackgroundUtils.findPersonPixel;
import static org.joanna.thesis.passportphotocreator.utils.BackgroundUtils.getContoursLengthOnTheImage;
import static org.joanna.thesis.passportphotocreator.utils.BackgroundUtils.paste;
import static org.opencv.core.CvType.CV_8UC1;

/**
 * Verifies if background is bright and uniform.
 */
public class BackgroundProcessing {

    private static final String TAG =
            BackgroundProcessing.class.getSimpleName();

    public  ImageSegmentor          segmentor;
    private GraphicOverlay<Graphic> mOverlay;
    private Graphic                 mBackgroundGraphic;
    private Context                 mContext;
    private BackgroundProperties    mBackgroundProperties;
    private Mat                     mBackground;


    public BackgroundProcessing(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) throws IOException {
        mOverlay = overlay;
        mBackgroundGraphic = new BackgroundGraphic(overlay);
        mContext = activity.getApplicationContext();
        mBackgroundProperties = new BackgroundProperties();
        segmentor = new ImageSegmentorFloatMobileUnet(activity);
    }

    /**
     * Performs the verification and sets the graphic overlay respectively.
     *
     * @param data image data under verification
     */
    public void verify(final byte[] data) {

        if (segmentor == null) {
            return;
        }

        mOverlay.add(mBackgroundGraphic);

        mBackground = getBackground(data);
        if (null == mBackground) {
            return;
        }
        if (!mBackground.isContinuous()) {
            mBackground = mBackground.clone();
        }

        processBackgroundColorBlobDetection();
        processEdgeDetection();
        processColorsDetection();

        List<Action> positions = new ArrayList<>();
        // Those detections are not fully exact, so it is enough that 2 out of 3
        // state that background is uniform to classify it as uniform.
        boolean isUniform =
                mBackgroundProperties.isUniform() ?
                        (mBackgroundProperties.isEdgesFree() ||
                                mBackgroundProperties.isUncolorful()) :
                        (mBackgroundProperties.isEdgesFree() &&
                                mBackgroundProperties.isUncolorful());
        if (!isUniform) {
            positions.add(BackgroundActions.NOT_UNIFORM);
        }
        if (null != mBackgroundProperties.isBright() &&
                !mBackgroundProperties.isBright()) {
            positions.add(BackgroundActions.TOO_DARK);
        }

        mBackgroundGraphic.setBarActions(positions, mContext,
                BackgroundGraphic.class);
        mBackground.release();
    }

    public void close() {
        if (segmentor != null) {
            segmentor.close();
        }
    }

    /**
     * Calls the tflite model for image segmentation to retrieve background
     * behind the person present on the image.
     *
     * @param data image frame from the camera in yuv bytes format
     * @return If person is detected on the image returs Mat with the
     *         background, null otherwise.
     */
    private Mat getBackground(final byte[] data) {
        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PhotoMakerActivity.PREVIEW_HEIGHT,
                PhotoMakerActivity.PREVIEW_WIDTH);
        image = ImageUtils.cropMatToFaceBoundingBox(
                image, mOverlay);
        if (image == null) {
            return null;
        }
        int imgWidth = (int) Math.ceil(
                ImageSegmentor.PROCESS_IMG_SIZE
                        * ImageUtils.FINAL_IMAGE_W_TO_H_RATIO);
        image = ImageUtils.resizeMat(image, imgWidth);
        image = ImageUtils.padMatToSquare(
                image,
                ImageSegmentor.PROCESS_IMG_SIZE);

        segmentor.segmentImg(image);
        image = segmentor.getBackground();
        image = ImageUtils.unpadMatFromSquare(image, imgWidth);
        return image;
    }

    /**
     * Attempts to detect background color and verify if it is uniform across
     * the whole background. To achieve this it picks a start point (one of
     * hardcoded, best possible locations - for performance reasons) and
     * searches for the whole area containing almost the same color. Performance
     * of this method depends on how good person was segmented out from the
     * background.
     */
    private void processBackgroundColorBlobDetection() {
        Point pixelBackgroundLeft = findNonPersonPixel(mBackground, true);
        Point pixelBackgroundRight = findNonPersonPixel(mBackground, false);
        Point pixelPerson = findPersonPixel(mBackground);
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
        bgContourDetectorLeft.process(mBackground, pixelBackgroundLeft);

        ColorBlobDetector bgContourDetectorRight = new ColorBlobDetector();
        bgContourDetectorRight.process(mBackground, pixelBackgroundRight);

        final Scalar rgbAverage = computeColorAverage(
                bgContourDetectorLeft.getmBlobColorRgba(),
                bgContourDetectorRight.getmBlobColorRgba());

        mBackgroundProperties.setBgColorRgba(rgbAverage);
        final double areaBgdLeft = bgContourDetectorLeft.getContoursTotalArea();
        final double areaBgdRight =
                bgContourDetectorRight.getContoursTotalArea();

        ColorBlobDetector personContourDetector = new ColorBlobDetector();
        personContourDetector.process(mBackground, pixelPerson);
        mBackgroundProperties.setPersonContourLen(
                (int) personContourDetector.getContoursMaxPerimeter());
        final double areaPerson = personContourDetector.getContoursMaxArea();

        final int imgArea = mBackground.width() * mBackground.height();
        // hard-coded value that seems to do a good job in most cases
        final int epsilon = imgArea / 10;
        if ((imgArea - areaPerson - areaBgdLeft) > epsilon
                && (imgArea - areaPerson - areaBgdRight) > epsilon) {
            Log.i(TAG, "Unfortunately background seems not to be uniform!");
            mBackgroundProperties.setUniform(false);
        } else {
            Log.i(TAG, "Background seems to be uniform.");
            mBackgroundProperties.setUniform(true);
        }
    }

    private Scalar computeColorAverage(
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
    private void processEdgeDetection() {

        final int imgArea = mBackground.width() * mBackground.height();
        // hard-coded value that seems to do a good job in most cases
        final int epsilon = imgArea / 150;
        int whitePixels = getContoursLengthOnTheImage(mBackground);
        int approxLengthOfEdges =
                whitePixels - mBackgroundProperties.getPersonContourLen();
        if (approxLengthOfEdges > epsilon) {
            Log.i(TAG, "Background seems to contain edges!");
            mBackgroundProperties.setEdgesFree(false);
        } else {
            Log.i(TAG, "Background seems to be plain.");
            mBackgroundProperties.setEdgesFree(true);
        }
    }

    /**
     * Calculates median and average of the hue of the background. If median and
     * average are close to each other there is a high probability that the
     * background have one uniform color. Performance of this method depends
     * on how good person was segmented out from the background.
     */
    private void processColorsDetection() {

        Mat hsvSource = new Mat();
        Imgproc.cvtColor(mBackground, hsvSource, Imgproc.COLOR_RGB2HSV);

        int hueBins = 60;
        float hueUpperRange = 180f;
        int hueChannel = 0;
        MatOfInt histSize = new MatOfInt(hueBins);
        MatOfFloat ranges = new MatOfFloat(0f, hueUpperRange);
        MatOfInt channels = new MatOfInt(hueChannel);

        Mat mask = new Mat();
        segmentor.getMaskedPerson().convertTo(mask, CV_8UC1, 255);
        mask = ImageUtils.unpadMatFromSquare(mask, mBackground.width());
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

        double avgHueValue = Core.mean(hsvSource, mask).val[hueChannel];
        double medianHueValue = getMedianChannelValue(
                hueBins, hueUpperRange, hueChannel, histSource);

        mask.release();
        hsvSource.release();
        histSource.release();

        // TODO: improve
        // hard-coded value that seems to do a good job in most cases
        final double epsilon = 3.6;
        if (Math.abs(medianHueValue - avgHueValue) > epsilon) {
            Log.i(TAG, "It seems that the background is colorful!");
            mBackgroundProperties.setUncolorful(false);
        } else {
            Log.i(TAG, "It seems that the background color is uniform.");
            mBackgroundProperties.setUncolorful(true);
        }
    }

    /**
     * Computes median of the channel from the received histogram.
     *
     * @param bins       amount of histogram bins
     * @param upperRange channel maximum allowed value
     * @param channel    Channel for which median shall be computed, eg h =
     *                   0, s = 1, v = 2
     * @param hist       Histogram containing the data
     * @return median value of the channel
     */
    private double getMedianChannelValue(
            final int bins, final float upperRange, final int channel,
            final Mat hist) {

        double medianHueValue = 0;
        double currentElements = 0;
        // First element representing black is removed as it mainly
        // represents masked person
        int half = (int) ((mBackground.width() * mBackground.height() -
                hist.get(0, 0)[channel]) / 2);
        for (int i = 1; i < 60; i++) {
            currentElements += hist.get(i, 0)[channel];
            if (currentElements > half) {
                medianHueValue = i * (upperRange / bins) + 1;
                break;
            }
        }
        return medianHueValue;
    }

    public Mat enhance(final Mat src) {

        if (segmentor == null) {
            return null;
        }

        Mat personMask = getPersonMask(src);

        Mat person = getPersonWithoutBackground(src, personMask);
        personMask.release();

        Mat background = new Mat();
        Imgproc.blur(src, background, new Size(10, 10));
        background.convertTo(background, -1, 1, 10); //brighten

        Mat dst = paste(person, background);
        person.release();
        background.release();

        return dst;
    }

    private Mat getPersonWithoutBackground(
            final Mat src, final Mat personMask) {

        List<Mat> channels = new ArrayList<>();
        Core.split(src, channels);
        channels.add(personMask);
        Mat dst = new Mat();
        Core.merge(channels, dst);
        return dst;
    }

    private Mat getPersonMask(final Mat src) {
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
        personMask = ImageUtils.unpadMatFromSquare(personMask, imgWidth);
        personMask = ImageUtils.resizeMat(personMask, src.width());
        return personMask;
    }

}
