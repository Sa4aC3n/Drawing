package com.art.drawingkids

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chipGroup: ChipGroup
    private lateinit var cardDailyChallenge: MaterialCardView
    private lateinit var txtDailyChallenge: TextView
    private lateinit var imgChallengeIcon: ImageView
    private lateinit var adView: AdView
    private lateinit var repository: DrawingLessonRepository
    private lateinit var challengeManager: DailyChallengeManager
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
        challengeManager = DailyChallengeManager(repository)

        // Setup Daily Challenge Card
        cardDailyChallenge = findViewById(R.id.cardDailyChallenge)
        txtDailyChallenge = findViewById(R.id.txtDailyChallenge)
        imgChallengeIcon = findViewById(R.id.imgChallengeIcon)

        val todayChallenge = challengeManager.getTodayChallenge()
        if (todayChallenge != null) {
            txtDailyChallenge.text = getString(R.string.daily_challenge_banner, todayChallenge.name)
            imgChallengeIcon.setImageResource(todayChallenge.iconResId)
            cardDailyChallenge.setOnClickListener {
                openDrawingActivity(todayChallenge.imageArrayKey)
            }
        }

        recyclerView = findViewById(R.id.recyclerView_main)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val initialLessons = repository.getLessons(CategoryGroup.ALL)
        adapter = DrawingAdapter(initialLessons) { category ->
            openDrawingActivity(category.imageArrayKey)
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

    private fun openDrawingActivity(imageArrayKey: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCategoryClickTime > 600) {
            lastCategoryClickTime = now
            val intent = Intent(this, DrawingActivity::class.java).apply {
                putExtra("Image_Array", imageArrayKey)
            }
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_gallery -> {
                startActivity(Intent(this, MyGalleryActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
