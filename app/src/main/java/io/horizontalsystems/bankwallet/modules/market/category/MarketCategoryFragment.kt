package io.horizontalsystems.bankwallet.modules.market.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import coil.annotation.ExperimentalCoilApi
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.modules.coin.CoinFragment
import io.horizontalsystems.bankwallet.modules.market.MarketModule.ViewItemState
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.components.*
import io.horizontalsystems.bankwallet.ui.extensions.SelectorDialog
import io.horizontalsystems.bankwallet.ui.extensions.SelectorItem
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.marketkit.models.CoinCategory

class MarketCategoryFragment : BaseFragment() {

    private val categoryUid by lazy {
        arguments?.getString(categoryUidKey)
    }
    private val categoryName by lazy {
        arguments?.getString(categoryNameKey)
    }
    private val categoryDescription by lazy {
        arguments?.getString(categoryDescriptionKey)
    }
    private val categoryImageUrl by lazy {
        arguments?.getString(categoryImageUrlKey)
    }

    val viewModel by viewModels<MarketCategoryViewModel> {
        MarketCategoryModule.Factory(
            categoryUid!!,
            categoryName!!,
            categoryDescription!!,
            categoryImageUrl!!
        )
    }

    @ExperimentalCoilApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    CategoryScreen(
                        viewModel,
                        { findNavController().popBackStack() },
                        { sortingFieldSelect, onSelectSortingField ->
                            onSortingClick(sortingFieldSelect, onSelectSortingField)
                        },
                        { coinUid -> onCoinClick(coinUid) }
                    )
                }
            }
        }
    }

    private fun onSortingClick(
        sortingFieldSelect: Select<SortingField>,
        onSelectSortingField: (SortingField) -> Unit
    ) {
        val items = sortingFieldSelect.options.map {
            SelectorItem(getString(it.titleResId), it == sortingFieldSelect.selected)
        }

        SelectorDialog
            .newInstance(items, getString(R.string.Market_Sort_PopupTitle)) { position ->
                val selectedSortingField = sortingFieldSelect.options[position]
                onSelectSortingField(selectedSortingField)
            }
            .show(childFragmentManager, "sorting_field_selector")
    }

    private fun onCoinClick(coinUid: String) {
        val arguments = CoinFragment.prepareParams(coinUid)

        findNavController().navigate(R.id.coinFragment, arguments, navOptions())
    }

    companion object {
        private const val categoryUidKey = "category_uid_field"
        private const val categoryNameKey = "category_name_field"
        private const val categoryDescriptionKey = "category_description_field"
        private const val categoryImageUrlKey = "category_image_url_field"

        fun prepareParams(coinCategory: CoinCategory): Bundle {
            return bundleOf(
                categoryUidKey to coinCategory.uid,
                categoryNameKey to coinCategory.name,
                categoryDescriptionKey to coinCategory.description["en"],
                categoryImageUrlKey to coinCategory.imageUrl,
            )
        }
    }

}

@ExperimentalCoilApi
@Composable
fun CategoryScreen(
    viewModel: MarketCategoryViewModel,
    onCloseButtonClick: () -> Unit,
    onSortMenuClick: (select: Select<SortingField>, onSelect: ((SortingField) -> Unit)) -> Unit,
    onCoinClick: (String) -> Unit,
) {
    val viewItemState by viewModel.viewStateLiveData.observeAsState()
    val header by viewModel.headerLiveData.observeAsState()
    val menu by viewModel.menuLiveData.observeAsState()
    val loading by viewModel.loadingLiveData.observeAsState()
    val isRefreshing by viewModel.isRefreshingLiveData.observeAsState()

    val interactionSource = remember { MutableInteractionSource() }

    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            TopCloseButton(interactionSource, onCloseButtonClick)
            header?.let { header ->
                DescriptionCard(header.title, header.description, header.icon)
            }
            menu?.let { menu ->
                HeaderWithSorting(
                    menu.sortingFieldSelect, viewModel::onSelectSortingField,
                    null, null,
                    menu.marketFieldSelect, viewModel::onSelectMarketField,
                    onSortMenuClick
                )
            }

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing ?: false || loading ?: false),
                onRefresh = {
                    viewModel.refresh()
                },
                indicator = { state, trigger ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = trigger,
                        scale = true,
                        backgroundColor = ComposeAppTheme.colors.claude,
                        contentColor = ComposeAppTheme.colors.oz,
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) {
                when (val state = viewItemState) {
                    is ViewItemState.Error -> {
                        ListErrorView(
                            stringResource(R.string.Market_SyncError)
                        ) {
                            viewModel.onErrorClick()
                        }
                    }
                    is ViewItemState.Data -> {
                        LazyColumn {
                            coinList(state.items, onCoinClick)
                        }
                    }
                }
            }
        }
    }
}