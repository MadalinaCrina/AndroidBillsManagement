package com.billApp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;

import com.billApp.content.User;
import com.billApp.service.BillManager;
import com.billApp.util.Cancellable;
import com.billApp.util.DialogUtils;
import com.billApp.util.OnErrorListener;
import com.billApp.util.OnSuccessListener;

public class Login extends AppCompatActivity {

  private Cancellable mCancellable;
  private BillManager mBillManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    mBillManager = ((App) getApplication()).getBillManager();
    User user = mBillManager.getCurrentUser();
    if (user != null) {
      startBillListActivity();
      finish();
    }
    setupToolbar();
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mCancellable != null) {
      mCancellable.cancel();
    }
  }

  private void setupToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        login();
        Snackbar.make(view, "Authenticating, please wait", Snackbar.LENGTH_INDEFINITE)
            .setAction("Action", null).show();
      }
    });
  }

  private void login() {
    EditText usernameEditText = (EditText) findViewById(R.id.username);
    EditText passwordEditText = (EditText) findViewById(R.id.password);
    mCancellable = mBillManager
        .loginAsync(
            usernameEditText.getText().toString(), passwordEditText.getText().toString(),
            new OnSuccessListener<String>() {
              @Override
              public void onSuccess(String s) {
                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    startBillListActivity();
                  }
                });
              }
            }, new OnErrorListener() {
              @Override
              public void onError(final Exception e) {
                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    DialogUtils.showError(Login.this, e);
                  }
                });
              }
            });
  }

  private void startBillListActivity() {
    startActivity(new Intent(this, BillListActivity.class));
  }
}
