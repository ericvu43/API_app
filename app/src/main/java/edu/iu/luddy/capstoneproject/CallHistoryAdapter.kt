package edu.iu.luddy.capstoneproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class CallHistoryAdapter(private val dataSet: List<Triple<String, String, Boolean>>, private val onItemClick: (Triple<String, String, Boolean>, String) -> Unit): RecyclerView.Adapter<CallHistoryAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val callBar: ConstraintLayout
        val nameText: TextView
        val dateTimeText: TextView
        val deleteButton: Button

        init {
            callBar = view.findViewById(R.id.call_bar)
            nameText = view.findViewById(R.id.name_text)
            dateTimeText = view.findViewById(R.id.date_time_text)
            deleteButton = view.findViewById(R.id.delete_button)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.call_history_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        if(dataSet[position].third)
            holder.callBar.setBackgroundResource(R.drawable.saved_call_background_pass)
        else
            holder.callBar.setBackgroundResource(R.drawable.saved_call_background_fail)
        holder.callBar.setOnClickListener {
            onItemClick(dataSet[position], "navigate")
        }
        holder.nameText.text = dataSet[position].first
        holder.dateTimeText.text = dataSet[position].second
        holder.deleteButton.setOnClickListener {
            onItemClick(dataSet[position], "delete")
        }
    }

    override fun getItemCount() = dataSet.size
}