package org.joanna.thesis.passportphotocreator.processing;

import org.opencv.core.Mat;

public interface Enhancer {
    Mat enhance(Mat src);

    void close();
}
