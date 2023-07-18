package kr.co.hoonproj.webviewappdemo.view.fragments

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kr.co.hoonproj.webviewappdemo.R
import kr.co.hoonproj.webviewappdemo.WebViewAppDemo
import kr.co.hoonproj.webviewappdemo.databinding.FragmentNativeViewBinding
import kr.co.hoonproj.webviewappdemo.model.remote.NetworkResponse
import kr.co.hoonproj.webviewappdemo.model.remote.ResponseEmployees
import kr.co.hoonproj.webviewappdemo.utils.CustomAlertDialog
import kr.co.hoonproj.webviewappdemo.view.MainActivity
import kr.co.hoonproj.webviewappdemo.view.adapters.EmployeesAdapter
import kr.co.hoonproj.webviewappdemo.view.listeners.NativeViewListener
import kr.co.hoonproj.webviewappdemo.viewmodel.MainViewModel

private const val TAG: String = "[WebViewAppDemo] NativeViewFragment"

@AndroidEntryPoint
class NativeViewFragment : Fragment(), NativeViewListener {

    companion object {
        fun newInstance() = NativeViewFragment()
    }

    private val mBinding: FragmentNativeViewBinding by lazy(LazyThreadSafetyMode.NONE) {
        FragmentNativeViewBinding.inflate(layoutInflater)
    }
    private lateinit var mainViewModel: MainViewModel
    private var isFragmentPaused: Boolean = false

    private lateinit var employeesAdapter: EmployeesAdapter

    private var tabTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // NativeViewFragment를 ViewPager에 추가 시, 자동으로 tag 값(f3)이 부여된다.
        // (onCreate() 순서대로 f0, f1, f2, f3)
        tag?.let { tabTag ->
            Log.i(TAG, "NativeViewFragment_$tabTag:: onCreate($savedInstanceState)")

            this.tabTag = tabTag
            setFragmentResultListener(tabTag)
        }
    }

    private fun setFragmentResultListener(tabTag: String) {
        parentFragmentManager.setFragmentResultListener(tabTag, this) { key, bundle ->
            Log.d(TAG, "FragmentResultListener_Key: $key, Bundle: $bundle")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainViewModel = (requireActivity() as MainActivity).mainViewModel
        mainViewModel.nativeViewListener = this
        mBinding.mainViewModel = mainViewModel
        mBinding.lifecycleOwner = requireActivity()

        initRecyclerView()
        setOnScrollChangeListener()

        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_native_view, container, false)
        return mBinding.root
    }

    private fun initRecyclerView() {
        // EmployeesAdapter를 생성하고, RecyclerView에 연동한다.
        employeesAdapter = EmployeesAdapter(requireContext()) { employeeData ->
            Log.d(TAG, "EmployeeData_Cell #${employeeData.id} has been selected.")
        }
        mBinding.recyclerView.adapter = employeesAdapter
        // 각 CellItem의 Vertical 마진값을 설정한다.
        mBinding.recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//                super.getItemOffsets(outRect, view, parent, state)
                outRect.top = 0
                outRect.bottom = 10
            }
        })
    }

    private fun setOnScrollChangeListener() {
        mBinding.recyclerView.setOnScrollChangeListener { view, newX, newY, oldX, oldY ->
            if ((newY - oldY) > 5) {
                (requireActivity() as MainActivity).scrollBottomNavigationView(false)
            } else if ((newY - oldY) < -5) {
                (requireActivity() as MainActivity).scrollBottomNavigationView(true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "NativeViewFragment_$tabTag:: onViewCreated()")

        setupObservers()

        val currentTabIndex = WebViewAppDemo.prefs.bottomTabIndex
        val targetTabIndex = tabTag?.substring(1, 2)!!.toInt()
        refreshNativeView(showAlertDialog = (currentTabIndex == targetTabIndex))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        mainViewModel.employees.observe(viewLifecycleOwner) { responseEmployees ->
            Log.d(TAG, "EmployeesCall_ResultData = ${responseEmployees.data.toString()}")

            if (!responseEmployees.data.isNullOrEmpty()) {
                // 서버에서 수신한 EmployeeData List를 RecyclerView에 Binding한다.
                val employeeDataList = responseEmployees.data as ArrayList<ResponseEmployees.EmployeeData>
                employeesAdapter.setEmployeeDataList(employeeDataList)
                employeesAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "NativeViewFragment_$tabTag:: onStart()")
    }

    override fun onResume() {
        super.onResume()

        // 처음 실행 시에는 onStart() -> onResume() -> onPause() -> onResume() 순서로 호출된다.
        // 첫번째 onResume()은 로직의 중복 방지를 위해 건너뛴다.
        if (isFragmentPaused == true) {
            isFragmentPaused = false
            Log.i(TAG, "NativeViewFragment_$tabTag:: onResume()")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.i(TAG, "NativeViewFragment_$tabTag:: onPause()")

        isFragmentPaused = true
    }

    override fun onStop() {
        super.onStop()
        Log.i(TAG, "NativeViewFragment_$tabTag:: onStop()")
    }

    override fun onDestroyView() {
        Log.i(TAG, "NativeViewFragment_$tabTag:: onDestroyView()")

        mBinding.recyclerView.adapter = null
        mainViewModel.employees.removeObservers(viewLifecycleOwner)
        mainViewModel.nativeViewListener = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.i(TAG, "NativeViewFragment_$tabTag:: onDestroy()")

//        mBinding = null
        super.onDestroy()
    }

    override fun onCompleteEmployeesCall(networkResponse: NetworkResponse, showAlertDialog: Boolean) {
        if (showAlertDialog == true) {
            requireActivity().runOnUiThread {
                CustomAlertDialog(requireContext(), message = networkResponse.resultMessage).show {
                    (requireActivity() as MainActivity).hideBottomNavigationBar()
                }
            }
        }
    }

    fun refreshNativeView(showAlertDialog: Boolean = true) {
        // 서버에 EmployeeData List를 요청한다.
        mainViewModel.requestEmployeesToAPI(showAlertDialog)
    }
}