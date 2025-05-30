package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeImage(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
    placeholderType: PlaceholderType = PlaceholderType.Large,
    aspectRatio: Float = 1f,
    useAspectRatio: Boolean = true,
    corners: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current

    val imageRequest = remember(episode.uuid, useEpisodeArtwork) {
        PocketCastsImageRequestFactory(context, cornerRadius = corners?.value?.toInt() ?: 0, placeholderType = placeholderType).themed().create(episode, useEpisodeArtwork)
    }

    CoilImage(
        imageRequest = imageRequest,
        title = stringResource(id = LR.string.episode_cover_description, episode.title),
        showTitle = false,
        contentScale = contentScale,
        modifier = modifier
            .then(if (useAspectRatio) Modifier.aspectRatio(aspectRatio) else Modifier),
    )
}
