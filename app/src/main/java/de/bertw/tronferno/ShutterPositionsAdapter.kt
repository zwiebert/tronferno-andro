package de.bertw.tronferno

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView


class ShutterPositionsAdapter(val ma: MainActivity) : RecyclerView.Adapter<ShutterPositionsAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val vpbArr = arrayOf(
                view.findViewById<View>(R.id.vpbPiM1) as ProgressBar,
                view.findViewById<View>(R.id.vpbPiM2) as ProgressBar,
                view.findViewById<View>(R.id.vpbPiM3) as ProgressBar,
                view.findViewById<View>(R.id.vpbPiM4) as ProgressBar,
                view.findViewById<View>(R.id.vpbPiM5) as ProgressBar,
                view.findViewById<View>(R.id.vpbPiM6) as ProgressBar,
                view.findViewById<View>(R.id.vpbPiM7) as ProgressBar
        )

        val vtvArr = arrayOf(
                view.findViewById<View>(R.id.vtvPiM1) as TextView,
                view.findViewById<View>(R.id.vtvPiM2) as TextView,
                view.findViewById<View>(R.id.vtvPiM3) as TextView,
                view.findViewById<View>(R.id.vtvPiM4) as TextView,
                view.findViewById<View>(R.id.vtvPiM5) as TextView,
                view.findViewById<View>(R.id.vtvPiM6) as TextView,
                view.findViewById<View>(R.id.vtvPiM7) as TextView
        )

        val vltPiLayout = view.findViewById<View>(R.id.vltPiLayout) as ConstraintLayout

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.position_indicator, parent, false)

        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return ma.nonEmptyGroupsCount
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val grp = ma.getGroupNumberByIdx(position)
        val colorNormal = ContextCompat.getColor(ma, R.color.background_material_light)
        val colorSelected = ContextCompat.getColor(ma, R.color.colorAccent)
        val colorNormal2 = ContextCompat.getColor(ma, R.color.button_material_light)
        val colorSelected2 = ContextCompat.getColor(ma, R.color.background_floating_material_dark)
        val memb = ma.getSelectedMember()
        val group = ma.getSelectedGroup()

     //Highlighting groups Disabled. because highlighted members already mark the selected group
        // holder.vltPiLayout.setBackgroundColor(if (grp == group || group == 0) colorSelected2 else colorNormal2)

        for (mi in 0..6) {
            val m = mi + 1
            val membEnable = m <= ma.membMax[grp]

            if (membEnable) {
                holder.vpbArr[mi].apply {
                    visibility = View.VISIBLE
                    progress = ma.pr.model.getPos(grp, mi+1)
                    tag = grp
                }

                holder.vtvArr[mi].apply {
                    text = ma.getMemberName(grp, m)
                    visibility = View.VISIBLE
                }.setBackgroundColor(if (group == 0 || (group == grp && (memb == 0 ||  memb == m))) colorSelected else colorNormal)


            } else {
                holder.vpbArr[mi].visibility = View.GONE
                holder.vtvArr[mi].visibility = View.GONE
            }

        }
    }


}

