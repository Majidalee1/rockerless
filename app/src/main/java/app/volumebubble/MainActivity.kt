package app.volumebubble

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Setup and settings screen. Follows the system light/dark theme and dynamic colors.
 */
class MainActivity : Activity() {

    private lateinit var palette: Palette
    private lateinit var permissionStatus: TextView
    private lateinit var permissionButton: TextView
    private lateinit var primaryButton: TextView
    private lateinit var hint: TextView
    private lateinit var opacityValue: TextView
    private lateinit var autoHideSwitch: Switch
    private lateinit var hero: BubbleView
    private var heroWatcher: Volume.Watcher? = null
    private val sizeChips = LinkedHashMap<Int, TextView>()

    private val sizes = listOf("Small" to 48, "Medium" to 56, "Large" to 64)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        palette = Palette.of(this)
        setContentView(buildUi())
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        heroWatcher = Volume.Watcher(this) { updateHero() }.also { it.start() }
    }

    override fun onPause() {
        heroWatcher?.stop()
        heroWatcher = null
        super.onPause()
    }

    private fun updateHero() {
        hero.setLevel(Volume.level(this).toFloat() / Volume.max(this), Volume.isMuted(this))
    }

    // ---------------------------------------------------------------- layout

    private fun buildUi(): View {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(24)
            setPadding(pad, pad, pad, pad)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(Ui.text(this@MainActivity, getString(R.string.app_name), 34f, palette.onSurface, Typeface.BOLD))
                addView(
                    Ui.text(
                        this@MainActivity,
                        "A floating volume control for a phone without working volume buttons.",
                        14f,
                        palette.onSurfaceDim,
                    ).apply {
                        setLineSpacing(dpf(3f), 1f)
                        setPadding(0, dp(6), 0, 0)
                    },
                )
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        hero = BubbleView(this)
        header.addView(hero, LinearLayout.LayoutParams(dp(104), dp(104)))
        column.addView(
            header,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(24) },
        )

        // --- permission
        permissionStatus = Ui.text(this, "", 13f, palette.onSurfaceDim, Typeface.BOLD)
        permissionButton = actionButton("Allow", primary = false) { openOverlaySettings() }
        val permissionCard = card()
        permissionCard.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        addView(Ui.text(this@MainActivity, "Draw over other apps", 15f, palette.onSurface, Typeface.BOLD))
                        addView(permissionStatus, wrapParams(top = 4))
                    },
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
                )
                addView(permissionButton, LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT))
            },
        )
        column.addView(permissionCard, cardParams())

        // --- primary action
        primaryButton = actionButton("Turn on bubble", primary = true) { toggleBubble() }
        column.addView(primaryButton, cardParams(top = 16))

        hint = Ui.text(this, "", 12.5f, palette.onSurfaceDim).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        column.addView(hint, cardParams())

        // --- appearance
        val appearance = card()
        appearance.addView(sectionTitle("Appearance"))

        appearance.addView(label("Size"), wrapParams(top = 12))
        val chipRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((name, sizeDp) in sizes) {
            val chip = chip(name) {
                Prefs.setSizeDp(this, sizeDp)
                reloadBubble()
                refresh()
            }
            sizeChips[sizeDp] = chip
            chipRow.addView(
                chip,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(6)
                },
            )
        }
        appearance.addView(chipRow, wrapParams(top = 8))

        val opacityRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        opacityValue = Ui.text(this, "", 13f, palette.onSurface, Typeface.BOLD)
        opacityRow.addView(
            label("Opacity"),
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        opacityRow.addView(opacityValue)
        appearance.addView(opacityRow, wrapParams(top = 16))

        val opacity = WavySlider(this).apply {
            min = 30
            max = 100
            value = Prefs.opacityPct(this@MainActivity)
            setColors(palette.ring, palette.onSurface.withAlpha(0x33), palette.ring)
            onValueChange = { opacityValue.text = "$it%" }
            onRelease = {
                Prefs.setOpacityPct(this@MainActivity, value)
                reloadBubble()
            }
        }
        opacityValue.text = "${Prefs.opacityPct(this)}%"
        appearance.addView(opacity, wrapParams(top = 4))

        autoHideSwitch = Switch(this).apply {
            text = "Hide when idle"
            setTextColor(palette.onSurface)
            setOnCheckedChangeListener { _, checked ->
                if (Prefs.autoHide(this@MainActivity) != checked) {
                    Prefs.setAutoHide(this@MainActivity, checked)
                    reloadBubble()
                }
            }
        }
        appearance.addView(autoHideSwitch, wrapParams(top = 16))
        appearance.addView(
            Ui.text(
                this,
                "After 3 seconds the bubble slides to the screen edge. Tap the sliver to bring it back.",
                12.5f,
                palette.onSurfaceDim,
            ).apply {
                setLineSpacing(dpf(3f), 1f)
                setPadding(0, dp(4), 0, 0)
            },
        )
        column.addView(appearance, cardParams(top = 24))

        // --- gestures
        val gestures = card()
        gestures.addView(sectionTitle("How it works"))
        gestures.addView(gestureLine("Tap", "open the system volume panel"))
        gestures.addView(gestureLine("Drag", "move the bubble; it snaps to the edge"))
        gestures.addView(gestureLine("Long-press", "mute or unmute media"))
        column.addView(gestures, cardParams(top = 16))

        // --- battery
        val battery = card()
        battery.addView(sectionTitle("Keep it running"))
        battery.addView(
            Ui.text(
                this,
                "Android may stop background services to save power. Set this app to unrestricted " +
                    "so the bubble survives a full day.",
                13f,
                palette.onSurfaceDim,
            ).apply {
                setLineSpacing(dpf(3f), 1f)
                setPadding(0, dp(6), 0, dp(14))
            },
        )
        battery.addView(actionButton("Open battery settings", primary = false) { openBatterySettings() })
        column.addView(battery, cardParams(top = 16))

        return ScrollView(this).apply {
            fitsSystemWindows = true
            setBackgroundColor(palette.surface)
            addView(column)
        }
    }

    // --------------------------------------------------------------- helpers

    private fun dp(v: Number): Int = Ui.dp(this, v)

    private fun dpf(v: Number): Float = Ui.dpf(this, v)

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.card(this@MainActivity, 28f, palette.card)
        val pad = dp(18)
        setPadding(pad, pad, pad, pad)
    }

    private fun cardParams(top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun wrapParams(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun sectionTitle(text: String): TextView =
        Ui.text(this, text, 15f, palette.onSurface, Typeface.BOLD)

    private fun label(text: String): TextView =
        Ui.text(this, text, 13f, palette.onSurfaceDim, Typeface.BOLD)

    private fun gestureLine(gesture: String, description: String): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            addView(
                Ui.text(this@MainActivity, gesture, 13.5f, palette.ring, Typeface.BOLD).apply {
                    minWidth = dp(96)
                },
            )
            addView(Ui.text(this@MainActivity, description, 13.5f, palette.onSurfaceDim))
        }

    private fun actionButton(label: String, primary: Boolean, onClick: () -> Unit): TextView =
        Ui.text(this, label, 14f, palette.onSurface, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(16), dp(18), dp(16))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            styleButton(this, primary)
            Motion.springPress(this)
        }

    private fun styleButton(view: TextView, primary: Boolean) {
        view.setTextColor(if (primary) palette.surface else palette.onSurface)
        view.background = Ui.ripple(
            Ui.pill(this, if (primary) palette.ring else palette.container),
            palette.onSurface.withAlpha(0x26),
        )
    }

    private fun chip(text: String, onClick: () -> Unit): TextView =
        Ui.text(this, text, 13f, palette.onSurface, Typeface.BOLD).apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(10), dp(10), dp(10))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            Motion.springPress(this)
        }

    private fun styleChip(view: TextView, selected: Boolean) {
        view.setTextColor(if (selected) palette.onContainer else palette.onSurfaceDim)
        view.background = Ui.ripple(
            Ui.pill(this, if (selected) palette.container else palette.surface),
            palette.onSurface.withAlpha(0x26),
        )
    }

    // ------------------------------------------------------------------ state

    private fun refresh() {
        val canDraw = BubbleService.canDrawOverlays(this)
        val running = canDraw && BubbleService.isEnabled(this)

        permissionStatus.text = if (canDraw) "Allowed" else "Not allowed yet"
        permissionStatus.setTextColor(if (canDraw) palette.ring else palette.muted)
        permissionButton.visibility = if (canDraw) View.GONE else View.VISIBLE

        primaryButton.text = if (running) "Turn off bubble" else "Turn on bubble"
        styleButton(primaryButton, primary = !running)

        hint.text = when {
            running -> "The bubble is on. Drag it wherever you like."
            !canDraw -> "Allow drawing over other apps to continue."
            else -> "Turn the bubble on to control volume from anywhere."
        }

        val currentSize = Prefs.sizeDp(this)
        for ((sizeDp, chip) in sizeChips) styleChip(chip, sizeDp == currentSize)

        autoHideSwitch.isChecked = Prefs.autoHide(this)
    }

    private fun reloadBubble() {
        BubbleService.reload(this)
    }

    private fun toggleBubble() {
        if (!BubbleService.canDrawOverlays(this)) {
            openOverlaySettings()
            return
        }
        if (BubbleService.isEnabled(this)) {
            BubbleService.stop(this)
        } else {
            BubbleService.start(this)
        }
        // The service attaches its window asynchronously; give it a beat before re-reading state.
        primaryButton.postDelayed({ refresh() }, 250)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun openBatterySettings() {
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }
}
