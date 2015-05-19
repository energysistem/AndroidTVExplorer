package com.energysystem.videoexplorerTV.leanback;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.energysystem.videoexplorerTV.R;
import com.energysystem.videoexplorerTV.presenters.CardPresenter;
import com.energysystem.videoexplorerTV.presenters.GridItemPresenter;
import com.energysystem.videoexplorerTV.presenters.PicassoBackgroundManagerTarget;
import com.energysystem.videoexplorerTV.video.Video;
import com.energysystem.videoexplorerTV.video.VideoItemLoader;
import com.energysystem.videoexplorerTV.video.VideoProvider;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONException;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ExplorerFragment extends BrowseFragment implements LoaderManager.LoaderCallbacks<HashMap<String, List<Video>>> {

    private static final String TAG = "ExplorerFragment";
    private ArrayObjectAdapter mRowsAdapter;
    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private Timer mBackgroundTimer;
    private final Handler mHandler = new Handler();
    private URI mBackgroundURI;
    private static String mVideosUrl;
    BroadcastReceiver mReceiver;
    ContentObserver mDbObserver;
    private ExplorerFragment dis;
    private VideoItemLoader vil;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onActivityCreated(savedInstanceState);

        loadVideoData(false);

        prepareBackgroundManager();
        setupUIElements();

        setupEventListeners();
        final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Cargando vídeos del dispositivo USB...");

        dis=this;





        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.e("onReceive","montamos usb-- "+intent.getAction());


                String action = intent.getAction();
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                    dialog.show();


                } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    dialog.hide();


                } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {

                } else if (action.equals(
                        Intent.ACTION_MEDIA_SCANNER_FINISHED)) {


                    getLoaderManager().restartLoader(0, null, dis);
                    dialog.hide();

                } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                    //loadVideoData(true);
                    getLoaderManager().restartLoader(0, null, dis);
                    dialog.hide();


                }

            }
        };

       /* getActivity().getContentResolver().registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true,
                new ContentObserver(new Handler()) {
                    @Override
                    public void onChange(boolean selfChange) {
                        loadVideoData();
                        //vil.reset();
                        Log.e("OnChange","Cambios: "+selfChange);
                        super.onChange(selfChange);
                    }

                });*/

        // install an intent filter to receive SD card related events.
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_STARTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        getActivity().registerReceiver(mReceiver, intentFilter);

        mDbObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                Log.e("onChange","Cambios en media");

                getLoaderManager().restartLoader(0, null, dis);
            }
        };


        getActivity().getContentResolver().registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                true, mDbObserver);


    }

    public void galleryAdd(String file) {
        File f = new File(file);
        Uri contentUri = Uri.fromFile(f);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    private void loadVideoData(boolean cambio) {
        VideoProvider.setContext(getActivity());

        mVideosUrl = getActivity().getResources().getString(R.string.catalog_url);
        if(cambio)
        {
            if(vil!=null)
                try {
                    Log.e("loadVideoData","Llamamos a buildMedia");
                    VideoProvider.buildMedia("",vil.MakeCursor());
                    vil.loadInBackground();
                    Log.e("loadVideoData","Num. de videos:"+ VideoProvider.getMovieList().size());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
        else
            getLoaderManager().initLoader(0, null, this);
        //
    }

    /*private Cursor MakeCursor() {
        ContentResolver resolver = getContentResolver();
        String[] cols = new String[] {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.ARTIST,
                MediaStore.Video.Media.CATEGORY
        };

        if (resolver == null) {
            System.out.println("resolver = null");
        } else {
            mSortOrder = MediaStore.Video.Media.TITLE + " COLLATE UNICODE";
            mWhereClause = MediaStore.Video.Media.TITLE + " != ''";
            mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    cols, mWhereClause , null, mSortOrder);
        }
        return mCursor;
    }*/


    private void onReceiveMediaBroadcast(Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
            // SD card available
            // TODO put up a "please wait" message
        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
            // SD card unavailable
            rebake(true, false);
        } else if (action.equals(Intent.ACTION_MEDIA_SCANNER_STARTED)) {
            rebake(false, true);
        } else if (action.equals(
                Intent.ACTION_MEDIA_SCANNER_FINISHED)) {
            rebake(false, false);
        } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
            rebake(true, false);
        }
    }

    private void rebake(boolean unmounted, boolean scanning) {


        if (scanning) {

        }
        loadVideoData(true);

        prepareBackgroundManager();
        setupUIElements();

        setupEventListeners();


    }

    private void prepareBackgroundManager() {

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setTitle(getString(R.string.browse_title));
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);
        setBrandColor(getResources().getColor(R.color.fastlane_background));
        setSearchAffordanceColor(getResources().getColor(R.color.search_opaque));
    }

    private void setupEventListeners() {
        setOnItemViewSelectedListener(getDefaultItemSelectedListener());
        setOnItemViewClickedListener(getDefaultItemClickedListener());
        setOnSearchClickedListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Implement your own in-app search", Toast.LENGTH_LONG)
                        .show();
            }
        });
    }

    protected OnItemViewSelectedListener getDefaultItemSelectedListener() {
        return new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder viewHolder, Object item, RowPresenter.ViewHolder viewHolder2, Row row) {
                if (item instanceof Video) {
                    mBackgroundURI = ((Video) item).getBackgroundImageURI();
                    startBackgroundTimer();
                }
            }
        };
    }

    protected OnItemViewClickedListener getDefaultItemClickedListener() {
        return new OnItemViewClickedListener() {
            @Override
            public void onItemClicked(Presenter.ViewHolder viewHolder, Object item, RowPresenter.ViewHolder viewHolder2, Row row) {
                if (item instanceof Video) {
                    Video video = (Video) item;
                    /*Intent videoIntent = new Intent(getActivity(), VideoDetailsActivity.class);
                    videoIntent.putExtra(getResources().getString(R.string.video), video);
                    startActivity(videoIntent);*/
                   /* Intent intent = new Intent(getActivity(), PlayerActivity.class);
                    intent.putExtra(getResources().getString(R.string.video), video);
                    startActivity(intent);*/
                    String videoUrl = video.getVideoUrl();
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setDataAndType(Uri.parse(videoUrl),"video/mp4");
                    startActivity(i);

                } else if (item instanceof String) {
                    Toast.makeText(getActivity(), (String) item, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };
    }

    protected void updateBackground(URI uri) {
        Picasso.with(getActivity())
                .load(uri.toString())
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .centerCrop()
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }

    private void startBackgroundTimer() {
        if (null != mBackgroundTimer) {
            mBackgroundTimer.cancel();
        }
        mBackgroundTimer = new Timer();
        mBackgroundTimer.schedule(new UpdateBackgroundTask(), 50);
    }

    private class UpdateBackgroundTask extends TimerTask {

        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBackgroundURI != null) {
                        updateBackground(mBackgroundURI);
                    }
                }
            });

        }
    }

    @Override
    public Loader<HashMap<String, List<Video>>> onCreateLoader(int arg0, Bundle arg1) {

         vil = new VideoItemLoader(getActivity(), mVideosUrl);



        return vil;

    }

    @Override
    public void onLoadFinished(Loader<HashMap<String, List<Video>>> arg0, HashMap<String, List<Video>> data) {

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        CardPresenter cardPresenter = new CardPresenter();
            //
        int i = 0;

        for (Map.Entry<String, List<Video>> entry : data.entrySet()) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);
            List<Video> list = entry.getValue();

            for (int j = 0; j < list.size(); j++) {
                listRowAdapter.add(list.get(j));
            }
            HeaderItem header = new HeaderItem(i, entry.getKey(), null);
            i++;
            Log.e("onLoadFinished","Header: "+header.toString()+" - i: "+i);
            mRowsAdapter.add(new ListRow(header, listRowAdapter));
        }

        HeaderItem gridHeader = new HeaderItem(i, getResources().getString(R.string.preferences), null);



        GridItemPresenter gridPresenter = new GridItemPresenter();
        /*ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.grid_view));
        gridRowAdapter.add(getResources().getString(R.string.send_feeback));
        gridRowAdapter.add(getResources().getString(R.string.personal_settings));
        mRowsAdapter.add(new ListRow(gridHeader, gridRowAdapter));*/

        Log.e("onLoadFinished","yiequepasa");
        setAdapter(mRowsAdapter);
        mRowsAdapter.notifyArrayItemRangeChanged(0,mRowsAdapter.size());
    }

    @Override
    public void onLoaderReset(Loader<HashMap<String, List<Video>>> arg0) {
        mRowsAdapter.clear();
    }

}
