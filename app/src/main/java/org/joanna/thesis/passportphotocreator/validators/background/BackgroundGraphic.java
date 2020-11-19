package org.joanna.thesis.passportphotocreator.validators.background;

import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.validators.PhotoValidity;

public class BackgroundGraphic extends Graphic {

    {
        getActionsMap().put(
                BackgroundActions.NOT_UNIFORM,
                new BitmapMetaData(
                        BackgroundGraphic.class, R.drawable.non_uniform,
                        PhotoValidity.WARNING));
        getActionsMap().put(
                BackgroundActions.TOO_DARK,
                new BitmapMetaData(
                        BackgroundGraphic.class, R.drawable.too_dark,
                        PhotoValidity.INVALID));
    }

    public BackgroundGraphic(final GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    public void draw(final Canvas canvas) {
        drawActionsToBePerformed(canvas);
    }
}
