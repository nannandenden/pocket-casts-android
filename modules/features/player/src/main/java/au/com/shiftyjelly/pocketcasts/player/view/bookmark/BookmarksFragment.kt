package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.reimagine.timestamp.ShareEpisodeTimestampFragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class BookmarksFragment : BaseFragment() {

    companion object {
        private const val ARG_SOURCE_VIEW = "sourceView"
        private const val ARG_EPISODE_UUID = "episodeUuid"
        private const val ARG_FORCE_DARK_THEME = "forceDarkTheme"
        fun newInstance(
            sourceView: SourceView,
            episodeUuid: String? = null,
            forceDarkTheme: Boolean = false,
        ) = BookmarksFragment().apply {
            arguments = bundleOf(
                ARG_SOURCE_VIEW to sourceView.analyticsValue,
                ARG_EPISODE_UUID to episodeUuid,
                ARG_FORCE_DARK_THEME to forceDarkTheme,
            )
        }
    }

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val bookmarksViewModel: BookmarksViewModel by viewModels({ requireParentFragment() })

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var sharingClient: SharingClient

    private val sourceView: SourceView
        get() = SourceView.fromString(arguments?.getString(ARG_SOURCE_VIEW))

    private val episodeUuid: String?
        get() = arguments?.getString(ARG_EPISODE_UUID)

    private val forceDarkTheme: Boolean
        get() = arguments?.getBoolean(ARG_FORCE_DARK_THEME) ?: false

    private val overrideTheme: Theme.ThemeType
        get() = when (sourceView) {
            SourceView.PLAYER -> if (Theme.isDark(context)) theme.activeTheme else Theme.ThemeType.DARK
            else -> if (forceDarkTheme && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(overrideTheme) {
            // Hack to allow nested scrolling inside bottom sheet viewpager
            // https://stackoverflow.com/a/70195667/193545
            Surface(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
                val listData = remember {
                    playerViewModel.listDataLive.asFlow()
                        // ignore the episode progress
                        .distinctUntilChanged { t1, t2 ->
                            t1.podcastHeader.episodeUuid == t2.podcastHeader.episodeUuid &&
                                t1.podcastHeader.isPlaying == t2.podcastHeader.isPlaying
                        }
                }.collectAsState(initial = null)

                val episodeUuid = episodeUuid(listData)
                val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
                BookmarksPage(
                    episodeUuid = episodeUuid,
                    backgroundColor = backgroundColor(listData),
                    textColor = textColor(listData),
                    sourceView = sourceView,
                    bookmarksViewModel = bookmarksViewModel,
                    multiSelectHelper = bookmarksViewModel.multiSelectHelper,
                    onRowLongPressed = { bookmark ->
                        bookmarksViewModel.multiSelectHelper.defaultLongPress(
                            multiSelectable = bookmark,
                            fragmentManager = childFragmentManager,
                            forceDarkTheme = sourceView == SourceView.PLAYER,
                        )
                    },
                    onShareBookmarkClick = ::onShareBookmarkClick,
                    onEditBookmarkClick = ::onEditBookmarkClick,
                    onUpgradeClicked = ::onUpgradeClicked,
                    showOptionsDialog = { showOptionsDialog(it) },
                    openFragment = { fragment ->
                        val bottomSheet = (parentFragment as? BottomSheetDialogFragment)
                        if (sourceView != SourceView.PROFILE) bottomSheet?.dismiss() // Do not close bookmarks container dialog if opened from profile
                        val fragmentHostListener = (activity as? FragmentHostListener)
                        fragmentHostListener?.apply {
                            closePlayer() // Closes player if open
                            openTab(R.id.navigation_profile)
                            addFragment(SettingsFragment())
                            addFragment(fragment)
                        }
                    },
                    onClearSearchTapped = {
                        bookmarksViewModel.clearSearchTapped()
                    },
                    onSearchBarClearButtonTapped = {
                        bookmarksViewModel.searchBarClearButtonTapped()
                    },
                    onHeadphoneControlsButtonTapped = {
                        bookmarksViewModel.onHeadphoneControlsButtonTapped()
                    },
                    bottomInset = if (sourceView == SourceView.PROFILE) {
                        0.dp + bottomInset.value.pxToDp(LocalContext.current).dp
                    } else {
                        28.dp
                    },
                    isDarkTheme = overrideTheme.darkTheme,
                )
            }
        }
    }

    @Composable
    private fun episodeUuid(listData: State<PlayerViewModel.ListData?>) =
        when (sourceView) {
            SourceView.PLAYER -> listData.value?.podcastHeader?.episodeUuid
            else -> episodeUuid
        }

    @Composable
    private fun backgroundColor(listData: State<PlayerViewModel.ListData?>) =
        when (sourceView) {
            SourceView.PLAYER -> listData.value?.let { Color(it.podcastHeader.backgroundColor) }
                ?: MaterialTheme.theme.colors.primaryUi01
            else -> MaterialTheme.theme.colors.primaryUi01
        }

    @Composable
    private fun textColor(listData: State<PlayerViewModel.ListData?>) =
        when (sourceView) {
            SourceView.PLAYER -> listData.value?.let { Color(it.podcastHeader.backgroundColor) }
                ?: MaterialTheme.theme.colors.primaryUi01
            else -> MaterialTheme.theme.colors.primaryText02
        }

    private val showOptionsDialog: (Int) -> Unit = { selectedValue ->
        activity?.supportFragmentManager?.let {
            OptionsDialog()
                .setForceDarkTheme(sourceView == SourceView.PLAYER)
                .addTextOption(
                    titleId = LR.string.bookmarks_select_option,
                    imageId = IR.drawable.ic_multiselect,
                    click = {
                        bookmarksViewModel.multiSelectHelper.isMultiSelecting = true
                    },
                )
                .addTextOption(
                    titleId = LR.string.bookmarks_sort_option,
                    imageId = IR.drawable.ic_sort,
                    valueId = selectedValue,
                    click = {
                        BookmarksSortByDialog(
                            settings = settings,
                            changeSortOrder = bookmarksViewModel::changeSortOrder,
                            sourceView = sourceView,
                            forceDarkTheme = sourceView == SourceView.PLAYER,
                        ).show(
                            context = requireContext(),
                            fragmentManager = it,
                        )
                    },
                ).show(it, "bookmarks_options_dialog")
        }
    }

    private fun onShareBookmarkClick() {
        lifecycleScope.launch {
            val (podcast, episode, bookmark) = bookmarksViewModel.getSharedBookmark() ?: return@launch
            bookmarksViewModel.onShare(podcast.uuid, episode.uuid, sourceView)
            val timestamp = bookmark.timeSecs.seconds
            if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
                ShareEpisodeTimestampFragment
                    .forBookmark(episode, timestamp, podcast.backgroundColor, sourceView)
                    .show(parentFragmentManager, "share_screen")
            } else {
                val request = SharingRequest.bookmark(podcast, episode, timestamp)
                    .setSourceView(sourceView)
                    .build()
                sharingClient.share(request)
            }
        }
    }

    private fun onEditBookmarkClick() {
        bookmarksViewModel.buildBookmarkArguments { arguments ->
            startActivity(arguments.getIntent(requireContext()))
        }
    }

    private fun onUpgradeClicked() {
        bookmarksViewModel.onGetBookmarksButtonTapped()
        val onboardingFlow = OnboardingFlow.Upsell(
            source = OnboardingUpgradeSource.BOOKMARKS,
        )
        OnboardingLauncher.openOnboardingFlow(requireActivity(), onboardingFlow)
    }

    fun onPlayerOpen() {
        bookmarksViewModel.onPlayerOpen()
    }

    fun onPlayerClose() {
        bookmarksViewModel.onPlayerClose()
    }
}
