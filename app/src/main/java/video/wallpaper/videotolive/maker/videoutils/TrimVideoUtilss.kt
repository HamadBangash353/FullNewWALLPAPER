package video.wallpaper.videotolive.maker.videoutils

import android.net.Uri
import android.util.Log
import androidx.annotation.NonNull
import video.wallpaper.videotolive.maker.`interface`.OnTrimVideoListener
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object TrimVideoUtils {

    private val TAG = TrimVideoUtils::class.java.simpleName

    @Throws(IOException::class)
    fun startTrim(
        @NonNull src: File,
        @NonNull dst: String,
        startMs: Long,
        endMs: Long,
        @NonNull callback: OnTrimVideoListener?
    ) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//        val fileName = "MP4_$timeStamp.mp4"
        val fileName = "wallpaper.mp4"
        val filePath = dst+fileName
        val file = File(filePath)
        file.parentFile.mkdirs()
        Log.d(TAG, "File PAth: $dst")
        Log.d(TAG, "Generated file path $filePath")
        if (src != null) {
            try {
                genVideoUsingMp4Parser(src, file, startMs, endMs, callback)
                Log.d("Start time","Generatinh")

            } catch (e2: Exception){
                print("out of memory")
            }
        }
    }

    //    @Throws(IOException::class)
    private fun genVideoUsingMp4Parser(@NonNull src: File, @NonNull dst: File, startMs: Long, endMs: Long,
        @NonNull callback: OnTrimVideoListener?) {
        // NOTE: Switched to using FileDataSourceViaHeapImpl since it does not use memory mapping (VM).
        // Otherwise we get OOM with large movie files.
        try {
            val movie = MovieCreator.build(FileDataSourceViaHeapImpl(src.toString()))
            val tracks = movie.tracks
            movie.tracks = LinkedList()
            // remove all tracks we will create new tracks from the old
            var startTime1 = (startMs / 1000).toDouble()
            var endTime1 = (endMs / 1000).toDouble()
            Log.d("Start time",startTime1.toString())
            Log.d("Start time",endTime1.toString())
            var timeCorrected = false

            // Here we try to find a track that has sync samples. Since we can only start decoding
            // at such a sample we SHOULD make sure that the start of the new fragment is exactly
            // such a frame
            for (track in tracks) {
                if (track.syncSamples != null && track.syncSamples.size > 0) {
                    if (timeCorrected) {
                        // This exception here could be a false positive in case we have multiple tracks
                        // with sync samples at exactly the same positions. E.g. a single movie containing
                        // multiple qualities of the same video (Microsoft Smooth Streaming file)
                        throw RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.")
                    }
                    startTime1 = correctTimeToSyncSample(track, startTime1, false)
                    endTime1 = correctTimeToSyncSample(track, endTime1, true)
                    timeCorrected = true
                }
            }
            for (track in tracks) {
                var currentSample: Long = 0
                var currentTime = 0.0
                var lastTime = -1.0
                var startSample1: Long = -1
                var endSample1: Long = -1
                for (i in track.sampleDurations.indices) {
                    val delta = track.sampleDurations[i]
                    if (currentTime > lastTime && currentTime <= startTime1) {
                        // current sample is still before the new starttime
                        startSample1 = currentSample
                    }
                    if (currentTime > lastTime && currentTime <= endTime1) {
                        // current sample is after the new start time and still before the new endtime
                        endSample1 = currentSample
                    }
                    lastTime = currentTime
                    currentTime += delta.toDouble() / track.trackMetaData.timescale.toDouble()
                    currentSample++
                }
                movie.addTrack(AppendTrack(CroppedTrack(track, startSample1, endSample1)))
            }
            dst.parentFile.mkdirs()
            if (!dst.exists()) {
                dst.createNewFile()
            }
            val out = DefaultMp4Builder().build(movie)
            val fos = FileOutputStream(dst)
            val fc = fos.channel
            out.writeContainer(fc)
            fc.close()
            fos.close()
            if (callback != null) callback.onFinishedTrimming(Uri.parse(dst.toString()))
        } catch (e: IOException) {
            Log.d("Log", e.message.toString())
        }
    }

    private fun correctTimeToSyncSample(
        @NonNull track: Track,
        cutHere: Double,
        next: Boolean
    ): Double {
        val timeOfSyncSamples = DoubleArray(track.syncSamples.size)
        var currentSample: Long = 0
        var currentTime = 0.0
        for (i in track.sampleDurations.indices) {
            val delta = track.sampleDurations[i]
            if (Arrays.binarySearch(track.syncSamples, currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.syncSamples, currentSample + 1)] =
                    currentTime
            }
            currentTime += delta.toDouble() / track.trackMetaData.timescale.toDouble()
            currentSample++
        }
        var previous = 0.0
        for (timeOfSyncSample in timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                return if (next) {
                    timeOfSyncSample
                } else {
                    previous
                }
            }
            previous = timeOfSyncSample
        }
        return timeOfSyncSamples[timeOfSyncSamples.size - 1]
    }

    fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val mFormatter = Formatter()
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }
}