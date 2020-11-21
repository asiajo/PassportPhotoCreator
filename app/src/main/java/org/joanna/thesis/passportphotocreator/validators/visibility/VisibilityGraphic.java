package org.joanna.thesis.passportphotocreator.validators.visibility;

import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.validators.PhotoValidity;

public class VisibilityGraphic extends Graphic {

    {
        getActionsMap().put(
                VisibilityActions.HIDDEN,
                new BitmapMetaData(
                        VisibilityGraphic.class, R.drawable.face_covered,
                        PhotoValidity.WARNING));
    }

    public VisibilityGraphic(final GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    public void draw(final Canvas canvas) {
        drawActionsToBePerformed(canvas);
    }
}
