package org.joanna.thesis.passportphotocreator.processing.face;

import android.content.Context;

import com.google.mlkit.vision.face.Face;

import org.joanna.thesis.passportphotocreator.camera.Graphic;
import org.joanna.thesis.passportphotocreator.camera.GraphicOverlay;
import org.joanna.thesis.passportphotocreator.processing.Action;

import java.util.ArrayList;
import java.util.List;

public class FaceTracker {

    private static final String TAG = FaceTracker.class.getSimpleName();

    private static final double                  NEUTRAL_FACE_THRESHOLD = 0.5;
    private static final double                  EYES_OPEN_THRESHOLD    = 0.7;
    private static final double                  ROTATION_THRESHOLD     = 4;
    private static final double                  ROTATION_X_THRESHOLD   = 8;
    private              List<Face>              mFaces;
    private              GraphicOverlay<Graphic> mOverlay;
    private              FaceGraphic             mFaceGraphic;
    private              Context                 mContext;

    public FaceTracker(
            final GraphicOverlay<Graphic> overlay,
            final Context context) {
        super();
        this.mContext = context;
        this.mOverlay = overlay;
        mFaceGraphic = new FaceGraphic(overlay, context);
    }

    public void processFaces(final List<Face> faces) {
        mFaces = faces;
        List<Action> positions = new ArrayList<>();
        mOverlay.add(mFaceGraphic);
        mFaceGraphic.updateFaces(faces);

        if (faces.size() == 0) {
            mOverlay.remove(mFaceGraphic);
            mFaceGraphic.clearActions();
            return;
        }
        if (faces.size() > 1) {
            mFaceGraphic.clearActions();
            positions.add(FaceActions.TOO_MANY_FACES);
            mFaceGraphic.setBarActions(positions, mContext, FaceGraphic.class);
            return;
        }

        Face face = faces.get(0);
        if (face.getHeadEulerAngleY() < -ROTATION_THRESHOLD) {
            positions.add(FaceActions.ROTATE_LEFT);
        } else if (face.getHeadEulerAngleY() > ROTATION_THRESHOLD) {
            positions.add(FaceActions.ROTATE_RIGHT);
        }
        if (face.getHeadEulerAngleX() < -ROTATION_X_THRESHOLD) {
            positions.add(FaceActions.FACE_UP);
        } else if (face.getHeadEulerAngleX() > ROTATION_X_THRESHOLD) {
            positions.add(FaceActions.FACE_DOWN);
        }
        if (face.getHeadEulerAngleZ() < -ROTATION_THRESHOLD) {
            positions.add(FaceActions.STRAIGHTEN_FROM_LEFT);
        } else if (face.getHeadEulerAngleZ() > ROTATION_THRESHOLD) {
            positions.add(FaceActions.STRAIGHTEN_FROM_RIGHT);
        }
        if (face.getLeftEyeOpenProbability() < EYES_OPEN_THRESHOLD) {
            positions.add(FaceActions.LEFT_EYE_OPEN);
        }
        if (face.getRightEyeOpenProbability() < EYES_OPEN_THRESHOLD) {
            positions.add(FaceActions.RIGHT_EYE_OPEN);
        }
        if (face.getSmilingProbability() > NEUTRAL_FACE_THRESHOLD) {
            positions.add(FaceActions.NEUTRAL_MOUTH);
        }
        mFaceGraphic.setBarActions(positions, mContext, FaceGraphic.class);
    }

    public List<Face> getFaces() {
        return mFaces;
    }

    public void clear() {
        mOverlay.remove(mFaceGraphic);
        mOverlay.slowlyPostinvalidate();
        mFaceGraphic.clearActions();
    }
}
