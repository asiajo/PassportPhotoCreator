package org.joanna.thesis.passportphotocreator.detectors.background;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.joanna.thesis.passportphotocreator.utils.ImageUtils;
import org.opencv.core.Mat;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Segments images with Tensorflow Lite.
 */
public abstract class ImageSegmentor {

    public static final int PROCESS_IMG_SIZE = 448;

    private static final String TAG = ImageSegmentor.class.getSimpleName();

    private static final int DIM_PIXEL_SIZE = 3;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();
    /** An instance of the driver class to run model inference with Tfl Lite. */
    protected     Interpreter         tflite;
    /** A ByteBuffer to hold image data, to be feed into Tfl Lite as inputs. */
    protected     ByteBuffer          imgData;
    /** Preallocated buffers for storing image data in. */
    private       int[]               intValues;
    /** The loaded TensorFlow Lite model. */
    private       MappedByteBuffer    tfliteModel;

    ImageSegmentor(Activity activity) throws IOException {
        tfliteModel = loadModelFile(activity);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
        intValues = new int[getImageSizeX() * getImageSizeY()];
        imgData = ByteBuffer.allocateDirect(
                getImageSizeX() * getImageSizeY()
                        * DIM_PIXEL_SIZE
                        * getNumBytesPerChannel());
        imgData.order(ByteOrder.nativeOrder());
        Log.d(TAG, "Created a Tensorflow Lite Image Segmentor.");
    }

    /**
     * Segments a frame from the preview stream.
     */
    public Mat segmentImgGetBackground(Mat image) {
        if (tflite == null) {
            Log.e(TAG, "Image segmentor has not been initialized; Skipped.");
        }

        Mat resizedMat128 = ImageUtils.resizeMat(image, getImageSizeX(),
                getImageSizeY());
        Bitmap tmp = ImageUtils.getBitmapFromMat(resizedMat128);
        resizedMat128.release();
        Bitmap bmp = tmp.copy(tmp.getConfig(), true);
        ImageUtils.safelyRemoveBitmap(tmp);
        convertBitmapToByteBuffer(bmp);
        ImageUtils.safelyRemoveBitmap(bmp);

        runInference();

        return getBackground(image);
    }

    /**
     * Closes tflite to release resources.
     */
    public void close() {
        tflite.close();
        tflite = null;
        tfliteModel = null;
    }

    /**
     * Memory-map the model file in Assets.
     */
    private MappedByteBuffer loadModelFile(Activity activity)
            throws IOException {
        AssetFileDescriptor fileDescriptor =
                activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset,
                declaredLength);
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
    }


    /**
     * Get the name of the model file stored in Assets.
     *
     * @return
     */
    protected abstract String getModelPath();

    /**
     * Get the image size along the x axis.
     *
     * @return
     */
    protected abstract int getImageSizeX();

    /**
     * Get the image size along the y axis.
     *
     * @return
     */
    protected abstract int getImageSizeY();

    /**
     * Get the number of bytes that is used to store a single color channel
     * value.
     *
     * @return
     */
    protected abstract int getNumBytesPerChannel();

    /**
     * Add pixelValue to byteBuffer.
     *
     * @param pixelValue
     */
    protected abstract void addPixelValue(int pixelValue);

    protected abstract void runInference();

    protected abstract Mat getBackground(Mat bg);
}
