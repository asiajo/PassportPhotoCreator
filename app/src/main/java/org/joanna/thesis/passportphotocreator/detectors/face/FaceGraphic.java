package org.joanna.thesis.passportphotocreator.detectors.face;

import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;

public class FaceGraphic extends Graphic {

    private Canvas canvas;

    private          double bbProportionLeft;
    private          double bbProportionTop;
    private          double bbProportionWidth;
    private          double bbProportionHeight;
    private          Rect   faceBoundingBox;
    private          PointF facePosition;
    private volatile Face   mFace;


    {
        getActionsMap().put(FaceActions.ROTATE_LEFT, R.drawable.arrow_left);
        getActionsMap().put(FaceActions.ROTATE_RIGHT, R.drawable.arrow_right);
        getActionsMap().put(
                FaceActions.STRAIGHTEN_FROM_LEFT,
                R.drawable.arrow_straighten_right);
        getActionsMap().put(
                FaceActions.STRAIGHTEN_FROM_RIGHT,
                R.drawable.arrow_straighten_left);
        getActionsMap().put(FaceActions.FACE_DOWN, R.drawable.arrow_down);
        getActionsMap().put(FaceActions.FACE_UP, R.drawable.arrow_up);
        getActionsMap().put(FaceActions.LEFT_EYE_OPEN, R.drawable.eye);
        getActionsMap().put(FaceActions.RIGHT_EYE_OPEN, R.drawable.eye);
        getActionsMap().put(FaceActions.NEUTRAL_MOUTH, R.drawable.mouth);
    }

    FaceGraphic(final GraphicOverlay overlay) {
        super(overlay);
    }

    void updateFace(final Face face) {
        mFace = face;
        postInvalidate();
    }

    @Override
    public void draw(final Canvas canvas) {
        this.canvas = canvas;
        Face face = mFace;
        if (face == null) {
            return;
        }
        facePosition = face.getPosition();
        faceBoundingBox = FaceUtils.getFaceBoundingBox(face, this);
        setBoundingBoxProportions();
        canvas.drawRect(faceBoundingBox, getmPaint());
        drawActionsToBePerformed(canvas);
    }

    private void setBoundingBoxProportions() {
        double canvasWidth = canvas.getWidth();
        double canvasHeight = canvas.getHeight();
        bbProportionLeft = faceBoundingBox.left / canvasWidth;
        bbProportionTop = faceBoundingBox.top / canvasHeight;
        bbProportionWidth = faceBoundingBox.width() / canvasWidth;
        bbProportionHeight = faceBoundingBox.height() / canvasHeight;
    }

    public Rect getFaceBoundingBox() {
        return faceBoundingBox;
    }

    public double getBbProportionLeft() {
        return bbProportionLeft;
    }

    public double getBbProportionTop() {
        return bbProportionTop;
    }

    public double getBbProportionWidth() {
        return bbProportionWidth;
    }

    public double getBbProportionHeight() {
        return bbProportionHeight;
    }

    public PointF getFacePosition() {
        return facePosition;
    }
}