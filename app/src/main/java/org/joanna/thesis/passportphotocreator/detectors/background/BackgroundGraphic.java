package org.joanna.thesis.passportphotocreator.detectors.background;

import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;

public class BackgroundGraphic extends Graphic {

    {
        getActionsMap().put(
                BackgroundActions.NOT_UNIFORM,
                new BitmapMetaData(
                        BackgroundGraphic.class, R.drawable.non_uniform,
                        false));
        getActionsMap().put(
                BackgroundActions.TOO_DARK,
                new BitmapMetaData(
                        BackgroundGraphic.class, R.drawable.too_dark, true));
    }

    public BackgroundGraphic(final GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    public void draw(final Canvas canvas) {
        drawActionsToBePerformed(canvas);
    }
}
