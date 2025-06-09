package cc.jtogashi.mapboxmapsupporthelper

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cc.jtogashi.mapboxmapsupporthelper.databinding.ViewholderListExampleBinding

class MainActivityListAdapter(
    private val exampleList: List<Class<out Activity>>
) : RecyclerView.Adapter<MainActivityListAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return exampleList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setActivityClass(exampleList[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderListExampleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(
            binding.root,
            parent.context
        )
    }

    class ViewHolder(
        private val view: View,
        private val context: Context
    ) : RecyclerView.ViewHolder(view) {
        private var activityClass: Class<*>? = null

        fun setActivityClass(activityClass: Class<out Activity>) {
            this.activityClass = activityClass
            view.findViewById<TextView>(R.id.textViewListExample).text = activityClass.simpleName

            view.setOnClickListener { v ->
                if (this.activityClass != null) {
                    val intent = Intent(context, this.activityClass)
                    context.startActivity(intent)
                }
            }
        }
    }
}