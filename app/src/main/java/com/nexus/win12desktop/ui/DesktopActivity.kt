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
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nexus.win12desktop.NexusApplication
import com.nexus.win12desktop.R
import com.nexus.win12desktop.mouse.VirtualMouseManager
import com.nexus.win12desktop.utils.AppInfo
import com.nexus.win12desktop.utils.AppUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DesktopActivity : AppCompatActivity() {

    private lateinit var rootContainer: FrameLayout
    private var wallpaperView: ImageView? = null
    private var taskbar: LinearLayout? = null
    private var startButton: ImageView? = null
    private var clockText: TextView? = null
    private var dateText: TextView? = null
    private var desktopIconsContainer: GridLayout? = null
    private var startMenuPanel: View? = null
    private var appListRecycler: RecyclerView? = null
    private var statusBar: LinearLayout? = null
    private var dockApps: LinearLayout? = null
    private var dockControl: View? = null
    private var widgetsBtn: View? = null
    private var controlCenter: View? = null

    private var mouseManager: VirtualMouseManager? = null
    private var isStartMenuOpen = false
    private var isControlCenterOpen = false
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
        try {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            @Suppress("DEPRECATION")
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
            setupDock()
            setupStartMenu()
            setupMouse()
            startClock()
            startWallpaperAnimation()
        } catch (e: Exception) {
            Log.e("DesktopActivity", "onCreate failed", e)
            try {
                val tv = TextView(this).apply {
                    text = "NEXUS WIN12\nError: ${e.message}"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 18f
                    setPadding(48, 48, 48, 48)
                }
                setContentView(tv)
            } catch (_: Exception) {
            }
        }
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
        dockApps = findViewById(R.id.dock_apps)
        dockControl = findViewById(R.id.dock_control)
        widgetsBtn = findViewById(R.id.widgets_btn)
        controlCenter = findViewById(R.id.control_center)
    }

    private fun setupDesktop() {
        val container = desktopIconsContainer ?: return
        val apps = try { AppUtils.getLaunchableApps(this) } catch (e: Exception) {
            Log.e("DesktopActivity", "get apps failed", e); emptyList()
        }
        container.removeAllViews()
        val iconSize = try {
            resources.getDimensionPixelSize(R.dimen.desktop_icon_size)
        } catch (_: Exception) {
            (96 * resources.displayMetrics.density).toInt()
        }
        apps.take(16).forEachIndexed { index, app ->
            try {
                val iconView = LayoutInflater.from(this)
                    .inflate(R.layout.item_desktop_icon, container, false)
                iconView.findViewById<ImageView>(R.id.icon_image)?.setImageDrawable(app.icon)
                iconView.findViewById<TextView>(R.id.icon_label)?.text = app.label
                iconView.setOnClickListener { launchApp(app) }
                iconView.setOnLongClickListener { showAppContextMenu(app); true }
                val params = GridLayout.LayoutParams().apply {
                    width = iconSize
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(index % 8)
                    rowSpec = GridLayout.spec(index / 8)
                    setMargins(10, 12, 10, 12)
                }
                container.addView(iconView, params)
            } catch (e: Exception) {
                Log.e("DesktopActivity", "icon $index failed", e)
            }
        }
    }

    private fun setupDock() {
        startButton?.setOnClickListener { toggleStartMenu() }
        dockControl?.setOnClickListener { toggleControlCenter() }
        statusBar?.setOnClickListener { Toast.makeText(this, "Calendar", Toast.LENGTH_SHORT).show() }
        widgetsBtn?.setOnClickListener { Toast.makeText(this, "Widgets", Toast.LENGTH_SHORT).show() }

        val dock = dockApps ?: return
        dock.removeAllViews()
        try {
            AppUtils.getLaunchableApps(this).take(5).forEach { app ->
                val iv = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(40, 40).apply { marginStart = 2; marginEnd = 2 }
                    setPadding(6, 6, 6, 6)
                    setImageDrawable(app.icon)
                    setBackgroundResource(R.drawable.bg_dock_btn)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setOnClickListener { launchApp(app) }
                }
                dock.addView(iv)
            }
        } catch (e: Exception) {
            Log.e("DesktopActivity", "dock apps failed", e)
        }

        findViewById<View?>(R.id.cc_wifi)?.setOnClickListener { Toast.makeText(this, "WLAN", Toast.LENGTH_SHORT).show() }
        findViewById<View?>(R.id.cc_bt)?.setOnClickListener { Toast.makeText(this, "BT", Toast.LENGTH_SHORT).show() }
        findViewById<View?>(R.id.cc_flight)?.setOnClickListener { Toast.makeText(this, "Airplane", Toast.LENGTH_SHORT).show() }
        findViewById<View?>(R.id.cc_eye)?.setOnClickListener { Toast.makeText(this, "Eye care", Toast.LENGTH_SHORT).show() }
        findViewById<View?>(R.id.cc_hotspot)?.setOnClickListener { Toast.makeText(this, "Hotspot", Toast.LENGTH_SHORT).show() }
        findViewById<View?>(R.id.cc_a11y)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            closeControlCenter()
        }
    }

    private fun toggleControlCenter() {
        if (isControlCenterOpen) closeControlCenter() else openControlCenter()
    }

    private fun openControlCenter() {
        val panel = controlCenter ?: return
        if (isStartMenuOpen) closeStartMenu()
        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.translationY = 16f
        panel.animate().alpha(1f).translationY(0f).setDuration(160).start()
        isControlCenterOpen = true
    }

    private fun closeControlCenter() {
        val panel = controlCenter ?: return
        panel.animate().alpha(0f).translationY(12f).setDuration(120)
            .withEndAction { panel.visibility = View.GONE }.start()
        isControlCenterOpen = false
    }

    private fun setupStartMenu() {
        startMenuPanel?.visibility = View.GONE
        isStartMenuOpen = false
        val recycler = appListRecycler ?: return
        val apps = try { AppUtils.getLaunchableApps(this) } catch (_: Exception) { emptyList() }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = AppListAdapter(apps) { app ->
            closeStartMenu()
            launchApp(app)
        }
        findViewById<View?>(R.id.btn_settings)?.setOnClickListener {
            closeStartMenu()
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View?>(R.id.btn_power)?.setOnClickListener {
            Toast.makeText(this, "Use system power", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupMouse() {
        try {
            mouseManager = VirtualMouseManager(this, rootContainer).apply {
                listener = object : VirtualMouseManager.MouseListener {
                    override fun onLeftClick(x: Float, y: Float) {}
                    override fun onRightClick(x: Float, y: Float) {}
                    override fun onMouseMove(x: Float, y: Float) {}
                }
                isEnabled = NexusApplication.getPrefs(this@DesktopActivity)
                    .getBoolean("mouse_enabled", true)
            }
        } catch (e: Exception) {
            Log.e("DesktopActivity", "mouse setup failed", e)
        }
    }

    private fun startClock() {
        updateClock()
        timeHandler.post(timeRunnable)
    }

    private fun updateClock() {
        val now = Date()
        clockText?.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        dateText?.text = SimpleDateFormat("MM-dd", Locale.getDefault()).format(now)
    }

    private fun startWallpaperAnimation() {
        val wp = wallpaperView ?: return
        try {
            wallpaperAnimator = ObjectAnimator.ofFloat(wp, "scaleX", 1f, 1.04f).apply {
                duration = 25000; repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); start()
            }
            ObjectAnimator.ofFloat(wp, "scaleY", 1f, 1.04f).apply {
                duration = 25000; repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE; interpolator = LinearInterpolator(); start()
            }
        } catch (_: Exception) {}
    }

    private fun toggleStartMenu() {
        if (isStartMenuOpen) closeStartMenu() else openStartMenu()
    }

    private fun openStartMenu() {
        val panel = startMenuPanel ?: return
        if (isControlCenterOpen) closeControlCenter()
        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.translationY = 80f
        panel.animate().alpha(1f).translationY(0f).setDuration(280)
            .setInterpolator(DecelerateInterpolator()).start()
        isStartMenuOpen = true
    }

    private fun closeStartMenu() {
        val panel = startMenuPanel ?: return
        panel.animate().alpha(0f).translationY(40f).setDuration(180)
            .withEndAction { panel.visibility = View.GONE }.start()
        isStartMenuOpen = false
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else Toast.makeText(this, "Cannot launch ${app.label}", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Launch failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppContextMenu(app: AppInfo) {
        try {
            AlertDialog.Builder(this, R.style.Theme_NexusWin12_Dialog)
                .setTitle(app.label)
                .setItems(arrayOf("Open", "Info", "Uninstall")) { _, which ->
                    when (which) {
                        0 -> launchApp(app)
                        1 -> try {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${app.packageName}")
                            })
                        } catch (_: Exception) {}
                        2 -> try {
                            startActivity(Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${app.packageName}")
                            })
                        } catch (_: Exception) {}
                    }
                }.show()
        } catch (_: Exception) {}
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyDown(keyCode, event)
        mouseManager?.let { if (it.isEnabled && it.onKeyDown(keyCode, event)) return true }
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> true
            KeyEvent.KEYCODE_BACK -> {
                when {
                    isStartMenuOpen -> { closeStartMenu(); true }
                    isControlCenterOpen -> { closeControlCenter(); true }
                    else -> { Toast.makeText(this, "Desktop", Toast.LENGTH_SHORT).show(); true }
                }
            }
            KeyEvent.KEYCODE_MENU -> { toggleStartMenu(); true }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyUp(keyCode, event)
        mouseManager?.let { if (it.isEnabled && it.onKeyUp(keyCode, event)) return true }
        return super.onKeyUp(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        try {
            mouseManager?.updateScreenSize()
            setupDesktop()
            setupDock()
        } catch (e: Exception) {
            Log.e("DesktopActivity", "onResume failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeRunnable)
        wallpaperAnimator?.cancel()
        mouseManager?.destroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            isStartMenuOpen -> closeStartMenu()
            isControlCenterOpen -> closeControlCenter()
            else -> Toast.makeText(this, "NEXUS WIN12", Toast.LENGTH_SHORT).show()
        }
    }
}

class AppListAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView? = view.findViewById(R.id.app_icon)
        val label: TextView? = view.findViewById(R.id.app_label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_grid, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon?.setImageDrawable(app.icon)
        holder.label?.text = app.label
        holder.itemView.setOnClickListener { onClick(app) }
    }

    override fun getItemCount() = apps.size
}
