package com.star.contacts;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.star.contacts.base.BaseActivity;
import com.star.contacts.model.Contact;
import com.star.contacts.util.MIUIUtils;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by xiongxingxing on 17/1/16.
 */

public class SearchActivity extends BaseActivity implements MaterialSearchView.OnQueryTextListener {
    public static final String TAG = "SearchActivity";
    private static final String ARG_CONTACTS = "contacts";
    private RecyclerView mRecyclerView;
    private Toolbar mToolbar;
    private MaterialSearchView mSearchView;
    private ArrayList<Contact> mContacts;
    private ContactAdapter mContactAdapter;

    @NonNull
    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Contact contact = mContactAdapter.getDatas().get(position);
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("contacts", contact.displayName + ", " + contact.phoneNumber);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(SearchActivity.this, R.string.toast_activity_search_copy_content, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mSearchView = (MaterialSearchView) findViewById(R.id.search_view);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            getSupportActionBar().setElevation(0);
        }
        mSearchView.setVoiceSearch(false);
        mSearchView.setEllipsize(true);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnBackListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mContacts = getIntent().getParcelableArrayListExtra(ARG_CONTACTS);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mContactAdapter = new ContactAdapter(mContacts);
        mContactAdapter.setOnItemClickListener(mOnItemClickListener);
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
        MenuItem item = menu.findItem(R.id.action_search);
        mSearchView.setMenuItem(item);
        mSearchView.showSearch(false);
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
        for (Contact contact : mContacts) {
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

        private AdapterView.OnItemClickListener mOnItemClickListener;

        public void setOnItemClickListener(AdapterView.OnItemClickListener onItemClickListener) {
            mOnItemClickListener = onItemClickListener;
        }

        public List<Contact> getDatas() {
            return mDatas;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(SearchActivity.this).inflate(R.layout.activity_search_list_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            ViewHolder viewHolder = (ViewHolder) holder;
            final Contact contact = mDatas.get(position);
            viewHolder.phoneView.setText(contact.phoneNumber);
            viewHolder.nameView.setText(contact.displayName);
            if (mOnItemClickListener != null) {
                viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickListener.onItemClick(null, v, holder.getAdapterPosition(), holder.getItemId());
                        return true;
                    }
                });
            }
            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && MIUIUtils.isMIUI()) {
                        //public static final int OP_CALL_PHONE = 13;
                        if (!MIUIUtils.checkOp(SearchActivity.this, 13)) {
                            MIUIUtils.openMiuiPermissionActivity(SearchActivity.this);
                            return;
                        }
                    }
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact.phoneNumber));
                    if (EasyPermissions.hasPermissions(SearchActivity.this, Manifest.permission.CALL_PHONE)) {
                        startActivity(intent);
                    }
                }
            });
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
