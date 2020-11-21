package org.joanna.thesis.passportphotocreator.validators;

import android.app.Activity;
import android.content.Context;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.opencv.core.Mat;

public abstract class Verifier {

    protected GraphicOverlay<Graphic> mOverlay;
    protected Context                 mContext;

    protected Verifier(
            final Activity activity,
            final GraphicOverlay<Graphic> overlay) {
        mOverlay = overlay;
        mContext = activity.getApplicationContext();
    }

    /**
     * Performs the verification and sets the graphic overlay respectively.
     *
     * @param data image data under verification
     */
    public abstract void verify(final byte[] data);

}
