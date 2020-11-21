package org.joanna.thesis.passportphotocreator.processing.face;

import org.joanna.thesis.passportphotocreator.processing.Action;

public enum FaceActions implements Action {
    ROTATE_LEFT,
    ROTATE_RIGHT,
    FACE_UP,
    FACE_DOWN,
    STRAIGHTEN_FROM_LEFT,
    STRAIGHTEN_FROM_RIGHT,
    LEFT_EYE_OPEN,
    RIGHT_EYE_OPEN,
    NEUTRAL_MOUTH
}
