package com.sachinbak.moviezilla;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailActivityFragment extends Fragment {

    private View curView;
    public Movie movie;
    RequestQueue mRequestQueue;
    public FloatingActionButton fab;
    public TrailerAdapter trailerAdapter;
    private static String LOG_TAG = "DetailView";
    public LinearLayout trailersList, reviewsList;
    ScrollView scrollView;
    static DetailActivityFragment instance;
    static final String YOUTUBE_URL_BASE = "http://www.youtube.com/watch?v=";
    private android.support.v7.widget.ShareActionProvider mShareActionProvider;
    // to save instance
    int scrollId = 0, scrollOverheadId = 0;

    public DetailActivityFragment() {
        instance = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        curView = inflater.inflate(R.layout.fragment_detail, container, false);
        //  get UI components
        trailersList = (LinearLayout) curView.findViewById(R.id.trailersList);
        reviewsList = (LinearLayout) curView.findViewById(R.id.reviewsList);
        scrollView = (ScrollView) curView.findViewById(R.id.detailScrollView);
        Log.v(LOG_TAG, "fragment on create view finished");
        return curView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fab = (FloatingActionButton) getActivity().findViewById(R.id.fab); // because fab is out of fragment
        fab.setOnClickListener(new FabOnClick());
        trailerAdapter = new TrailerAdapter(getActivity());
        mRequestQueue = Volley.newRequestQueue(getActivity());
        Log.v(LOG_TAG, "fragment on activity created finished");
        updateUI();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.v(LOG_TAG, "HI " + trailersList.getChildCount());

        // save scroll instance
        Rect scrollBounds = new Rect();
        scrollView.getHitRect(scrollBounds);
        scrollId = 0;
        scrollOverheadId = 0;

        RelativeLayout container = (RelativeLayout) curView.findViewById(R.id.detailScrollViewContainer);
        LinearLayout listContainer;
        int j;
        for (int i = 0; i < container.getChildCount(); i++) {
            if (container.getChildAt(i).getLocalVisibleRect(scrollBounds)) {
                scrollId = (container.getChildAt(i)).getId();
                Log.v(LOG_TAG, "found at " + container.getChildAt(i).toString());
                if (container.getChildAt(i).getId() == R.id.trailersList || container.getChildAt(i).getId() == R.id.reviewsList){
                    listContainer = (LinearLayout) container.getChildAt(i);
                    for (j = 0; j < listContainer.getChildCount(); j++){
                        if (listContainer.getChildAt(j).getLocalVisibleRect(scrollBounds)){
                            scrollId = listContainer.getChildAt(j).getId();
                            scrollOverheadId = listContainer.getId();
                            break;
                        }
                    }
                    break;
                } else {
                    break;
                }
            }
        }
        if (scrollId == R.id.posterImageView) scrollId = 0;
        Log.v(LOG_TAG, "id " + scrollId + " overhead id " + scrollOverheadId);
        outState.putInt("SCROLLID", scrollId);
        outState.putInt("SCROLLOVERHEADID", scrollOverheadId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.v(LOG_TAG, "Destroy view");
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (getArguments() != null)
            movie = getArguments().getParcelable("movie");
        if (savedInstanceState != null) {
            scrollId = savedInstanceState.getInt("SCROLLID");
            scrollOverheadId = savedInstanceState.getInt("SCROLLOVERHEADID");
        }
    }

    public static DetailActivityFragment newInstance(Movie newMovie) {
        Bundle args = new Bundle();
        DetailActivityFragment fragment = new DetailActivityFragment();
        args.putParcelable("movie", newMovie);
        fragment.setArguments(args);
        return fragment;
    }

    public void updateUI(){
        MoviesDB moviesDB = new MoviesDB();
        boolean favStatus = moviesDB.isMovieFavorited(getActivity().getContentResolver(), movie.id);
        if (favStatus)
            fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), android.R.drawable.btn_star_big_on));
        else
            fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), android.R.drawable.btn_star_big_off));
        // fill fields
        ((TextView) curView.findViewById(R.id.detailTextView)).setText(movie.display_name);
        Picasso.with(getContext()).load(movie.poster_url).
            placeholder(R.mipmap.ic_launcher).into((ImageView) curView.findViewById(R.id.posterImageView));
        ((TextView) curView.findViewById(R.id.overviewTextView)).setText(movie.overview);
        ((RatingBar) curView.findViewById(R.id.rating)).setRating(movie.rating / 2f);
        ((TextView) curView.findViewById(R.id.ratingTextView)).setText((float) Math.round(movie.rating*10d)/10d + "/10");

        SimpleDateFormat df = new SimpleDateFormat("dd MMM, yyyy");
        SimpleDateFormat dfInput = new SimpleDateFormat("yyyy-MM-dd");
        String releasedDate;
        try {
            releasedDate = df.format(dfInput.parse(movie.released_date));
        } catch (ParseException e){
            e.printStackTrace();
            releasedDate = movie.released_date;
        }
        ((TextView) curView.findViewById(R.id.releaseDate)).setText(releasedDate);

        getTrailers(movie.id);
        getReviews(movie.id);
    }

    class FabOnClick implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            ContentResolver contentResolver = getContext().getContentResolver();
            MoviesDB mdb = new MoviesDB();
            String message;
            if (mdb.isMovieFavorited(contentResolver, movie.id)){
                message = "Removed from Favorites";
                mdb.removeMovie(contentResolver, movie.id);
                fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), android.R.drawable.btn_star_big_off));
            } else {
                mdb.addMovie(contentResolver, movie);
                message = "Added to favorites";
                fab.setImageDrawable(ContextCompat.getDrawable(getActivity(), android.R.drawable.btn_star_big_on));
            }
            (MainActivityFragment.instance).updateFavoritesGrid(); // till I start using a Loader, this one should suffice
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Log.v(LOG_TAG, "on create menu details");
        inflater.inflate(R.menu.menu_share, menu);

        MenuItem shareItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (android.support.v7.widget.ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        if (mShareActionProvider != null){
            if (trailerAdapter.trailers.size() > 0)
                mShareActionProvider.setShareIntent(createVideoShareIntent(YOUTUBE_URL_BASE +
                    trailerAdapter.trailers.get(0).url));
            else
                mShareActionProvider.setShareIntent(createVideoShareIntent("<No Videos Found>"));
        } else {
            Log.d(LOG_TAG, "Share Action Provider not working");
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    public void getTrailers(int id){
        String url = "http://api.themoviedb.org/3/movie/" + id + "/videos?api_key=" + DataStore.API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray items = response.getJSONArray("results");
                            JSONObject trailerObj;
                            for (int i=0; i<items.length(); i++){
                                trailerObj = items.getJSONObject(i);
                                Trailer trailer = new Trailer();
                                trailer.id = trailerObj.getString("id");
                                trailer.url = trailerObj.getString("key");
                                trailer.label = trailerObj.getString("name");
                                trailerAdapter.addItem(trailer);
                            }
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                        for (int i = 0; i < trailerAdapter.getCount(); i++){
                            trailersList.addView(trailerAdapter.getView(i, null, null));
                        }
                        // update share intent
                        if (trailerAdapter.trailers.size() > 0) {
                            try {
                                mShareActionProvider.setShareIntent(createVideoShareIntent(YOUTUBE_URL_BASE +
                                        trailerAdapter.trailers.get(0).url));
                            } catch (NullPointerException e) { // cached trailers. var not defined yet
                                Log.v(LOG_TAG, "Share Action Provider not defined");
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Error in JSON Parsing");
            }
        });

        mRequestQueue.add(req);
    }

    public void getReviews(int id){
        String url = "http://api.themoviedb.org/3/movie/" + id + "/reviews?api_key=" + DataStore.API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray items = response.getJSONArray("results");
                            JSONObject reviewObj;
                            View view;
                            for (int i=0; i<items.length(); i++){
                                reviewObj = items.getJSONObject(i);
                                Review review = new Review();
                                review.author = reviewObj.getString("author");
                                review.url = reviewObj.getString("url");
                                review.content = reviewObj.getString("content");
                                reviewsList.addView(view = createReviewView(review, i));
                                collapseReviewView(view);
                            }
                            // scroll saved
                            Log.v(LOG_TAG, "scroll " + scrollId);
                            if (scrollId != 0) {
                                scrollView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        scrollView.scrollTo(0,
                                                ((scrollOverheadId>0) ? curView.findViewById(scrollOverheadId).getTop() : 0) +
                                                curView.findViewById(scrollId).getTop());
                                    }
                                });
                            }
                        } catch (JSONException e){
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG_TAG, "Error in JSON Parsing");
            }
        });

        mRequestQueue.add(req);
    }

    public View createReviewView(Review review, int i){
        View view;
        view  = View.inflate(getContext(), R.layout.review, null);
        ((TextView) view.findViewById(R.id.reviewAuthor)).setText(review.author);
        ((TextView) view.findViewById(R.id.reviewContent)).setText(review.content);
        view.setId(2000 + i);
        return view;
    }

    public void collapseReviewView(final View view){
        final TextView contentView = (TextView) view.findViewById(R.id.reviewContent);
        contentView.post(new Runnable() { // run on UI thread for getLineCount
            @Override
            public void run() {
                if (contentView.getLineCount() <= 5) {
                    ((TextView) view.findViewById(R.id.statusCollapsed)).setVisibility(View.GONE);
                } else {
                    contentView.setMaxLines(5);
                    view.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            TextView statusView = (TextView) view.findViewById(R.id.statusCollapsed);
                            TextView contentView2 = (TextView) view.findViewById(R.id.reviewContent);
                            if (statusView.getText().equals(getString(R.string.text_more))) {
                                contentView2.setMaxLines(10000);
                                statusView.setText(getString(R.string.text_less));
                            } else {
                                contentView2.setMaxLines(5);
                                statusView.setText(getString(R.string.text_more));
                            }
                        }
                    });
                }
            }
        });
    }

    public void watchYoutubeVideo(String id){
        // http://stackoverflow.com/a/12439378/2295672
        try{
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id));
            startActivity(intent);
        }catch (ActivityNotFoundException ex){
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_URL_BASE+id));
            startActivity(intent);
        }
    }

    private Intent createVideoShareIntent(String url){
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, url);
        return shareIntent;
    }
}
