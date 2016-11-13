package com.billApp;

import android.app.Application;
import android.util.Log;

import com.billApp.net.BillRestClient;
import com.billApp.net.BillSocketClient;
import com.billApp.service.BillManager;

public class App extends Application {
  public static final String TAG = App.class.getSimpleName();
  private BillManager mBillManager;
  private BillRestClient mBillRestClient;
  private BillSocketClient mBillSocketClient;

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate");
    super.onCreate();
    mBillManager = new BillManager(this);
    mBillRestClient = new BillRestClient(this);
    mBillSocketClient = new BillSocketClient(this);
    mBillManager.setBillRestClient(mBillRestClient);
    mBillManager.setBillSocketClient(mBillSocketClient);
  }

  public BillManager getBillManager() {
    return mBillManager;
  }

  @Override
  public void onTerminate() {
    Log.d(TAG, "onTerminate");
    super.onTerminate();
  }
}
