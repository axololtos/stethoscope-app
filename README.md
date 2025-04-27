# RedLine StethoAI

**Summary**

RedLine StethoAI is an Android application designed to analyze respiratory sounds in real-time. Utilizing a 3.5mm microphone, the app records 5-second audio samples, processes them, and classifies them as either "Normal" or "Sick." The core of the app's functionality lies in a pre-trained TensorFlow Lite model, trained on the ICBHI 2017 dataset, which achieves a classification accuracy of 73.3%. This app provides a lightweight and user-friendly interface for preliminary respiratory health assessments.

**Features**

*   **Real-time Respiratory Sound Analysis:** Records audio via a 3.5mm microphone.
*   **AI-Powered Classification:** Employs a pre-trained TensorFlow Lite model (`model.tflite`) for audio classification.
*   **High Accuracy:** The AI model boasts a 73.3% accuracy, leveraging data from the ICBHI 2017 dataset.
*   **User-Friendly Interface:**
    *   **Header:** Displays "RedLine StethoAI" in bold Calibri font (24pt, white on orange).
    *   **Record Button:** A large, centered circular button (orange with white icon) with an animated press effect.
    *   **Instructions:** Clear guidance text "Place Digital Stethoscope around Trachea" (18pt, fade-in animation).
    *   **Waveform Visualization:** Visual feedback of the recording process.
    *   **Result Display:** Shows "Normal" or "Sick" with a smooth fade-in/out transition.
    *   **Retry Button:** A green button (animated hover) to re-record.
*   **Lightweight Design:** The app is designed to be less than 100 MB in size.
*   **Android Compatibility:** Supports Android 8.0 (Oreo) and later versions.
*   **Smooth Transitions:** All UI transitions are designed to be seamless, with a duration of 200ms.
*   **Material Design:** Utilizes Material Design principles, including rounded corners and subtle shadows.
* **MFCC Features:** Extratcts MFCC features using Librosa.
* **16000 Hz Audio:** Records 5 seconds of audio at 16000 Hz.

**Technical Specifications**

*   **TensorFlow Lite Model:** `model.tflite` (pre-trained on ICBHI 2017 dataset).
*   **Audio Input:** 3.5mm microphone.
*   **Recording Duration:** 5 seconds.
*   **Sampling Rate:** 16000 Hz.
*   **Audio Feature Extraction:** MFCC using Librosa
*   **Classification Output:** "Normal" or "Sick."
*   **UI Fonts:** Calibri for header text.
*   **UI Colors:** Orange and white, Green.
*   **Animation Duration:** 200ms.
* **Minimum Android API:** 26.

**Installation**

1.  Download the latest APK from [link to be added].
2.  Install the APK on your Android device (ensure "Install from unknown sources" is enabled in your device settings).

**Usage**

1.  Connect a 3.5mm microphone to your Android device.
2.  Launch the RedLine StethoAI app.
3.  Follow the on-screen instructions: "Place Digital Stethoscope around Trachea."
4.  Press the "RECORD" button.
5.  Wait for 5 seconds while the app records and displays the waveform.
6.  The app will display either "Normal" or "Sick."
7.  Press "Retry" to re-record.

**Dependencies**

*   TensorFlow Lite
*   Librosa
*   Android SDK

**Screenshots**

[Add screenshots here]

**Future Enhancements**

*   Enhanced model training for increased accuracy.
*   Multi-language support.
*   Cloud synchronization.

**Contact**

[Your Name/Organization]

[Your Email/Website]