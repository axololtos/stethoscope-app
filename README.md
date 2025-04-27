# RedLine StethoAI

## Project Summary

RedLine StethoAI is an Android application designed to function as an AI-powered digital stethoscope. This app leverages the capabilities of a pre-trained TensorFlow Lite model to analyze respiratory sounds captured via a 3.5mm microphone. The primary goal is to determine if the recorded sounds indicate a "Normal" or "Sick" condition.

## Features

-   **AI-Powered Analysis:** Utilizes a pre-trained TensorFlow Lite model, trained on the ICBHI 2017 dataset with 73.3% accuracy, to classify respiratory sounds.
-   **Audio Recording:** Records 5 seconds of audio at a 16000 Hz sampling rate through a 3.5mm microphone.
-   **User-Friendly Interface:**
    -   Header: Displays "RedLine StethoAI" in bold Calibri font (24pt), with white text on an orange background.
    -   Record Button: A centered, circular "RECORD" button (orange with a white icon), featuring an animated press effect.
    -   Instructions: Clear instructions ("Place Digital Stethoscope around Trachea") displayed in 18pt font with a fade-in animation.
    -   Waveform Visualization: Provides a sample waveform display to visualize recording progress.
    -   Result Display: Presents the analysis result ("Normal" or "Sick") with a fade-in/out transition effect.
    -   Retry Button: A green "Retry" button with an animated hover effect for re-recording.
-   **MFCC Feature Extraction:** Employs Librosa for extracting Mel-Frequency Cepstral Coefficients (MFCCs) from the recorded audio.
-   **Lightweight Design:** The app is designed to be lightweight, with a target size under 100 MB.
-   **Smooth Transitions:** Incorporates smooth transitions (200ms duration) between different states (e.g., recording to result display).
-   **Material Design:** Implements Material Design guidelines, featuring rounded corners and subtle shadows.
-   **Android Compatibility:** Compatible with Android 8.0 (Oreo) and later versions.

## Functionality

1.  **Recording:** When the "RECORD" button is pressed, the app starts recording audio for 5 seconds at 16000 Hz.
2.  **Visualization:** During recording, an animated waveform is displayed to provide real-time feedback.
3.  **Feature Extraction:** Once recording is complete, MFCC features are extracted from the audio data using Librosa.
4.  **Classification:** The extracted features are fed into the pre-trained TensorFlow Lite model for classification.
5.  **Result Display:** The app displays the result of the classification, either "Normal" or "Sick," with a smooth transition effect.
6.  **Re-recording:** Users can press the "Retry" button to discard the current recording and start a new one.

## Technical Details

-   **TensorFlow Lite Model:** The core of the app relies on a pre-trained TensorFlow Lite model for audio classification.
-   **Librosa:** Used for extracting MFCC features, which are crucial for the model's accuracy.
-   **MediaRecorder:** Used for audio recording.
-   **Material Design:** The app follows Material Design principles to ensure a modern and consistent UI.
-   **Android SDK:** The app targets Android 8.0 (API level 26) and above.

## Future Enhancements

-   **Real-time Waveform:** Implement a more advanced real-time waveform visualization.
-   **Advanced Classification:** Investigate the possibility of adding more classification labels.
- **Accuracy improvements:** Investigate improving the accuracy of the model, either by training with more data, or by using a different algorithm.
-   **Data Analytics:** Add the ability to collect and view data from previous uses.
- **User data:** Investigate the possibility of storing user data.

## Installation

*Note:* The model is not provided in this repository, since it is a dummy one.

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on an Android device or emulator with a microphone.
4. To include the model, the `model.tflite` needs to be added in the assets folder.