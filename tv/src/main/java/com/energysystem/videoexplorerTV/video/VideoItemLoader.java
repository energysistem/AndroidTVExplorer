/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.energysystem.videoexplorerTV.video;

import android.content.AsyncTaskLoader;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import java.util.HashMap;
import java.util.List;

/*
 * This class asynchronously loads videos from a backend
 */
public class VideoItemLoader extends AsyncTaskLoader<HashMap<String, List<Video>>> {

    private static final String TAG = "VideoItemLoader";
    private final String mUrl;
    private Context mContext;

    public VideoItemLoader(Context context, String url) {
        super(context);
        mContext = context;
        mUrl = url;
    }

    @Override
    public HashMap<String, List<Video>> loadInBackground() {
        try {
            if(Looper.myLooper()==null)// check already Looper is associated or not.
                Looper.prepare();// No Looper is defined So define a new one


            Log.e("loadInBackground","VideoItemLoader");
            return VideoProvider.buildMedia(mUrl, MakeCursor());
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch media data", e);
            return null;
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    private Cursor mCursor;
    private String mWhereClause;
    private String mSortOrder;

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    public Cursor MakeCursor() {
        ContentResolver resolver = mContext.getContentResolver();
        String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.CATEGORY,
                MediaStore.Video.Media.DATA

        };

        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            mSortOrder = MediaStore.Video.Media.TITLE + " COLLATE UNICODE";
            mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
            mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    cols, mWhereClause , null, mSortOrder);
        }
        Log.e("Cursor","Tamano: "+mCursor.getCount());
        return mCursor;
    }

}
