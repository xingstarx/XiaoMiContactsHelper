package com.star.contacts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private ListView mListView;
    ContractAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.list_view);

        List<Contract> contracts = getContracts(getContentResolver());
        mAdapter = new ContractAdapter(this, contracts);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Contract contract = (Contract) mAdapter.getItem(position);
                Log.e(TAG, "contract id == " + contract.contactId);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                alertDialogBuilder.setTitle("删除联系人").setMessage("确定要删除该联系人吗?").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteContract(contract.contactId);
                        dialog.dismiss();
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        });
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
                        String phoneId = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));
                        Log.e(TAG, "name == " + name + ", phoneNo == " + phoneNo +
                                ", phoneId == " + phoneId);
                        contracts.add(new Contract(name, phoneNo, phoneId));
                    }
                    pCur.close();
                }
            }
        }
        cur.close();
        return contracts;
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
                .withSelection(ContactsContract.Data._ID + "=? and " + ContactsContract.Data.MIMETYPE + "=?",
                        new String[]{contactId, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE})
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

    public static class Contract {
        String displayName;
        String phoneNumber;
        String contactId;

        Contract(String displayName, String phoneNumber, String contactId) {
            this.displayName = displayName;
            this.phoneNumber = phoneNumber;
            this.contactId = contactId;
        }
    }

    class ContractAdapter extends BaseAdapter {
        public Context context;
        public List<Contract> contracts;

        public ContractAdapter(Context context, List<Contract> contracts) {
            this.context = context;
            this.contracts = contracts;
        }

        public void setData(List<Contract> contracts) {
            this.contracts = contracts;
        }

        @Override
        public int getCount() {
            return contracts.size();
        }

        @Override
        public Object getItem(int i) {
            return contracts.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, container, false);
                viewHolder = new ViewHolder();
                viewHolder.displayNameTv = (TextView) convertView.findViewById(android.R.id.text1);
                viewHolder.phoneNumberTv = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Contract contract = contracts.get(position);
            viewHolder.displayNameTv.setText(contract.displayName);
            viewHolder.phoneNumberTv.setText(contract.phoneNumber);
            return convertView;
        }

    }

    static class ViewHolder {
        TextView displayNameTv;
        TextView phoneNumberTv;
    }
}
