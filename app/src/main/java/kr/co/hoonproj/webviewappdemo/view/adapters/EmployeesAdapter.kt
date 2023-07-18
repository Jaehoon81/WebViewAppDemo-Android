package kr.co.hoonproj.webviewappdemo.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kr.co.hoonproj.webviewappdemo.R
import kr.co.hoonproj.webviewappdemo.databinding.ItemEmployeeCellBinding
import kr.co.hoonproj.webviewappdemo.model.remote.ResponseEmployees

class EmployeesAdapter(
    private val context: Context,
    private val onClickCellItem: (employeeData: ResponseEmployees.EmployeeData) -> Unit
) : RecyclerView.Adapter<EmployeesAdapter.EmployeesViewHolder>() {

    private var employeeDataList: ArrayList<ResponseEmployees.EmployeeData> = arrayListOf()

    fun setEmployeeDataList(employeeDataList: ArrayList<ResponseEmployees.EmployeeData>) {
        this.employeeDataList = employeeDataList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemEmployeeCellBinding.inflate(layoutInflater, parent, false)
        return EmployeesViewHolder(binding)
    }

    override fun getItemCount(): Int = employeeDataList.size

    override fun onBindViewHolder(holder: EmployeesViewHolder, position: Int) {
        holder.bind(employeeDataList[position], onClickCellItem)
    }

    inner class EmployeesViewHolder(
        private val binding: ItemEmployeeCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            employeeData: ResponseEmployees.EmployeeData,
            onClickCellItem: (employeeData: ResponseEmployees.EmployeeData) -> Unit
        ) {
            binding.employeeData = employeeData
            binding.executePendingBindings()

            Glide.with(itemView).load(
                employeeData.profileImage.ifEmpty { R.drawable.ic_launcher_foreground }
            ).into(binding.profileImage)

            binding.employeeCellItem.setOnClickListener {
                onClickCellItem(employeeData)
            }
        }
    }
}