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

    private String getNamedDataId(String contactId) {
        ContentResolver cr = getContentResolver();
        Cursor pCur = cr.query(
                ContactsContract.Data.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?",
                new String[]{contactId, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE},
                null);
        if (pCur.moveToNext()) {
            return pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName._ID));
        }
        return null;
    }


    private void deleteRawContacts(Data data) {
        List<String> contactIds = getContactIds(data.contacts);
        List<String> dataIds = new ArrayList<>();
        Log.d(TAG, "删除操作开始，首先列出来可能要删除的contactIds的集合 : " + contactIds.toString());

        ContentResolver cr = getContentResolver();
        String selection = ContactsContract.Contacts._ID + " in (" + TextUtils.join(",", contactIds) + ")";
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, selection, null, null);
        while(cur.moveToNext()) {
            String contactId = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
            if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                Log.e(TAG, "这个contactId == " + contactId + " , 不需要删除，data表中还含有phone类型的记录");
                contactIds.remove(contactId);
            } else {
                String dataId = getNamedDataId(contactId);
                if (!TextUtils.isEmpty(dataId)) {
                    dataIds.add(dataId);
                }
            }
        }
        Log.d(TAG, "真正需要删除的contactIds的集合 : " + contactIds.toString());
        if (contactIds.size() == 0) {
            Log.e(TAG, "此次操作不需要删除系统的contacts表,raw_contacts表中的记录,结束任务");
            return;
        }

        ArrayList<ContentProviderOperation> dataOps = new ArrayList<>();
        for (String dataId : dataIds) {
            dataOps.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(ContactsContract.Data._ID + "=?", new String[]{dataId})
                    .build());
        }
        Log.e(TAG, "先删除data表中类型是name的记录, 他们的dataId是 : " + dataIds);
        try {
            cr.applyBatch(ContactsContract.AUTHORITY, dataOps);
        } catch (RemoteException | OperationApplicationException e) {
        }

        String where = ContactsContract.RawContacts.CONTACT_ID + " = ? ";
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (String contactId : contactIds) {
            ops.add(ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                    .withSelection(where, new String[]{contactId})
                    .build());
        }
        Log.e(TAG, "删除完data表中的记录，再删除contacts表，和raw_contacts表中的记录, contactId是 : " + contactIds);

        try {
            cr.applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
        }
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
