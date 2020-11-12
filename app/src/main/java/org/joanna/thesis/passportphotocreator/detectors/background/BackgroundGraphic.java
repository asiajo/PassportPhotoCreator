package org.joanna.thesis.passportphotocreator.detectors.background;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.detectors.Action;

import java.util.List;

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
