package fyp.project.datingapp.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

actual class ImagePickerLauncher internal constructor(private val onLaunch: () -> Unit) {
    actual fun launch() = onLaunch()
}

@Composable
actual fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull() ?: return@launch
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            onImagePicked(bytes, mime)
        }
    }
    return remember {
        ImagePickerLauncher {
            launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
