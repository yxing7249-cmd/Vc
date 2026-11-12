package com.nexus.win12desktop.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nexus.win12desktop.NexusApplication
import com.nexus.win12desktop.R
import com.nexus.win12desktop.mouse.VirtualMouseManager
import com.nexus.win12desktop.utils.AppInfo
import com.nexus.win12desktop.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Desktop Activity - Windows 12 style launcher for Android TV.
 */
class DesktopActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private lateinit var wallpaperView: ImageView
    private lateinit var taskbar: LinearLayout
    private lateinit var startButton: ImageView
    private lateinit var clockText: TextView
    private lateinit var dateText: TextView
    private lateinit var desktopIconsContainer: GridLayout
    private lateinit var startMenuPanel: View
    private lateinit var appListRecycler: RecyclerView
    private lateinit var statusBar: LinearLayout

    private var mouseManager: VirtualMouseManager? = null
    private var isStartMenuOpen = false
    private val timeHandler = Handler(Looper.getMainLooper())
    private val timeRunnable = object : Runnable {
        override fun run() {
            updateClock()
            timeHandler.postDelayed(this, 1000)
        }
    }

    private var wallpaperAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )

        setContentView(R.layout.activity_desktop)

        bindViews()
        setupDesktop()
        setupTaskbar()
        setupStartMenu()
        setupMouse()
        startClock()
        startWallpaperAnimation()
    }

    private fun bindViews() {
        rootContainer = findViewById(R.id.root_container)
        wallpaperView = findViewById(R.id.wallpaper)
        taskbar = findViewById(R.id.taskbar)
        startButton = findViewById(R.id.start_button)
        clockText = findViewById(R.id.clock_text)
        dateText = findViewById(R.id.date_text)
        desktopIconsContainer = findViewById(R.id.desktop_icons)
        startMenuPanel = findViewById(R.id.start_menu_panel)
        appListRecycler = findViewById(R.id.app_list_recycler)
        statusBar = findViewById(R.id.status_bar)
    }

    private fun setupDesktop() {
        val apps = AppUtils.getLaunchableApps(this)
        desktopIconsContainer.removeAllViews()

        apps.take(12).forEachIndexed { index, app ->
            val iconView = LayoutInflater.from(this)
                .inflate(R.layout.item_desktop_icon, desktopIconsContainer, false)

            val iconImage = iconView.findViewById<ImageView>(R.id.icon_image)
            val iconLabel = iconView.findViewById<TextView>(R.id.icon_label)

            iconImage.setImageDrawable(app.icon)
            iconLabel.text = app.label

            iconView.setOnClickListener { launchApp(app) }
            iconView.setOnLongClickListener {
                showAppContextMenu(app)
                true
            }

            val params = GridLayout.LayoutParams().apply {
                width = resources.getDimensionPixelSize(R.dimen.desktop_icon_size)
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(index % 6)
                rowSpec = GridLayout.spec(index / 6)
                setMargins(24, 24, 24, 24)
            }
            desktopIconsContainer.addView(iconView, params)
        }
    }

    private fun setupTaskbar() {
        startButton.setOnClickListener { toggleStartMenu() }
        taskbar.alpha = 0.92f
    }

    private fun setupStartMenu() {
        startMenuPanel.visibility = View.GONE
        isStartMenuOpen = false

        val apps = AppUtils.getLaunchableApps(this)
        appListRecycler.layoutManager = GridLayoutManager(this, 4)
        appListRecycler.adapter = AppListAdapter(apps) { app ->
            closeStartMenu()
            launchApp(app)
        }

        findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            closeStartMenu()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View>(R.id.btn_power)?.setOnClickListener {
            Toast.makeText(this, "\u957f\u6309\u7535\u6e90\u952e\u6216\u4f7f\u7528\u7cfb\u7edf\u8bbe\u7f6e\u5173\u673a", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMouse() {
        mouseManager = VirtualMouseManager(this, rootContainer).apply {
            listener = object : VirtualMouseManager.MouseListener {
                override fun onLeftClick(x: Float, y: Float) {}
                override fun onRightClick(x: Float, y: Float) {}
                override fun onMouseMove(x: Float, y: Float) {}
            }
            isEnabled = NexusApplication.getPrefs(this@DesktopActivity)
                .getBoolean("mouse_enabled", true)
        }
    }

    private fun startClock() {
        updateClock()
        timeHandler.post(timeRunnable)
    }

    private fun updateClock() {
        val now = Date()
        clockText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText.text = SimpleDateFormat("MM\u6708dd\u65e5 EEEE", Locale.CHINA).format(now)
    }

    private fun startWallpaperAnimation() {
        wallpaperAnimator = ObjectAnimator.ofFloat(wallpaperView, "scaleX", 1.0f, 1.05f).apply {
            duration = 20000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(wallpaperView, "scaleY", 1.0f, 1.05f).apply {
            duration = 20000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun toggleStartMenu() {
        if (isStartMenuOpen) closeStartMenu() else openStartMenu()
    }

    private fun openStartMenu() {
        startMenuPanel.visibility = View.VISIBLE
        startMenuPanel.alpha = 0f
        startMenuPanel.translationY = 40f
        startMenuPanel.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(220)
            .start()
        isStartMenuOpen = true
    }

    private fun closeStartMenu() {
        startMenuPanel.animate()
            .alpha(0f)
            .translationY(40f)
            .setDuration(180)
            .withEndAction { startMenuPanel.visibility = View.GONE }
            .start()
        isStartMenuOpen = false
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Toast.makeText(this, "\u65e0\u6cd5\u542f\u52a8 ${app.label}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "\u542f\u52a8\u5931\u8d25: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppContextMenu(app: AppInfo) {
        val options = arrayOf("\u6253\u5f00", "\u5e94\u7528\u4fe1\u606f", "\u5378\u8f7d")
        AlertDialog.Builder(this, R.style.Theme_NexusWin12_Dialog)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(app)
                    1 -> openAppInfo(app.packageName)
                    2 -> uninstallApp(app.packageName)
                }
            }
            .show()
    }

    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "\u65e0\u6cd5\u6253\u5f00\u5e94\u7528\u4fe1\u606f", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "\u65e0\u6cd5\u5378\u8f7d", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyDown(keyCode, event)

        mouseManager?.let {
            if (it.isEnabled && it.onKeyDown(keyCode, event)) {
                return true
            }
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> true
            KeyEvent.KEYCODE_BACK -> {
                if (isStartMenuOpen) {
                    closeStartMenu()
                    true
                } else {
                    Toast.makeText(this, "\u5df2\u5728\u684c\u9762\uff0c\u6309 Home \u952e\u53ef\u8fd4\u56de", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            KeyEvent.KEYCODE_MENU -> {
                toggleStartMenu()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyUp(keyCode, event)
        mouseManager?.let {
            if (it.isEnabled && it.onKeyUp(keyCode, event)) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        mouseManager?.updateScreenSize()
        setupDesktop()
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeRunnable)
        wallpaperAnimator?.cancel()
        mouseManager?.destroy()
    }

    override fun onBackPressed() {
        if (isStartMenuOpen) {
            closeStartMenu()
        } else {
            Toast.makeText(this, "NEXUS WIN12 \u684c\u9762\u8fd0\u884c\u4e2d", Toast.LENGTH_SHORT).show()
        }
    }
}

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val label: TextView = view.findViewById(R.id.app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.itemView.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = apps.size
}
