package com.star.contacts.service;

import android.app.Service;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import com.star.contacts.model.Contact;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class UpdateContactService extends Service {
    public static final String TAG = "UpdateContactService";
    public static String TYPE = "type";
    public static final int TYPE_UPDATE_CONTACTS = 1;
    public static String EXTRA_CONTACTS = "contacts";
    private HandlerThread handlerThread;
    private WeakHandler handler;
    private static final int MSG_TYPE_UPDATE_CONTACTS = 100;

    public UpdateContactService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handlerThread = new HandlerThread("workerThread", Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        handler = new WeakHandler(this, handlerThread.getLooper());
    }

    private class WeakHandler extends Handler {
        private WeakReference<UpdateContactService> service;
        public WeakHandler(UpdateContactService service, Looper looper) {
            super(looper);
            this.service = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (service.get() != null) {
                service.get().handleMessage(msg);
            }
        }
    }

    private synchronized void handleMessage(Message msg){
        switch (msg.what) {
            case MSG_TYPE_UPDATE_CONTACTS:
                Data data = (Data) msg.obj;
                deleteRawContacts(data);
                break;
            default:
                break;
        }
    }

    //去重
    private List<String> getContactIds(List<Contact> contacts) {
        final Set<String> contactIds = new LinkedHashSet<>();
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            contactIds.add(contact.contactId);
        }
        return new ArrayList<>(contactIds);
    }

    private List<String> getRawContactIds(List<Contact> contacts) {
        final Set<String> rawContactIds = new LinkedHashSet<>();
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            rawContactIds.add(contact.rawContactId);
        }
        return new ArrayList<>(rawContactIds);
    }

    private void deleteRawContacts(Data data) {
        List<String> contactIds = getContactIds(data.contacts);
        List<String> rawContactIds = getRawContactIds(data.contacts);
        Log.d(TAG, "deleteRawContacts start, need to delete contactIds is: " + contactIds.toString());
        Log.d(TAG, "deleteRawContacts start, need to delete rawContactIds is: " + rawContactIds.toString());

        ContentResolver cr = getContentResolver();
        String selection = ContactsContract.Contacts._ID + " in (" + TextUtils.join(",", contactIds) + ")";
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, selection, null, null);
        while(cur.moveToNext()) {
            String contactId = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
            if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
//                String contactId2 = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.NAME_RAW_CONTACT_ID));
                Log.e(TAG, "your moved a contactId that is " + contactId);
                contactIds.remove(contactId);
            } else {
                //查询到data_id,并统计下来
            }
        }
        if (contactIds.size() == 0) {
            Log.e(TAG, "don't need delete system contacts table or raw_contacts table records!");
            return;
        }
//留下来的就是这种无用的记录在data表中也只有对应的一条
        //先删除data表中，类型是name的这种记录
//        ArrayList<ContentProviderOperation> dataOps = new ArrayList<>();

//        ops.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
//                .withSelection(Data._ID + "=?", new String[]{String.valueOf(dataId)})
//                .build());
//        getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);


        String where = ContactsContract.RawContacts.CONTACT_ID + " = ? ";
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (String contactId : rawContactIds) {
            Log.e(TAG, "you will delete contact record, that contactId is " + contactId);
            ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection(where, new String[]{contactId})
                    .build());
        }
//        try {
//            cr.applyBatch(ContactsContract.AUTHORITY, ops);
//        } catch (RemoteException | OperationApplicationException e) {
//        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void updateContacts(Context context, ArrayList<Contact> contacts) {
        Intent intent = new Intent(context, UpdateContactService.class);
        intent.putExtra(TYPE, TYPE_UPDATE_CONTACTS);
        intent.putParcelableArrayListExtra(EXTRA_CONTACTS, contacts);
        context.startService(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int type = intent.getIntExtra(TYPE, -1);
        if (type == TYPE_UPDATE_CONTACTS) {
            List<Contact> contacts = intent.getParcelableArrayListExtra(EXTRA_CONTACTS);
            Message message = handler.obtainMessage(MSG_TYPE_UPDATE_CONTACTS, new Data(contacts));
            handler.sendMessage(message);
        }
        return START_NOT_STICKY;
    }

    public static class Data{
        public List<Contact> contacts;

        public Data(List<Contact> contacts) {
            this.contacts = contacts;
        }
    }
}
