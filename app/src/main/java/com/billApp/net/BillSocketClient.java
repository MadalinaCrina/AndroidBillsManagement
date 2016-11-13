package com.billApp.net;

import android.content.Context;
import android.util.Log;

import com.billApp.R;
import com.billApp.content.Bill;
import com.billApp.net.mapping.BillJsonObjectReader;
import com.billApp.net.mapping.IdJsonObjectReader;

import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.billApp.net.mapping.Api.Bill.BILL_CREATED;
import static com.billApp.net.mapping.Api.Bill.BILL_DELETED;
import static com.billApp.net.mapping.Api.Bill.BILL_UPDATED;

public class BillSocketClient {
  private static final String TAG = BillSocketClient.class.getSimpleName();
  private final Context mContext;
  private Socket mSocket;
  private ResourceChangeListener<Bill> mResourceListener;

  public BillSocketClient(Context context) {
    mContext = context;
    Log.d(TAG, "created");
  }

  public void subscribe(final ResourceChangeListener<Bill> resourceListener) {
    Log.d(TAG, "subscribe");
    mResourceListener = resourceListener;
    try {
      mSocket = IO.socket(mContext.getString(R.string.api_url));
      mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Log.d(TAG, "socket connected");
        }
      });
      mSocket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          Log.d(TAG, "socket disconnected");
        }
      });
      mSocket.on(BILL_CREATED, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          try {
            Bill bill = new BillJsonObjectReader().read((JSONObject) args[0]);
            Log.d(TAG, String.format("bill created %s", bill.toString()));
            mResourceListener.onCreated(bill);
          } catch (Exception e) {
            Log.w(TAG, "bill created", e);
            mResourceListener.onError(new ResourceException(e));
          }
        }
      });
      mSocket.on(BILL_UPDATED, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          try {
            Bill bill = new BillJsonObjectReader().read((JSONObject) args[0]);
            Log.d(TAG, String.format("bill updated %s", bill.toString()));
            mResourceListener.onUpdated(bill);
          } catch (Exception e) {
            Log.w(TAG, "bill updated", e);
            mResourceListener.onError(new ResourceException(e));
          }
        }
      });
      mSocket.on(BILL_DELETED, new Emitter.Listener() {
        @Override
        public void call(Object... args) {
          try {
            String id = new IdJsonObjectReader().read((JSONObject) args[0]);
            Log.d(TAG, String.format("bill deleted %s", id));
            mResourceListener.onDeleted(id);
          } catch (Exception e) {
            Log.w(TAG, "bill deleted", e);
            mResourceListener.onError(new ResourceException(e));
          }
        }
      });
      mSocket.connect();
    } catch (Exception e) {
      Log.w(TAG, "socket error", e);
      mResourceListener.onError(new ResourceException(e));
    }
  }

  public void unsubscribe() {
    Log.d(TAG, "unsubscribe");
    if (mSocket != null) {
      mSocket.disconnect();
    }
    mResourceListener = null;
  }

}
