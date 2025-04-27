package com.example.redlinestethoai;

import android.Manifest;
import android.animation.ValueAnimator;
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
import android.widget.LinearLayout;
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
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SAMPLE_RATE = 16000;
    private static final int RECORD_TIME = 5000; // 5 seconds
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private String[] possibleResults = {"Healthy", "Upper Respiratory Tract Infection", "Bronchiectasis", "Chronic Obstructive Pulmonary Disease (COPD)", "Pneumonia", "Other"};
    private double[] resultWeights = {8.0, 1.0, 0.5, 0.3, 0.2, 0.1};
    private boolean showInstruction = true;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordingThread;
    private Button recordButton;
    private Button retryButton;
    private LinearLayout waveFormContainer;
    private View[] waveformBars = new View[100];
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
        waveFormContainer = findViewById(R.id.waveformContainer);

        // Load TensorFlow Lite model
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            Log.e(TAG, "Error loading model", e);
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }

        // Initialize handler
        handler = new Handler(Looper.getMainLooper());

        //Create wave form bars
        createWaveFormBars();
        // Set up record button click listener
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRecord();
            }

            private void handleRecord() {
                startRecording();
                setResult(null);
                setShowInstruction(false);

                // Simulate 5-second recording
                handler.postDelayed(MainActivity.this::stopRecordingAndProcess, RECORD_TIME);
            }
        });

        // Set up retry button click listener
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryRecording();
                setResult(null);
                setShowInstruction(true);
            }
        });
        
        // Initialize UI state
        updateUI();
    }
    private void createWaveFormBars() {
        for (int i = 0; i < waveformBars.length; i++) {
            View bar = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    2, LinearLayout.LayoutParams.MATCH_PARENT);
            params.setMargins(1, 0, 1, 0); // Add space between bars
            bar.setLayoutParams(params);
            bar.setBackgroundColor(getResources().getColor(R.color.orange, null));
            waveformBars[i] = bar;
            waveFormContainer.addView(bar);
        }
    }
    private void updateUI() {
        //Update instruction
        instructionTextView.setVisibility(showInstruction ? View.VISIBLE : View.GONE);

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
                        }else{
                            animateWaveform(audioBuffer);
                        }
                        //TODO: Implement waveform
                    }

                    stopRecordingAndProcess(audioBuffer);
                }
            });
            recordingThread.start();
            // Update UI
            instructionTextView.setText("Recording...");
            recordButton.setEnabled(false);
        }
    }
    
    // Animate waveform during recording
    private void animateWaveform(short[] audioBuffer) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                // Generate random waveform data for visualization
                int[] newWaveform = new int[100];
                for (int i = 0; i < 100; i++) {
                    newWaveform[i] = (int) (Math.random() * 40 + 10);
                }
                updateWaveform(newWaveform);
            }
        });
    }

    private void updateWaveform(int[] waveformData) {
        for (int i = 0; i < waveformBars.length; i++) {
            int targetHeight = waveformData[i];
            ValueAnimator animator = ValueAnimator.ofInt(waveformBars[i].getHeight(), targetHeight);
            animator.setDuration(100); // Adjust duration for smoother transition
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int animatedValue = (int) animation.getAnimatedValue();
                    waveformBars[i].getLayoutParams().height = animatedValue;
                    waveformBars[i].requestLayout();
                }
            });
            animator.start();
        }
    }

    private String getRandomResult() {
        double totalWeight = 0;
        for (double weight : resultWeights) {
            totalWeight += weight;
        }
        double random = Math.random() * totalWeight;
        double currentWeight = 0;

        for (int i = 0; i < possibleResults.length; i++) {
            currentWeight += resultWeights[i];
            if (random <= currentWeight) {
                return possibleResults[i];
            }
        }

        return "Healthy"; // fallback
    }

    private void stopRecordingAndProcess() {
        isRecording = false;

        if (audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        short[] audioBuffer = new short[SAMPLE_RATE * RECORD_TIME / 1000];
        if (audioBuffer.length > 0) {
            // Process audio and classify
            recordButton.setEnabled(false);
            instructionTextView.setText("Recording...");
            //TODO: Show Waveform
        }
    }

    private void stopRecordingAndProcess(short[] audioBuffer) {
        
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
        handler.post(new Runnable() {
            @Override
            public void run() {
                setResult(getRandomResult());
            }
        });

    }

    private void setResult(final String result) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(result);
                fadeIn(resultTextView);
                fadeOut(instructionTextView);
                retryButton.setVisibility(result != null ? View.VISIBLE : View.GONE);
                recordButton.setEnabled(result == null);
            }
        });
    }private void retryRecording() {
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
    private void setShowInstruction(boolean show) {
        showInstruction = show;
        handler.post(new Runnable() {
            @Override
            public void run() {
                instructionTextView.setVisibility(showInstruction ? View.VISIBLE : View.GONE);
            }
        });
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