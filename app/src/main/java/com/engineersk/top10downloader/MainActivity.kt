package com.engineersk.top10downloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
    private val downloadData by lazy {
        DownloadData(this, xmlListView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "onCreate: called...")
        xmlListView = findViewById(R.id.xmlListView)
        downloadData.execute("http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=40/xml")
        Log.d(TAG, "onCreate: Done!!!")
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
//                val arrayAdapter = ArrayAdapter(
//                    propContext, R.layout.list_item,
//                    parseApplications.applications
//                )

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

    override fun onDestroy() {
        super.onDestroy()
        downloadData.cancel(true)
    }

}