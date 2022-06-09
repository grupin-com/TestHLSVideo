package com.lukasdylan.hlsvideo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.DefaultHlsDataSourceFactory
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.lukasdylan.hlsvideo.databinding.ActivityFullScreenVideoBinding

class FullScreenVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullScreenVideoBinding

    private val ivClose: AppCompatImageView
        get() = binding.floatingVideoPlayer.findViewById(R.id.iv_close)

    private val ivAudio: AppCompatImageView
        get() = binding.floatingVideoPlayer.findViewById(R.id.iv_audio)

    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val url = intent?.getStringExtra("URL").orEmpty()
        val isAutoRepeat = intent?.getBooleanExtra("IS_AUTO_REPEAT", false) ?: false
        val latestMillisecond = intent?.getLongExtra("LATEST_MILLIS", 0L) ?: 0L
        initializeExoPlayer(url, isAutoRepeat, latestMillisecond)

        ivClose.setOnClickListener {
            setResult(RESULT_OK, Intent().apply {
                putExtra("LATEST_MILLIS", exoPlayer?.currentPosition ?: 0L)
            })
            finish()
        }

        ivAudio.setOnClickListener {
            val imageIcon = if (exoPlayer?.volume == 0f) {
                exoPlayer?.volume = 1f
                R.drawable.vector_audio_off
            } else {
                exoPlayer?.volume = 0f
                R.drawable.vector_audio_on
            }
            ivAudio.setImageDrawable(ContextCompat.getDrawable(this, imageIcon))
        }

        ivAudio.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.vector_audio_off))
    }

    override fun onBackPressed() {
        setResult(RESULT_OK, Intent().apply {
            putExtra("LATEST_MILLIS", exoPlayer?.currentPosition ?: 0L)
        })
        finish()
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
        exoPlayer?.release()
        exoPlayer = null
        binding.floatingVideoPlayer.player = null
        super.onDestroy()
    }

    private fun initializeExoPlayer(url: String, isAutoRepeat: Boolean, latestMillis: Long) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this)
                .setTrackSelector(DefaultTrackSelector(this))
                .build()
            exoPlayer?.apply {
                val factory = DefaultHlsDataSourceFactory(DefaultHttpDataSource.Factory())
                setMediaSource(
                    HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(url))
                )
                repeatMode = if (isAutoRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                prepare()
            }

            binding.floatingVideoPlayer.controllerAutoShow = false
            binding.floatingVideoPlayer.controllerShowTimeoutMs = 300
            binding.floatingVideoPlayer.player = exoPlayer
            exoPlayer?.seekTo(latestMillis)
        }
    }
}