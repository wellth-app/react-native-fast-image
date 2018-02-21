package com.dylanvann.fastimage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
class ImageViewWithUrl extends ImageView {
    public Uri imageURI;
    public ImageViewWithUrl(Context context) {
        super(context);
    }
}

class FastImageViewManager extends SimpleViewManager<ImageViewWithUrl> implements ProgressListener {

    private static final String REACT_CLASS = "FastImageView";
    private static final String REACT_ON_LOAD_START_EVENT = "onFastImageLoadStart";
    private static final String REACT_ON_PROGRESS_EVENT = "onFastImageProgress";
    private static final String REACT_ON_ERROR_EVENT = "onFastImageError";
    private static final String REACT_ON_LOAD_EVENT = "onFastImageLoad";
    private static final String REACT_ON_LOAD_END_EVENT = "onFastImageLoadEnd";
    private static final Drawable TRANSPARENT_DRAWABLE = new ColorDrawable(Color.TRANSPARENT);
    private static final Map<String, List<ImageViewWithUrl>> VIEWS_FOR_URLS = new HashMap<>();

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ImageViewWithUrl createViewInstance(final ThemedReactContext reactContext) {
        return new ImageViewWithUrl(reactContext);
    }

    private static class LocalLoadCallback implements Callback {
        private final ImageViewWithUrl view;
        private final String uri;

        LocalLoadCallback(final ImageViewWithUrl view, final String uri) {
            this.view = view;
            this.uri = uri;
        }

        @Override
        public void onSuccess() {
            drawableListenerReady(this.view);
        }

        @Override
        public void onError() {
            drawableListenerException(this.uri, this.view, new Exception());
        }
    }

    private static class NetworkLoadCallback implements Callback {
        private final ImageViewWithUrl view;
        private final Uri uri;

        NetworkLoadCallback(final ImageViewWithUrl view, final Uri uri) {
            this.view = view;
            this.uri = uri;
        }

        @Override
        public void onSuccess() {
            drawableListenerReady(this.view);
        }

        @Override
        public void onError() {
            drawableListenerException(this.uri.toString(), this.view, new Exception());
        }
    }

