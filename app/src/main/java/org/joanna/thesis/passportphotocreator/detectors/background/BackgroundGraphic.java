package org.joanna.thesis.passportphotocreator.detectors.background;

import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;

public class BackgroundGraphic extends Graphic {

    {
        getActionsMap().put(
                BackgroundActions.NOT_UNIFORM,
                R.drawable.non_uniform);
        getActionsMap().put(
                BackgroundActions.TOO_DARK,
                R.drawable.too_dark);
    }

    public BackgroundGraphic(final GraphicOverlay overlay) {
        super(overlay);
    }

    @Override
    public void draw(final Canvas canvas) {
        drawActionsToBePerformed(canvas);
    }
}
