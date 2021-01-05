package org.joanna.thesis.passportphotocreator.processing;

import android.app.Activity;
import android.content.Context;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;

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
     * @param face face discovered on the iamge
     */
    public abstract void verify(final byte[] data, final Face face);

    public void close() {

    }

}
