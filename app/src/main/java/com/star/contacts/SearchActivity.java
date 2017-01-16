package com.star.contacts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.star.contacts.model.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiongxingxing on 17/1/16.
 */

public class SearchActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final String TAG = "SearchActivity";
    private static final String ARG_CONTACTS = "contacts";
    private RecyclerView mRecyclerView;
    private SearchView mSearchView;
    private ArrayList<Contact> mContacts;
    private ContactAdapter mContactAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setElevation(0);
        }
        setContentView(R.layout.activity_search);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mContacts = getIntent().getParcelableArrayListExtra(ARG_CONTACTS);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mContactAdapter = new ContactAdapter(mContacts);
        mRecyclerView.setAdapter(mContactAdapter);
    }

    public static void startSearchContact(Context context, ArrayList<Parcelable> contacts) {
        Intent intent = new Intent(context, SearchActivity.class);
        intent.putParcelableArrayListExtra(ARG_CONTACTS, contacts);
        context.startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        mSearchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        mSearchView.setIconified(false);
        mSearchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        List<Contact> contacts = filterContacts(newText);
        mContactAdapter.setData(contacts);
        return true;
    }

    private List<Contact> filterContacts(String newText) {
        List<Contact> contacts = new ArrayList<>();
        for(Contact contact : mContacts) {
            if (filter(newText, contact)) {
                contacts.add(contact);
            }
        }
        return contacts;
    }

    private boolean filter(String text, Contact contact) {
        if (contact.displayName.toLowerCase().contains(text.toLowerCase()) ||
                contact.phoneNumber.replaceAll("\\s", "").contains(text)) {
            return true;
        }
        return false;
    }


    class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<Contact> mDatas = new ArrayList<>();

        public ContactAdapter(List<Contact> datas) {
            this.mDatas = datas;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(SearchActivity.this).inflate(R.layout.activity_search_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            Contact contact = mDatas.get(position);
            viewHolder.phoneView.setText(contact.phoneNumber);
            viewHolder.nameView.setText(contact.displayName);
        }

        @Override
        public int getItemCount() {
            return mDatas.size();
        }

        public void setData(List<Contact> data) {
            this.mDatas = data;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;
            TextView phoneView;

            public ViewHolder(View itemView) {
                super(itemView);
                nameView = (TextView) itemView.findViewById(R.id.name);
                phoneView = (TextView) itemView.findViewById(R.id.phone);
            }
        }
    }
}
