package org.mariotaku.twidere.activity

import android.content.Context
import android.content.Intent
import android.net.Uri

import org.mariotaku.pickncrop.library.ImagePickerActivity
import org.mariotaku.twidere.util.RestFuNetworkStreamDownloader

class ThemedImagePickerActivity : ImagePickerActivity() {

    override fun onStart() {
        super.onStart()
        setVisible(true)
    }

    class ThemedIntentBuilder(private val context: Context) {
        private val intentBuilder: ImagePickerActivity.IntentBuilder

        init {
            this.intentBuilder = ImagePickerActivity.IntentBuilder(context)
            intentBuilder.cropImageActivityClass(ImageCropperActivity::class.java)
            intentBuilder.streamDownloaderClass(RestFuNetworkStreamDownloader::class.java)
        }

        fun takePhoto(): ThemedIntentBuilder {
            intentBuilder.takePhoto()
            return this
        }

        fun getImage(uri: Uri): ThemedIntentBuilder {
            intentBuilder.getImage(uri)
            return this
        }

        fun build(): Intent {
            val intent = intentBuilder.build()
            intent.setClass(context, ThemedImagePickerActivity::class.java)
            return intent
        }

        fun pickImage(): ThemedIntentBuilder {
            intentBuilder.pickImage()
            return this
        }

        fun addEntry(name: String, value: String, result: Int): ThemedIntentBuilder {
            intentBuilder.addEntry(name, value, result)
            return this
        }

        fun maximumSize(w: Int, h: Int): ThemedIntentBuilder {
            intentBuilder.maximumSize(w, h)
            return this
        }

        fun aspectRatio(x: Int, y: Int): ThemedIntentBuilder {
            intentBuilder.aspectRatio(x, y)
            return this
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {

        fun withThemed(context: Context): ThemedIntentBuilder {
            return ThemedIntentBuilder(context)
        }
    }
}
