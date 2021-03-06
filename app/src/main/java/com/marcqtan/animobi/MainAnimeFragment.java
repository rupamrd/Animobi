package com.marcqtan.animobi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marc Q. Tan on 18/02/2018.
 */

public class MainAnimeFragment extends Fragment implements AnimeListAdapter.OnItemClicked, Utility.interface1  {

    RecyclerView animelist;
    String animeListUrl = "https://otakustream.tv/anime/";
    String animeTrendingUrl = "https://otakustream.tv/trending-animes";

    AnimeListAdapter animeAdapter;
    List<Anime> anime_list = null;
    EditText query;
    FrameLayout frame;
    AsyncTask task = null;

    private CollapsingToolbarLayout collapsingToolbar;
    private AppBarLayout appBarLayout;
    private Toolbar toolbar;

    public MainAnimeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.anime_main_layout, container, false);

        toolbar = rootView.findViewById(R.id.toolbar);


        //Utility.initCollapsingToolbar((CollapsingToolbarLayout)rootView.findViewById(R.id.collapsing_toolbar),(AppBarLayout)rootView.findViewById(R.id.appbar), getString(R.string.app_name));
        appBarLayout = rootView.findViewById(R.id.appbar);

        collapsingToolbar = rootView.findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle("Trending Anime");
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                R.drawable.cover_trending);

        Palette.from(bitmap).generate(new Palette.PaletteAsyncListener() {
            @SuppressWarnings("ResourceType")
            @Override
            public void onGenerated(Palette palette) {
                int vibrantColor = palette.getVibrantColor(R.color.colorPrimary);
                //collapsingToolbar.setContentScrimColor(vibrantColor);
                collapsingToolbar.setStatusBarScrimColor(R.color.black_trans80);
            }
        });

        animelist = rootView.findViewById(R.id.animelist);
        animeAdapter = new AnimeListAdapter(this, getActivity());
        query = rootView.findViewById(R.id.searchET);
        frame = rootView.findViewById(R.id.progressBarContainer);

        //RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
        //animelist.setLayoutManager(layoutManager);
        animelist.setLayoutManager(new ScrollingLayoutManager(getActivity(),2, 1000));
        animelist.addItemDecoration(new Utility.GridSpacingItemDecoration(2, Utility.dpToPx(10, getResources()), true));
        animelist.setItemAnimator(new DefaultItemAnimator());
        animelist.setHasFixedSize(true);
        animelist.setAdapter(animeAdapter);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            anime_list = (List<Anime>) savedInstanceState.getSerializable("cacheAnime");
        } else if (MainActivity.getCacheAnimeList() != null) {
            anime_list = MainActivity.getCacheAnimeList();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(task != null) {
            task.cancel(true);
        }
    }

    @Override
    public void showVisibilty() {
        frame.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideVisibility() {
        frame.setVisibility(View.GONE);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(anime_list == null) {
            task = new getAnimeList(this).execute(animeTrendingUrl);
        } else {
            animeAdapter.setAnimeData(MainActivity.getCacheAnimeList());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("cacheAnime", (Serializable) anime_list);
        super.onSaveInstanceState(outState);
    }

    @Override
    public FragmentActivity getFragActivity() {
        return getActivity();
    }

    private static class getAnimeList extends AsyncTask<String, Void, Void> {

        private WeakReference<MainAnimeFragment> activity;

        getAnimeList(MainAnimeFragment activity){
            this.activity = new WeakReference<MainAnimeFragment>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            activity.get().frame.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Document doc = Jsoup.connect(params[0]).timeout(30000).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36 OPR/48.0.2685.50")
                        .followRedirects(true)
                        .get();
                activity.get().anime_list = new ArrayList<>();
                activity.get().parseTrendingAnimeName(doc);
                Elements page = doc.select("div.wp-pagenavi").select("a");
                for(int i = 0; i < page.size() - 1;i++) {
                    String pageUrl = page.get(i).attr("href");
                    doc = Jsoup.connect(pageUrl).timeout(30000).userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36 OPR/48.0.2685.50")
                            .followRedirects(true)
                            .get();
                    activity.get().parseTrendingAnimeName(doc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            if(activity.get().anime_list != null) {
                activity.get().animeAdapter.setAnimeData(activity.get().anime_list);
            } else {
                Log.v("ERROR HERE", "ERROR!!!");
            }

            activity.get().frame.setVisibility(View.GONE);

            MainActivity.cacheAnimeList(activity.get().anime_list);
        }
    }

    public void parseTrendingAnimeName(Document doc) {
        Elements animeInfo = doc.select("article.article-block");
        for(Element info : animeInfo ){
            Anime anime = new Anime();
            Element hrefInfo = info.select("h3").first();
            anime.setAnimeName(hrefInfo.text());
            anime.setAnimeLink(hrefInfo.select("a").attr("href"));
            anime.setThumbNail(info.select("img").attr("src"));
            Elements ep = info.select("div.some-more-info").select("tr+tr+tr").first().select("td");
            String epCount = "";
            if (ep.first().text().equals("Episodes :")){
                epCount = ep.last().text();
                epCount = epCount.equals("?") ? "Ongoing" : "Episodes: " + epCount;
            }
            anime.setEpisodeCount(epCount); //TODO
            anime_list.add(anime);
        }
    }

    @Override
    public void onItemClick(int position, ImageView image) {
        Anime animeSelected = anime_list.get(position);

        EpisodeListFragment episodeList = new EpisodeListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("anime", animeSelected);
        bundle.putString("transitionName", ViewCompat.getTransitionName(image));
        episodeList.setArguments(bundle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            episodeList.setSharedElementEnterTransition(new DetailsTransition());
            episodeList.setEnterTransition(new Fade());
            setSharedElementReturnTransition(new DetailsTransition());
            setExitTransition(new Fade());
        }

        getActivity().getSupportFragmentManager()
                .beginTransaction().
                addSharedElement(image, ViewCompat.getTransitionName(image)).
                replace(R.id.frame_fragmentholder, episodeList, "episodeList").addToBackStack("episodeList").commit();
        //new Utility.getAnimeEpisode(this).execute(animeSelected);
    }

    public void scrolltoTop(){
        animelist.smoothScrollToPosition(0);
    }



}
