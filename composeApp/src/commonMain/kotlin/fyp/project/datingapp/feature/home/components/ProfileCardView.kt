package fyp.project.datingapp.feature.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import fyp.project.datingapp.feature.home.HomeStore
import fyp.project.datingapp.records.UserProfile

/**
 * Visual-only profile card in the swipe stack. Mirrors the prototype's
 * `ProfileCardView`:
 *
 *  - Background: the current [HomeStore.State.PictureState] rendered as
 *    a progress indicator (Loading) or a coloured placeholder Box
 *    (Loaded). Real picture fetching lands with the peer feed in G.2;
 *    until then every Loaded state renders a deterministic placeholder
 *    Box so swipe / layout can be visually verified.
 *  - Gradient overlay from transparent to black for legibility of the
 *    name/age text at the bottom.
 *  - Top strip showing `pictures.size` segments (current highlighted).
 *  - Left/right tap zones cycle the visible picture.
 *  - Bottom row: name + age + info icon.
 */
@Composable
fun ProfileCardView(
    profile: UserProfile,
    pictures: List<HomeStore.State.PictureState>,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
) {
    var currentIndex by remember { mutableStateOf(0) }
    // Guard against out-of-range if a picture gets dropped mid-swipe.
    val safeIndex = currentIndex.coerceIn(0, (pictures.size - 1).coerceAtLeast(0))

    val darkFade = remember {
        Brush.verticalGradient(
            colorStops = arrayOf(
                .68f to Color.Transparent,
                .92f to Color.Black,
            ),
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(.6f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Picture slot.
            if (pictures.isEmpty()) {
                PlaceholderPicture(ref = "no-picture")
            } else when (val picture = pictures[safeIndex]) {
                is HomeStore.State.PictureState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is HomeStore.State.PictureState.Loaded -> BlobImage(filePath = picture.filePath)
            }

            // Dark vertical fade for text legibility.
            Box(Modifier.matchParentSize().background(darkFade))

            Box(contentModifier.fillMaxSize()) {
                // Upper picture-index strip (only when there are pictures).
                if (pictures.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(6.dp)) {
                        repeat(pictures.size) { index ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .padding(horizontal = 4.dp)
                                    .alpha(if (index == safeIndex) 1f else .5f)
                                    .background(if (index == safeIndex) Color.White else Color.LightGray),
                            )
                        }
                    }
                }

                // Clickable left / right halves to cycle pictures.
                Row(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable { if (currentIndex > 0) currentIndex-- },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clickable { if (currentIndex < pictures.size - 1) currentIndex++ },
                    )
                }

                // Name / age / info bottom row.
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = profile.displayName,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 30.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = profile.age.toString(),
                        color = Color.White,
                        fontSize = 28.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/**
 * Deterministic colour placeholder in lieu of a real image. Picks one of
 * eight pastel fills from the hash of the blob reference so adjacent
 * cards don't collide. Replaced in Phase G.2 with real blob fetching.
 *
 * Also exposed as [PlaceholderBackdrop] for reuse by [NewMatchView],
 * which needs the same placeholder treatment at full-bleed dialog size.
 */
/**
 * Renders a fetched profile photo from its on-disk [filePath] (Part 3) with Coil 3,
 * which handles async decode + memory/disk caching off the main thread. The card
 * already shows a progress indicator during the preceding Loading state, so a brief
 * blank here is acceptable while Coil decodes the (already-local) file.
 */
@Composable
private fun BlobImage(filePath: String) {
    AsyncImage(
        model = "file://$filePath",
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun PlaceholderPicture(ref: String) = PlaceholderBackdrop(ref)

@Composable
internal fun PlaceholderBackdrop(ref: String) {
    val palette = remember {
        listOf(
            Color(0xffffc1b6), Color(0xffc8f7c5), Color(0xffc5d1f7),
            Color(0xfff7e6c5), Color(0xffe6c5f7), Color(0xffc5f7f5),
            Color(0xfff7c5e6), Color(0xffd8f7c5),
        )
    }
    val colour = palette[(ref.hashCode() and 0x7fffffff) % palette.size]
    Box(Modifier.fillMaxSize().background(colour))
}
