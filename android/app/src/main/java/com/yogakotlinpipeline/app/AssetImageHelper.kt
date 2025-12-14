package com.yogakotlinpipeline.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import java.io.IOException

object AssetImageHelper {
    
    // ADDED: LruCache for bitmap recycling to prevent memory leaks
    private val bitmapCache = LruCache<String, BitmapDrawable>(20) // Max 20 images
    
    fun loadImageFromAssets(context: Context, fileName: String): Drawable? {
        // ADDED: Check cache first to avoid repeated bitmap creation
        bitmapCache.get(fileName)?.let { return it }
        
        return try {
            val inputStream = context.assets.open(fileName)
            
            // ADDED: Get image dimensions without loading full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // ADDED: Calculate sampling to reduce memory usage
            val sampleSize = calculateInSampleSize(options, 512, 512) // Max 512x512
            
            // ADDED: Load bitmap with sampling
            val inputStream2 = context.assets.open(fileName)
            val options2 = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options2)
            inputStream2.close()
            
            if (bitmap != null) {
                val drawable = BitmapDrawable(context.resources, bitmap)
                bitmapCache.put(fileName, drawable) // ADDED: Cache the drawable
                drawable
            } else {
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    // ADDED: Calculate appropriate sample size to reduce memory usage
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            val inputStream = context.assets.open(fileName)
            
            // ADDED: Get image dimensions without loading full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            // ADDED: Calculate sampling to reduce memory usage
            val sampleSize = calculateInSampleSize(options, 512, 512) // Max 512x512
            
            // ADDED: Load bitmap with sampling
            val inputStream2 = context.assets.open(fileName)
            val options2 = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
            }
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, options2)
            inputStream2.close()
            bitmap
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    
    // ADDED: Method to clear cache when memory is low
    fun clearCache() {
        bitmapCache.evictAll()
    }
}

