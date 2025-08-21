# ML-Based Plant Disease Detection Android App

A modern Android application built in Kotlin for detecting plant diseases using machine learning. The app provides a clean, professional interface for capturing or uploading leaf images and analyzing them for disease detection.

## Features

### Current Implementation
- **Welcome Page**: Clean introduction screen with app branding
- **Image Capture**: Take photos using device camera
- **Gallery Upload**: Select images from device gallery
- **Disease Analysis**: Display disease information with placeholder values
- **Runtime Permissions**: Proper handling of camera and storage permissions
- **Modern UI**: Material Design 3 with green plant-themed color scheme

### Future ML Integration
- **GLCM Feature Extraction**: Gray-Level Co-occurrence Matrix texture analysis
- **Decision Tree Classifier**: Machine learning model for disease classification
- **Real-time Analysis**: Instant disease detection from captured images

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/plantdiseasedetection/
│   │   ├── MainActivity.kt              # Welcome page
│   │   ├── DetectionActivity.kt         # Main detection interface
│   │   └── MLModelHelper.kt             # ML model integration utility
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml        # Welcome page layout
│   │   │   └── activity_detection.xml   # Detection page layout
│   │   ├── drawable/                    # UI graphics and placeholders
│   │   ├── values/
│   │   │   ├── colors.xml               # Color scheme
│   │   │   ├── strings.xml              # String resources
│   │   │   └── themes.xml               # App theme
│   │   └── xml/
│   │       └── file_paths.xml           # File provider paths
│   └── AndroidManifest.xml              # App permissions and activities
```

## Technical Details

### Architecture
- **Traditional Android Views**: XML layouts with Kotlin activities
- **Material Design 3**: Modern UI components and theming
- **Activity-based Navigation**: Simple navigation between screens
- **Permission Handling**: Runtime permissions for Android 6.0+

### Key Components

#### MainActivity
- Welcome screen with app introduction
- Navigation to detection page
- Clean, professional design

#### DetectionActivity
- Image capture and gallery selection
- Real-time image display
- Disease analysis results
- Permission management

#### MLModelHelper
- **Clean ML Integration Interface**: Designed for easy model replacement
- **Image Preprocessing**: Resize and normalize images for ML input
- **GLCM Feature Extraction**: Texture analysis preparation
- **Model Prediction**: Decision tree classification interface
- **Error Handling**: Robust error management and callbacks

### ML Integration Points

The app is designed for seamless ML model integration:

1. **Image Preprocessing** (`MLModelHelper.preprocessImage()`)
   - Resize to 224x224 pixels
   - Normalize pixel values
   - Convert to appropriate format

2. **Feature Extraction** (`MLModelHelper.extractGLCMFeatures()`)
   - GLCM matrix calculation
   - Texture feature extraction
   - Feature vector generation

3. **Model Prediction** (`MLModelHelper.runModelPrediction()`)
   - Decision tree classification
   - Disease mapping
   - Confidence scoring

## Setup and Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (API level 24)
- Kotlin 1.8+

### Building the App
1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and run on device/emulator

### Permissions Required
- `CAMERA`: For capturing leaf images
- `READ_EXTERNAL_STORAGE`: For accessing gallery images (Android < 13)
- `READ_MEDIA_IMAGES`: For accessing gallery images (Android 13+)

## ML Model Integration Guide

### Current State
The app currently uses placeholder disease detection results. To integrate your trained ML model:

1. **Replace Placeholder Methods**:
   - `MLModelHelper.preprocessImage()`: Implement actual image preprocessing
   - `MLModelHelper.extractGLCMFeatures()`: Implement GLCM feature extraction
   - `MLModelHelper.runModelPrediction()`: Integrate your Decision Tree model

2. **Model File Integration**:
   - Add your trained model file to `app/src/main/assets/`
   - Update `MLModelHelper.initializeModel()` to load the model
   - Implement model inference in `runModelPrediction()`

3. **Feature Extraction**:
   - Implement GLCM matrix calculation
   - Extract texture features (contrast, homogeneity, energy, correlation)
   - Ensure feature vector matches your model's input requirements

### Example Integration Points

```kotlin
// In MLModelHelper.kt
private fun extractGLCMFeatures(bitmap: Bitmap): FloatArray {
    // TODO: Implement actual GLCM feature extraction
    // 1. Convert to grayscale
    // 2. Calculate GLCM matrices for different angles
    // 3. Extract texture features
    // 4. Return feature vector
}

private fun runModelPrediction(features: FloatArray): DetectionResult {
    // TODO: Implement actual model prediction
    // 1. Load your trained Decision Tree model
    // 2. Pass GLCM features to the model
    // 3. Get prediction and confidence
    // 4. Map to disease information
}
```

## UI/UX Design

### Design Principles
- **Clean and Minimal**: Professional appearance without clutter
- **Plant-themed Colors**: Green color scheme appropriate for plant health
- **Intuitive Navigation**: Clear user flow from welcome to detection
- **Responsive Layout**: Works on various screen sizes

### Color Scheme
- **Primary**: Green (#4CAF50) - Represents healthy plants
- **Secondary**: Orange (#FF9800) - For action buttons
- **Background**: Light gray (#F5F5F5) - Clean, neutral background
- **Text**: Dark gray (#212121) - High contrast for readability

## Future Enhancements

### Planned Features
- **Multiple Disease Support**: Expand beyond current placeholder diseases
- **Treatment Recommendations**: Detailed treatment plans for each disease
- **History Tracking**: Save and review previous detections
- **Offline Mode**: Local ML model for offline detection
- **Image Enhancement**: Auto-crop and enhance leaf images

### Technical Improvements
- **MVVM Architecture**: Implement ViewModel and LiveData
- **Room Database**: Local storage for detection history
- **Coroutines**: Asynchronous image processing
- **Unit Testing**: Comprehensive test coverage

## Contributing

1. Fork the repository
2. Create a feature branch
3. Implement your changes
4. Add appropriate tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions or support regarding ML model integration, please refer to the integration guide above or create an issue in the repository. 