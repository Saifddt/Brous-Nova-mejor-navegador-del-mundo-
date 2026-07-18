package com.pl.novabrowser

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView

class TabAdapter(
    private val tabs: MutableList<BrowserTab>,
    private val onSelect: (Int) -> Unit,
    private val onClose: (Int) -> Unit
) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

    inner class TabViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val title: android.widget.TextView = itemView.findViewById(R.id.tabTitle)
        val url: android.widget.TextView = itemView.findViewById(R.id.tabUrl)
        val closeBtn: android.widget.ImageButton = itemView.findViewById(R.id.btnCloseTab)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
        return TabViewHolder(view)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifBlank { "Nueva pestaña" }
        holder.url.text = tab.url

        // pequeña animación de aparición escalonada para que se sienta vivo
        if (Prefs.animationsEnabled) {
            val anim = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.tab_slide_in_right)
            anim.startOffset = (position * 40).toLong()
            holder.itemView.startAnimation(anim)
        }

        holder.itemView.setOnClickListener { onSelect(holder.adapterPosition) }
        holder.closeBtn.setOnClickListener { onClose(holder.adapterPosition) }
    }

    override fun getItemCount(): Int = tabs.size
}
