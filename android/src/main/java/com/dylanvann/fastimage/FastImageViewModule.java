package com.dylanvann.fastimage;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;

class FastImageViewModule extends ReactContextBaseJavaModule {

    private static final String REACT_CLASS = "FastImageView";

    FastImageViewModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @ReactMethod
    public void preload(final ReadableArray sources) {
        final Activity activity = getCurrentActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < sources.size(); i++) {
                    final ReadableMap source = sources.getMap(i);
                    final String url = FastImageViewConverter.getURL(source);
                    Picasso.with(activity.getApplicationContext()).load(url).fetch();
                }
            }
        });
    }

    @ReactMethod
    public void setImage(String localPath, String cacheKey) {
        final Activity activity = getCurrentActivity();
        final File fileHandle = new File(localPath);
        Picasso.with(activity.getApplicationContext()).load(fileHandle).stableKey(cacheKey).into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                Log.i("FASTIMAGE", "setImage bitmap loaded!");
            }

            @Override
            public void onBitmapFailed(Drawable drawable) {
                Log.i("FASTIMAGE", "setImage bitmap failed!");
            }

            @Override
            public void onPrepareLoad(Drawable drawable) {
                Log.i("FASTIMAGE", "setImage bitmap prepare loaded!");

            }
        });
    }
}
