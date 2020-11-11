package org.joanna.thesis.passportphotocreator.detectors.face;

import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.utils.ImageUtils;

public final class FaceUtils {

    private static final float BB_SCALING = 1.2f;

    private FaceUtils() {
    }

    public static Rect getFaceBoundingBox(final Face face,
                                          final Graphic graphic) {
        // getPosition returns left top corner of the face bounding box
        double centerX =
                graphic.translateX(face.getPosition().x + face.getWidth() / 2);
        double centerY =
                graphic.translateY(face.getPosition().y + face.getHeight() / 2);

        // find location of bounding box edges
        final int RatioDivisor = 16;
        final float upperEdgeRatio = (RatioDivisor / 2.0f + 1) / RatioDivisor;
        final float lowerEdgeRatio = (RatioDivisor / 2.0f - 1) / RatioDivisor;
        final float half = 0.5f;

        double widthWithOffset = graphic.scaleX(face.getWidth()) * BB_SCALING;
        double heightWithOffset =
                widthWithOffset / ImageUtils.FINAL_IMAGE_W_TO_H_RATIO;

        int left = (int) (centerX - widthWithOffset * half);
        int top = (int) (centerY - heightWithOffset * upperEdgeRatio);
        int right = (int) (centerX + widthWithOffset * half);
        int bottom = (int) (centerY + heightWithOffset * lowerEdgeRatio);

        return new Rect(left, top, right, bottom);
    }
}
