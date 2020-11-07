package org.joanna.thesis.passportphotocreator.detectors.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;

import com.google.android.gms.vision.face.Face;

import org.joanna.thesis.passportphotocreator.R;
import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceGraphic extends Graphic {

    private static final float  BOX_STROKE_WIDTH = 5.0f;
    private static final int    VALID_COLOR      = Color.GREEN;
    private static final int    INVALID_COLOR    = Color.RED;
    public               Canvas canvas;
    public volatile      Face   mFace;
    private              Paint  mPaint;
    private              Rect   faceBoundingBox  = null;
    private              PointF facePosition;

    private List<Bitmap>               headActions;
    private Map<FacePosition, Integer> headActionsMap = new HashMap<>();

    {
        headActionsMap.put(FacePosition.ROTATE_LEFT, R.drawable.arrow_left);
        headActionsMap.put(FacePosition.ROTATE_RIGHT, R.drawable.arrow_right);
        headActionsMap.put(
                FacePosition.STRAIGHTEN_FROM_LEFT,
                R.drawable.arrow_straighten_right);
        headActionsMap.put(
                FacePosition.STRAIGHTEN_FROM_RIGHT,
                R.drawable.arrow_straighten_left);
        headActionsMap.put(FacePosition.FACE_DOWN, R.drawable.arrow_down);
        headActionsMap.put(FacePosition.FACE_UP, R.drawable.arrow_up);
        headActionsMap.put(FacePosition.LEFT_EYE_OPEN, R.drawable.eye);
        headActionsMap.put(FacePosition.RIGHT_EYE_OPEN, R.drawable.eye);
        headActionsMap.put(FacePosition.NEUTRAL_MOUTH, R.drawable.mouth);

    }

    FaceGraphic(final GraphicOverlay overlay) {
        super(overlay);
        mPaint = new Paint();
        mPaint.setColor(INVALID_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(BOX_STROKE_WIDTH);
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
        drawFaceBoundingBox(canvas, face);
        drawFaceActionsToBePerformed(canvas);
    }

    private void drawFaceActionsToBePerformed(final Canvas canvas) {
        // draws actions to be performed to correct face position
        int i = 0;
        int iconSize = canvas.getWidth() / 12;
        int padding = iconSize / 5;
        for (Bitmap headAction : headActions) {
            final Rect rectSrc = new Rect(0, 0, headAction.getWidth(),
                    headAction.getHeight());
            final Rect rectDst = new Rect(
                    (i + 1) * padding + i * iconSize,
                    padding,
                    (i + 1) * padding + (i + 1) * iconSize,
                    padding + iconSize);
            canvas.drawBitmap(headAction, rectSrc, rectDst, mPaint);
            i++;
        }
    }

    private void drawFaceBoundingBox(final Canvas canvas, final Face face) {
        // TODO: refactor

        facePosition = face.getPosition();
        // get center of the face
        double centerX = translateX(face.getPosition().x + face.getWidth() / 2);
        double centerY =
                translateY(face.getPosition().y + face.getHeight() / 2);

        // find location of bounding box edges
        final int RatioDivisor = 24;
        final float upperEdgeRatio = (RatioDivisor / 2.0f + 1) / RatioDivisor;
        final float lowerEdgeRatio = (RatioDivisor / 2.0f - 1) / RatioDivisor;
        double widthWithOffset = scaleX(face.getWidth()) * 1.3f;
        double heightWithOffset = widthWithOffset / 3.5f * 4.5f;
        double left = (float) (centerX - widthWithOffset / 2);
        double top = (float) (centerY - heightWithOffset * upperEdgeRatio);
        double right = (float) (centerX + widthWithOffset / 2);
        double bottom = centerY + heightWithOffset * lowerEdgeRatio;
        faceBoundingBox = new Rect(
                (int) left, (int) top, (int) right, (int) bottom);

        // draw a bounding box around the face.
        canvas.drawRect((float) left, (float) top, (float) right,
                (float) bottom, mPaint);
    }

    @Override
    public void setBarActions(
            final List<FacePosition> positions,
            final Context context) {
        headActions = new ArrayList<>();
        for (FacePosition position : positions) {
            headActions.add(BitmapFactory.decodeResource(
                    context.getResources(),
                    headActionsMap.get(position)));
        }
    }

    public Rect getFaceBoundingBox() {
        return faceBoundingBox;
    }

    public PointF getFacePosition() {
        return facePosition;
    }

    void setIsValid(final boolean isValid) {
        mPaint.setColor(isValid ? VALID_COLOR : INVALID_COLOR);
    }
}