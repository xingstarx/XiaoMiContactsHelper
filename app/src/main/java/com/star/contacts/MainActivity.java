package com.star.contacts;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.star.contacts.model.Contact;
import com.star.contacts.service.UpdateContactService;
import com.star.contacts.util.Log;
import com.star.contacts.view.MergeRecyclerAdapter;
import com.star.contacts.view.SwipeableRecyclerViewTouchListener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    public static final String TAG = "MainActivity";
    private static final int RC_PERMISSION_CONTACTS = 11;
    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private MergeRecyclerAdapter mergeRecyclerAdapter;
    private DupContactAdapter mDupContactAdapter;
    private ContactAdapter mContactAdapter;
    private HandleContactTask mContactTask;
    private boolean mIsDeleteAction = false;
    private Handler mHandler;
    private SwipeableRecyclerViewTouchListener mSwipeableRecyclerViewTouchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        mRecyclerView = (RecyclerView) findViewById(R.id.list_view);
        mEmptyView = (TextView) findViewById(android.R.id.empty);
        setDrawableTop(mEmptyView, R.drawable.ic_phone_contacts_black);
        mergeRecyclerAdapter = new MergeRecyclerAdapter();
        mDupContactAdapter = new DupContactAdapter();
        mContactAdapter = new ContactAdapter();
        mergeRecyclerAdapter.addAdapter(mDupContactAdapter);
        mergeRecyclerAdapter.addAdapter(mContactAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        mRecyclerView.setAdapter(mergeRecyclerAdapter);
        initItemTouch();
        loadContacts();
    }

    public void setDrawableTop(TextView textView, @DrawableRes int resourceId) {
        Drawable drawable = VectorDrawableCompat.create(textView.getResources(), resourceId, textView.getContext().getTheme());
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(textView, null, drawable, null, null);
    }


    @AfterPermissionGranted(RC_PERMISSION_CONTACTS)
    private void loadContacts() {
        String[] perms = {Manifest.permission.WRITE_CONTACTS, Manifest.permission.CALL_PHONE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            mContactTask = new HandleContactTask();
            mContactTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
            EasyPermissions.requestPermissions(this, getString(R.string.permission_request_contacts),
                    RC_PERMISSION_CONTACTS, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
    }

    private void initItemTouch() {
        mSwipeableRecyclerViewTouchListener =
                new SwipeableRecyclerViewTouchListener(
                        this,
                        mRecyclerView,
                        R.id.foreground,
                        R.id.background,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                return ContactAdapter.ITEM_CONTENT == mergeRecyclerAdapter.getItemViewType(position);
                            }

                            @Override
                            public void onDismissedBySwipe(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                List<Contact> contacts = new ArrayList<>();
                                for (int position : reverseSortedPositions) {
                                    contacts.add(mContactAdapter.getItem(mergeRecyclerAdapter.getViewAdapterPosition(position)));
                                }
                                deleteMultiContract(contacts);
                            }

                        });
        mRecyclerView.addOnItemTouchListener(mSwipeableRecyclerViewTouchListener);

    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!mIsDeleteAction) {
            menu.removeItem(R.id.action_remove);
        } else {
            menu.removeItem(R.id.action_search);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove:
                if (mSwipeableRecyclerViewTouchListener.getPendingDismissesSize() == 0) {
                    handleDeleteContacts();
                } else {
                    Toast.makeText(this, R.string.toast_delete_mutil_contacts_tips, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.action_search:
                if (mDupContactAdapter.getData().size() + mContactAdapter.getData().size() > 0) {
                    List<Contact> contacts = new CopyOnWriteArrayList<>();
                    if (mDupContactAdapter.getData().size() > 0) {
                        contacts.addAll(mDupContactAdapter.getData());
                    }
                    if (mContactAdapter.getData().size() > 0) {
                        contacts.addAll(mContactAdapter.getData());
                    }
                    SearchActivity.startSearchContact(this, new ArrayList<Parcelable>(contacts));
                }
                break;
            case R.id.action_about:
                AboutActivity.showAbout(MainActivity.this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleDeleteContacts() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setTitle(R.string.alert_dialog_delete_title).setMessage(R.string.alert_dialog_delete_message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                deleteContacts();
                dialog.dismiss();
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).show();
    }

    private void deleteContacts() {
        List<Contact> contacts = mDupContactAdapter.getCheckedData();
        if (contacts.size() == 0) {
            return;
        }
        deleteMultiContract(contacts);
    }

    private void deleteMultiContract(List<Contact> contacts) {
        showProgressDialog();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (int i = 0; i < contacts.size(); i++) {
            Log.d(TAG, "deleteMultiContract contacts.dataId == " + contacts.get(i).dataId + ", name == " + contacts.get(i).displayName);
            ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data._ID + "=?",
                            new String[]{contacts.get(i).dataId})
                    .build());
        }
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
        UpdateContactService.updateContacts(MainActivity.this, (ArrayList<Contact>) contacts);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mContactTask = new HandleContactTask();
                mContactTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        }, 100);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mContactTask != null) {
            mContactTask.cancel(true);
            mContactTask = null;
        }
    }

    /**
     * 用来查询联系人，把相同用户名的id放到一组，类似于{xingxing:100,139,40}这样的形式
     *
     * @param cr
     * @return
     */
    private Map<String, String> divideContacts(ContentResolver cr) {
        Map<String, String> map = new LinkedHashMap<>();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (map.containsKey(name)) {
                    String values = map.get(name);
                    values += ("," + id);
                    map.put(name, values);
                } else {
                    map.put(name, id);
                }
            }
        }
        cur.close();
        return map;
    }

    private List<Contact> getContactById(ContentResolver cr, String contactId, String displayName) {
        List<Contact> contacts = new ArrayList<>();
        Cursor pCur = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                new String[]{contactId},
                null);
        while (pCur.moveToNext()) {
            String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String dataId = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));//其实是data表中的_id
            String rawContactId = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID));
            Log.d(TAG, "name == " + displayName + ", phoneNo == " + phoneNo + ", dataId == " + dataId + ", contactId == " + contactId + ", rawContactId == " + rawContactId);
            contacts.add(new Contact(displayName, phoneNo, contactId, dataId, rawContactId));
        }
        pCur.close();
        return contacts;
    }

    private void generateContracts(ContentResolver cr, Map<String, String> map, List<Contact> dupContacts, List<Contact> contacts) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value.contains(",")) {
                String ids[] = value.split(",");
                for (String id : ids) {
                    List<Contact> subContacts = getContactById(cr, id, key);
                    if (subContacts.size() > 0) {
                        dupContacts.addAll(subContacts);
                    }
                }
            } else {
                List<Contact> subContacts = getContactById(cr, value, key);
                if (subContacts.size() > 0) {
                    if (subContacts.size() == 1) {
                        contacts.addAll(subContacts);
                    } else {
                        dupContacts.addAll(subContacts);
                    }
                }
            }
        }
    }

    private ProgressDialog mProgressDialog;

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setCancelable(false);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    class DupContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MergeRecyclerAdapter.OnViewTypeCheckListener {
        public static final int ITEM_HEADER = 0;
        public static final int ITEM_CONTENT = 1;
        private List<Contact> mData = new ArrayList<>();
        private List<Contact> mCheckedData = new ArrayList<>();

        public List<Contact> getCheckedData() {
            return mCheckedData;
        }

        public DupContactAdapter(List<Contact> mData) {
            this.mData = mData;
        }

        public DupContactAdapter() {
        }

        public void setData(List<Contact> data) {
            this.mData = data;
        }

        public List<Contact> getData() {
            return mData;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_HEADER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_header_1, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == ITEM_CONTENT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_content_1, parent, false);
                return new ContentViewHolder(view);
            }
            throw new IllegalStateException("Adapter don't have this viewType " + viewType);
        }

        private void handleMenu() {
            if (!mIsDeleteAction && mCheckedData.size() > 0) {
                mIsDeleteAction = true;
                invalidateOptionsMenu();
            } else if (mIsDeleteAction && mCheckedData.size() == 0) {
                mIsDeleteAction = false;
                invalidateOptionsMenu();
            }
        }

        public void resetMenu() {
            if (mIsDeleteAction) {
                mIsDeleteAction = false;
                mCheckedData.clear();
                invalidateOptionsMenu();
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof ContentViewHolder) {
                final Contact contact = mData.get(position - 1);
                ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
                contentViewHolder.phoneView.setText(contact.phoneNumber);
                contentViewHolder.nameView.setText(contact.displayName);
                contentViewHolder.checkBox.setVisibility(View.VISIBLE);
                contentViewHolder.checkBox.setChecked(false);
                contentViewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        if (b) {
                            mCheckedData.add(contact);
                        } else {
                            mCheckedData.remove(contact);
                        }
                        handleMenu();
                    }
                });
                contentViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact.phoneNumber));
                        if (EasyPermissions.hasPermissions(MainActivity.this, Manifest.permission.CALL_PHONE)) {
                            startActivity(intent);
                        }
                    }
                });
            } else if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.titleView.setText(R.string.activity_main_dup_contact_header_title);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size() == 0 ? 0 : mData.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return ITEM_HEADER;
            }
            return ITEM_CONTENT;
        }

        @Override
        public boolean checkViewType(int viewType) {
            return viewType == ITEM_HEADER || viewType == ITEM_CONTENT;
        }

        class ContentViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;
            TextView phoneView;
            CheckBox checkBox;

            public ContentViewHolder(View itemView) {
                super(itemView);
                nameView = (TextView) itemView.findViewById(R.id.name);
                phoneView = (TextView) itemView.findViewById(R.id.phone);
                checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
            }
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            CheckBox checkBox;

            public HeaderViewHolder(View itemView) {
                super(itemView);
                titleView = (TextView) itemView.findViewById(R.id.title);
                checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
            }
        }
    }

    class ContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MergeRecyclerAdapter.OnViewTypeCheckListener {
        public static final int ITEM_HEADER = 2;
        public static final int ITEM_CONTENT = 3;
        private List<Contact> mData = new ArrayList<>();

        public ContactAdapter(List<Contact> mData) {
            this.mData = mData;
        }

        public ContactAdapter() {
        }

        public void setData(List<Contact> data) {
            this.mData = data;
        }

        public List<Contact> getData() {
            return mData;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_HEADER) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_header_1, parent, false);
                return new HeaderViewHolder(view);
            } else if (viewType == ITEM_CONTENT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_content_1, parent, false);
                return new ContentViewHolder(view);
            }
            throw new IllegalStateException("Adapter don't have this viewType " + viewType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentViewHolder) {
                final Contact contact = mData.get(position - 1);
                ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
                contentViewHolder.phoneView.setText(contact.phoneNumber);
                contentViewHolder.nameView.setText(contact.displayName);
                contentViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + contact.phoneNumber));
                        if (EasyPermissions.hasPermissions(MainActivity.this, Manifest.permission.CALL_PHONE)) {
                            startActivity(intent);
                        }
                    }
                });
            }
            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.titleView.setText(R.string.activity_main_contact_header_title);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size() == 0 ? 0 : mData.size() + 1;
        }

        public Contact getItem(int position) {
            if (position == 0) {
                return null;
            }
            return mData.get(position - 1);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return ITEM_HEADER;
            }
            return ITEM_CONTENT;
        }

        @Override
        public boolean checkViewType(int viewType) {
            return viewType == ITEM_HEADER || viewType == ITEM_CONTENT;
        }

        public void remove(int position) {
            mData.remove(position - 1);
        }

        class ContentViewHolder extends RecyclerView.ViewHolder {
            TextView nameView;
            TextView phoneView;
            CheckBox checkBox;

            public ContentViewHolder(View itemView) {
                super(itemView);
                nameView = (TextView) itemView.findViewById(R.id.name);
                phoneView = (TextView) itemView.findViewById(R.id.phone);
                checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
            }
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView titleView;
            CheckBox checkBox;

            public HeaderViewHolder(View itemView) {
                super(itemView);
                titleView = (TextView) itemView.findViewById(R.id.title);
                checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);
            }
        }
    }

    private class HandleContactTask extends AsyncTask<Void, Void, String> {

        private List<Contact> mDupContacts = new ArrayList<>();
        private List<Contact> mContacts = new ArrayList<>();

        @Override
        protected String doInBackground(Void... params) {
            Map<String, String> map = divideContacts(getContentResolver());
            generateContracts(getContentResolver(), map, mDupContacts, mContacts);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            hideProgressDialog();
            if (mRecyclerView.getVisibility() == View.GONE) {
                mRecyclerView.setVisibility(View.VISIBLE);
            }
            if (mEmptyView.getVisibility() == View.VISIBLE) {
                mEmptyView.setVisibility(View.GONE);
            }
            mDupContactAdapter.setData(mDupContacts);
            mDupContactAdapter.resetMenu();
            mContactAdapter.setData(mContacts);
            mergeRecyclerAdapter.notifyDataSetChanged();
            if (mDupContacts.size() == 0 && mContacts.size() == 0) {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
