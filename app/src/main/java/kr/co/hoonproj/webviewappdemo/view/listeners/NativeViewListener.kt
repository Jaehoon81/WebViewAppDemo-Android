package kr.co.hoonproj.webviewappdemo.view.listeners

import kr.co.hoonproj.webviewappdemo.model.remote.NetworkResponse

interface NativeViewListener {

    fun onCompleteEmployeesCall(networkResponse: NetworkResponse, showAlertDialog: Boolean)
}