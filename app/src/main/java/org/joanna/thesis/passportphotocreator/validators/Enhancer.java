package org.joanna.thesis.passportphotocreator.validators;

import org.opencv.core.Mat;

public interface Enhancer {
    Mat enhance(Mat src);

    void close();
}
