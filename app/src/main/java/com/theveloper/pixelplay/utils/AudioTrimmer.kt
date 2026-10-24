package com.theveloper.pixelplay.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(UnstableApi::class)
object AudioTrimmer {

    private const val TAG = "AudioTrimmer"

    /**
     * Trims the audio file referenced by [sourceUri] from [startMs] to [endMs]
     * and saves the output to [destinationFile].
     *
     * First attempts fast lossless extraction with MediaExtractor + MediaMuxer.
     * If the source codec is incompatible with MediaMuxer, falls back to Media3 Transformer.
     */
    suspend fun trimAudio(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
        startMs: Long,
        endMs: Long
    ): Result<File> = withContext(Dispatchers.IO) {
        require(startMs >= 0) { "startMs must be non-negative" }
        require(endMs > startMs) { "endMs must be greater than startMs" }

        // Attempt 1: Fast direct stream copy with MediaExtractor + MediaMuxer
        val fastResult = runCatching {
            trimWithMuxer(context, sourceUri, destinationFile, startMs, endMs)
        }

        if (fastResult.isSuccess) {
            scanDestinationFile(context, destinationFile)
            return@withContext fastResult
        }

        Timber.tag(TAG).w(fastResult.exceptionOrNull(), "Fast muxer trimming failed, falling back to Media3 Transformer")

        // Attempt 2: Universal transcoding via Media3 Transformer
        val transformerResult = runCatching {
            trimWithTransformer(context, sourceUri, destinationFile, startMs, endMs)
        }

        if (transformerResult.isSuccess) {
            scanDestinationFile(context, destinationFile)
        }

        transformerResult
    }

    private fun trimWithMuxer(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
        startMs: Long,
        endMs: Long
    ): File {
        val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
            ?: throw IllegalArgumentException("Cannot open file descriptor for $sourceUri")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            pfd.use { descriptor ->
                extractor.setDataSource(descriptor.fileDescriptor)
                val numTracks = extractor.trackCount
                var audioTrackIndex = -1
                var audioFormat: MediaFormat? = null

                for (i in 0 until numTracks) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME)
                    if (mime != null && mime.startsWith("audio/")) {
                        audioTrackIndex = i
                        audioFormat = format
                        break
                    }
                }

                if (audioTrackIndex < 0 || audioFormat == null) {
                    throw IllegalStateException("No audio track found in $sourceUri")
                }

                extractor.selectTrack(audioTrackIndex)
                val startUs = startMs * 1000L
                val endUs = endMs * 1000L
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

                destinationFile.parentFile?.mkdirs()
                if (destinationFile.exists()) destinationFile.delete()

                muxer = MediaMuxer(destinationFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val muxerTrackIndex = muxer.addTrack(audioFormat)
                muxer.start()

                val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
                } else {
                    256 * 1024
                }
                val buffer = ByteBuffer.allocateDirect(maxBufferSize)
                val bufferInfo = MediaCodec.BufferInfo()

                var sampleCount = 0
                while (true) {
                    buffer.clear()
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    val sampleTimeUs = extractor.sampleTime
                    if (sampleTimeUs > endUs) break

                    if (sampleTimeUs >= startUs) {
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.flags = extractor.sampleFlags
                        bufferInfo.presentationTimeUs = (sampleTimeUs - startUs).coerceAtLeast(0L)

                        muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                        sampleCount++
                    }

                    if (!extractor.advance()) break
                }

                if (sampleCount == 0) {
                    throw IllegalStateException("No audio samples extracted in specified time range")
                }
            }
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
        }

        return destinationFile
    }

    private suspend fun trimWithTransformer(
        context: Context,
        sourceUri: Uri,
        destinationFile: File,
        startMs: Long,
        endMs: Long
    ): File = suspendCancellableCoroutine { continuation ->
        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(endMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(clippingConfig)
            .build()

        val editedItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveVideo(true)
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) continuation.resume(destinationFile)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (continuation.isActive) continuation.resumeWithException(exportException)
                }
            })
            .build()

        Handler(Looper.getMainLooper()).post {
            try {
                destinationFile.parentFile?.mkdirs()
                if (destinationFile.exists()) destinationFile.delete()
                transformer.start(editedItem, destinationFile.absolutePath)
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }

        continuation.invokeOnCancellation {
            Handler(Looper.getMainLooper()).post {
                runCatching { transformer.cancel() }
            }
        }
    }

    private fun scanDestinationFile(context: Context, destinationFile: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(destinationFile.absolutePath),
            arrayOf("audio/mp4", "audio/m4a"),
            null
        )
    }
}
