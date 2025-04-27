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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.util.fft.FFT;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SAMPLE_RATE = 16000;
    private static final int RECORD_DURATION = 5000; // 5 seconds
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private Button recordButton;
    private Button retryButton;
    private TextView resultTextView;
    private TextView instructionTextView;
    private WaveformView waveformView;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private Handler mainHandler;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        recordButton = findViewById(R.id.recordButton);
        retryButton = findViewById(R.id.retryButton);
        resultTextView = findViewById(R.id.resultTextView);
        instructionTextView = findViewById(R.id.instructionTextView);
        waveformView = findViewById(R.id.waveformView);
        mainHandler = new Handler(Looper.getMainLooper());

        // Request audio recording permission
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        // Load the TensorFlow Lite model
        try {
            tfliteModel = FileUtil.loadMappedFile(this, "model.tflite");
            tflite = new Interpreter(tfliteModel);
        } catch (IOException e) {
            Log.e(TAG, "Error loading TFLite model: " + e.getMessage());
            Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show();
        }

        // Set up record button click listener
        recordButton.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });

        // Set up retry button click listener
        retryButton.setOnClickListener(v -> {
            resetUI();
        });
    }

    private void startRecording() {
        // Initialize audio recording
        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException("AudioRecord failed to initialize");
            }

            audioRecord.startRecording();
            isRecording = true;
            runOnUiThread(() -> {
                recordButton.setText("Recording...");
                instructionTextView.setVisibility(View.GONE);
            });

            // Start recording and visualizing waveform
            new Thread(() -> {
                short[] buffer = new short[BUFFER_SIZE];
                try {
                    while (isRecording) {
                        int readSize = audioRecord.read(buffer, 0, BUFFER_SIZE);
                        if (readSize > 0) {
                            final short[] bufferCopy = Arrays.copyOf(buffer, readSize);
                            runOnUiThread(() -> waveformView.addSamples(bufferCopy));
                        }
                    }
                }
                catch (Exception e){
                    Log.e(TAG, "Error during recording: " + e.getMessage());
                    showError("Error during recording");
                }
            }).start();

            // Stop recording after 5 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isRecording) {
                    stopRecording();
                }
            }, RECORD_DURATION);

        } catch (Exception e) {
            Log.e(TAG, "Error starting recording: " + e.getMessage());
            showError("Error starting recording");
            stopRecording();
        }
    }

    private void stopRecording() {
        // Stop audio recording
        if (audioRecord != null) {
            isRecording = false;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            runOnUiThread(() -> recordButton.setText("RECORD"));

            //Process audio data
            new Thread(this::processAudio).start();
        }
    }

    private void processAudio() {
        //get waveform
        short[] audioData = waveformView.getAudioData();

        // check if no audio data
        if (audioData == null || audioData.length == 0) {
            showError("No audio data recorded");
            return;
        }

        // Extract MFCC features and run classification
        float[] mfccFeatures = extractMFCC(audioData);
        if (mfccFeatures == null) {
            showError("Error extracting MFCC features");
            return;
        }
        String result = classify(mfccFeatures);
        showResult(result);
    }

    private float[] extractMFCC(short[] audioData) {
        try {
            float[] floatBuffer = new float[audioData.length];
            for (int i = 0; i < audioData.length; i++) {
                floatBuffer[i] = (float) audioData[i] / Short.MAX_VALUE;
            }

            TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(floatBuffer,SAMPLE_RATE);
            AudioDispatcher dispatcher = new AudioDispatcher(audioStream,audioData.length,0);
            float[][] allMFCCs = new float[1][40];

            MFCC mfccProcessor = new MFCC(audioData.length, SAMPLE_RATE, 40, 50, 40, 300, 3000);
            dispatcher.addAudioProcessor(mfccProcessor);
            dispatcher.addAudioProcessor(new AudioProcessor() {
                @Override
                public boolean process(AudioEvent audioEvent) {
                    double[] mfcc = mfccProcessor.getMFCC();
                    for (int j = 0; j < mfcc.length; j++){
                        allMFCCs[0][j] = (float) mfcc[j];
                    }
                    return true;
                }

                @Override
                public void processingFinished() {
                    Log.d(TAG,"processingFinished");
                }
            });

            dispatcher.run();
            return allMFCCs[0];
        }
        catch (Exception e){
            Log.e(TAG, "Error extracting MFCC features: " + e.getMessage());
            return null;
        }

    }

    private String classify(float[] mfccFeatures) {
        // Run classification with TensorFlow Lite model
        if (tflite == null) {
            Log.e(TAG, "TensorFlow Lite model not loaded");
            return null;
        }
        try {
            float[][] input = {mfccFeatures};
            float[][] output = new float[1][2];

            tflite.run(input, output);

            //get result
            float normalProb = output[0][0];
            float sickProb = output[0][1];

            if (normalProb > sickProb) {
                return "Normal";
            } else {
                return "Sick";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during classification: " + e.getMessage());
            return null;
        }
    }

    private void showResult(String result) {
        // Display the classification result with fade animation
        if (result != null) {
            runOnUiThread(() -> {
                resultTextView.setText(result);
                resultTextView.setVisibility(View.VISIBLE);
                AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                fadeIn.setDuration(200);
                resultTextView.startAnimation(fadeIn);
                retryButton.setVisibility(View.VISIBLE);

            });
        } else {
            showError("Error during classification");
        }

    }
    private void showError(String message){
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        resetUI();
    }
    private void resetUI() {
        // Reset UI elements to initial state
        runOnUiThread(() -> {
            waveformView.clear();
            resultTextView.setVisibility(View.INVISIBLE);
            instructionTextView.setVisibility(View.VISIBLE);
            retryButton.setVisibility(View.INVISIBLE);
            recordButton.setText("RECORD");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the TensorFlow Lite model
        if (tflite != null) {
            tflite.close();
        }
    }
}