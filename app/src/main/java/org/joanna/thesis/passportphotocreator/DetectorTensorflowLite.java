package org.joanna.thesis.passportphotocreator;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public abstract class DetectorTensorflowLite {

    public static final int COLOR_CHANNELS_NO = 3;

    /** Options for configuring the Interpreter. */
    private final Interpreter.Options mTfliteOptions =
            new Interpreter.Options();
    /** An instance of the driver class to run model inference with Tfl Lite. */
    protected     Interpreter         mTflite;
    /** A ByteBuffer to hold image data, to be feed into Tfl Lite as inputs. */
    protected     ByteBuffer          mImgData;
    /** Preallocated buffers for storing image data in. */
    private       int[]               mIntValues;
    /** The loaded TensorFlow Lite model. */
    private       MappedByteBuffer    mTfliteModel;

    protected DetectorTensorflowLite(final Activity activity)
            throws IOException {
        mTfliteModel = loadModelFile(activity);
        mTflite = new Interpreter(mTfliteModel, mTfliteOptions);
        mIntValues = new int[getImageSizeX() * getImageSizeY()];
        mImgData = ByteBuffer.allocateDirect(
                getImageSizeX() * getImageSizeY()
                        * COLOR_CHANNELS_NO
                        * getNumBytesPerChannel());
        mImgData.order(ByteOrder.nativeOrder());
    }

    /**
     * Memory-map the model file in Assets.
     */
    protected MappedByteBuffer loadModelFile(Activity activity)
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
    protected void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (mImgData == null) {
            return;
        }
        mImgData.rewind();
        bitmap.getPixels(mIntValues, 0, bitmap.getWidth(), 0, 0,
                bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < getImageSizeX(); ++i) {
            for (int j = 0; j < getImageSizeY(); ++j) {
                final int val = mIntValues[pixel++];
                addPixelValue(val);
            }
        }
    }

    /**
     * Closes tflite to release resources.
     */
    public void close() {
        mTflite.close();
        mTflite = null;
        mTfliteModel = null;
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

    /**
     * Executes call to the network.
     */
    protected abstract void runInference();

}
