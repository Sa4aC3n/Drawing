package com.art.drawingkids

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.chip.ChipGroup

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var adView: AdView
    private lateinit var repository: DrawingLessonRepository
    private lateinit var adapter: DrawingAdapter

    private var lastCategoryClickTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize AdMob
        MobileAds.initialize(this)

        adView = findViewById(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())

        repository = DrawingLessonRepository(this)

        recyclerView = findViewById(R.id.recyclerView_main)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val initialLessons = repository.getLessons(CategoryGroup.ALL)
        adapter = DrawingAdapter(initialLessons) { category ->
            // Prevent multiple rapid clicks from spawning multiple activities
            val now = SystemClock.elapsedRealtime()
            if (now - lastCategoryClickTime > 600) {
                lastCategoryClickTime = now
                val intent = Intent(this, DrawingActivity::class.java).apply {
                    putExtra("Image_Array", category.imageArrayKey)
                }
                startActivity(intent)
            }
        }
        recyclerView.adapter = adapter

        // Setup filter chips
        chipGroup = findViewById(R.id.chipGroupFilters)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedGroup = when (checkedIds.first()) {
                R.id.chipAnimals -> CategoryGroup.ANIMALS
                R.id.chipVehicles -> CategoryGroup.VEHICLES
                R.id.chipCharacters -> CategoryGroup.CHARACTERS
                R.id.chipNature -> CategoryGroup.NATURE
                else -> CategoryGroup.ALL
            }
            val filtered = repository.getLessons(selectedGroup)
            adapter.updateCategories(filtered)
        }

        // Setup back button press handling
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_name))
            .setIcon(R.drawable.app_icon)
            .setMessage(getString(R.string.exit_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finish()
            }
            .setNegativeButton(getString(R.string.rate_app)) { _, _ ->
                val appPackageName = packageName
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName"))
                    )
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$appPackageName")
                        )
                    )
                }
            }
            .setNeutralButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onPause() {
        super.onPause()
        adView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        adView.destroy()
    }
}
