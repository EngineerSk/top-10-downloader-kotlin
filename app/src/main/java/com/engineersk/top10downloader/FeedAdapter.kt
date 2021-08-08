package com.engineersk.top10downloader

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.google.android.material.textview.MaterialTextView

class FeedAdapter(
    context: Context, private val resource: Int,
    private val applications: List<FeedEntry>
) : ArrayAdapter<FeedEntry>(context, resource) {

    private val TAG = "FeedAdapter"
    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        Log.d(TAG, "getCount: called...")
        return applications.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: inflater.inflate(resource, parent, false)

        val tvName: MaterialTextView = view.findViewById(R.id.tvName)
        val tvArtist: MaterialTextView = view.findViewById(R.id.tvArtist)
        val tvSummary: MaterialTextView = view.findViewById(R.id.tvSummary)

        val currentApplication = applications[position]

        tvName.text = currentApplication.name
        tvArtist.text = currentApplication.artist
        tvSummary.text = currentApplication.summary

        return view
    }
}