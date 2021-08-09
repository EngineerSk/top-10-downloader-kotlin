package com.engineersk.top10downloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.ListView
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.properties.Delegates

class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""
    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            release = $releaseDate
            imageURL = $imageURL
        """.trimIndent()
    }

}

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var xmlListView: ListView
    private var downloadData: DownloadData? = null
    private var feedURL: String =
        "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit: Int = 10

    private var feedCachedUrl = "INVALIDATED"
    private val STATE_URL = "feedUrl"
    private val STATE_LIMIT = "feedLimit"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate: called...")

        if(savedInstanceState != null){
            feedURL = savedInstanceState.getString(STATE_URL)!!
            feedLimit = savedInstanceState.getInt(STATE_LIMIT)
        }

        xmlListView = findViewById(R.id.xmlListView)
        downloadData = DownloadData(this, xmlListView)
        downloadData?.execute(feedURL.format(feedLimit))
        Log.d(TAG, "onCreate: Done!!!")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)
        if (feedLimit == 10) {
            menu?.findItem(R.id.menu_top_10)?.isChecked = true
        } else
            menu?.findItem(R.id.menu_top_25)?.isChecked = true
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_free -> feedURL =
                "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.menu_paid -> feedURL =
                "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.menu_songs -> feedURL =
                "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.menu_refresh -> feedCachedUrl = "INVALIDATED"
            R.id.menu_top_10, R.id.menu_top_25 -> {
                if (!item.isChecked) {
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                    Log.d(
                        TAG,
                        "onOptionsItemSelected: ${item.title} setting feedLimit to $feedLimit"
                    )
                } else {
                    Log.d(
                        TAG, "onOptionsItemSelected: ${item.title} setting feedLimit unchanged..."
                    )
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        downloadUrl(feedURL.format(feedLimit))
        return true
    }

    private fun downloadUrl(feedURL: String) {
        Log.d(TAG, "downloadUrl: Starting Async Task...")
        if(feedURL != feedCachedUrl) {
            downloadData = DownloadData(this, xmlListView)
            downloadData?.execute(feedURL.format(feedLimit))
            feedCachedUrl = feedURL
            Log.d(TAG, "downloadUrl: Done!!!")
        }else{
            Log.d(TAG, "downloadUrl - URL not changed!!!")
        }
    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) :
            AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            var propContext: Context by Delegates.notNull()
            var propListView: ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun doInBackground(vararg urls: String?): String {
                Log.d(TAG, "doInBackground: starts with ${urls[0]}")
                val rssFeed: String = downloadXML(urls[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error Downloading!!!")
                }
                return rssFeed
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                val feedAdapter = FeedAdapter(
                    propContext, R.layout.list_record,
                    parseApplications.applications
                )
                propListView.adapter = feedAdapter
            }

            private fun downloadXML(urlPath: String?): String {
                val xmlResult = StringBuilder()

                try {
                    val url = URL(urlPath)
                    val urlConnection: HttpURLConnection =
                        url.openConnection() as HttpURLConnection
                    val response = urlConnection.responseCode
                    Log.d(TAG, "downloadXML: The response code was $response")

                    urlConnection.inputStream.buffered().reader().use {
                        xmlResult.append(
                            it.readText()
                        )
                    }
                    Log.d(TAG, "downloadXML: Received ${xmlResult.length} bytes")
                    return xmlResult.toString()
                } catch (e: Exception) {
                    val errorMessage: String = when (e) {
                        is MalformedURLException -> "downloadXML: Invalid URL!!! ${e.message}"
                        is IOException -> "downloadXML: IOException Reading data ${e.message}"
                        is SecurityException -> {
                            e.printStackTrace()
                            "downloadXML: Security Exception. Needs permission? ${e.message}"
                        }
                        else -> "downloadXML: Unknown error ${e.message}"
                    }
                    Log.e(TAG, errorMessage)
                }
                return ""
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_URL, feedURL)
        outState.putInt(STATE_LIMIT, feedLimit)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)
    }

}