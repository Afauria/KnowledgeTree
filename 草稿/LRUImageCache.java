package com.cvte.tv.launcher.mosaix.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;

import java.io.InputStream;
import java.lang.ref.SoftReference;

/**
 *
 * @功能 LRU算法海报默认图片内存缓存
 */
public class LRUImageCache {

    private LRUImageCache() {
    }

    private static LRUImageCache single = null;

    public synchronized static LRUImageCache getInstance() {
        if (single == null) {
            single = new LRUImageCache();
        }
        return single;
    }

    private LruCache<String, SoftReference<Bitmap>> mMemoryCache;
    private Context mContext;

    public void init(Context ctx) {
        mContext = ctx;
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int mCacheSize = maxMemory / 8;
        Log.i("InspireX", "LRUImageCache------>CacheSize:" + mCacheSize);
        mMemoryCache = new LruCache<String, SoftReference<Bitmap>>(mCacheSize) {
            @Override
            protected int sizeOf(String key, SoftReference<Bitmap> value) {
                return super.sizeOf(key, value);
            }
        };
    }

    /**
     * 获取图片资源
     *
     * @param resId
     * @return
     */
    public Bitmap getBitmap(int resId) {
        try {
            String key = String.valueOf(resId);
            SoftReference<Bitmap> softReference = mMemoryCache.get(key);
            if (softReference == null || softReference.get() == null) {
                SoftReference<Bitmap> reference = new SoftReference<Bitmap>(readBitMap(mContext, resId, Bitmap.Config.RGB_565));
                mMemoryCache.put(key, reference);
            }
            return mMemoryCache.get(key).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 保存图片到缓存
     *
     * @param key
     * @param drawable
     * @return
     */
    public void saveBitmap(String key, Drawable drawable) {
        try {
            if (drawable == null) return;
            SoftReference<Bitmap> softReference = mMemoryCache.get(key);
            if (softReference == null || softReference.get() == null) {
                Bitmap bitmap = drawableToBitmap(drawable);
                SoftReference<Bitmap> reference = new SoftReference<Bitmap>(bitmap);
                mMemoryCache.put(key, reference);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 适配Android 8.0 Drawable To Bitmap
     *
     * @return
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if(drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }
        Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    /**
     * 通过key获取图片缓存
     *
     * @param key
     * @return
     */
    public Bitmap getBitmap(String key) {
        SoftReference<Bitmap> softReference = mMemoryCache.get(key);
        if (null != softReference)
            return softReference.get();
        return null;
    }

    /**
     * 获取图片资源
     *
     * @param resId
     * @param config default:Bitmap.Config.RGB_565
     * @return
     */
    public Bitmap getBitmap(int resId, Bitmap.Config config) {
        try {
            String key = String.valueOf(resId);
            SoftReference<Bitmap> softReference = mMemoryCache.get(key);
            if (softReference == null || softReference.get() == null) {
                SoftReference<Bitmap> reference = new SoftReference<Bitmap>(readBitMap(mContext, resId, config));
                mMemoryCache.put(key, reference);
            }
            return mMemoryCache.get(key).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过压缩图片大小
     *
     * @param context
     * @param resId
     * @param config
     * @return
     */
    private static Bitmap readBitMap(Context context, int resId, Bitmap.Config config) {
        try {
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = config == null ? Bitmap.Config.RGB_565 : config;
            opt.inPurgeable = true;
            opt.inInputShareable = true;
            InputStream is = context.getResources().openRawResource(resId);
            return BitmapFactory.decodeStream(is, null, opt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
