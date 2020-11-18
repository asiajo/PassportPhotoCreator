package org.joanna.thesis.passportphotocreator.detectors.light;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;

import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

public class ShadowRemoverPix2Pix extends ShadowRemover {

    public static final int MODEL_INPUT_IMG_SIZE = 256;

    /** An array to hold inference results. */
    private float[][][][] modelOutput;

    /**
     * Initializes an {@code ShadowRemoverPix2Pix}.
     *
     * @param activity
     */
    public ShadowRemoverPix2Pix(Activity activity) throws IOException {
        super(activity);
        modelOutput = new float
                [1][MODEL_INPUT_IMG_SIZE][MODEL_INPUT_IMG_SIZE][COLOR_CHANNELS_NO];
    }

    @Override
    protected String getModelPath() {
        return "pix2pix.tflite";
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
        mImgData.putFloat(((pixelValue >> 16) & 0xFF) / 255.0f);
        mImgData.putFloat(((pixelValue >> 8) & 0xFF) / 255.0f);
        mImgData.putFloat((pixelValue & 0xFF) / 255.0f);
    }

    @Override
    protected void runInference() {
        mTflite.run(mImgData, modelOutput);
    }

    @Override
    protected Mat getDeshadowedOverlay() {

        final int alpha = 255;
        Bitmap tmp = Bitmap.createBitmap(
                MODEL_INPUT_IMG_SIZE, MODEL_INPUT_IMG_SIZE,
                Bitmap.Config.ARGB_8888);

        for (int x = 0; x < MODEL_INPUT_IMG_SIZE; x++) {
            for (int y = 0; y < MODEL_INPUT_IMG_SIZE; y++) {
                int red = Math.round(modelOutput[0][x][y][0] * 255);
                int green = Math.round(modelOutput[0][x][y][1] * 255);
                int blue = Math.round(modelOutput[0][x][y][2] * 255);

                int color = Color.argb(alpha, red, green, blue);
                tmp.setPixel(y, x, color);
            }
        }
        Mat deshadowed = new Mat();
        Utils.bitmapToMat(tmp, deshadowed);
        ImageUtils.safelyRemoveBitmap(tmp);

        // Model returns an image with slightly modified size: a little bit
        // smaller and moved. Correct it here. Hard-coded values, as in ideal
        // world those two lines should not have been here.
        Imgproc.resize(deshadowed, deshadowed, new Size(259, 259));
        deshadowed = deshadowed.submat(3, 259, 1, 257);

        return deshadowed;
    }

    @Override
    protected int getOutputImageWidth() {
        return (int) Math.ceil(
                ShadowRemoverPix2Pix.MODEL_INPUT_IMG_SIZE
                        * ImageUtils.FINAL_IMAGE_W_TO_H_RATIO);
    }

}
