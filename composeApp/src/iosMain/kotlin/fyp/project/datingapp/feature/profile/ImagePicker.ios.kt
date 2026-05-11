package fyp.project.datingapp.feature.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// iOS photo picking is stubbed (no-op) until the iOS app leaves the lazy state;
// the rest of the upload path (PhotoUploader, blob store) is multiplatform.
actual class ImagePickerLauncher {
    actual fun launch() {}
}

@Composable
actual fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): ImagePickerLauncher =
    remember { ImagePickerLauncher() }
