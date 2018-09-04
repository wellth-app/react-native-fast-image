package com.dylanvann.fastimage;

import android.net.Uri;
import android.widget.ImageView.ScaleType;

import com.squareup.picasso.Picasso.Priority;
import com.facebook.react.bridge.ReadableMap;

import java.util.HashMap;
import java.util.Map;

class FastImageViewConverter {

    private static final Map<String, Priority> PRIORITY_MAP =
            new HashMap<String, Priority>() {{
                put("low", Priority.LOW);
                put("normal", Priority.NORMAL);
                put("high", Priority.HIGH);
            }};

    private static final Map<String, ScaleType> RESIZE_MODE_MAP =
            new HashMap<String, ScaleType>() {{
                put("contain", ScaleType.FIT_CENTER);
                put("cover", ScaleType.CENTER_CROP);
                put("stretch", ScaleType.FIT_XY);
                put("center", ScaleType.CENTER);
            }};

    /**
     * Gets a URL from the source map
     * @param source
     * @return
     */
    public static String getURL(final ReadableMap source) {
        final String uriProp = source.getString("uri");
        return uriProp;
    }

    /**
     * Gets a URI from the source map
     * @param source
     * @return
     */
    public static Uri getURI(final ReadableMap source) {
        final String uriProp = source.getString("uri");
        return Uri.parse(uriProp);
    }

    /**
     * Gets the placeholder location from the source map
     * @param source
     * @return
     */
    public static String placeholder(final ReadableMap source) {
        try {
            return source.getString("placeholder");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a priority from the source map
     * @param source
     * @return
     */
    public static Priority priority(final ReadableMap source) {
        String priorityProp = "normal";
        try {
            priorityProp = source.getString("priority");
        } catch (Exception e) {
            // Do nothing, priority is already set to normal
        }
        return PRIORITY_MAP.get(priorityProp);
    }

    /**
     * Gets a ScaleType from a string representation of that type
     * @param resizeMode
     * @return
     */
    public static ScaleType scaleType(String resizeMode) {
        if (resizeMode == null) {
            resizeMode = "contain";
        }
        return RESIZE_MODE_MAP.get(resizeMode);
    }

}