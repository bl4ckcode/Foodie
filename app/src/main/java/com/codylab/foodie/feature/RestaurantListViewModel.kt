package com.codylab.foodie.feature

import android.arch.lifecycle.MutableLiveData
import android.arch.paging.PagedList
import android.content.res.Resources
import com.codylab.foodie.R
import com.codylab.foodie.core.coroutine.ScopedViewModel
import com.codylab.foodie.core.extension.NonNullMediatorLiveData
import com.codylab.foodie.core.extension.exhaustive
import com.codylab.foodie.core.livedata.Event
import com.codylab.foodie.core.paging.NetworkState
import com.codylab.foodie.core.paging.Status
import com.codylab.foodie.core.reactive.BaseObserver
import com.codylab.foodie.core.room.RestaurantEntity
import com.codylab.foodie.usecase.GetPagedRestaurantsListUseCase
import com.codylab.foodie.usecase.RefreshPagedRestaurantsListUseCase
import io.reactivex.rxkotlin.plusAssign
import org.jetbrains.annotations.TestOnly
import javax.inject.Inject
import javax.inject.Singleton

data class RestaurantListUiModel(
    var zomatoRestaurantList: PagedList<RestaurantEntity>? = null,
    var isLoading: Boolean = false,
    var message: Event<String>? = null
)

@Singleton
class RestaurantListViewModel @Inject constructor(
    private val resources: Resources,
    private val getPagedRestaurantsListUseCase: GetPagedRestaurantsListUseCase,
    private val refreshPagedRestaurantsListUseCase: RefreshPagedRestaurantsListUseCase
) : ScopedViewModel() {

    val uiModelLiveData = NonNullMediatorLiveData<RestaurantListUiModel>()
    private val uiModelData = RestaurantListUiModel()

    fun onLocationRequested(grant: Boolean) {
        if (!grant) {
            uiModelData.message = Event(resources.getString(R.string.error_no_location_permission))
            uiModelLiveData.postValue(uiModelData)
            return
        }

        disposables += getPagedRestaurantsListUseCase().subscribeWith(RestaurantObserver(uiModelData, uiModelLiveData))
    }

    fun onRefresh() {
        disposables += refreshPagedRestaurantsListUseCase().subscribeWith(RestaurantObserver(uiModelData, uiModelLiveData))
    }

    @TestOnly
    fun showNoPermissionError() {
        uiModelData.message = Event(resources.getString(R.string.error_no_location_permission))
        uiModelLiveData.postValue(uiModelData)
    }

    class RestaurantObserver(
        private val uiModelData: RestaurantListUiModel,
        private val uiModelLiveData: MutableLiveData<RestaurantListUiModel>
    ): BaseObserver<Pair<PagedList<RestaurantEntity>, NetworkState>>() {

        override fun onNext(pair: Pair<PagedList<RestaurantEntity>, NetworkState>) {
            val (pagedList, networkState) = pair
            when(networkState.status) {
                Status.RUNNING -> {
                    uiModelData.isLoading = true
                }
                Status.SUCCESS -> {
                    uiModelData.isLoading = false
                }
                Status.FAILED -> {
                    uiModelData.isLoading = false
                    uiModelData.message = Event(networkState.msg?: "")
                }
            }.exhaustive

            uiModelData.zomatoRestaurantList = pagedList
            uiModelLiveData.postValue(uiModelData)
        }

        override fun onComplete() {
            super.onComplete()
            uiModelData.isLoading = false
            uiModelLiveData.postValue(uiModelData)
        }

        override fun onError(e: Throwable) {
            super.onError(e)
            uiModelData.isLoading = false
            uiModelData.message = Event(e.message.toString())
            uiModelLiveData.postValue(uiModelData)
        }
    }
}