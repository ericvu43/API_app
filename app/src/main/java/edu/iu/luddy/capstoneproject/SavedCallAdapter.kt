package edu.iu.luddy.capstoneproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavedCallAdapter(
    private val dataSet: MutableList<Pair<String, String>>,
    private val onAction: (clickedItem: Pair<String, String>, function: String) -> Unit
) : RecyclerView.Adapter<SavedCallAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.name_text)
        val descText: TextView = itemView.findViewById(R.id.description_text)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
        val callBar: View = itemView.findViewById(R.id.call_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.saved_call_recyclerview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = dataSet[position]
        holder.nameText.text = item.first
        holder.descText.text = item.second

        holder.callBar.setOnClickListener {
            onAction(item, "navigate")
        }

        holder.deleteButton.setOnClickListener {
            onAction(item, "delete")
        }
    }

    override fun getItemCount(): Int = dataSet.size
}


