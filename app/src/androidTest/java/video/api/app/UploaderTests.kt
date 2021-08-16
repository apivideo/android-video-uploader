package video.api.app

import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import video.api.videouploader_module.UploaderRequestExecutorImpl
import video.api.videouploader_module.ApiError
import video.api.videouploader_module.CallBack
import video.api.videouploader_module.VideoUploader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.IllegalArgumentException
import java.net.URI
import java.util.concurrent.CountDownLatch

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class UploaderTests {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val client = OkHttpClient();


    class TestSuccessCallBack(private val signal: CountDownLatch) : CallBack {
        override fun onError(apiError: ApiError) {
            assertNull(apiError);
            signal.countDown();
        }

        override fun onFatal(e: IOException) {
            assertNull(e);
            signal.countDown();
        }

        override fun onSuccess(result: JSONObject) {
            assertNotNull(result.getString("videoId"));
            signal.countDown();
        }
    }

    @Test
    fun invalidChunkSize() {
        try {
            val videoUploader = VideoUploader(
                "https://ws.api.video",
                UploaderRequestExecutorImpl(client),
                client,
                1024 * 1024 * 4
            )
            assertNull(videoUploader)
        } catch(e: IllegalArgumentException) {
            assertTrue(true)
        }

        try {
            val videoUploader = VideoUploader(
                "https://ws.api.video",
                UploaderRequestExecutorImpl(client),
                client,
                1024 * 1024 * 130
            )
            assertNull(videoUploader)
        } catch(e: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun uploadSmallFile() {
        val signal = CountDownLatch(1)
        val uploader = VideoUploader("https://ws.api.video", UploaderRequestExecutorImpl(client), client);
        uploader.uploadWithDelegatedToken("to4UcvhqOJLLE3NBRBl7TtGJ", File(assetToFIle("574k.mp4")), TestSuccessCallBack(signal))
        signal.await();
    }

    @Test
    fun uploadBigFile() {
        val signal = CountDownLatch(1)
        val uploader = VideoUploader("https://ws.api.video", UploaderRequestExecutorImpl(client), client, 1024*1024*5);
        uploader.uploadWithDelegatedToken("to4UcvhqOJLLE3NBRBl7TtGJ", File(assetToFIle("10m.mp4")), TestSuccessCallBack(signal))
        signal.await();
    }

    private fun assetToFIle(assetName: String): URI? {
        val f = File(ContextCompat.getDataDir(appContext).toString() + "/" + assetName)
        try {
            val inputStream: InputStream = appContext.getAssets().open(assetName)
            val buffer = ByteArray(1024)
            val fos = FileOutputStream(f)
            do {
                var read = inputStream.read(buffer)
                fos.write(buffer)
            } while (read != -1)
            inputStream.close()
            fos.close()
            return f.toURI();
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        return null;
    }
}