package org.joanna.thesis.passportphotocreator.processing.light.verification;

import android.app.Activity;
import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_HEIGHT;
import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_WIDTH;
import static org.joanna.thesis.passportphotocreator.processing.light.ShadowUtils.isEvenlyLightened;

/**
 * Verifies if face does not contain side shadows.
 */
public class ShadowVerification extends Verifier {

    private static final String TAG =
            ShadowVerification.class.getSimpleName();

    private Graphic mShadowGraphic;
    private ShadowVerificator mShadowVerificator;

    public ShadowVerification(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) {
        super(activity, overlay);
        mShadowGraphic = new ShadowGraphic(overlay);
        try {
            mShadowVerificator =
                    new ShadowVerificatorFloatMobileNetV2(activity);
            Log.i(TAG, "Succesfully initialized shadow verifier.");
        } catch (IOException e) {
            Log.e(
                    TAG,
                    "Could not initialize shadow verifier tensorflow model. " +
                            "Program will run without it.");
            e.printStackTrace();
        }
    }

    @Override
    public void verify(final byte[] data, final Face face) {

        mOverlay.add(mShadowGraphic);
        List<Action> positions = new ArrayList<>();

        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PREVIEW_HEIGHT,
                PREVIEW_WIDTH);
        Rect bbox = face.getBoundingBox();
        int left = bbox.left + bbox.width() / 8;
        int right = bbox.right - bbox.width() / 8;
        int top = bbox.top + bbox.height() / 4;
        image = ImageUtils.cropMatToBoundingBox(
                image,
                new Rect(left, top, right, bbox.bottom));

        if (null == image) {
            return;
        }

        ShadowVerificator.EvenlyLigtened isEvenlyLightened = null;
        if (mShadowVerificator != null) {
            mShadowVerificator.classify(image);
            isEvenlyLightened = mShadowVerificator.isEvenlyLightened();
        }

        if (null != isEvenlyLightened &&
                isEvenlyLightened == ShadowVerificator.EvenlyLigtened.SHADOW) {
            positions.add(ShadowActions.NOT_UNIFORM);
            Log.i(TAG, "Face is not evenly lightened.");
        } else if (null == isEvenlyLightened || isEvenlyLightened ==
                ShadowVerificator.EvenlyLigtened.NOT_SURE) {
            Log.i(TAG,"Verifying with OpenCV if face's evenly lightened.");
            if (!isEvenlyLightened(image)) {
                positions.add(ShadowActions.NOT_UNIFORM);
            }
        }

        mShadowGraphic.setBarActions(positions, mContext,
                ShadowGraphic.class);

        image.release();
    }

}
