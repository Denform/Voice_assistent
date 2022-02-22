package com.example.voiceassistent

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wolfram.alpha.WAEngine
import com.wolfram.alpha.WAPlainText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {

    //APP NAME: VoiceAssForEducate
    //
    //APPID: 5X4PRV-AWT67KP4RR
    //
    //USAGE TYPE: Personal/Non-commercial Only

    val TAG: String = "MainActivity"

    lateinit var requestInput: TextInputEditText
    lateinit var  podsAdapter: SimpleAdapter
    lateinit var progressBar: ProgressBar
    lateinit var waEngine: WAEngine

    val pods = mutableListOf<HashMap<String, String>>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews();
        initWolframEngine()
    }

    fun initViews()
    {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        requestInput = findViewById(R.id.text_input_edit)
        requestInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE)
            {
                pods.clear()
                podsAdapter.notifyDataSetChanged()
                val question = requestInput.text.toString()
                askWolfram(question)
            }
            return@setOnEditorActionListener false
        }
        val podsList: ListView = findViewById(R.id.pods_list)

        podsAdapter = SimpleAdapter(
            applicationContext,
            pods,
            R.layout.item_pod,
            arrayOf("Title", "Content"),
            intArrayOf(R.id.title, R.id.content)
        )
        podsList.adapter = podsAdapter

        val voiceInputButton: FloatingActionButton = findViewById(R.id.voice_input_button)
        voiceInputButton.setOnClickListener {
            Log.d(TAG, "FAB")
        }

        progressBar = findViewById(R.id.progress_bar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbaar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId)
        {
            R.id.action_stop ->
            {
                Log.d(TAG,"action stop");
                return true
            }

            R.id.action_clear ->
            {
                requestInput.text?.clear()
                pods.clear()
                podsAdapter.notifyDataSetChanged()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun initWolframEngine()
    {
        waEngine = WAEngine().apply {
            appID = "5X4PRV-AWT67KP4RR"
            addFormat("plaintext")
        }
    }

    fun showSnacbar(message: String)
    {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_INDEFINITE).apply {
            setAction(android.R.string.ok)
            {
                dismiss()
            }
            show()
        }
    }

    fun askWolfram(request: String)
    {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val query = waEngine.createQuery().apply { input = request }
            runCatching {
                waEngine.performQuery(query)
            }.onSuccess {   result ->
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.GONE
                    if (result.isError)
                    {
                        showSnacbar(result.errorMessage)
                        return@withContext
                    }

                    if (!result.isSuccess)
                    {
                        requestInput.error = getString(R.string.error_do_not_understand)
                        return@withContext
                    }

                    for (pod in result.pods)
                    {
                        if (!pod.isError) continue
                        val content = StringBuilder()
                        for (subpod in pod.subpods)
                        {
                            for (element in subpod.contents)
                            {
                                if (element is WAPlainText)
                                {
                                    content.append(element.text)
                                }
                            }
                        }
                        pods.add(0, HashMap<String, String>().apply {
                            put("Title", pod.title)
                            put("Content", content.toString())
                        })
                    }
                    podsAdapter.notifyDataSetChanged()
                }
            }.onFailure { t ->
                withContext(Dispatchers.Main){
                    progressBar.visibility = View.GONE
                    showSnacbar(t.message ?: getString(R.string.error_something_went_wrong))
                }
            }
        }
    }
}