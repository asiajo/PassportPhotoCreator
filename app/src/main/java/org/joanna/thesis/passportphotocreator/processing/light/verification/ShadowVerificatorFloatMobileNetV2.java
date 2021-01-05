package org.joanna.thesis.passportphotocreator.processing.light.verification;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;

import static org.joanna.thesis.passportphotocreator.processing.light.verification.ShadowVerificator.EvenlyLigtened.EVENLY;
import static org.joanna.thesis.passportphotocreator.processing.light.verification.ShadowVerificator.EvenlyLigtened.NOT_SURE;
import static org.joanna.thesis.passportphotocreator.processing.light.verification.ShadowVerificator.EvenlyLigtened.SHADOW;

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
    public EvenlyLigtened isEvenlyLightened() {
        if (segmap == null) {
            return null;
        }
        if (segmap[0][0] > 1){
            return EVENLY;
        } else if (segmap[0][0] < -1){
            return SHADOW;
        } else {
            return NOT_SURE;
        }
    }

}
