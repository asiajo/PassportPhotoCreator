package org.joanna.thesis.passportphotocreator.detectors.face;

import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;

public final class FaceUtils {

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
        final int RatioDivisor = 24;
        final float upperEdgeRatio = (RatioDivisor / 2.0f + 1) / RatioDivisor;
        final float lowerEdgeRatio = (RatioDivisor / 2.0f - 1) / RatioDivisor;
        final float half = 0.5f;

        double widthWithOffset = graphic.scaleX(face.getWidth()) * 1.3f;
        double heightWithOffset = widthWithOffset / 3.5f * 4.5f;

        double left = (float) (centerX - widthWithOffset * half);
        double top = (float) (centerY - heightWithOffset * upperEdgeRatio);
        double right = (float) (centerX + widthWithOffset * half);
        double bottom = centerY + heightWithOffset * lowerEdgeRatio;

        return new Rect((int) left, (int) top, (int) right, (int) bottom);
    }
}
