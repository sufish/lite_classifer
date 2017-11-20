package com.example.classifier;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by fuqiang on 2017/11/20.
 */

public class Classifier {
    public static int INPUT_SIZE = 224;
    private String labelFileName;
    private String modelFileName;
    private AssetManager assetManager;
    private Interpreter interpreter;
    private List<String> labels = new ArrayList<>();

    public Classifier(String labelFileName, String modelFileName, AssetManager assetManager) {
        this.labelFileName = labelFileName;
        this.modelFileName = modelFileName;
        this.assetManager = assetManager;
    }

    public void load() throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        MappedByteBuffer mbb = fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
        interpreter = new Interpreter(mbb);

        InputStream labelsInput = assetManager.open(labelFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            labels.add(line);
        }
    }

    public List<Result> classify(Bitmap bitmap) {
        Bitmap bitmapInput = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmapInput.getPixels(pixels, 0, bitmapInput.getWidth(), 0, 0, bitmapInput.getWidth(), bitmapInput.getHeight());

        ByteBuffer input = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3);
        input.order(ByteOrder.nativeOrder());

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = pixels[pixel++];
                input.put((byte) ((val >> 16) & 0xFF));
                input.put((byte) ((val >> 8) & 0xFF));
                input.put((byte) (val & 0xFF));
            }
        }
        byte[][] output = new byte[1][labels.size()];
        interpreter.run(input, output);
        bitmapInput.recycle();

        final List<Result> results = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            results.add(new Result(labels.get(i), (output[0][i] & 0xFF) / 255.0f));
        }
        Collections.sort(results, new Comparator<Result>() {
            @Override
            public int compare(Result t1, Result t2) {
                return Float.valueOf(t2.getScore()).compareTo(Float.valueOf(t1.getScore()));
            }
        });
        return results;
    }
}
