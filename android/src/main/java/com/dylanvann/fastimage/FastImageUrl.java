package com.dylanvann.fastimage;

import android.util.Log;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import javax.annotation.Nullable;

public class FastImageUrl extends GlideUrl {
    @Nullable
    private String remoteUrl;

    @Nullable
    private String localPath;

    public FastImageUrl(String uri, boolean isLocal) {
        super(uri);
        Log.d("FastImageUrl", String.format("Setting up FastImageUrl for \"%s\"%s", remoteUrl, isLocal ? " from file" : ""));
        if (isLocal) {
            this.localPath = uri;
        } else {
            this.remoteUrl = uri;
        }
    }

    public FastImageUrl(String remoteUrl, String localPath) {
        super(localPath);
        Log.d("FastImageUrl", String.format("Setting up FastImageUrl for local path:\"%s\" and \"%s\"", localPath, remoteUrl));
        this.localPath = localPath;
        this.remoteUrl = remoteUrl;
    }

    public FastImageUrl(String uri, LazyHeaders headers, boolean isLocal) {
        super(uri, headers);
        if (isLocal) {
            this.localPath = uri;
        } else {
            this.remoteUrl = uri;
        }
    }

    public FastImageUrl(String remoteUrl, String localPath, LazyHeaders headers) {
        super(remoteUrl, headers);
        this.localPath = localPath;
        this.remoteUrl = remoteUrl;
    }

    @Override
    public String getCacheKey() {
        Log.d("FastImageUrl", String.format("Getting cache key for %s and %s", localPath, remoteUrl));
        if (localPath != null && remoteUrl != null) {
            return remoteUrl;
        }

        if (isLocalImage()) {
            return localPath;
        }

        if (isRemoteImage()) {
            return remoteUrl;
        }

        return super.getCacheKey();
    }

    @Override
    public String toString() {
        return getCacheKey();
    }

    public boolean isLocalImage() { return localPath != null && remoteUrl == null; }
    public boolean isRemoteImage() { return remoteUrl != null && localPath == null; }

    public String getLocalPath() { return localPath; }
    public String getRemoteUrl() { return remoteUrl; }
}
