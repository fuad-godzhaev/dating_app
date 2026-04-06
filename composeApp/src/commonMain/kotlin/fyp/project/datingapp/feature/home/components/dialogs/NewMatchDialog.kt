package fyp.project.datingapp.feature.home.components.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fyp.project.datingapp.feature.home.HomeStore

/**
 * Full-screen "It's a match!" dialog. Thin wrapper around [NewMatchView]
 * that disables the platform's default dialog width constraint so the
 * match view can truly fill the screen.
 */
@Composable
fun NewMatchDialog(
    pictureStates: List<HomeStore.State.PictureState>,
    onSendMessage: (String) -> Unit,
    onCloseClicked: () -> Unit,
) {
    Dialog(
        onDismissRequest = onCloseClicked,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        NewMatchView(
            pictureStates = pictureStates,
            onSendMessage = onSendMessage,
            onCloseClicked = onCloseClicked,
        )
    }
}
