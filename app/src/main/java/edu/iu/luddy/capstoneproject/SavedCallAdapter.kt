package edu.iu.luddy.capstoneproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class SavedCallAdapter(private val dataSet: Array<Pair<String, String>>, private val onItemClick: (Pair<String, String>, String) -> Unit): RecyclerView.Adapter<SavedCallAdapter.ViewHolder>() {
    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val callBar: ConstraintLayout
        val nameText: TextView
        val descriptionText: TextView
        val deleteButton: Button

        init {
            callBar = view.findViewById(R.id.call_bar)
            nameText = view.findViewById(R.id.name_text)
            descriptionText = view.findViewById(R.id.description_text)
            deleteButton = view.findViewById(R.id.delete_button)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.saved_call_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.callBar.setOnClickListener {
            onItemClick(dataSet[position], "navigate")
        }
        holder.nameText.text = dataSet[position].first
        holder.descriptionText.text = dataSet[position].second
        holder.deleteButton.setOnClickListener {
            onItemClick(dataSet[position], "delete")
        }
    }

    override fun getItemCount() = dataSet.size
}