package org.joanna.thesis.passportphotocreator.validators.background;

import android.app.Activity;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_32FC3;
import static org.opencv.core.CvType.CV_8UC3;

/**
 * Based on Portrait Segmentation Sample from Tensorflow.
 * This segmentor works with the float mobile-unet model.
 */
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
    public ImageSegmentorFloatMobileUnet(Activity activity) throws IOException {
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
        mImgData.putFloat(
                (((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        mImgData.putFloat(
                (((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
        mImgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
    }

    @Override
    protected void runInference() {
        mTflite.run(mImgData, segmap);
    }

    @Override
    public Mat getBackground() {
        Mat maskInverted = getMaskedPerson();
        Mat background = applyMask(mImage, maskInverted);
        maskInverted.release();

        return background;
    }

    @Override
    public Mat getForeground() {

        Mat mask = getMaskedBackground();
        if (mask == null) {
            return null;
        }
        Mat foreground = applyMask(mImage, mask);
        mask.release();

        return foreground;
    }

    @Override
    public Mat getMaskedBackground() {
        if (segmap == null) {
            return null;
        }
        Mat mask = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE, CV_32F);
        mask.put(0, 0, segmap[0]);
        return convertMask(mask);
    }

    @Override
    public Mat getMaskedPerson() {
        if (segmap == null) {
            return null;
        }
        Mat mask = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE, CV_32F);
        mask.put(0, 0, segmap[0]);

        Mat maskInverted = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE,
                CV_32F, new Scalar(1.0));
        Core.subtract(maskInverted, mask, maskInverted);
        mask.release();
        return convertMask(maskInverted);
    }

    private Mat applyMask(final Mat src, final Mat mask) {

        if (!(src.width() == PROCESS_IMG_SIZE &&
                src.height() == PROCESS_IMG_SIZE)) {
            return null;
        }
        Mat masked = new Mat();
        Imgproc.cvtColor(src, masked, Imgproc.COLOR_RGBA2RGB);
        masked.convertTo(masked, CV_32FC3, 1.0 / 255.0);
        Core.multiply(masked, mask, masked);
        masked.convertTo(masked, CV_8UC3, 255);

        return masked;
    }

    private Mat convertMask(final Mat src) {
        Mat dst = new Mat();
        Core.multiply(src, new Scalar(2.0), dst);
        Imgproc.threshold(dst, dst, 1.0, 1.0, Imgproc.THRESH_TRUNC);
        Imgproc.resize(dst, dst, new Size(PROCESS_IMG_SIZE, PROCESS_IMG_SIZE));
        Imgproc.cvtColor(dst, dst, Imgproc.COLOR_GRAY2BGR);
        return dst;
    }

}
