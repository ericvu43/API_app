package edu.iu.luddy.capstoneproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class CallHistoryAdapter(
    private val dataSet: List<CallHistoryActivity.HistoryItem>,
    private val onItemAction: (CallHistoryActivity.HistoryItem, String) -> Unit
) : RecyclerView.Adapter<CallHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val callBar: ConstraintLayout = view.findViewById(R.id.call_bar)
        val nameText: TextView = view.findViewById(R.id.name_text)
        val dateTimeText: TextView = view.findViewById(R.id.date_time_text)
        val deleteButton: Button = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.call_history_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]

        when (item.passed) {
            true -> {
                holder.callBar.setBackgroundResource(R.drawable.saved_call_background_pass)
                holder.nameText.text = "PASS"
            }
            false -> {
                holder.callBar.setBackgroundResource(R.drawable.saved_call_background_fail)
                holder.nameText.text = "FAIL"
            }
            null -> {
                holder.callBar.setBackgroundResource(R.drawable.saved_call_background_fail)
                holder.nameText.text = "NO EXPECTED"
            }
        }

        holder.dateTimeText.text =
            if (item.dateTime.isBlank()) "(no time saved)" else item.dateTime

        holder.callBar.setOnClickListener { onItemAction(item, "navigate") }
        holder.deleteButton.setOnClickListener { onItemAction(item, "delete") }
    }


    override fun getItemCount(): Int = dataSet.size
}
