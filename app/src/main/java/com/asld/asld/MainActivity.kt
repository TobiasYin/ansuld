package com.asld.asld

import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.asld.asld.databinding.ActivityMainBinding
import com.asld.asld.service.ShellDaemon
import com.asld.asld.vnc.VncActivity
import com.asld.asld.tools.DownloadManager
import com.asld.asld.tools.Process

class MainActivity : AppCompatActivity() {
    var checkFiles = hashSetOf("proot", "lubuntu-desktop.tar.gz")

    override fun onStart() {
        super.onStart()
        var notExisted = false
        downloadFiles.forEach {
            if (checkFiles.contains(it.fileName)) {
                if (!it.checkStatus(filesDir.absoluteFile)) {
                    notExisted = true
                }
            }
        }
        if (notExisted){
            val intent = Intent(this, DownloadImages::class.java)
            startActivity(intent)
            Toast.makeText(this, "Please download: ${checkFiles.joinToString(",")} before start!", Toast.LENGTH_SHORT).show()
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