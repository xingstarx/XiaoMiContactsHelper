package com.star.contacts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.TextView;

import com.star.contacts.view.MergeRecyclerAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private RecyclerView mListView;
    private MergeRecyclerAdapter mergeRecyclerAdapter;
    private DupContactAdapter mDupContactAdapter;
    private ContactAdapter mContactAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (RecyclerView) findViewById(R.id.list_view);
        mergeRecyclerAdapter = new MergeRecyclerAdapter();
        mDupContactAdapter = new DupContactAdapter();
        mContactAdapter = new ContactAdapter();
        mergeRecyclerAdapter.addAdapter(mDupContactAdapter);
        mergeRecyclerAdapter.addAdapter(mContactAdapter);

        List<Contact> contracts = getContracts(getContentResolver());

        mAdapter = new ContractAdapter(this, contracts);
        mListView.setAdapter(mAdapter);
    }

    private List<Contract> getContracts(ContentResolver cr) {
        List<Contract> contracts = new ArrayList<>();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.SORT_KEY_PRIMARY);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String dataId = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
                        Log.e(TAG, "name == " + name + ", phoneNo == " + phoneNo +
                                ", dataId == " + dataId + ", contactId == " + id);
                        contracts.add(new Contract(name, phoneNo, dataId));
                    }
                    pCur.close();
                }
            }
        }
        cur.close();
        return contacts;
    }


    private ProgressDialog mProgressDialog;


    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Loading...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.cancel();
            mProgressDialog = null;
        }
    }

    private void deleteContract(String contactId) {
        showProgressDialog();
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                .withSelection(ContactsContract.Data._ID + "=?", new String[]{contactId})
                .build());
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }

        mListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideProgressDialog();
                mAdapter.setData(getContracts(getContentResolver()));
                mAdapter.notifyDataSetChanged();
            }
        }, 500);
    }

    public static class Contact {
        String displayName;
        String phoneNumber;
        String dataId;//代表data表里面的id

        Contract(String displayName, String phoneNumber, String dataId) {
            this.displayName = displayName;
            this.phoneNumber = phoneNumber;
            this.dataId = dataId;
        }
    }


    class DupContactAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements MergeRecyclerAdapter.OnViewTypeCheckListener {
        private static final int ITEM_HEADER = 0;
        private static final int ITEM_CONTENT = 1;
        private List<Contact> mData = new ArrayList<>();

        public DupContactAdapter(List<Contact> mData) {
            this.mData = mData;
        }

        public DupContactAdapter() {
        }

        public void setData(List<Contact> data) {
            this.mData = data;
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
                Contact contact = mData.get(position - 1);
                ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
                contentViewHolder.phoneView.setText(contact.phoneNumber);
                contentViewHolder.nameView.setText(contact.displayName);
                contentViewHolder.checkBox.setVisibility(View.VISIBLE);
                contentViewHolder.checkBox.setChecked(false);
            } else if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.titleView.setText("重复联系人");
                headerViewHolder.checkBox.setVisibility(View.VISIBLE);
                headerViewHolder.checkBox.setChecked(false);
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
        private static final int ITEM_HEADER = 2;
        private static final int ITEM_CONTENT = 3;
        private List<Contact> mData = new ArrayList<>();

        public ContactAdapter(List<Contact> mData) {
            this.mData = mData;
        }

        public ContactAdapter() {
        }

        public void setData(List<Contact> data) {
            this.mData = data;
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
                Contact contact = mData.get(position - 1);
                ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
                contentViewHolder.phoneView.setText(contact.phoneNumber);
                contentViewHolder.nameView.setText(contact.displayName);
            }
            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.titleView.setText("联系人");
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

    private class HandleContactTask extends AsyncTask<Void, Void, String> {

        private List<Contact> mDupContacts;
        private List<Contact> mContacts;
        @Override
        protected String doInBackground(Void... params) {
            List<Contact> contacts = getContracts(getContentResolver());
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
