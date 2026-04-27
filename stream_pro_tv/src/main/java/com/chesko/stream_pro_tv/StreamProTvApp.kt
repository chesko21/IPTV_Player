package com.chesko.stream_pro_tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class StreamProTvApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05) // Larger disk cache for TV
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
