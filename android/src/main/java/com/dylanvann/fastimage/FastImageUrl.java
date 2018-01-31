package com.dylanvann.fastimage;

import android.net.Uri;
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

        if (isLocal) {
            this.localPath = uri;
        } else {
            this.remoteUrl = uri;
        }
    }

    public FastImageUrl(String uri, boolean isLocal, @Nullable LazyHeaders headers) {
        super(uri, headers);

        if (isLocal) {
            this.localPath = uri;
        } else {
            this.remoteUrl = uri;
        }
    }

    public FastImageUrl(String remoteUrl, String localPath) {
        super(remoteUrl);

        Log.d("FastImageUrl", String.format("Setting up FastImageUrl for local path:\"%s\" and \"%s\"", localPath, remoteUrl));

        this.localPath = localPath;
        this.remoteUrl = remoteUrl;
    }

    public FastImageUrl(String remoteUrl, String localPath, @Nullable LazyHeaders headers) {
        super(remoteUrl, headers);

        Log.d("FastImageUrl", String.format("Setting up FastImageUrl for local path:\"%s\" and \"%s\"", localPath, remoteUrl));

        this.localPath = localPath;
        this.remoteUrl = remoteUrl;
    }

    @Override
    public String getCacheKey() {
        if (localPath != null && remoteUrl != null) {
            return getRemoteUri().toString();
        } else if (isLocalImage()) {
            return getLocalUri().toString();
        } else if (isRemoteImage()) {
            return getRemoteUri().toString();
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

    public Uri getLocalUri() {
        String uriString = localPath.contains("file://")
                ? localPath
                : String.format("file://%s", localPath);
        return Uri.parse(uriString);
    }

    public Uri getRemoteUri() {
        String uriString = remoteUrl.contains("http")
                ? remoteUrl
                : String.format("https://%s", remoteUrl);

        return Uri.parse(uriString);
    }
}
