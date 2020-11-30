package org.joanna.thesis.passportphotocreator.processing.background.verification;

import android.app.Activity;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.joanna.thesis.passportphotocreator.processing.background.BackgroundUtils;
import org.joanna.thesis.passportphotocreator.processing.background.ImageSegmentor;
import org.joanna.thesis.passportphotocreator.processing.background.ImageSegmentorFloatMobileUnet;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_HEIGHT;
import static org.joanna.thesis.passportphotocreator.PhotoMakerActivity.PREVIEW_WIDTH;

/**
 * Verifies if background is bright and uniform.
 */
public class BackgroundVerifier extends Verifier {

    private static final String TAG = BackgroundVerifier.class.getSimpleName();

    public  ImageSegmentor       mSegmentor;
    private Graphic              mBackgroundGraphic;
    private BackgroundProperties mBackgroundProperties;
    private Mat                  mBackground;


    public BackgroundVerifier(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) throws IOException {
        super(activity, overlay);
        mBackgroundGraphic = new BackgroundGraphic(overlay);
        mBackgroundProperties = new BackgroundProperties();
        mSegmentor = new ImageSegmentorFloatMobileUnet(activity);
    }

    @Override
    public void close() {
        if (mSegmentor != null) {
            mSegmentor.close();
        }
    }

    @Override
    public void verify(final byte[] data) {

        if (mSegmentor == null) {
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

        List<Action> positions = new ArrayList<>();

        if (BackgroundUtils.isUniform(
                mBackground, mBackgroundProperties, mSegmentor) >= 3) {
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
                PREVIEW_HEIGHT,
                PREVIEW_WIDTH);
        image = ImageUtils.cropMatToFaceBoundingBox(
                image, mOverlay);
        if (image == null) {
            return null;
        }
        return BackgroundUtils.getBackground(image, mSegmentor);
    }
}
