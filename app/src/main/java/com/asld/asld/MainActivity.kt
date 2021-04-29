package com.asld.asld

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.asld.asld.databinding.ActivityMainBinding
import com.asld.asld.service.ShellDaemon
import com.asld.asld.vnc.VncActivity
import com.asld.asld.tools.DownloadManager
import com.asld.asld.tools.Process
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log

class MainActivity : AppCompatActivity() {
    var checkFiles = hashMapOf(Pair("proot", false), Pair("lubuntu", false))

    override fun onStart() {
        super.onStart()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE).toTypedArray(),
                1
            )
        }

        downloadFiles.forEach {
            for (k in checkFiles) {
                if (k.value)
                    continue
                if (it.fileName.contains(k.key)) {
                    if (it.checkStatus(filesDir)) {
                        k.setValue(true)
                    }
                }
            }
        }
        var notExisted = false
        for (k in checkFiles) {
            if (!k.value) {
                notExisted = true
                break
            }
        }
        if (notExisted) {
            val intent = Intent(this, DownloadImages::class.java)
            startActivity(intent)
            Toast.makeText(
                this,
                "Please download: ${
                    checkFiles.filter { !it.value }.map { it.key }.joinToString(",")
                } before start!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))

        Process("${filesDir.absolutePath}/fserver").exec()
        ShellDaemon.init(filesDir.absolutePath, "lubuntu")

        binding.goToTerm.setOnClickListener {
            val intent = Intent(this, TerminalActivity::class.java)
            startActivity(intent)
        }


        binding.goToVnc.setOnClickListener {
            val intent = Intent(this, VncActivity::class.java)
            startActivity(intent)
        }
        binding.goToOterm.setOnClickListener {
            val intent = Intent(this, OriginShellActivity::class.java)
            startActivity(intent)
        }
        binding.goDownload.setOnClickListener {
            val intent = Intent(this, DownloadImages::class.java)
            startActivity(intent)
        }
        DownloadManager.relativeRoot = filesDir.absolutePath
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}