package org.joanna.thesis.passportphotocreator.detectors.background;

import android.app.Activity;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC1;

/** This segmentor works with the float mobile-unet model. */
public class ImageSegmentorFloatMobileUnet extends ImageSegmentor {

    public static final  int   MODEL_INPUT_IMG_SIZE = 128;
    private static final float IMAGE_MEAN           = 127.5f;
    private static final float IMAGE_STD            = 127.5f;

    /** An array to hold inference results. */
    private float[][] segmap;

    /**
     * Initializes an {@code ImageSegmentorFloatMobileUnet}.
     *
     * @param activity
     */
    ImageSegmentorFloatMobileUnet(Activity activity) throws IOException {
        super(activity);
        segmap = new float[1][MODEL_INPUT_IMG_SIZE * MODEL_INPUT_IMG_SIZE];
    }

    @Override
    protected String getModelPath() {
        return "deconv_fin_munet.tflite";
    }

    @Override
    protected int getImageSizeX() {
        return MODEL_INPUT_IMG_SIZE;
    }

    @Override
    protected int getImageSizeY() {
        return MODEL_INPUT_IMG_SIZE;
    }

    @Override
    protected int getNumBytesPerChannel() {
        return Float.SIZE / Byte.SIZE;
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat(
                (((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    }

    @Override
    protected void runInference() {
        tflite.run(imgData, segmap);
    }

    @Override
    protected Mat getBackground(final Mat src) {

        if (segmap == null) {
            return null;
        }
        Mat mask = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE,
                CV_32F);
        Mat maskInverted = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE,
                CV_32F, new Scalar(1.0));
        mask.put(0, 0, segmap[0]);
        Core.subtract(maskInverted, mask, maskInverted);
        mask.release();

        Mat background = applyMask(src, maskInverted);
        maskInverted.release();

        return background;
    }

    @Override
    protected Mat getForeground(final Mat src) {

        if (segmap == null) {
            return null;
        }
        Mat mask = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE, CV_32F);
        mask.put(0, 0, segmap[0]);
        Mat foreground = applyMask(src, mask);
        mask.release();

        return foreground;
    }

    private Mat applyMask(final Mat src, final Mat mask) {
        Core.multiply(mask, new Scalar(2.0), mask);
        Imgproc.threshold(mask, mask, 1.0, 1.0, Imgproc.THRESH_TRUNC);

        if (!(src.width() == PROCESS_IMG_SIZE &&
                src.height() == PROCESS_IMG_SIZE)) {
            return null;
        }

        Imgproc.resize(mask, mask, new Size(src.width(), src.height()));
        List<Mat> channels = new ArrayList<>();
        Core.split(src, channels);
        mask.convertTo(mask, CV_8UC1, 255);
        channels.remove(channels.size() - 1);
        channels.add(mask);
        Mat masked = new Mat();
        Core.merge(channels, masked);

        return masked;
    }

}
