package com.hebrewclock

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var clockView: ClockView
    private lateinit var tvHourHebrew: TextView
    private lateinit var tvMinuteLabel: TextView
    private lateinit var tvTimeDisplay: TextView
    private lateinit var btnNow: View
    private lateinit var btnNowText: TextView
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var tvStars: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var tickerRunnable: Runnable? = null
    private var isLiveMode = true

    // Hebrew hour names (1-indexed, index 0 unused)
    private val hebrewHours = arrayOf(
        "",          // 0 unused
        "אחת",       // 1
        "שתיים",     // 2
        "שלוש",      // 3
        "ארבע",      // 4
        "חמש",       // 5
        "שש",        // 6
        "שבע",       // 7
        "שמונה",     // 8
        "תשע",       // 9
        "עשר",       // 10
        "אחת עשרה", // 11
        "שתים עשרה" // 12
    )

    // Hebrew minute descriptions for common fractions
    private fun hebrewMinuteText(minute: Int): String = when {
        minute == 0 -> "בדיוק"
        minute in 1..14 -> "ו-$minute דקות"
        minute == 15 -> "ורבע"
        minute in 16..29 -> "ו-$minute דקות"
        minute == 30 -> "וחצי"
        minute in 31..44 -> "ו-$minute דקות"
        minute == 45 -> "שלושה רבעים"
        else -> "ו-$minute דקות"
    }

    private fun fullHebrewTime(hour: Int, minute: Int): String {
        val h = if (hour == 0) 12 else hour
        val hourWord = hebrewHours[h]
        val minutePart = hebrewMinuteText(minute)
        return if (minute == 0) "שעה $hourWord!" else "שעה $hourWord $minutePart"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        clockView = findViewById(R.id.clockView)
        tvHourHebrew = findViewById(R.id.tvHourHebrew)
        tvMinuteLabel = findViewById(R.id.tvMinuteLabel)
        tvTimeDisplay = findViewById(R.id.tvTimeDisplay)
        btnNow = findViewById(R.id.btnNow)
        btnNowText = findViewById(R.id.btnNowText)
        rootLayout = findViewById(R.id.rootLayout)
        tvStars = findViewById(R.id.tvStars)

        // Start in live-clock mode
        setLiveMode()

        clockView.onTimeChangedListener = object : ClockView.OnTimeChangedListener {
            override fun onTimeChanged(hour: Int, minute: Int) {
                isLiveMode = false
                updateTimeText(hour, minute)
                spawnStarBurst()
            }
        }

        btnNow.setOnClickListener {
            setLiveMode()
            animateButton(it)
        }

        // Toddler delight: tap on the time text to bounce it
        tvHourHebrew.setOnClickListener { bounceView(it) }
        tvTimeDisplay.setOnClickListener { bounceView(it) }
    }

    private fun setLiveMode() {
        isLiveMode = true
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR)
        val minute = cal.get(Calendar.MINUTE)
        clockView.animateToTime(hour, minute)
        updateTimeText(hour, minute)
        startTicker()
    }

    private fun startTicker() {
        stopTicker()
        tickerRunnable = object : Runnable {
            override fun run() {
                if (isLiveMode) {
                    val cal = Calendar.getInstance()
                    val h = cal.get(Calendar.HOUR)
                    val m = cal.get(Calendar.MINUTE)
                    clockView.setTime(h, m)
                    updateTimeText(h, m)
                    handler.postDelayed(this, 15_000) // update every 15s
                }
            }
        }
        handler.postDelayed(tickerRunnable!!, 15_000)
    }

    private fun stopTicker() {
        tickerRunnable?.let { handler.removeCallbacks(it) }
        tickerRunnable = null
    }

    private fun updateTimeText(hour: Int, minute: Int) {
        val h = if (hour == 0) 12 else hour
        tvHourHebrew.text = hebrewHours[h]
        tvTimeDisplay.text = fullHebrewTime(hour, minute)

        // Minute label
        tvMinuteLabel.text = when {
            minute == 0 -> "o'clock"
            minute == 30 -> "חצי שעה"
            minute == 15 -> "רבע שעה"
            minute == 45 -> "שלושת רבעי שעה"
            else -> "$minute דקות"
        }

        // Change background accent color based on hour (fun for toddlers)
        val bgColor = hourBackgroundColor(hour)
        rootLayout.setBackgroundColor(bgColor)
    }

    private fun hourBackgroundColor(hour: Int): Int {
        val colors = listOf(
            "#1A1A2E", // midnight - deep navy
            "#16213E", // 1am
            "#0F3460", // 2am
            "#533483", // 3am
            "#E94560", // 4am - warm rose
            "#FF6B6B", // 5am - sunrise red
            "#FF8E53", // 6am - orange
            "#FFC300", // 7am - golden
            "#FFE66D", // 8am - bright yellow
            "#06D6A0", // 9am - fresh green
            "#118AB2", // 10am - sky blue
            "#073B4C", // 11am - teal
            "#264653", // noon
            "#2A9D8F", // 1pm
            "#57CC99", // 2pm - spring green
            "#80B918", // 3pm
            "#DDCC77", // 4pm - warm afternoon
            "#FF9F1C", // 5pm - golden hour
            "#FFBF69", // 6pm - sunset
            "#CB997E", // 7pm - dusk
            "#9B72CF", // 8pm - evening purple
            "#6D3FA5", // 9pm
            "#4A2FBD", // 10pm
            "#2D2D8B"  // 11pm
        )
        return Color.parseColor(colors[hour % 24])
    }

    private fun spawnStarBurst() {
        val emojis = listOf("⭐", "🌟", "✨", "🎉", "🎊")
        tvStars.text = emojis.random()
        tvStars.alpha = 0f
        tvStars.scaleX = 0.3f
        tvStars.scaleY = 0.3f
        tvStars.visibility = View.VISIBLE

        val fadeIn = ObjectAnimator.ofFloat(tvStars, "alpha", 0f, 1f).apply { duration = 200 }
        val scaleX = ObjectAnimator.ofFloat(tvStars, "scaleX", 0.3f, 1.4f, 1f).apply { duration = 400 }
        val scaleY = ObjectAnimator.ofFloat(tvStars, "scaleY", 0.3f, 1.4f, 1f).apply { duration = 400 }
        val fadeOut = ObjectAnimator.ofFloat(tvStars, "alpha", 1f, 0f).apply {
            duration = 300
            startDelay = 600
        }

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY, fadeOut)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    tvStars.visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun bounceView(v: View) {
        ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.15f, 1f).apply {
            duration = 300
            interpolator = BounceInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.15f, 1f).apply {
            duration = 300
            interpolator = BounceInterpolator()
            start()
        }
    }

    private fun animateButton(v: View) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.9f, 1.1f, 1f).apply { duration = 300 },
                ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.9f, 1.1f, 1f).apply { duration = 300 }
            )
            interpolator = OvershootInterpolator()
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isLiveMode) startTicker()
    }

    override fun onPause() {
        super.onPause()
        stopTicker()
    }
}
