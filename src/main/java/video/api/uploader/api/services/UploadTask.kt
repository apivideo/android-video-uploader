package video.api.uploader.api.services

import android.util.Log
import video.api.uploader.api.models.Video
import video.api.uploader.api.upload.UploadProgressListener
import java.io.File
import java.io.InterruptedIOException
import java.util.concurrent.Callable

class UploadTask(
    private val id: String,
    private val file: File,
    private val onUploadTask: (File, UploadProgressListener) -> Video,
    private val listener: UploadServiceListener
) : Callable<Video> {
    companion object {
        private const val TAG = "UploadTask"
    }

    override fun call(): Video {
        return try {
            listener.onUploadStarted(id)
            val video =
                onUploadTask(file) { bytesWritten, totalBytes, _, _ ->
                    val progress = (bytesWritten.toFloat() / totalBytes * 100).toInt()
                    listener.onUploadProgress(id, progress)
                }
            listener.onUploadComplete(video)
            video
        } catch (e: Exception) {
            if (e.rootCause is InterruptedIOException) {
                Log.i(TAG, "Interrupt by user")
                listener.onUploadCancelled(id)
            } else {
                listener.onUploadError(id, e)
            }
            throw e
        }
    }
}