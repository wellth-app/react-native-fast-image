package com.dylanvann.fastimage;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.data.LocalUriFetcher;
import com.bumptech.glide.load.data.StreamLocalUriFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.model.GenericLoaderFactory;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.stream.StreamModelLoader;
import com.bumptech.glide.module.GlideModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

class FastImageUrlLoader implements StreamModelLoader<FastImageUrl> {
    private final Call.Factory client;
    private final Context context;

    public FastImageUrlLoader(Context context, Call.Factory client) {
        this.context = context;
        this.client = client;
    }

    @Override
    public DataFetcher<InputStream> getResourceFetcher(FastImageUrl model, int width, int height) {
        if (model.getRemoteUrl() != null && model.getLocalPath() != null) {
            Log.d("FastImageUrlLoader", String.format("We should load the local file from %s", model.getLocalPath()));
            return new StreamLocalUriFetcher(this.context, Uri.parse(model.getLocalPath()));
        }

        return new OkHttpStreamFetcher(client, new GlideUrl(model.getRemoteUrl()));
    }

    public static class Factory implements ModelLoaderFactory<FastImageUrl, InputStream> {
        private Call.Factory client;

        public Factory(Call.Factory client) {
            this.client = client;
        }

        @Override
        public ModelLoader<FastImageUrl, InputStream> build(Context context, GenericLoaderFactory factories) {
            return new FastImageUrlLoader(context, client);
        }

        @Override
        public void teardown() { }
    }
}

public class OkHttpProgressGlideModule implements GlideModule {
    @Override
    public void applyOptions(Context context, GlideBuilder builder) {}

    @Override
    public void registerComponents(Context context, Glide glide) {
        OkHttpClient client = new OkHttpClient
                .Builder()
                .addInterceptor(createInterceptor(new DispatchingProgressListener()))
                .build();

        glide.register(FastImageUrl.class, InputStream.class, new FastImageUrlLoader.Factory(client));
    }

    private static Interceptor createInterceptor(final ResponseProgressListener listener) {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Response response = chain.proceed(request);
                final String key = request.url().toString();
                return response
                        .newBuilder()
                        .body(new OkHttpProgressResponseBody(key, response.body(), listener))
                        .build();
            }
        };
    }

    public static void forget(String key) {
        DispatchingProgressListener.forget(key);
    }

    public static void expect(String key, ProgressListener listener) {
        DispatchingProgressListener.expect(key, listener);
    }

    private interface ResponseProgressListener {
        void update(String key, long bytesRead, long contentLength);
    }

    private static class DispatchingProgressListener implements ResponseProgressListener {
        private static final Map<String, ProgressListener> LISTENERS = new HashMap<>();
        private static final Map<String, Long> PROGRESSES = new HashMap<>();

        private final Handler handler;

        DispatchingProgressListener() {
            this.handler = new Handler(Looper.getMainLooper());
        }

        static void forget(String key) {
            LISTENERS.remove(key);
            PROGRESSES.remove(key);
        }

        static void expect(String key, ProgressListener listener) {
            LISTENERS.put(key, listener);
        }

        @Override
        public void update(final String key, final long bytesRead, final long contentLength) {
            final ProgressListener listener = LISTENERS.get(key);
            if (listener == null) {
                return;
            }
            if (contentLength <= bytesRead) {
                forget(key);
            }
            if (needsDispatch(key, bytesRead, contentLength, listener.getGranularityPercentage())) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        listener.onProgress(key, bytesRead, contentLength);
                    }
                });
            }
        }

        private boolean needsDispatch(String key, long current, long total, float granularity) {
            if (granularity == 0 || current == 0 || total == current) {
                return true;
            }
            float percent = 100f * current / total;
            long currentProgress = (long) (percent / granularity);
            Long lastProgress = PROGRESSES.get(key);
            if (lastProgress == null || currentProgress != lastProgress) {
                PROGRESSES.put(key, currentProgress);
                return true;
            } else {
                return false;
            }
        }
    }

    private static class OkHttpProgressResponseBody extends ResponseBody {
        private final String key;
        private final ResponseBody responseBody;
        private final ResponseProgressListener progressListener;
        private BufferedSource bufferedSource;

        OkHttpProgressResponseBody(
                String key,
                ResponseBody responseBody,
                ResponseProgressListener progressListener
        ) {
            this.key = key;
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    long fullLength = responseBody.contentLength();
                    if (bytesRead == -1) {
                        // this source is exhausted
                        totalBytesRead = fullLength;
                    } else {
                        totalBytesRead += bytesRead;
                    }
                    progressListener.update(key, totalBytesRead, fullLength);
                    return bytesRead;
                }
            };
        }
    }
}
