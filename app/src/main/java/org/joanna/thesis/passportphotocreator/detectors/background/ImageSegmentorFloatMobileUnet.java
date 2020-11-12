package org.joanna.thesis.passportphotocreator.detectors.background;

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
import static org.opencv.imgproc.Imgproc.COLOR_RGBA2RGB;

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

        Mat mskmat = new Mat(MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE,
                CV_32F);
        Mat invmskmat = new Mat(PROCESS_IMG_SIZE, PROCESS_IMG_SIZE,
                CV_32FC3, new Scalar(1.0, 1.0, 1.0));

        if (segmap == null) {
            return null;
        }

        mskmat.put(0, 0, segmap[0]);
        Core.multiply(mskmat, new Scalar(2.0), mskmat);
        Imgproc.threshold(mskmat, mskmat, 1.0, 1.0, Imgproc.THRESH_TRUNC);

        Imgproc.resize(mskmat, mskmat, new Size(
                PROCESS_IMG_SIZE,
                PROCESS_IMG_SIZE));
        Imgproc.cvtColor(mskmat, mskmat, Imgproc.COLOR_GRAY2BGR);
        Mat background = new Mat();
        Imgproc.cvtColor(src, background, COLOR_RGBA2RGB);

        background.convertTo(background, CV_32FC3, 1.0 / 255.0);
        Core.subtract(invmskmat, mskmat, invmskmat);
        Core.multiply(background, invmskmat, background);
        mskmat.release();
        invmskmat.release();

        background.convertTo(background, CV_8UC3, 255);
        return background;
    }

}
