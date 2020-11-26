package org.joanna.thesis.passportphotocreator.processing.light.verification;

import android.app.Activity;

import java.io.IOException;

/**
 * This classifier works with the float mobile-net-V2 model.
 */
public class ShadowVerificatorFloatMobileNetV2 extends ShadowVerificator {

    public static final int MODEL_INPUT_IMG_SIZE = 160;

    /** An array to hold inference results. */
    private float[][] segmap;

    /**
     * Initializes an {@code ShadowVerificatorFloatMobileNetV2}.
     *
     * @param activity
     */
    public ShadowVerificatorFloatMobileNetV2(Activity activity)
            throws IOException {
        super(activity);
        segmap = new float[1][1];
    }

    @Override
    protected String getModelPath() {
        return "shadow_verification_mobileNetV2.tflite";
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
        mImgData.putFloat((pixelValue >> 16) & 0xFF);
        mImgData.putFloat((pixelValue >> 8) & 0xFF);
        mImgData.putFloat(pixelValue & 0xFF);
    }

    @Override
    protected void runInference() {
        mTflite.run(mImgData, segmap);
    }

    @Override
    public boolean isEvenlyLightened() {
        if (segmap == null) {
            return false;
        }
        return segmap[0][0] > 0;
    }

}
