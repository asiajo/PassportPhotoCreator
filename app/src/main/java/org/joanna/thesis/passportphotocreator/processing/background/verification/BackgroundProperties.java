package org.joanna.thesis.passportphotocreator.processing.background.verification;

import org.joanna.thesis.passportphotocreator.processing.background.BackgroundUtils;
import org.opencv.core.Scalar;

/**
 * Collection of background properties required for background verification.
 */
public class BackgroundProperties {

    private int     personContourLen;
    private int     isUniform;
    private int     isUncolorful;
    private Boolean isBright = true;
    private int     isEdgesFree;

    public void setBgColorRgba(final Scalar bgColor) {
        final double brightness = BackgroundUtils.getBrigtness(bgColor);
        isBright = BackgroundUtils.isBright(brightness);
    }

    public int getPersonContourLen() {
        return personContourLen;
    }

    public void setPersonContourLen(final int personContourLen) {
        this.personContourLen = personContourLen;
    }

    public int isUniform() {
        return isUniform;
    }

    public void setUniform(final int uniform) {
        isUniform = uniform;
    }

    public Boolean isBright() {
        return isBright;
    }

    public int isEdgesFree() {
        return isEdgesFree;
    }

    public void setEdgesFree(final int edgesFree) {
        isEdgesFree = edgesFree;
    }

    public int isUncolorful() {
        return isUncolorful;
    }

    public void setUncolorful(final int uncolorful) {
        isUncolorful = uncolorful;
    }
}
