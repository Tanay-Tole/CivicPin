package com.spot.civicpin.ai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CivicIssueClassifier {

    private static final String MODEL_FILE = "model_unquant.tflite";
    private static final String LABELS_FILE = "labels.txt";

    private final Interpreter interpreter;
    private final List<String> labels;

    private int inputImageWidth = 224;
    private int inputImageHeight = 224;
    private int numChannels = 3;

    public CivicIssueClassifier(Context context) throws IOException {
        MappedByteBuffer modelBuffer = loadModelFile(context, MODEL_FILE);
        Interpreter.Options options = new Interpreter.Options();
        this.interpreter = new Interpreter(modelBuffer, options);

        this.labels = loadLabels(context, LABELS_FILE);

        // Inspect input tensor shape
        int[] inputShape = interpreter.getInputTensor(0).shape();
        if (inputShape.length >= 4) {
            inputImageHeight = inputShape[1];
            inputImageWidth = inputShape[2];
            numChannels = inputShape[3];
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelFilename) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels(Context context, String labelsFilename) throws IOException {
        List<String> labelList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open(labelsFilename)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    // Extract label without leading numbers e.g. "0 Garbage" -> "Garbage"
                    int spaceIndex = line.indexOf(' ');
                    if (spaceIndex != -1) {
                        labelList.add(line.substring(spaceIndex + 1));
                    } else {
                        labelList.add(line);
                    }
                }
            }
        }
        return labelList;
    }

    public ClassificationResult classifyImage(Bitmap bitmap) {
        if (bitmap == null) {
            return new ClassificationResult("Others", getDisplayNameForLabel("Others"), 0.0f, new HashMap<>());
        }
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true);

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight * numChannels);
        inputBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[inputImageWidth * inputImageHeight];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixelIndex = 0;
        for (int i = 0; i < inputImageWidth; ++i) {
            for (int j = 0; j < inputImageHeight; ++j) {
                int val = intValues[pixelIndex++];
                // Normalize floating point model values to [0, 1] or [-1, 1]
                float r = ((val >> 16) & 0xFF) / 255.0f;
                float g = ((val >> 8) & 0xFF) / 255.0f;
                float b = (val & 0xFF) / 255.0f;

                inputBuffer.putFloat(r);
                inputBuffer.putFloat(g);
                inputBuffer.putFloat(b);
            }
        }

        int numLabels = labels.size();
        float[][] outputArray = new float[1][numLabels];

        interpreter.run(inputBuffer, outputArray);

        int maxIndex = -1;
        float maxConfidence = -1f;
        Map<String, Float> probabilities = new HashMap<>();

        for (int i = 0; i < numLabels; i++) {
            float conf = outputArray[0][i];
            String label = labels.get(i);
            probabilities.put(label, conf);
            if (conf > maxConfidence) {
                maxConfidence = conf;
                maxIndex = i;
            }
        }

        String topLabel = maxIndex != -1 ? labels.get(maxIndex) : "Others";
        String displayName = getDisplayNameForLabel(topLabel);

        return new ClassificationResult(topLabel, displayName, maxConfidence, probabilities);
    }

    public static String getDisplayNameForLabel(String internalLabel) {
        if (internalLabel == null) return "Other / No Supported Issue";
        switch (internalLabel) {
            case "Garbage":
                return "Garbage Accumulation";
            case "Water_Leak_Sewer":
                return "Water Leakage / Sewage";
            case "Potholes":
                return "Potholes / Road Damage";
            case "Others":
            default:
                return "Other / No Supported Issue";
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
        }
    }
}
