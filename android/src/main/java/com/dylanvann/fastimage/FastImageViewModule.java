package com.dylanvann.fastimage;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.MediaStoreSignature;
import com.bumptech.glide.signature.StringSignature;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class FastImageViewModule extends ReactContextBaseJavaModule {

    private static final String REACT_CLASS = "FastImageView";

    FastImageViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    private static Drawable TRANSPARENT_DRAWABLE = new ColorDrawable(Color.TRANSPARENT);

    @ReactMethod
    public void preload(final ReadableArray sources) {
        final Activity activity = getCurrentActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            for (int i = 0; i < sources.size(); i++) {
                final ReadableMap source = sources.getMap(i);
                final FastImageUrl url = FastImageViewConverter.fastImageUrl(source);
                final Priority priority = FastImageViewConverter.priority(source);
                Glide
                    .with(activity.getApplicationContext())
                    .load(url)
                    .priority(priority)
                    .placeholder(TRANSPARENT_DRAWABLE)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .preload();
            }
            }
        });
    }

    /**
     * Moves the file at `localPath` to the key `remoteUrl`.
     *
     * @param localPath
     * @param remoteUrl
     */
    @ReactMethod
    public void setImage(final String localPath, final String remoteUrl) {
        final Activity activity = getCurrentActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            Glide.with(activity.getApplicationContext())
                    .load(new FastImageUrl(remoteUrl, localPath))
                    .priority(Priority.HIGH)
                    .placeholder(TRANSPARENT_DRAWABLE)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .preload();
            }
        });
    }
}
