package com.example.redlinestethoai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SAMPLE_RATE = 16000;
    private static final int RECORD_TIME = 5000; // 5 seconds
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private Button recordButton;
    private Button retryButton;
    private TextView resultTextView;
    private TextView instructionTextView;
    private ConstraintLayout waveFormLayout;
    private Interpreter tflite;
    private Handler handler;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request audio recording permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // Initialize UI elements
        recordButton = findViewById(R.id.recordButton);
        retryButton = findViewById(R.id.retryButton);
        resultTextView = findViewById(R.id.resultTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        waveFormLayout = findViewById(R.id.waveformLayout);

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }

        // Initialize handler
        handler = new Handler(Looper.getMainLooper());
        // Set up record button click listener
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });

        // Set up retry button click listener
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryRecording();
            }
        });
        retryButton.setVisibility(View.GONE);
        // Fade in animation for instruction text
        fadeIn(instructionTextView);
    }

    private void startRecording() {
        if (isRecording) return;

        if (audioRecord == null) {
            audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        }

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            isRecording = true;
            audioRecord.startRecording();

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    short[] audioBuffer = new short[SAMPLE_RATE * RECORD_TIME / 1000];
                    int read = 0;
                    long startTime = System.currentTimeMillis();

                    while (isRecording && System.currentTimeMillis() - startTime < RECORD_TIME) {
                        read = audioRecord.read(audioBuffer, 0, audioBuffer.length);

                        if (read < 0) {
                            Log.e(TAG, "Error reading from audio record");
                            break;
                        }
                        //TODO: Implement waveform
                    }

                    stopRecordingAndProcess(audioBuffer);
                }
            });
            recordingThread.start();
            recordButton.setEnabled(false);
            instructionTextView.setText("Recording...");
            //TODO: Show Waveform
        }
    }

    private void stopRecordingAndProcess(short[] audioBuffer) {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }

        if (audioBuffer.length > 0) {
            // Convert short array to float array
            float[] floatAudioBuffer = new float[audioBuffer.length];
            for (int i = 0; i < audioBuffer.length; i++) {
                floatAudioBuffer[i] = audioBuffer[i] / 32768.0f; // Convert to -1.0 to +1.0
            }

            // Process audio and classify
            classifyAudio(floatAudioBuffer);
        } else {
            showError("No audio recorded");
        }
    }

    private void classifyAudio(float[] audioData) {
        // Extract MFCC features (placeholder, requires Librosa integration)
        float[][] mfccFeatures = extractMFCC(audioData);

        // Classify using TensorFlow Lite model
        if (tflite != null) {
            float[][] input = new float[1][mfccFeatures.length]; // Expecting input of shape (1, num_mfcc_features)
            input[0] = mfccFeatures[0];
            TensorBuffer inputFeature = TensorBuffer.createFixedSize(new int[]{1, input[0].length}, DataType.FLOAT32);
            inputFeature.loadArray(input);
            TensorBuffer outputFeature = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
            tflite.run(inputFeature.getBuffer(), outputFeature.getBuffer());

            float[] result = outputFeature.getFloatArray();
            // Display the classification result
            displayResult(result);
        } else {
            showError("Model not loaded");
        }
    }

    private float[][] extractMFCC(float[] audioData) {
        // Placeholder for MFCC extraction
        // This should be replaced with actual Librosa MFCC extraction
        //For this example, return dummy features.
        float[][] dummyMfcc = new float[1][120];
        for (int i = 0; i < 120; i++) {
            dummyMfcc[0][i] = 0.5f;
        }
        return dummyMfcc;
    }

    private void displayResult(float[] result) {
        // Find the index of the maximum value
        int maxIndex = 0;
        for (int i = 1; i < result.length; i++) {
            if (result[i] > result[maxIndex]) {
                maxIndex = i;
            }
        }

        // Determine the diagnosis based on the index of the maximum value
        String diagnosis = (maxIndex == 0) ? "Normal" : "Sick";

        handler.post(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(diagnosis);
                fadeIn(resultTextView);
                fadeOut(instructionTextView);
                retryButton.setVisibility(View.VISIBLE);
                recordButton.setEnabled(true);

            }
        });

    }

    private void retryRecording() {
        resultTextView.setText("");
        fadeOut(resultTextView);
        fadeIn(instructionTextView);
        instructionTextView.setText("Place Digital Stethoscope around Trachea");
        retryButton.setVisibility(View.GONE);
        recordButton.setEnabled(true);
    }

    private void showError(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        // Load the model from the assets folder
        FileInputStream fileInputStream = new FileInputStream("model.tflite"); //Replace this with the asset folder file
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Helper methods for animations
    private void fadeIn(final View view) {
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(200);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeIn);
    }

    private void fadeOut(final View view) {
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(200);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeOut);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tflite != null) {
            tflite.close();
        }
        if(audioRecord != null){
            audioRecord.release();
            audioRecord = null;
        }
    }
}