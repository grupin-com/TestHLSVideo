package com.lukasdylan.hlsvideo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.lukasdylan.hlsvideo.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null
    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var isFloatingPlayerOnLeft = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeExoPlayer()
        binding.ivClose.setOnClickListener {
            lifecycleScope.launch {
                binding.floatingVideoPlayer.isInvisible = true
                binding.ivClose.isVisible = false
                binding.ivShowPlayer.isVisible = true
                exoPlayer?.pause()
            }
        }
        binding.ivShowPlayer.setOnClickListener {
            lifecycleScope.launch {
                binding.ivShowPlayer.isVisible = false
                binding.floatingVideoPlayer.isVisible = true
                binding.ivClose.isVisible = true
                exoPlayer?.apply {
                    prepare()
                    play()
                }
            }
        }
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
            exoPlayer = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
            binding.floatingVideoPlayer.player = exoPlayer
            exoPlayer?.apply {
                val factory = DefaultHlsDataSourceFactory(DefaultHttpDataSource.Factory())
                setMediaSource(
                    HlsMediaSource.Factory(factory)
                        .createMediaSource(MediaItem.fromUri("https://cdn3.wowza.com/2/SEdqNS9FTUYvYmFx/VDRZUTA3/hls/0spdjphd/playlist.m3u8"))
                )
            }
            binding.floatingVideoPlayer.setOnTouchListener { _, motionEvent ->
                val layoutParams = binding.frameLayout.layoutParams as ViewGroup.MarginLayoutParams
                return@setOnTouchListener when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = motionEvent.rawX
                        downRawY = motionEvent.rawY
                        dX = binding.frameLayout.x - downRawX
                        dY = binding.frameLayout.y - downRawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val upRawX = motionEvent.rawX
                        val upRawY = motionEvent.rawY

                        val upDX: Float = upRawX - downRawX
                        val upDY: Float = upRawY - downRawY
                        val viewParent = binding.frameLayout.parent as View
                        val parentWidth = viewParent.width

                        return@setOnTouchListener if (abs(upDX) < 10f && abs(upDY) < 10f) {
                            binding.frameLayout.performClick()
                        } else {
                            val currentX = binding.frameLayout.x + (binding.frameLayout.width / 2)
                            val halfParentWidth =
                                (parentWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2
                            isFloatingPlayerOnLeft = if (currentX < halfParentWidth) {
                                binding.frameLayout.animate()
                                    .x(layoutParams.leftMargin.toFloat())
                                    .setDuration(0)
                                    .start()
                                true
                            } else {
                                binding.frameLayout.animate()
                                    .x((parentWidth - binding.frameLayout.width - layoutParams.rightMargin).toFloat())
                                    .setDuration(0)
                                    .start()
                                false
                            }
                            binding.ivShowPlayer.rotation = if (isFloatingPlayerOnLeft) 180f else 0f
                            updateImageViewShowPlayerPosition()
                            true
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val viewWidth = binding.frameLayout.width
                        val viewHeight = binding.frameLayout.height

                        val viewParent = binding.frameLayout.parent as View
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

                        binding.frameLayout.animate()
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

    private fun updateImageViewShowPlayerPosition() {
        val ivShowPlayerLayoutParams = (binding.ivShowPlayer.layoutParams as FrameLayout.LayoutParams)
        if (isFloatingPlayerOnLeft) {
            ivShowPlayerLayoutParams.gravity = Gravity.CENTER or Gravity.START
        } else {
            ivShowPlayerLayoutParams.gravity = Gravity.CENTER or Gravity.END
        }
        binding.ivShowPlayer.layoutParams = ivShowPlayerLayoutParams
    }

    private fun releaseExoPlayer() {
        exoPlayer?.release()
        exoPlayer = null
        binding.floatingVideoPlayer.player = null
    }
}