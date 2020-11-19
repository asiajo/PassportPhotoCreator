package org.joanna.thesis.passportphotocreator.detectors.light;

import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.camera.PhotoValidity;

public class ShadowGraphic extends Graphic {

    {
        getActionsMap().put(
                ShadowActions.NOT_UNIFORM,
                new BitmapMetaData(
                        ShadowGraphic.class, R.drawable.face_shadow,
                        PhotoValidity.WARNING));
    }

    public ShadowGraphic(final GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    public void draw(final Canvas canvas) {
        drawActionsToBePerformed(canvas);
    }
}
