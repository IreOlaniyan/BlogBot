package com.ire.blogbot.fragments;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ire.blogbot.model.RecyclerItemClickListener;
import com.ire.blogbot.activity.MainActivity;
import com.ire.blogbot.utils.NetworkUtils;
import com.ire.blogbot.model.News;
import com.ire.blogbot.adapter.NewsAdapter;
import com.ire.blogbot.R;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class EntertainmentFragment extends Fragment {
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mErrorMessage;
    ArrayList<News> news;
    NetworkInfo info;
    //    The Loader takes in a bundle
    Bundle sourceBundle = new Bundle();

    private final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String ENTERTAINMENT_NEWS_QUERY_URL = "query";
    private static final String ENTERTAINMENT_NEWS_SOURCE = "metro";
    private static final String ENTERTAINMENT_SOURCE_CATEGORY = "latest";
    private static final int ENTERTAINMENT_NEWS_LOADER = 21;

    private RecyclerView mRecyclerView;

    public EntertainmentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        mErrorMessage = (TextView) view.findViewById(R.id.tv_error_message);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view_main);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipeRefresh);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

//        COMPLETED: Change to real data

        getActivity().getSupportLoaderManager().initLoader(ENTERTAINMENT_NEWS_LOADER, null, new NewsDataLoader());

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.v(LOG_TAG, "Refreshing");
                restartLoader();
                mSwipeRefreshLayout.setColorSchemeResources(
                        R.color.colorPrimary,
                        R.color.colorPrimaryDark);
            }
        });
        return view;
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager) getActivity()
                .getSystemService(CONNECTIVITY_SERVICE);

        info = cm.getActiveNetworkInfo();

        return info != null && info.isConnectedOrConnecting();
    }

    private int anyRandomInt(Random random) {
        return random.nextInt();
    }

    private void restartLoader() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                URL entertainmentNewsUrl = NetworkUtils.buildUrl(ENTERTAINMENT_NEWS_SOURCE, ENTERTAINMENT_SOURCE_CATEGORY);
                sourceBundle.putString(ENTERTAINMENT_NEWS_QUERY_URL, entertainmentNewsUrl.toString());

                Random random = new Random();
                int uniqueId = anyRandomInt(random); //Generates a new ID for each loader call;

                LoaderManager loaderManager = getActivity().getSupportLoaderManager();

                if (loaderManager.getLoader(ENTERTAINMENT_NEWS_LOADER) == null) {
                    loaderManager.initLoader(uniqueId, sourceBundle, new NewsDataLoader());
                } else {
                    loaderManager.restartLoader(ENTERTAINMENT_NEWS_LOADER, sourceBundle, new
                            NewsDataLoader());
                }
            }
        }, 5000);
        mSwipeRefreshLayout.setRefreshing(false);
        Log.v(LOG_TAG, "Finished refreshing");
    }

    private void showErrorScreen(){
        mErrorMessage.setVisibility(View.VISIBLE);
        mRecyclerView.setVisibility(View.INVISIBLE);
        mErrorMessage.setText(getString(R.string.internet_error));
    }

    public class NewsDataLoader implements LoaderManager.LoaderCallbacks<ArrayList<News>> {
        private NewsAdapter mNewsAdapter;

        @Override
        public Loader<ArrayList<News>> onCreateLoader(int id, final Bundle args) {
            if (isConnected()){
                mErrorMessage.setVisibility(View.INVISIBLE);
                mRecyclerView.setVisibility(View.VISIBLE);
                return new AsyncTaskLoader<ArrayList<News>>(getActivity()) {
                    ArrayList<News> mNewsData;

                    @Override
                    protected void onStartLoading() {
                        super.onStartLoading();
                        if (mNewsData != null){
                            deliverResult(mNewsData);
                        }else{
                            forceLoad();
                            mSwipeRefreshLayout.setRefreshing(true);
                        }
                    }

                    @Override
                    public ArrayList<News> loadInBackground() {
                        try {
                            ArrayList<News> news = NetworkUtils.parseJSON(ENTERTAINMENT_NEWS_SOURCE, ENTERTAINMENT_SOURCE_CATEGORY);
                            return news;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }

                    public void deliverResult(ArrayList<News> data) {
                        mNewsData = data;
                        super.deliverResult(data);
                    }
                };
            }else{
                showErrorScreen();
                return null;
            }
        }

        @Override
        public void onLoadFinished(Loader<ArrayList<News>> loader, ArrayList<News> data) {
            mSwipeRefreshLayout.setRefreshing(false);
            if (null == data) {
                showErrorScreen();
            } else {
                mErrorMessage.setVisibility(View.INVISIBLE);
                mRecyclerView.setVisibility(View.VISIBLE);
                if (news != null) {
                    news.clear();
                    news.addAll(data);
                    mNewsAdapter = new NewsAdapter(news);
                    mRecyclerView.setAdapter(mNewsAdapter);
                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
                    mNewsAdapter.notifyDataSetChanged();

//                Separator between list items
                    DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                            LinearLayoutManager.VERTICAL);
                    mRecyclerView.addItemDecoration(dividerItemDecoration);

                    mRecyclerView.addOnItemTouchListener(
                            new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
                                @Override
                                public void onItemClick(View view, int position) {

                                    News currentNews = news.get(position);

                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentNews.getUrl()));
                                    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(intent);
                                    }
                                }
                            })
                    );
                } else {
                    news = data;
                }
            }

        }

        @Override
        public void onLoaderReset(Loader<ArrayList<News>> loader) {

        }
    }

}
