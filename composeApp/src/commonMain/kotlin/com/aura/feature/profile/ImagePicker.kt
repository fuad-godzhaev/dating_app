package com.aura.feature.profile

import androidx.compose.runtime.Composable

/** Handle returned by [rememberImagePicker]; call [launch] to open the OS photo picker. */
expect class ImagePickerLauncher {
    fun launch()
}

/**
 * Platform photo picker (E: photo upload). [onImagePicked] receives the chosen image's raw
 * bytes + MIME type. Android uses the system Photo Picker; iOS is a stub for now.
 */
@Composable
expect fun rememberImagePicker(onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit): ImagePickerLauncher
