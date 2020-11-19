package org.joanna.thesis.passportphotocreator.validators.background;

import org.joanna.thesis.passportphotocreator.utils.BackgroundUtils;
import org.opencv.core.Scalar;

/**
 * Collection of background properties required for background verification.
 */
class BackgroundProperties {

    private int     personContourLen;
    private Boolean isUniform    = true;
    private Boolean isUncolorful = true;
    private Boolean isBright     = true;
    private Boolean isEdgesFree  = true;

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

    public Boolean isUniform() {
        return isUniform;
    }

    public void setUniform(final boolean uniform) {
        isUniform = uniform;
    }

    public Boolean isBright() {
        return isBright;
    }

    public Boolean isEdgesFree() {
        return isEdgesFree;
    }

    public void setEdgesFree(final boolean edgesFree) {
        isEdgesFree = edgesFree;
    }

    public Boolean isUncolorful() {
        return isUncolorful;
    }

    public void setUncolorful(final Boolean uncolorful) {
        isUncolorful = uncolorful;
    }
}
