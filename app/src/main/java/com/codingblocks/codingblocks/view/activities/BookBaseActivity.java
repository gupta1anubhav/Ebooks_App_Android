package com.codingblocks.codingblocks.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ExpandableListView;

import com.codingblocks.codingblocks.MainActivity;
import com.codingblocks.codingblocks.network.API;
import com.codingblocks.codingblocks.network.APIBook;
import com.codingblocks.codingblocks.network.interfaces.GitbookAPI;
import com.codingblocks.codingblocks.R;
import com.codingblocks.codingblocks.adapters.ExapndableListAdapter;

import com.codingblocks.codingblocks.models.BookData;
import com.codingblocks.codingblocks.models.Chapter;
import com.codingblocks.codingblocks.models.Contents;
import com.codingblocks.codingblocks.view.fragments.BookPageFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookBaseActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    ExpandableListView expandableListView;
    public static final String TAG = "BookBase";
    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_base);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Blocks of JS");

        final ArrayList<String> groupList = new ArrayList<>();
        final HashMap<String, ArrayList<String>> childMap = new HashMap<>();
        final ExapndableListAdapter exapndableListAdapter = new ExapndableListAdapter(this, groupList, childMap);
        GitbookAPI fetchInterface = API.getInstance().retrofit.create(GitbookAPI.class);
        final Contents[] thisContents = new Contents[1];

        fetchInterface.getContent().enqueue(new Callback<Contents>() {
            @Override
            public void onResponse(Call<Contents> call, Response<Contents> response) {

                Pattern pattern = Pattern.compile("(?<=^| )\\d+(\\.\\d+)?(?=$| )");
                String thisGroup = "";
                ArrayList<String> thisGroupChild = new ArrayList<String>();
                thisContents[0] = response.body();
                for (Chapter chapter : response.body().getProgress().getChapters()) {
                    if (pattern.matcher(chapter.getLevel()).matches()) {
                        thisGroup = chapter.getTitle();
                        groupList.add(thisGroup);
                        thisGroupChild = new ArrayList<String>();
                        Log.d(TAG, "onResponse: if" + thisGroup);
                        childMap.put(thisGroup, thisGroupChild);
                    } else {
                        thisGroupChild.add(chapter.getTitle());
                        childMap.put(thisGroup, thisGroupChild);
                        Log.d(TAG, "onResponse: else" + chapter.getTitle());
                    }


                }


                fragment = BookPageFragment.getInstance(thisContents[0].getSections().get(0).getContent());
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.add(R.id.flBookFrame, fragment);
                fragmentTransaction.commit();

                exapndableListAdapter.notifyDataSetChanged();

            }

            @Override
            public void onFailure(Call<Contents> call, Throwable t) {

            }
        });

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        expandableListView = (ExpandableListView) findViewById(R.id.evNavigationList);

        expandableListView.setAdapter(exapndableListAdapter);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);


        drawer.addDrawerListener(toggle);

        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);

        expandableListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                Log.d(TAG, "onGroupClick: ");
                String thisJson = groupList.get(groupPosition);
                Chapter ch = new Chapter();
                for (Chapter contents : thisContents[0].getProgress().getChapters()) {
                    if (thisJson.equals(contents.getTitle())) {
                        ch = contents;
                        ch.setPath(ch.getPath().replace(".md", ".json"));
                    }
                }
                APIBook.getInstance().retrofit
                        .create(GitbookAPI.class)
                        .getBook(ch.getPath())
                        .enqueue(new Callback<BookData>() {
                            @Override
                            public void onResponse(Call<BookData> call, Response<BookData> response) {
                                Log.d(TAG, "onResponse: ");
                                ((BookPageFragment) fragment).replacePage(response.body().getSections().get(0).getContent());
                            }

                            @Override
                            public void onFailure(Call<BookData> call, Throwable t) {
                                Log.d(TAG, "onFailure: " + t.getMessage());
                            }
                        });

                return false;
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Log.d(TAG, "onChildClick: " + v.toString());
                String thisJson = childMap.get(groupList.get(groupPosition)).get(childPosition);
                Chapter ch = new Chapter();
                for (Chapter contents : thisContents[0].getProgress().getChapters()) {
                    if (thisJson.equals(contents.getTitle())) {
                        ch = contents;
                        ch.setPath(ch.getPath().replace(".md", ".json"));
                    }
                }
                Log.d(TAG, "onChildClick: " + ch.getPath());
                APIBook.getInstance().retrofit
                        .create(GitbookAPI.class)
                        .getBook(ch.getPath())
                        .enqueue(new Callback<BookData>() {
                            @Override
                            public void onResponse(Call<BookData> call, Response<BookData> response) {
                                Log.d(TAG, "onResponse: ");
                                ((BookPageFragment) fragment).replacePage((response.body().getSections().get(0).getContent()));
                            }

                            @Override
                            public void onFailure(Call<BookData> call, Throwable t) {
                                Log.d(TAG, "onFailure: " + t.getMessage());
                            }
                        });
                drawer.closeDrawer(GravityCompat.START);
                return true;
            }
        });


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Log.d("TAG", "onNavigationItemSelected: " + item.getTitle());


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


}
