package au.com.shiftyjelly.pocketcasts.compose.bookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonStyle
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

sealed class BookmarkRowColors {
    @Composable
    abstract fun dividerColor(): Color

    @Composable
    abstract fun backgroundColor(
        isMultiSelecting: () -> Boolean,
        isSelected: Boolean,
    ): Color

    @Composable
    abstract fun primaryTextColor(): Color

    @Composable
    abstract fun secondaryTextColor(): Color

    object Player : BookmarkRowColors() {
        @Composable
        override fun dividerColor(): Color = MaterialTheme.theme.colors.playerContrast05

        @Composable
        override fun backgroundColor(
            isMultiSelecting: () -> Boolean,
            isSelected: Boolean,
        ): Color {
            return if (isMultiSelecting() && isSelected) {
                MaterialTheme.theme.colors.primaryUi02Selected
            } else {
                Color.Transparent
            }
        }

        @Composable
        override fun primaryTextColor() = MaterialTheme.theme.colors.playerContrast01

        @Composable
        override fun secondaryTextColor() = MaterialTheme.theme.colors.playerContrast02
    }

    object Default : BookmarkRowColors() {
        @Composable
        override fun dividerColor(): Color = MaterialTheme.theme.colors.primaryUi05

        @Composable
        override fun backgroundColor(
            isMultiSelecting: () -> Boolean,
            isSelected: Boolean,
        ): Color {
            return if (isMultiSelecting() && isSelected) {
                MaterialTheme.theme.colors.primaryUi02Selected
            } else {
                MaterialTheme.theme.colors.primaryUi02
            }
        }

        @Composable
        override fun primaryTextColor() = MaterialTheme.theme.colors.primaryText01

        @Composable
        override fun secondaryTextColor() = MaterialTheme.theme.colors.primaryText02
    }
}

@Composable
fun BookmarkRow(
    bookmark: Bookmark,
    episode: BaseEpisode?,
    isMultiSelecting: () -> Boolean,
    isSelected: (Bookmark) -> Boolean,
    onPlayClick: (Bookmark) -> Unit,
    modifier: Modifier = Modifier,
    colors: BookmarkRowColors,
    timePlayButtonStyle: TimePlayButtonStyle,
    timePlayButtonColors: TimePlayButtonColors,
    showIcon: Boolean,
    useEpisodeArtwork: Boolean,
    isDarkTheme: Boolean,
    showEpisodeTitle: Boolean = false,
) {
    Column(
        modifier = modifier,
    ) {
        Divider(
            color = colors.dividerColor(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colors.backgroundColor(
                        isMultiSelecting = isMultiSelecting,
                        isSelected = isSelected(bookmark),
                    ),
                ),
        ) {
            val createdAtText = bookmark.createdAt
                .toLocalizedFormatPattern(bookmark.createdAtDatePattern())

            if (isMultiSelecting()) {
                Checkbox(
                    checked = isSelected(bookmark),
                    onCheckedChange = null,
                    modifier = Modifier
                        .padding(start = 16.dp),
                )
            }

            if (showIcon) {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    if (episode != null) {
                        EpisodeImage(
                            episode = episode,
                            corners = 8.dp,
                            useEpisodeArtwork = useEpisodeArtwork,
                            modifier = modifier.size(56.dp),
                        )
                    } else {
                        Image(
                            painter = painterResource(if (isDarkTheme) IR.drawable.defaultartwork_dark else IR.drawable.defaultartwork),
                            contentDescription = bookmark.title,
                            modifier = modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            ) {
                val shouldShowEpisodeTitle = showEpisodeTitle && bookmark.episodeTitle.isNotEmpty()
                if (shouldShowEpisodeTitle) {
                    TextH70(
                        text = bookmark.episodeTitle,
                        color = colors.secondaryTextColor(),
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(
                    modifier = Modifier.padding(
                        top = if (shouldShowEpisodeTitle) 4.dp else 16.dp,
                    ),
                )

                TextH40(
                    text = bookmark.title,
                    color = colors.primaryTextColor(),
                    maxLines = 2,
                    lineHeight = 18.sp,
                )

                TextH70(
                    text = createdAtText,
                    color = colors.secondaryTextColor(),
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(
                    modifier = Modifier.padding(
                        bottom = if (shouldShowEpisodeTitle) 8.dp else 16.dp,
                    ),
                )
            }

            Box(modifier = Modifier.padding(end = 16.dp)) {
                TimePlayButton(
                    timeSecs = bookmark.timeSecs,
                    contentDescriptionId = LR.string.bookmark_play,
                    onClick = { onPlayClick(bookmark) },
                    buttonStyle = timePlayButtonStyle,
                    colors = timePlayButtonColors,
                )
            }
        }
    }
}

@ShowkaseComposable(name = "BookmarkRow", group = "Bookmark", styleName = "Default - Light")
@Preview(name = "Light")
@Composable
fun BookmarkRowLightPreview() {
    BookmarkRowNormalPreview(Theme.ThemeType.LIGHT)
}

@ShowkaseComposable(name = "BookmarkRow", group = "Bookmark", styleName = "Default - Dark")
@Preview(name = "Dark")
@Composable
fun BookmarkRowDarkPreview() {
    BookmarkRowNormalPreview(Theme.ThemeType.DARK)
}

@ShowkaseComposable(name = "BookmarkRow", group = "Bookmark", styleName = "Default - Rose")
@Preview(name = "Rose")
@Composable
fun BookmarkRowRosePreview() {
    BookmarkRowNormalPreview(Theme.ThemeType.ROSE)
}

@Composable
private fun BookmarkRowNormalPreview(themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        BookmarkRow(
            bookmark = Bookmark(
                uuid = "",
                podcastUuid = "",
                episodeTitle = "Episode Title",
                timeSecs = 10,
                title = "Bookmark Title",
                createdAt = Date(),
            ),
            episode = PodcastEpisode(
                uuid = "",
                publishedDate = Date(),
            ),
            isMultiSelecting = { false },
            isSelected = { false },
            onPlayClick = {},
            modifier = Modifier,
            colors = BookmarkRowColors.Default,
            timePlayButtonStyle = TimePlayButtonStyle.Outlined,
            timePlayButtonColors = TimePlayButtonColors.Default,
            showIcon = false,
            useEpisodeArtwork = false,
            isDarkTheme = false,
        )
    }
}

@ShowkaseComposable(name = "BookmarkRow", group = "Bookmark", styleName = "Player")
@Preview(name = "Style - Player")
@Composable
fun BookmarkRowPlayerPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        BookmarkRow(
            bookmark = Bookmark(
                uuid = "",
                podcastUuid = "",
                episodeTitle = "Episode Title",
                timeSecs = 10,
                title = "Bookmark Title",
                createdAt = Date(),
            ),
            episode = PodcastEpisode(
                uuid = "",
                publishedDate = Date(),
            ),
            isMultiSelecting = { false },
            isSelected = { false },
            onPlayClick = {},
            modifier = Modifier,
            colors = BookmarkRowColors.Player,
            timePlayButtonStyle = TimePlayButtonStyle.Solid,
            timePlayButtonColors = TimePlayButtonColors.Player(textColor = Color.Black),
            showIcon = true,
            useEpisodeArtwork = false,
            isDarkTheme = false,
        )
    }
}
