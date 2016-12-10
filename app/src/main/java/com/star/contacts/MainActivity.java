package com.star.contacts;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        List<Contract> contracts = getContracts(this.getContentResolver());
        mAdapter = new ContractAdapter(this, contracts);
        mListView.setAdapter(mAdapter);
    }

    public List<Contract> getContracts(ContentResolver cr) {
        List<Contract> contracts = new ArrayList<>();
        Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (phones.moveToNext()) {
            String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            Log.e(TAG, "name == " + name + ", phoneNumber == " + phoneNumber);
            contracts.add(new Contract(name, phoneNumber));
        }
        phones.close();
        return contracts;
    }

    public static class Contract {
        public String displayName;
        public String phoneNumber;

        public Contract(String displayName, String phoneNumber) {
            this.displayName = displayName;
            this.phoneNumber = phoneNumber;
        }
    }

    class ContractAdapter extends BaseAdapter {
        public Context context;
        public List<Contract> contracts;
        public ContractAdapter(Context context, List<Contract> contracts) {
            this.context = context;
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
