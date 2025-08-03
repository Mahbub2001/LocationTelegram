package com.example.locationtest.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.locationtest.R
import com.example.locationtest.data.SearchResult

class SearchResultAdapter(
    private val onItemClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

    private var searchResults = mutableListOf<SearchResult>()

    fun updateResults(results: List<SearchResult>) {
        searchResults.clear()
        searchResults.addAll(results)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(searchResults[position])
    }

    override fun getItemCount(): Int = searchResults.size

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlaceName: TextView = itemView.findViewById(R.id.tv_place_name)
        private val tvPlaceAddress: TextView = itemView.findViewById(R.id.tv_place_address)

        fun bind(searchResult: SearchResult) {
            tvPlaceName.text = searchResult.name
            tvPlaceAddress.text = searchResult.address

            itemView.setOnClickListener {
                onItemClick(searchResult)
            }
        }
    }
}
