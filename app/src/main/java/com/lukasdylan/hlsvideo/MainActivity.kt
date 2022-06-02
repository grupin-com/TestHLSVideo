package com.lukasdylan.hlsvideo

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.lukasdylan.hlsvideo.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null
    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeExoPlayer()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.apply {
            prepare()
            play()
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        releaseExoPlayer()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeExoPlayer() {
        if (exoPlayer == null) {
            val trackSelector = DefaultTrackSelector(this)
            exoPlayer = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
            binding.floatingVideoPlayer.player = exoPlayer
            exoPlayer?.apply {
                val factory = DefaultHlsDataSourceFactory(DefaultHttpDataSource.Factory())
                setMediaSource(
                    HlsMediaSource.Factory(factory)
                        .createMediaSource(MediaItem.fromUri("https://multiplatform-f.akamaihd.net/i/multi/april11/sintel/sintel-hd_,512x288_450_b,640x360_700_b,768x432_1000_b,1024x576_1400_m,.mp4.csmil/master.m3u8"))
                )
                prepare()
            }
            binding.floatingVideoPlayer.setOnTouchListener { view, motionEvent ->
                val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                Log.d("Touch Action", "Action ${motionEvent.action}")
                return@setOnTouchListener when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = motionEvent.rawX
                        downRawY = motionEvent.rawY
                        dX = view.x - downRawX
                        dY = view.y - downRawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val upRawX = motionEvent.rawX
                        val upRawY = motionEvent.rawY

                        val upDX: Float = upRawX - downRawX
                        val upDY: Float = upRawY - downRawY
                        val viewParent = view.parent as View
                        val parentWidth = viewParent.width

                        return@setOnTouchListener if (abs(upDX) < 10f && abs(upDY) < 10f) {
                            view.performClick()
                        } else {
                            val currentX = view.x + (view.width / 2)
                            val halfParentWidth =
                                (parentWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2
                            if (currentX < halfParentWidth) {
                                view.animate()
                                    .x(layoutParams.leftMargin.toFloat())
                                    .setDuration(0)
                                    .start()
                            } else {
                                view.animate()
                                    .x((parentWidth - view.width - layoutParams.rightMargin).toFloat())
                                    .setDuration(0)
                                    .start()
                            }
                            true
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val viewWidth = view.width
                        val viewHeight = view.height

                        val viewParent = view.parent as View
                        val parentWidth = viewParent.width
                        val parentHeight = viewParent.height

                        var newX: Float = motionEvent.rawX + dX
                        newX = layoutParams.leftMargin.toFloat().coerceAtLeast(newX)
                        newX = (parentWidth - viewWidth - layoutParams.rightMargin).toFloat()
                            .coerceAtMost(newX)

                        var newY: Float = motionEvent.rawY + dY
                        newY = layoutParams.topMargin.toFloat().coerceAtLeast(newY)
                        newY = (parentHeight - viewHeight - layoutParams.bottomMargin).toFloat()
                            .coerceAtMost(newY)

                        view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()

                        true
                    }
                    else -> super.onTouchEvent(motionEvent)
                }
            }
        }
    }

    private fun releaseExoPlayer() {
        exoPlayer?.release()
        exoPlayer = null
        binding.floatingVideoPlayer.player = null
    }
}