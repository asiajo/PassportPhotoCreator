package org.joanna.thesis.passportphotocreator.processing.background.verification;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Action;
import org.joanna.thesis.passportphotocreator.processing.Verifier;
import org.joanna.thesis.passportphotocreator.processing.background.BackgroundUtils;
import org.joanna.thesis.passportphotocreator.processing.background.ImageSegmentor;
import org.joanna.thesis.passportphotocreator.processing.background.ImageSegmentorFloatMobileUNet;
import org.joanna.thesis.passportphotocreator.processing.face.FaceUtils;
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

    public        ImageSegmentor       mSegmentor;
    private final Graphic              mBackgroundGraphic;
    private final BackgroundProperties mBackgroundProperties;


    public BackgroundVerifier(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) throws IOException {
        super(activity, overlay);
        mBackgroundGraphic = new BackgroundGraphic(overlay);
        mBackgroundProperties = new BackgroundProperties();
        mSegmentor = new ImageSegmentorFloatMobileUNet(activity);
    }

    @Override
    public void close() {
        if (mSegmentor != null) {
            mSegmentor.close();
        }
    }

    @Override
    public Boolean verify(final byte[] data, final Face face) {

        if (mSegmentor == null) {
            return null;
        }

        mOverlay.add(mBackgroundGraphic);

        Mat mBackground = getBackground(data, face);
        if (null == mBackground) {
            return null;
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
        return positions.size() == 0;
    }

    /**
     * Calls the tflite model for image segmentation to retrieve background
     * behind the person present on the image.
     *
     * @param data image frame from the camera in yuv bytes format
     * @param face face discovered on the image
     * @return If person is detected on the image returns Mat with the
     *         background, null otherwise.
     */
    private Mat getBackground(final byte[] data, final Face face) {
        Mat image = ImageUtils.getMatFromYuvBytes(
                data,
                PREVIEW_HEIGHT,
                PREVIEW_WIDTH);
        final Rect bBox = FaceUtils.getFaceBoundingBox(face, null);
        image = ImageUtils.cropMatToBoundingBox(image, bBox);
        if (image == null) {
            return null;
        }
        return BackgroundUtils.getBackground(image, mSegmentor);
    }
}
