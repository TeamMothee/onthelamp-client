package com.example.onthelamp
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class POIAdapter(private val poiList: List<POI>) : RecyclerView.Adapter<POIAdapter.POIViewHolder>() {

    class POIViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val poiName: TextView = itemView.findViewById(R.id.poiName)
        val poiAddress: TextView = itemView.findViewById(R.id.poiAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): POIViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.poi_item, parent, false)
        return POIViewHolder(view)
    }

    override fun onBindViewHolder(holder: POIViewHolder, position: Int) {
        val poi = poiList[position]
        holder.poiName.text = poi.name
        holder.poiAddress.text = "${poi.upperAddrName} ${poi.middleAddrName} ${poi.lowerAddrName}"
    }

    override fun getItemCount(): Int {
        return poiList.size
    }
}
