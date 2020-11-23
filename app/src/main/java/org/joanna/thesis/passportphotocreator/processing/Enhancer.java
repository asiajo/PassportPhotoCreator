package org.joanna.thesis.passportphotocreator.processing;

import org.opencv.core.Mat;

public interface Enhancer {
    Mat enhance(final Mat src);

    boolean verify(final Mat src);

    void close();
}
