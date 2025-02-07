package com.antonetti.mazegenerator

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    private lateinit var canvasView: MazeCanvasView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get reference to the custom canvas view
        canvasView = findViewById(R.id.canvasView)

        // Find the Toolbar and set it as the ActionBar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }


    // Handle menu item clicks
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.larger -> {
                canvasView.larger()
                canvasView.invalidate()
                true
            }
            R.id.smaller -> {
                canvasView.smaller()
                canvasView.invalidate()
                true
            }
            R.id.zoom -> {
                canvasView.toggleZoom()
                canvasView.invalidate()
                true
            }
            R.id.hint -> {
                canvasView.hintOn()
                canvasView.invalidate()
                true
            }
            R.id.hardmode -> {
                canvasView.maze.toggleCulling()
                canvasView.invalidate()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}