    /**
     * Passes an exception along to the JS event emitter
     * @param uri
     * @param view
     * @param exception
     * @return
     */
    private static void drawableListenerException(final String uri, final ImageViewWithUrl view, final Exception exception) {
        Picasso.with(view.getContext()).invalidate(uri);

        final ThemedReactContext context = (ThemedReactContext) view.getContext();
        final RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
        final int viewId = view.getId();

        final WritableNativeMap errorMap = new WritableNativeMap();
        errorMap.putString("uri", uri);
        errorMap.putString("localizedMessage", exception.getLocalizedMessage());
        errorMap.putString("message", exception.getMessage());
        errorMap.putString("string", exception.toString());

        eventEmitter.receiveEvent(viewId, REACT_ON_ERROR_EVENT, errorMap);
        eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_END_EVENT, new WritableNativeMap());
    }

    /**
     * Passes view ready status along to the JS event emitter
     * @param view
     */
    private static void drawableListenerReady(ImageViewWithUrl view) {
        final ThemedReactContext context = (ThemedReactContext) view.getContext();
        final RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
        final int viewId = view.getId();

        eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_EVENT, new WritableNativeMap());
        eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_END_EVENT, new WritableNativeMap());
    }

    /**
     * Sets the source of the image
     * @param view
     * @param source
     */
    @ReactProp(name = "source")
    public void setSrc(final ImageViewWithUrl view, @Nullable ReadableMap source) {

        // Define the Picasso instance we'll be working with
        final Picasso picasso = Picasso.with(view.getContext());

        // If the source map is null, stop everything and clear the ImageView
        if (source == null) {

            // Cancel all requests associated with this view
            picasso.cancelRequest(view);
            picasso.cancelTag(view);

            // Invalidate any request with the URI in that ImageView
            if (view.imageURI != null) {
                picasso.invalidate(view.imageURI);
                picasso.cancelTag(view.imageURI.toString());
            }

            // Actually clear the image from the ImageView
            view.setImageDrawable(null);
            return;
        }

        // Get the image URI from the source map & set it on the view object
        final Uri uri = FastImageViewConverter.getURI(source);
        view.imageURI = uri;

        // Get the priority
        final Priority priority = FastImageViewConverter.priority(source);

        // Get the placeholder path
        final String placeholderPath = FastImageViewConverter.placeholder(source);

        // Cancel any existing request for this view
        picasso.cancelRequest(view);
        picasso.cancelTag(view);

        // Determine the cache key
        final String key = uri.toString();

        // Determine what views use this particular key
        final List<ImageViewWithUrl> viewsForKey = VIEWS_FOR_URLS.get(key);

        if (viewsForKey != null && !viewsForKey.contains(view)) {
            viewsForKey.add(view);
        } else if (viewsForKey == null) {
            List<ImageViewWithUrl> newViewsForKeys = new ArrayList<>(Arrays.asList(view));
            VIEWS_FOR_URLS.put(key, newViewsForKeys);
        }

        final ThemedReactContext context = (ThemedReactContext) view.getContext();
        final RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
        final int viewId = view.getId();
        eventEmitter.receiveEvent(viewId, REACT_ON_LOAD_START_EVENT, new WritableNativeMap());

        // In the case of a provided placeholder...
        if (placeholderPath != null && !placeholderPath.isEmpty()) {
            loadPlaceholderInto(view, placeholderPath, uri, priority);

        // In the case where not placeholder was provided...
        } else {
            if (key.startsWith("http")) {
                Log.i("FASTIMAGE", "setSrc starts with http uri = " + uri.toString());
                loadNetworkInto(view, uri, priority, new NetworkLoadCallback(view, uri));
            } else {
                Log.i("FASTIMAGE", "setSrc starts with !http key = " + key);
                loadLocalInto(view, key, priority, new LocalLoadCallback(view, key));
            }
        }
    }

    /**
     * Loads a local image into a view as a placeholder and then downloads the remote image and
     * then displays that when complete.
     * @param view
     * @param localPath
     * @param remoteURI
     * @param priority
     */
    private void loadPlaceholderInto(final ImageViewWithUrl view, final String localPath, final Uri remoteURI, final Priority priority) {

        final Picasso picasso = Picasso.with(view.getContext().getApplicationContext());

        picasso.load(localPath).placeholder(TRANSPARENT_DRAWABLE).priority(priority).into(view, new Callback() {
            @Override
            public void onSuccess() {
                Log.i("FASTIMAGE", "loadPlaceholderInto stableKey = " + remoteURI.toString());
                picasso.load(remoteURI).stableKey(remoteURI.toString()).priority(priority).fetch(new Callback() {
                    @Override
                    public void onSuccess() {
                        loadNetworkInto(view, remoteURI, priority, new NetworkLoadCallback(view, remoteURI));
                    }

                    @Override
                    public void onError() {

                    }
                });
            }

            @Override
            public void onError() {

            }
        });

    }

    /**
     * Loads a network image into a view
     * @param view
     * @param uri
     * @param priority
     * @param listener
     */
    private void loadNetworkInto(final ImageView view, final Uri uri, final Priority priority, final Callback listener) {
        Log.i("FASTIMAGE", "loadNetworkInto stableKey = " + uri.toString());
        Picasso
                .with(view.getContext().getApplicationContext())
                .load(uri)
                .placeholder(TRANSPARENT_DRAWABLE)
                .stableKey(uri.toString())
                .priority(priority)
                .into(view, listener);
    }

    /**
     * Loads a local image into a view
     * @param view
     * @param path
     * @param priority
     * @param listener
     */
    private void loadLocalInto(final ImageView view, final String path, final Priority priority, final Callback listener) {
        Picasso
                .with(view.getContext().getApplicationContext())
                .load(path)
                .placeholder(TRANSPARENT_DRAWABLE)
                .stableKey(path)
                .priority(priority)
                .into(view, listener);
    }

    @ReactProp(name = "resizeMode")
    public void setResizeMode(ImageViewWithUrl view, String resizeMode) {
        final ImageViewWithUrl.ScaleType scaleType = FastImageViewConverter.scaleType(resizeMode);
        view.setScaleType(scaleType);
    }

    @Override
    public void onDropViewInstance(ImageViewWithUrl view) {
        Picasso.with(view.getContext().getApplicationContext()).invalidate(view.imageURI);

        final String key = view.imageURI.toString();
        List<ImageViewWithUrl> viewsForKey = VIEWS_FOR_URLS.get(key);
        if (viewsForKey != null) {
            viewsForKey.remove(view);
            if (viewsForKey.size() == 0) VIEWS_FOR_URLS.remove(key);
        }
        super.onDropViewInstance(view);
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.of(
                REACT_ON_LOAD_START_EVENT,
                MapBuilder.of("registrationName", REACT_ON_LOAD_START_EVENT),
                REACT_ON_PROGRESS_EVENT,
                MapBuilder.of("registrationName", REACT_ON_PROGRESS_EVENT),
                REACT_ON_LOAD_EVENT,
                MapBuilder.of("registrationName", REACT_ON_LOAD_EVENT),
                REACT_ON_ERROR_EVENT,
                MapBuilder.of("registrationName", REACT_ON_ERROR_EVENT),
                REACT_ON_LOAD_END_EVENT,
                MapBuilder.of("registrationName", REACT_ON_LOAD_END_EVENT)
        );
    }

    @Override
    public void onProgress(final String key, final long bytesRead, final long expectedLength) {
        List<ImageViewWithUrl> viewsForKey = VIEWS_FOR_URLS.get(key);
        if (viewsForKey != null) {
            for (ImageViewWithUrl view: viewsForKey) {
                WritableMap event = new WritableNativeMap();
                event.putInt("loaded", (int) bytesRead);
                event.putInt("total", (int) expectedLength);
                ThemedReactContext context = (ThemedReactContext) view.getContext();
                RCTEventEmitter eventEmitter = context.getJSModule(RCTEventEmitter.class);
                int viewId = view.getId();
                eventEmitter.receiveEvent(viewId, REACT_ON_PROGRESS_EVENT, event);
            }
        }
    }

    @Override
    public float getGranularityPercentage() {
        return 0.5f;
    }

}
