package com.billApp.service;

import android.content.Context;
import android.util.Log;

import com.billApp.content.Bill;
import com.billApp.content.User;
import com.billApp.content.database.Database;
import com.billApp.net.LastModifiedList;
import com.billApp.net.BillRestClient;
import com.billApp.net.BillSocketClient;
import com.billApp.net.ResourceChangeListener;
import com.billApp.net.ResourceException;
import com.billApp.util.Cancellable;
import com.billApp.util.CancellableCallable;
import com.billApp.util.OnErrorListener;
import com.billApp.util.OnSuccessListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BillManager extends Observable {
  private static final String TAG = BillManager.class.getSimpleName();
  private final Database mKD;

  private ConcurrentMap<String, Bill> mBills = new ConcurrentHashMap<String, Bill>();
  private String mBillsLastUpdate;

  private final Context mContext;
  private BillRestClient mBillRestClient;
  private BillSocketClient mBillSocketClient;
  private String mToken;
  private User mCurrentUser;

  public BillManager(Context context) {
    mContext = context;
    mKD = new Database(context);
  }

  public CancellableCallable<LastModifiedList<Bill>> getBillsCall() {
    Log.d(TAG, "getBillsCall");
    return mBillRestClient.search(mBillsLastUpdate);
  }

  public List<Bill> executeBillsCall(CancellableCallable<LastModifiedList<Bill>> getBillsCall) throws Exception {
    Log.d(TAG, "execute getBills...");
    LastModifiedList<Bill> result = getBillsCall.call();
    List<Bill> bills = result.getList();
    if (bills != null) {
      mBillsLastUpdate = result.getLastModified();
      updateCachedBills(bills);
      notifyObservers();
    }
    return cachedBillsByUpdated();
  }

  public BillLoader getBillLoader() {
    Log.d(TAG, "getBillLoader...");
    return new BillLoader(mContext, this);
  }

  public void setBillRestClient(BillRestClient billRestClient) {
    mBillRestClient = billRestClient;
  }

  public Cancellable getBillsAsync(final OnSuccessListener<List<Bill>> successListener, OnErrorListener errorListener) {
    Log.d(TAG, "getBillsAsync...");
    return mBillRestClient.searchAsync(mBillsLastUpdate, new OnSuccessListener<LastModifiedList<Bill>>() {

      @Override
      public void onSuccess(LastModifiedList<Bill> result) {
        Log.d(TAG, "getBillsAsync succeeded");
        List<Bill> bills = result.getList();
        if (bills != null) {
          mBillsLastUpdate = result.getLastModified();
          updateCachedBills(bills);
        }
        successListener.onSuccess(cachedBillsByUpdated());
        notifyObservers();
      }
    }, errorListener);
  }

  public Cancellable getBillAsync(
      final String billId,
      final OnSuccessListener<Bill> successListener,
      final OnErrorListener errorListener) {
    Log.d(TAG, "getBillAsync...");
    return mBillRestClient.readAsync(billId, new OnSuccessListener<Bill>() {

      @Override
      public void onSuccess(Bill bill) {
        Log.d(TAG, "getBillAsync succeeded");
        if (bill == null) {
          setChanged();
          mBills.remove(billId);
        } else {
          if (!bill.equals(mBills.get(bill.getId()))) {
            setChanged();
            mBills.put(billId, bill);
          }
        }
        successListener.onSuccess(bill);
        notifyObservers();
      }
    }, errorListener);
  }

  public Cancellable saveBillAsync(
      final Bill bill,
      final OnSuccessListener<Bill> successListener,
      final OnErrorListener errorListener) {
    Log.d(TAG, "saveBillAsync...");
    return mBillRestClient.updateAsync(bill, new OnSuccessListener<Bill>() {

      @Override
      public void onSuccess(Bill bill) {
        Log.d(TAG, "saveBillAsync succeeded");
        mBills.put(bill.getId(), bill);
        successListener.onSuccess(bill);
        setChanged();
        notifyObservers();
      }
    }, errorListener);
  }

  public void subscribeChangeListener() {
    mBillSocketClient.subscribe(new ResourceChangeListener<Bill>() {
      @Override
      public void onCreated(Bill bill) {
        Log.d(TAG, "changeListener, onCreated");
        ensureBillCached(bill);
      }

      @Override
      public void onUpdated(Bill bill) {
        Log.d(TAG, "changeListener, onUpdated");
        ensureBillCached(bill);
      }

      @Override
      public void onDeleted(String billId) {
        Log.d(TAG, "changeListener, onDeleted");
        if (mBills.remove(billId) != null) {
          setChanged();
          notifyObservers();
        }
      }

      private void ensureBillCached(Bill bill) {
        if (!bill.equals(mBills.get(bill.getId()))) {
          Log.d(TAG, "changeListener, cache updated");
          mBills.put(bill.getId(), bill);
          setChanged();
          notifyObservers();
        }
      }

      @Override
      public void onError(Throwable t) {
        Log.e(TAG, "changeListener, error", t);
      }
    });
  }

  public void unsubscribeChangeListener() {
    mBillSocketClient.unsubscribe();
  }

  public void setBillSocketClient(BillSocketClient billSocketClient) {
    mBillSocketClient = billSocketClient;
  }

  private void updateCachedBills(List<Bill> bills) {
    Log.d(TAG, "updateCachedBills");
    for (Bill bill : bills) {
      mBills.put(bill.getId(), bill);
    }
    setChanged();
  }

  private List<Bill> cachedBillsByUpdated() {
    ArrayList<Bill> bills = new ArrayList<>(mBills.values());
    Collections.sort(bills, new BillByUpdatedComparator());
    return bills;
  }

  public List<Bill> getCachedBills() {
    return cachedBillsByUpdated();
  }

  public Cancellable loginAsync(
      String username, String password,
      final OnSuccessListener<String> successListener,
      final OnErrorListener errorListener) {
    final User user = new User(username, password);
    return mBillRestClient.getToken(
        user, new OnSuccessListener<String>() {

          @Override
          public void onSuccess(String token) {
            mToken = token;
            if (mToken != null) {
              user.setToken(mToken);
              setCurrentUser(user);
              mKD.saveUser(user);
              successListener.onSuccess(mToken);
            } else {
              errorListener.onError(new ResourceException(new IllegalArgumentException("Invalid credentials")));
            }
          }
        }, errorListener);
  }

  public void setCurrentUser(User currentUser) {
    mCurrentUser = currentUser;
    mBillRestClient.setUser(currentUser);
  }

  public User getCurrentUser() {
    return mKD.getCurrentUser();
  }

  private class BillByUpdatedComparator implements java.util.Comparator<Bill> {
    @Override
    public int compare(Bill b1, Bill b2) {
      return (int) (b1.getUpdated() - b2.getUpdated());
    }
  }
}
