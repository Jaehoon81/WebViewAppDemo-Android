package kr.co.hoonproj.webviewappdemo.utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

enum class GlobalEvent {

    OtherTabsDidReload,
    AnotherTabDidMove,
    IsBottomNaviViewHidden
}

object EventBus {

    // OtherTabsDidReload 실행 시 전달 값
    var currentTabTag: String = ""

    // AnotherTabDidMove 실행 시 전달 값
    var tabTagToMove: String = ""
    var targetUrl: String = ""

    // IsBottomNaviViewHidden 실행 시 전달 값
    var isBottomTabsVisible: Boolean = false

    private val events = MutableSharedFlow<GlobalEvent>()

    suspend fun subscribe(event: GlobalEvent, onEvent: () -> Unit) {
        events.filter {
            event == it
        }.collect {
            onEvent()
        }
    }

    fun post(event: GlobalEvent) {
        GlobalScope.launch {
            events.emit(event)
        }
    }
}