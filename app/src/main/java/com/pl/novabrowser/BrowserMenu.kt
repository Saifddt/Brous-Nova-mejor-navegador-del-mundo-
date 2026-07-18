package com.pl.novabrowser

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow

/**
 * Menú principal estilo Kiwi Browser: lista vertical con ícono + texto,
 * algunas filas con switch inline (bloqueador de anuncios, modo oscuro,
 * sitio de escritorio). Se muestra como PopupWindow anclado al botón menú.
 */
class BrowserMenu(private val context: Context) {

    data class Row(
        val label: String,
        val iconRes: Int,
        val isSwitch: Boolean = false,
        val switchChecked: Boolean = false,
        val onClick: (() -> Unit)? = null,
        val onSwitchChanged: ((Boolean) -> Unit)? = null
    )

    private var popupWindow: PopupWindow? = null

    fun show(anchor: View, rows: List<Row?>) {
        val cardView = LayoutInflater.from(context).inflate(R.layout.popup_main_menu, null)
        val container = cardView.findViewById<LinearLayout>(R.id.menuList)

        rows.forEach { row ->
            if (row == null) {
                val divider = View(context)
                divider.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1
                ).apply { topMargin = 6; bottomMargin = 6 }
                divider.setBackgroundResource(R.drawable.bg_menu_divider)
                container.addView(divider)
                return@forEach
            }

            val rowView = LayoutInflater.from(context).inflate(R.layout.item_menu_row, container, false)
            rowView.findViewById<android.widget.ImageView>(R.id.rowIcon).setImageResource(row.iconRes)
            rowView.findViewById<android.widget.TextView>(R.id.rowLabel).text = row.label

            val switchView = rowView.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.rowSwitch)
            if (row.isSwitch) {
                switchView.visibility = View.VISIBLE
                switchView.isChecked = row.switchChecked
                switchView.setOnCheckedChangeListener { _, checked ->
                    row.onSwitchChanged?.invoke(checked)
                }
                rowView.setOnClickListener { switchView.isChecked = !switchView.isChecked }
            } else {
                rowView.setOnClickListener {
                    row.onClick?.invoke()
                    popupWindow?.dismiss()
                }
            }
            container.addView(rowView)
        }

        val popup = PopupWindow(
            cardView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popup.isOutsideTouchable = true
        popup.elevation = 16f
        popup.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popupWindow = popup

        popup.showAsDropDown(anchor, -220, 8, Gravity.END)
    }
}
