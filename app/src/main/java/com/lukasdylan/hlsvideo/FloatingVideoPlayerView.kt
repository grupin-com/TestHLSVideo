package com.lukasdylan.hlsvideo

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.lukasdylan.hlsvideo.databinding.ViewFloatingVideoPlayerBinding
import kotlin.math.abs

class FloatingVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding =
        ViewFloatingVideoPlayerBinding.inflate(LayoutInflater.from(context), this, true)

    private val ivAudio
        get() = binding.floatingVideoPlayer.findViewById<AppCompatImageView>(R.id.iv_audio)

    private val ivFullScreen
        get() = binding.floatingVideoPlayer.findViewById<AppCompatImageView>(R.id.iv_fullscreen)

    private var exoPlayer: ExoPlayer? = null
    private var dX = 0f
    private var dY = 0f
    private var downRawX = 0f
    private var downRawY = 0f
    private var isFloatingPlayerOnLeft = false
    private var isFullScreenPlayer = false
    private var videoUrl = ""

    fun initializePlayer(url: String, isAutoRepeat: Boolean) {
        if (exoPlayer == null) {
            videoUrl = url
            exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(DefaultTrackSelector(context))
                .build()
            exoPlayer?.apply {
                val factory = DefaultHlsDataSourceFactory(DefaultHttpDataSource.Factory())
                setMediaSource(
                    HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(url))
                )
                repeatMode = if (isAutoRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = 0f
            }
            binding.floatingVideoPlayer.controllerAutoShow = false
            binding.floatingVideoPlayer.controllerShowTimeoutMs = 300
            binding.floatingVideoPlayer.player = exoPlayer
            background = ContextCompat.getDrawable(context, R.drawable.bg_black_opaque)

            binding.floatingVideoPlayer.setOnTouchListener { _, motionEvent ->
                Log.d("MotionEvent", "${motionEvent.action}")
                val layoutParams = layoutParams as MarginLayoutParams
                return@setOnTouchListener when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downRawX = motionEvent.rawX
                        downRawY = motionEvent.rawY
                        dX = x - downRawX
                        dY = y - downRawY
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val upRawX = motionEvent.rawX
                        val upRawY = motionEvent.rawY

                        val upDX: Float = upRawX - downRawX
                        val upDY: Float = upRawY - downRawY
                        val viewParent = parent as View
                        val parentWidth = viewParent.width

                        return@setOnTouchListener if (abs(upDX) < 10f && abs(upDY) < 10f) {
                            if (binding.floatingVideoPlayer.isControllerFullyVisible) {
                                binding.floatingVideoPlayer.hideController()
                            } else {
                                binding.floatingVideoPlayer.showController()
                            }
                            performClick()
                        } else {
                            val currentX = x + (width / 2)
                            val halfParentWidth =
                                (parentWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2
                            isFloatingPlayerOnLeft = if (currentX < halfParentWidth) {
                                animate()
                                    .x(layoutParams.leftMargin.toFloat())
                                    .setDuration(0)
                                    .start()
                                true
                            } else {
                                animate()
                                    .x((parentWidth - width - layoutParams.rightMargin).toFloat())
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
                        val viewWidth = width
                        val viewHeight = height

                        val viewParent = parent as View
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

                        animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()

                        true
                    }
                    else -> super.onTouchEvent(motionEvent)
                }
            }

            binding.ivClose.setOnClickListener {
                if (isFullScreenPlayer) {
                    updatePlayerVideoSize(false)
                } else {
                    background = null
                    binding.floatingVideoPlayer.isInvisible = true
                    binding.ivClose.isVisible = false
                    binding.ivShowPlayer.isVisible = true
                    exoPlayer?.pause()
                }
            }

            binding.ivShowPlayer.setOnClickListener {
                background = ContextCompat.getDrawable(context, R.drawable.bg_black_opaque)
                binding.ivShowPlayer.isVisible = false
                binding.floatingVideoPlayer.isVisible = true
                binding.ivClose.isVisible = true
                exoPlayer?.run {
                    prepare()
                    play()
                }
            }

            ivAudio?.setOnClickListener {
                val imageIcon = if (exoPlayer?.volume == 0f) {
                    exoPlayer?.volume = 1f
                    R.drawable.vector_audio_off
                } else {
                    exoPlayer?.volume = 0f
                    R.drawable.vector_audio_on
                }
                ivAudio?.setImageDrawable(ContextCompat.getDrawable(context, imageIcon))
            }

            ivFullScreen?.setOnClickListener {
                updatePlayerVideoSize(!isFullScreenPlayer)
            }
        }
    }

    fun onResume() {
        exoPlayer?.run {
            prepare()
            play()
        }
    }

    fun onPause() {
        exoPlayer?.pause()
    }

    fun onDestroy() {
        exoPlayer?.release()
        exoPlayer = null
        binding.floatingVideoPlayer.player = null
    }

    fun minimizeOnBackPressed(): Boolean {
        if (isFullScreenPlayer) {
            updatePlayerVideoSize(false)
            return true
        }
        return false
    }

    fun seekTo(latestMillis: Long) {
        exoPlayer?.seekTo(latestMillis)
    }

    private fun updateImageViewShowPlayerPosition() {
        val ivShowPlayerLayoutParams = (binding.ivShowPlayer.layoutParams as LayoutParams)
        ivShowPlayerLayoutParams.gravity = if (isFloatingPlayerOnLeft) {
            Gravity.CENTER or Gravity.START
        } else {
            Gravity.CENTER or Gravity.END
        }
        binding.ivShowPlayer.layoutParams = ivShowPlayerLayoutParams
    }

    private fun updatePlayerVideoSize(isFullScreen: Boolean = false) {
//        isFullScreenPlayer = isFullScreen
//        val framePlayerLayoutParams = (layoutParams as ViewGroup.LayoutParams)
//        if (isFullScreen) {
//            framePlayerLayoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
//            framePlayerLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
//        } else {
//            framePlayerLayoutParams.width = TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                90f,
//                resources.displayMetrics
//            ).roundToInt()
//            framePlayerLayoutParams.height = TypedValue.applyDimension(
//                TypedValue.COMPLEX_UNIT_DIP,
//                160f,
//                resources.displayMetrics
//            ).roundToInt()
//        }
//        layoutParams = framePlayerLayoutParams
//
//        background = if (isFullScreen) {
//            ContextCompat.getDrawable(context, R.drawable.bg_black)
//        } else {
//            ContextCompat.getDrawable(context, R.drawable.bg_black_opaque)
//        }
//
//        val imageIcon = if (isFullScreen) {
//            R.drawable.vector_minimize
//        } else {
//            R.drawable.vector_fullscreen
//        }
//        ivFullScreen?.setImageDrawable(ContextCompat.getDrawable(context, imageIcon))
//
//        binding.floatingVideoPlayer.hideController()
        val intent = Intent(context, FullScreenVideoActivity::class.java).apply {
            putExtra("URL", videoUrl)
            putExtra("IS_AUTO_REPEAT", true)
            putExtra("LATEST_MILLIS", exoPlayer?.currentPosition ?: 0L)
        }
        (context as? AppCompatActivity)?.startActivityForResult(intent, 0)
    }
}