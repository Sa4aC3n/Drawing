package com.art.drawingkids

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.io.File
import java.io.FileOutputStream

class DrawingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var txtStep: TextView
    private lateinit var btnBack: Button
    private lateinit var btnNext: Button
    private lateinit var relDraw: RelativeLayout
    private lateinit var imgColorPick: ImageView
    private lateinit var imgPencil: ImageView
    private lateinit var imgStroke: ImageView
    private lateinit var imgEraser: ImageView
    private lateinit var imgClear: ImageView
    private lateinit var imgSave: ImageView
    private lateinit var adView: AdView
    private lateinit var paintView: PaintView

    private var totalImages: Int = 0
    private var strokeWidth: Float = 6f
    private var imageArrayKey: String = "image_car"

    // Click debounce timestamps
    private var lastNavClickTime: Long = 0
    private var isSavingOrSharing: Boolean = false

    // Menu references for Undo / Redo
    private var undoMenuItem: MenuItem? = null
    private var redoMenuItem: MenuItem? = null

    /** Map from key → array of drawable resource IDs */
    private val imageArrays: Map<String, IntArray> by lazy { buildImageArrays() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawing)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        imageArrayKey = intent.getStringExtra("Image_Array") ?: "image_car"

        adView = findViewById(R.id.adView)
        adView.loadAd(AdRequest.Builder().build())

        viewPager = findViewById(R.id.view_pager)
        txtStep = findViewById(R.id.textView_stepsize)
        btnBack = findViewById(R.id.button_prev)
        btnNext = findViewById(R.id.button_next)
        relDraw = findViewById(R.id.rell)
        imgColorPick = findViewById(R.id.imageView2_color)
        imgStroke = findViewById(R.id.imageView4_stroke)
        imgClear = findViewById(R.id.imageView5_clear)
        imgEraser = findViewById(R.id.imageView3_eraser)
        imgPencil = findViewById(R.id.imageView1_pencil)
        imgSave = findViewById(R.id.imageView1_save)

        // Set up custom drawing canvas
        paintView = PaintView(this)
        paintView.togglePencil(true)
        relDraw.addView(
            paintView,
            RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
            )
        )

        paintView.onHistoryChangedListener = { canUndo, canRedo ->
            undoMenuItem?.isEnabled = canUndo
            undoMenuItem?.icon?.alpha = if (canUndo) 255 else 100
            redoMenuItem?.isEnabled = canRedo
            redoMenuItem?.icon?.alpha = if (canRedo) 255 else 100
        }

        imgPencil.setImageResource(R.drawable.brush_active_btn)

        // Category step-by-step images
        val images = imageArrays[imageArrayKey] ?: imageArrays["image_car"]!!
        totalImages = images.size - 1

        val pagerAdapter = ImagePagerAdapter(images)
        viewPager.adapter = pagerAdapter
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                txtStep.text = "$position/$totalImages"
            }
        })
        txtStep.text = "0/$totalImages"

        // Set dynamic title
        supportActionBar?.title = imageArrayKey
            .removePrefix("image_")
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }

        // Setup back button press dispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExitIfNeeded()
            }
        })

        // ───────── UI Listeners ─────────

        btnBack.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastNavClickTime < 250) return@setOnClickListener
            lastNavClickTime = SystemClock.elapsedRealtime()
            val current = viewPager.currentItem
            val next = if (current > 0) current - 1 else totalImages
            viewPager.setCurrentItem(next, true)
        }

        btnNext.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastNavClickTime < 250) return@setOnClickListener
            lastNavClickTime = SystemClock.elapsedRealtime()
            val current = viewPager.currentItem
            val next = if (current < totalImages) current + 1 else 0
            viewPager.setCurrentItem(next, true)
        }

        imgColorPick.setOnClickListener { showColorPickerDialog() }

        imgStroke.setOnClickListener { showStrokeDialog() }

        imgClear.setOnClickListener { promptClearCanvas() }

        imgEraser.setOnClickListener {
            paintView.togglePencil(false)
            imgEraser.setImageResource(R.drawable.erase_active_btn)
            imgPencil.setImageResource(R.drawable.brush_btn)
        }

        imgPencil.setOnClickListener {
            paintView.togglePencil(true)
            imgPencil.setImageResource(R.drawable.brush_active_btn)
            imgEraser.setImageResource(R.drawable.erase_btn)
        }

        imgSave.setOnClickListener {
            if (!isSavingOrSharing) {
                saveDrawing()
            }
        }
    }

    // ───────── Exit Confirmation ─────────

    private fun confirmExitIfNeeded() {
        if (paintView.hasDrawnStrokes()) {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.leave_prompt_title))
                .setMessage(getString(R.string.leave_prompt_message))
                .setPositiveButton(getString(R.string.save)) { _, _ ->
                    saveDrawing { success ->
                        if (success) finish()
                    }
                }
                .setNeutralButton(getString(R.string.discard)) { _, _ ->
                    finish()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        } else {
            finish()
        }
    }

    // ───────── Menu Handling ─────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_drawing, menu)
        undoMenuItem = menu.findItem(R.id.action_undo)
        redoMenuItem = menu.findItem(R.id.action_redo)

        // Initialize menu item states
        undoMenuItem?.isEnabled = paintView.canUndo()
        undoMenuItem?.icon?.alpha = if (paintView.canUndo()) 255 else 100
        redoMenuItem?.isEnabled = paintView.canRedo()
        redoMenuItem?.icon?.alpha = if (paintView.canRedo()) 255 else 100

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                confirmExitIfNeeded()
                true
            }
            R.id.action_undo -> {
                paintView.undo()
                true
            }
            R.id.action_redo -> {
                paintView.redo()
                true
            }
            R.id.Share -> {
                if (!isSavingOrSharing) {
                    shareDrawing()
                }
                true
            }
            R.id.RateApp -> {
                rateApp()
                true
            }
            R.id.About -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ───────── Color Picker ─────────

    private fun showColorPickerDialog() {
        val dialog = ColorPickerDialog(this, paintView.getColor()) { color ->
            paintView.setDrawColor(color)
            imgPencil.setImageResource(R.drawable.brush_active_btn)
            imgEraser.setImageResource(R.drawable.erase_btn)
        }
        dialog.show()
    }

    // ───────── Stroke Size Dialog ─────────

    private fun showStrokeDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        val layout = layoutInflater.inflate(R.layout.dialog_stroke, null)
        val seekBar: SeekBar = layout.findViewById(R.id.dialog_seekbar)
        val strokeText: TextView = layout.findViewById(R.id.stroke_text)
        val okButton: Button = layout.findViewById(R.id.dialog_button)

        val currentProgress = (strokeWidth * 2).toInt().coerceIn(4, 100)
        seekBar.max = 100
        seekBar.progress = currentProgress
        strokeText.text = strokeWidth.toInt().toString()

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val value = (progress / 2).coerceAtLeast(2)
                strokeText.text = value.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        okButton.setOnClickListener {
            val selectedWidth = (seekBar.progress / 2).coerceAtLeast(2).toFloat()
            strokeWidth = selectedWidth
            paintView.setStrokeWidth(strokeWidth)
            dialog.dismiss()
        }

        dialog.setContentView(layout)
        dialog.show()
    }

    // ───────── Clear Confirmation ─────────

    private fun promptClearCanvas() {
        if (!paintView.hasDrawnStrokes()) {
            Toast.makeText(this, getString(R.string.empty_canvas_warning), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_confirm_title))
            .setMessage(getString(R.string.clear_confirm_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                paintView.clear()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ───────── Verified Save ─────────

    private fun saveDrawing(onComplete: ((Boolean) -> Unit)? = null) {
        if (!paintView.hasDrawnStrokes()) {
            Toast.makeText(this, getString(R.string.empty_canvas_warning), Toast.LENGTH_SHORT).show()
            onComplete?.invoke(false)
            return
        }

        isSavingOrSharing = true
        imgSave.isEnabled = false

        Thread {
            val bitmap = paintView.exportBitmap()
            val savedUri = performSaveBitmap(bitmap)

            runOnUiThread {
                isSavingOrSharing = false
                imgSave.isEnabled = true
                if (savedUri != null) {
                    Toast.makeText(this, getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(true)
                } else {
                    Toast.makeText(this, getString(R.string.save_error), Toast.LENGTH_LONG).show()
                    onComplete?.invoke(false)
                }
            }
        }.start()
    }

    private fun performSaveBitmap(bitmap: Bitmap): Uri? {
        val fileName = "Drawing_${imageArrayKey}_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+) MediaStore API
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DrawingForKids")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            var uri: Uri? = null
            try {
                uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                        stream.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uri?.let { contentResolver.delete(it, null, null) }
                return null
            }
            uri
        } else {
            // Android 9 and below
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "DrawingForKids").apply { mkdirs() }
            val file = File(appDir, fileName)

            try {
                FileOutputStream(file).use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                    stream.flush()
                }
                if (file.exists() && file.length() > 0) {
                    val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    scanIntent.data = Uri.fromFile(file)
                    sendBroadcast(scanIntent)
                    Uri.fromFile(file)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to internal app pictures directory
                try {
                    val fallbackDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: filesDir
                    val fallbackFile = File(fallbackDir, fileName)
                    FileOutputStream(fallbackFile).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                        stream.flush()
                    }
                    if (fallbackFile.exists() && fallbackFile.length() > 0) {
                        Uri.fromFile(fallbackFile)
                    } else null
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    null
                }
            }
        }
    }

    // ───────── Verified Share ─────────

    private fun shareDrawing() {
        if (!paintView.hasDrawnStrokes()) {
            Toast.makeText(this, getString(R.string.empty_canvas_warning), Toast.LENGTH_SHORT).show()
            return
        }

        isSavingOrSharing = true

        Thread {
            val bitmap = paintView.exportBitmap()
            val cacheFolder = File(cacheDir, "share_cache").apply { mkdirs() }
            val shareFile = File(cacheFolder, "drawing_share.jpg")

            var writeSuccess = false
            try {
                FileOutputStream(shareFile).use { fOut ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut)
                    fOut.flush()
                }
                writeSuccess = shareFile.exists() && shareFile.length() > 0
            } catch (e: Exception) {
                e.printStackTrace()
            }

            runOnUiThread {
                isSavingOrSharing = false
                if (writeSuccess) {
                    try {
                        val shareUri = FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider",
                            shareFile
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(this, getString(R.string.sharing_error), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, getString(R.string.sharing_error), Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun rateApp() {
        val appPackageName = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (e: android.content.ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    // ───────── Image Arrays ─────────

    private fun buildImageArrays(): Map<String, IntArray> = mapOf(
        "image_car" to intArrayOf(
            R.drawable.car_1, R.drawable.car_2, R.drawable.car_3, R.drawable.car_4,
            R.drawable.car_5, R.drawable.car_6, R.drawable.car_7, R.drawable.car_8,
            R.drawable.car_9, R.drawable.car_10, R.drawable.car_11, R.drawable.car_12,
            R.drawable.car_13, R.drawable.blank
        ),
        "image_house" to intArrayOf(
            R.drawable.house_1, R.drawable.house_2, R.drawable.house_3, R.drawable.house_4,
            R.drawable.house_5, R.drawable.house_6, R.drawable.house_7, R.drawable.house_8,
            R.drawable.house_9, R.drawable.house_10, R.drawable.house_11, R.drawable.house_12,
            R.drawable.house_13, R.drawable.house_14, R.drawable.house_15, R.drawable.house_16,
            R.drawable.blank
        ),
        "image_spider" to intArrayOf(
            R.drawable.spider_1, R.drawable.spider_2, R.drawable.spider_3, R.drawable.spider_4,
            R.drawable.spider_5, R.drawable.spider_6, R.drawable.spider_7, R.drawable.spider_8,
            R.drawable.spider_9, R.drawable.spider_10, R.drawable.spider_11, R.drawable.spider_12,
            R.drawable.spider_13, R.drawable.spider_14, R.drawable.blank
        ),
        "image_gun" to intArrayOf(
            R.drawable.gun_1, R.drawable.gun_2, R.drawable.gun_3, R.drawable.gun_4,
            R.drawable.gun_5, R.drawable.gun_6, R.drawable.gun_7, R.drawable.gun_8,
            R.drawable.gun_9, R.drawable.gun_10, R.drawable.blank
        ),
        "image_boat" to intArrayOf(
            R.drawable.boat_1, R.drawable.boat_2, R.drawable.boat_3, R.drawable.boat_4,
            R.drawable.boat_5, R.drawable.boat_6, R.drawable.boat_7, R.drawable.boat_8,
            R.drawable.boat_9, R.drawable.boat_10, R.drawable.boat_11, R.drawable.boat_12,
            R.drawable.boat_13, R.drawable.blank
        ),
        "image_bird" to intArrayOf(
            R.drawable.bird_1, R.drawable.bird_2, R.drawable.bird_3, R.drawable.bird_4,
            R.drawable.bird_5, R.drawable.bird_6, R.drawable.bird_7, R.drawable.bird_8,
            R.drawable.bird_9, R.drawable.bird_10, R.drawable.bird_11, R.drawable.blank
        ),
        "image_elephant" to intArrayOf(
            R.drawable.elephant_1, R.drawable.elephant_2, R.drawable.elephant_3, R.drawable.elephant_4,
            R.drawable.elephant_5, R.drawable.elephant_6, R.drawable.elephant_7, R.drawable.elephant_8,
            R.drawable.elephant_9, R.drawable.elephant_10, R.drawable.elephant_11, R.drawable.elephant_12,
            R.drawable.elephant_13, R.drawable.elephant_14, R.drawable.elephant_15, R.drawable.elephant_16,
            R.drawable.elephant_17, R.drawable.elephant_18, R.drawable.elephant_19, R.drawable.elephant_20,
            R.drawable.elephant_21, R.drawable.elephant_22, R.drawable.blank
        ),
        "image_flower" to intArrayOf(
            R.drawable.flower_1, R.drawable.flower_2, R.drawable.flower_3, R.drawable.flower_4,
            R.drawable.flower_5, R.drawable.flower_6, R.drawable.flower_7, R.drawable.flower_8,
            R.drawable.flower_9, R.drawable.flower_10, R.drawable.blank
        ),
        "image_frog" to intArrayOf(
            R.drawable.frog_1, R.drawable.frog_2, R.drawable.frog_3, R.drawable.frog_4,
            R.drawable.frog_5, R.drawable.frog_6, R.drawable.frog_7, R.drawable.frog_8,
            R.drawable.frog_9, R.drawable.frog_10, R.drawable.frog_11, R.drawable.frog_12,
            R.drawable.frog_13, R.drawable.frog_14, R.drawable.frog_15, R.drawable.frog_16,
            R.drawable.blank
        ),
        "image_horse" to intArrayOf(
            R.drawable.horse_1, R.drawable.horse_2, R.drawable.horse_3, R.drawable.horse_4,
            R.drawable.horse_5, R.drawable.horse_6, R.drawable.horse_7, R.drawable.horse_8,
            R.drawable.horse_9, R.drawable.horse_10, R.drawable.horse_11, R.drawable.horse_12,
            R.drawable.horse_13, R.drawable.horse_14, R.drawable.horse_15, R.drawable.blank
        ),
        "image_lion" to intArrayOf(
            R.drawable.lion_1, R.drawable.lion_2, R.drawable.lion_3, R.drawable.lion_4,
            R.drawable.lion_5, R.drawable.lion_6, R.drawable.lion_7, R.drawable.lion_8,
            R.drawable.lion_9, R.drawable.lion_10, R.drawable.lion_11, R.drawable.lion_12,
            R.drawable.lion_13, R.drawable.blank
        ),
        "image_mickey_mouse" to intArrayOf(
            R.drawable.mickey_1, R.drawable.mickey_2, R.drawable.mickey_3, R.drawable.mickey_4,
            R.drawable.mickey_5, R.drawable.mickey_6, R.drawable.mickey_7, R.drawable.mickey_8,
            R.drawable.mickey_9, R.drawable.mickey_10, R.drawable.mickey_11, R.drawable.mickey_12,
            R.drawable.mickey_13, R.drawable.mickey_14, R.drawable.mickey_15, R.drawable.mickey_16,
            R.drawable.blank
        ),
        "image_dog" to intArrayOf(
            R.drawable.dog_1, R.drawable.dog_2, R.drawable.dog_3, R.drawable.dog_4,
            R.drawable.dog_5, R.drawable.dog_6, R.drawable.dog_7, R.drawable.dog_8,
            R.drawable.dog_9, R.drawable.dog_10, R.drawable.dog_11, R.drawable.dog_12,
            R.drawable.dog_13, R.drawable.dog_14, R.drawable.dog_15, R.drawable.dog_16,
            R.drawable.dog_17, R.drawable.blank
        ),
        "image_turtle" to intArrayOf(
            R.drawable.turtle_1, R.drawable.turtle_2, R.drawable.turtle_3, R.drawable.turtle_4,
            R.drawable.turtle_5, R.drawable.turtle_6, R.drawable.turtle_7, R.drawable.turtle_8,
            R.drawable.turtle_9, R.drawable.turtle_10, R.drawable.turtle_11, R.drawable.turtle_12,
            R.drawable.turtle_13, R.drawable.turtle_14, R.drawable.turtle_15, R.drawable.turtle_16,
            R.drawable.turtle_17, R.drawable.turtle_18, R.drawable.turtle_19, R.drawable.turtle_20,
            R.drawable.turtle_21, R.drawable.turtle_22, R.drawable.turtle_23, R.drawable.turtle_24,
            R.drawable.turtle_25, R.drawable.blank
        ),
        "image_princess" to intArrayOf(
            R.drawable.princess_1, R.drawable.princess_2, R.drawable.princess_3, R.drawable.princess_4,
            R.drawable.princess_5, R.drawable.princess_6, R.drawable.princess_7, R.drawable.princess_8,
            R.drawable.princess_9, R.drawable.princess_10, R.drawable.princess_11, R.drawable.princess_12,
            R.drawable.princess_13, R.drawable.blank
        )
    )

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
