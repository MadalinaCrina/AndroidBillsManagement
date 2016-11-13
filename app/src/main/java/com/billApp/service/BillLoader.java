package com.billApp.service;

import android.content.Context;
import android.util.Log;

import com.billApp.content.Bill;
import com.billApp.net.LastModifiedList;
import com.billApp.util.CancellableCallable;
import com.billApp.util.OkAsyncTaskLoader;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class BillLoader extends OkAsyncTaskLoader<List<Bill>> implements Observer {
  private static final String TAG = BillLoader.class.getSimpleName();
  private final BillManager mBillManager;
  private List<Bill> mCachedBills;
  private CancellableCallable<LastModifiedList<Bill>> mCancellableCall;

  public BillLoader(Context context, BillManager billManager) {
    super(context);
    mBillManager = billManager;
  }

  @Override
  public List<Bill> tryLoadInBackground() throws Exception {
    // This method is called on a background thread and should generate a
    // new set of data to be delivered back to the client
    Log.d(TAG, "tryLoadInBackground");
    mCancellableCall = mBillManager.getBillsCall();
    mCachedBills = mBillManager.executeBillsCall(mCancellableCall);
    return mCachedBills;
  }

  @Override
  public void deliverResult(List<Bill> data) {
    Log.d(TAG, "deliverResult");
    if (isReset()) {
      Log.d(TAG, "deliverResult isReset");
      // The Loader has been reset; ignore the result and invalidate the data.
      return;
    }
    mCachedBills = data;
    if (isStarted()) {
      Log.d(TAG, "deliverResult isStarted");
      // If the Loader is in a started state, deliver the results to the
      // client. The superclass method does this for us.
      super.deliverResult(data);
    }
  }

  @Override
  protected void onStartLoading() {
    Log.d(TAG, "onStartLoading");
    if (mCachedBills != null) {
      Log.d(TAG, "onStartLoading cached not null");
      // Deliver any previously loaded data immediately.
      deliverResult(mCachedBills);
    }
    // Begin monitoring the underlying data source.
    mBillManager.addObserver(this);
    if (takeContentChanged() || mCachedBills == null) {
      // When the observer detects a change, it should call onContentChanged()
      // on the Loader, which will cause the next call to takeContentChanged()
      // to return true. If this is ever the case (or if the current data is
      // null), we force a new load.
      Log.d(TAG, "onStartLoading cached null force reload");
      forceLoad();
    }
  }

  @Override
  protected void onStopLoading() {
    // The Loader is in a stopped state, so we should attempt to cancel the
    // current load (if there is one).
    Log.d(TAG, "onStopLoading");
    cancelLoad();
    // Bill that we leave the observer as is. Loaders in a stopped state
    // should still monitor the data source for changes so that the Loader
    // will know to force a new load if it is ever started again.
  }

  @Override
  protected void onReset() {
    // Ensure the loader has been stopped.
    Log.d(TAG, "onReset");
    onStopLoading();
    // At this point we can release the resources associated with 'mData'.
    if (mCachedBills != null) {
      mCachedBills = null;
    }
    // The Loader is being reset, so we should stop monitoring for changes.
    mBillManager.deleteObserver(this);
  }

  @Override
  public void onCanceled(List<Bill> data) {
    // Attempt to cancel the current asynchronous load.
    Log.d(TAG, "onCanceled");
    super.onCanceled(data);
  }

  @Override
  public void update(Observable o, Object arg) {
    mCachedBills = mBillManager.getCachedBills();
  }
}
