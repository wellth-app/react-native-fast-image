package com.dylanvann.fastimage;

import android.util.Log;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import javax.annotation.Nullable;

public class FastImageUrl extends GlideUrl {
    private String remoteUrl;

    @Nullable
    private String localPath;

    public FastImageUrl(String remoteUrl) {
        super(remoteUrl);
        Log.d("FastImageUrl", String.format("Setting up FastImageUrl for \"%s\"", remoteUrl));
        this.remoteUrl = remoteUrl;
    }

    public FastImageUrl(String remoteUrl, String localPath) {
        super(localPath);
        Log.d("FastImageUrl", String.format("Setting up FastImageUrl for local path:\"%s\" and \"%s\"", localPath, remoteUrl));
        this.localPath = localPath;
        this.remoteUrl = remoteUrl;
    }

    public FastImageUrl(String remoteUrl, LazyHeaders headers) {
        super(remoteUrl, headers);
    }

    @Override
    public String getCacheKey() {
        Log.d("FastImageUrl", String.format("Getting cache key for %s and %s", localPath, remoteUrl);
        return remoteUrl;
    }

    @Override
    public String toString() {
        return super.getCacheKey();
    }

    public boolean isLocalImage() { return localPath != null; }

    public String getLocalPath() { return localPath; }
    public String getRemoteUrl() { return remoteUrl; }
}
