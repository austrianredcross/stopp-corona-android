package at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.list

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import at.roteskreuz.stopcorona.skeleton.core.R
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.State
import at.roteskreuz.stopcorona.skeleton.core.model.helpers.skipIf
import at.roteskreuz.stopcorona.skeleton.core.screens.base.fragment.BaseFragment
import at.roteskreuz.stopcorona.skeleton.core.utils.handleBaseErrors
import at.roteskreuz.stopcorona.skeleton.core.utils.observeOnMainThread
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.TypedEpoxyController
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.base_list_fragment.*
import kotlinx.android.synthetic.main.base_list_fragment.view.*

/**
 * Definition of base list of items, which are specified in the implementation.
 * The data to the list is loaded from viewModel of type [BaseDataObserverViewModel].
 * Received data is automatically inserted to the [controller].
 */
abstract class BaseListFragment<Data>(@LayoutRes layoutId: Int = R.layout.base_list_fragment) : BaseFragment(layoutId) {

    protected abstract val dataViewModel: BaseDataObserverViewModel<Data>
    protected abstract val controller: TypedEpoxyController<Data>

    /**
     * Reference to the swipe refresh layout view or null in not used.
     */
    protected open val swipeRefreshLayout: SwipeRefreshLayout?
        get() = view?.swipeRefreshLayout

    /**
     * Defining an action for swipe refresh.
     */
    protected open val onSwipeRefresh: (() -> Unit)? = { dataViewModel.fetchData() }

    /**
     * Reference to the epoxy recycler view.
     */
    protected open val recyclerView: EpoxyRecyclerView
        get() = epoxyRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            dataViewModel.getLastData()?.let { data ->
                onDataLoaded(data, savedInstanceState)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefreshLayout?.apply {
            isEnabled = onSwipeRefresh != null
            setOnRefreshListener(onSwipeRefresh)
        }
        recyclerView.setController(controller)

        disposables += dataViewModel.observeData()
            .observeOnMainThread()
            .skipIf { savedInstanceState != null }
            .subscribe { data ->
                onDataLoaded(data, savedInstanceState)
            }
        disposables += dataViewModel.observeState()
            .observeOnMainThread()
            .subscribe { state ->
                onDataLoading(state is State.Loading)
                when (state) {
                    is State.Error -> onError(state.error)
                }
            }
    }

    open fun onDataLoading(loading: Boolean) {
        swipeRefreshLayout?.isRefreshing = loading
    }

    open fun onDataLoaded(data: Data, savedInstanceState: Bundle?) {
        controller.setData(data)
    }

    open fun onError(error: Throwable) {
        handleBaseErrors(error)
    }
}