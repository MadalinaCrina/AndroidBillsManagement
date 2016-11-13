package com.billApp.net;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import com.billApp.R;
import com.billApp.content.Bill;
import com.billApp.content.User;
import com.billApp.net.mapping.CredentialsWriter;
import com.billApp.net.mapping.IssueReader;
import com.billApp.net.mapping.BillReader;
import com.billApp.net.mapping.BillWriter;
import com.billApp.net.mapping.TokenReader;
import com.billApp.net.mapping.ResourceListReader;
import com.billApp.util.Cancellable;
import com.billApp.util.CancellableCallable;
import com.billApp.util.OnErrorListener;
import com.billApp.util.OnSuccessListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import io.socket.client.Socket;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BillRestClient {
  private static final String TAG = BillRestClient.class.getSimpleName();
  public static final String APPLICATION_JSON = "application/json";
  public static final String UTF_8 = "UTF-8";
  public static final String LAST_MODIFIED = "Last-Modified";

  private final OkHttpClient mOkHttpClient;
  private final String mApiUrl;
  private final String mBillUrl;
  private final Context mContext;
  private final String mAuthUrl;
  private Socket mSocket;
  private User mUser;

  public BillRestClient(Context context) {
    mContext = context;
    mOkHttpClient = new OkHttpClient();
    mApiUrl = context.getString(R.string.api_url);
    mBillUrl = mApiUrl.concat("/api/bill");
    mAuthUrl = mApiUrl.concat("/api/auth");
    Log.d(TAG, "BillRestClient created");
  }

  public CancellableOkHttpAsync<String> getToken(User user, OnSuccessListener<String> successListener, OnErrorListener errorListener) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    JsonWriter writer = null;
    try {
      writer = new JsonWriter(new OutputStreamWriter(baos, UTF_8));
      new CredentialsWriter().write(user, writer);
      writer.close();
    } catch (Exception e) {
      Log.e(TAG, "getToken failed", e);
      throw new ResourceException(e);
    }
    return new CancellableOkHttpAsync<String>(
        new Request.Builder()
            .url(String.format("%s/session", mAuthUrl))
            .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), baos.toByteArray()))
            .build(),
        new ResponseReader<String>() {
          @Override
          public String read(Response response) throws Exception {
            JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
            if (response.code() == 201) { //created
              return new TokenReader().read(reader);
            } else {
              return null;
            }
          }
        },
        successListener,
        errorListener
    );
  }

  public OkHttpCancellableCallable<LastModifiedList<Bill>> search(String mNotesLastUpdate) {
    Request.Builder requestBuilder = new Request.Builder().url(mBillUrl);
    if (mNotesLastUpdate != null) {
      requestBuilder.header(LAST_MODIFIED, mNotesLastUpdate);
    }
    addAuthToken(requestBuilder);
    return new OkHttpCancellableCallable<LastModifiedList<Bill>>(
        requestBuilder.build(),
        new ResponseReader<LastModifiedList<Bill>>() {
          @Override
          public LastModifiedList<Bill> read(Response response) throws Exception {
            JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
            if (response.code() == 304) { //not modified
              return new LastModifiedList<Bill>(response.header(LAST_MODIFIED), null);
            } else {
              return new LastModifiedList<Bill>(
                  response.header(LAST_MODIFIED),
                  new ResourceListReader<Bill>(new BillReader()).read(reader));
            }
          }
        }
    );
  }

  private void addAuthToken(Request.Builder requestBuilder) {
    User user = mUser;
    if (user != null) {
      requestBuilder.header("Authorization", String.format("Bearer %s", user.getToken()));
    }
  }

  public Cancellable searchAsync(
      String mBillsLastUpdate,
      final OnSuccessListener<LastModifiedList<Bill>> successListener,
      final OnErrorListener errorListener)  {
    Request.Builder requestBuilder = new Request.Builder().url(mBillUrl);
    if (mBillsLastUpdate != null) {
      requestBuilder.header(LAST_MODIFIED, mBillsLastUpdate.substring(0,40));
    }
    addAuthToken(requestBuilder);
    return new CancellableOkHttpAsync<LastModifiedList<Bill>>(
        requestBuilder.build(),
        new ResponseReader<LastModifiedList<Bill>>() {
          @Override
          public LastModifiedList<Bill> read(Response response) throws Exception {
            JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
            if (response.code() == 304) { //not modified
              return new LastModifiedList<Bill>(response.header(LAST_MODIFIED), null);
            } else {
              return new LastModifiedList<Bill>(
                  response.header(LAST_MODIFIED),
                  new ResourceListReader<Bill>(new BillReader()).read(reader));
            }
          }
        },
        successListener,
        errorListener
    );
  }

  public Cancellable readAsync(String noteId,
                               final OnSuccessListener<Bill> successListener,
                               final OnErrorListener errorListener) {
    Request.Builder builder = new Request.Builder().url(String.format("%s/%s", mBillUrl, noteId));
    addAuthToken(builder);
    return new CancellableOkHttpAsync<Bill>(
        builder.build(),
        new ResponseReader<Bill>() {
          @Override
          public Bill read(Response response) throws Exception {
            if (response.code() == 200) {
              JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
              return new BillReader().read(reader);
            } else { //404 not found
              return null;
            }
          }
        },
        successListener,
        errorListener
    );
  }

  public Cancellable updateAsync(Bill bill,
                                 final OnSuccessListener<Bill> successListener,
                                 final OnErrorListener errorListener) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      JsonWriter writer = new JsonWriter(new OutputStreamWriter(baos, UTF_8));
      new BillWriter().write(bill, writer);
      writer.close();
    } catch (Exception e) {
      Log.e(TAG, "updateAsync failed", e);
      errorListener.onError(new ResourceException(e));
    } finally {
      Request.Builder builder = new Request.Builder()
          .url(String.format("%s/%s", mBillUrl, bill.getId()))
          .put(RequestBody.create(MediaType.parse(APPLICATION_JSON), baos.toByteArray()));
      addAuthToken(builder);
      return new CancellableOkHttpAsync<Bill>(
          builder.build(),
          new ResponseReader<Bill>() {
            @Override
            public Bill read(Response response) throws Exception {
              int code = response.code();
              JsonReader reader = new JsonReader(new InputStreamReader(response.body().byteStream(), UTF_8));
              if (code == 400 || code == 409 || code == 405) { //bad request, conflict, method not allowed
                throw new ResourceException(new ResourceListReader<Issue>(new IssueReader()).read(reader));
              }
              return new BillReader().read(reader);
            }
          },
          successListener,
          errorListener
      );
    }
  }

  public void setUser(User user) {
    mUser = user;
  }

  private class OkHttpCancellableCallable<E> implements CancellableCallable<E> {
    private final Call mCall;
    private final Request mRequest;
    private final ResponseReader<E> mResponseReader;

    public OkHttpCancellableCallable(Request request, ResponseReader<E> responseReader) {
      mRequest = request;
      mResponseReader = responseReader;
      mCall = mOkHttpClient.newCall(request);
    }

    @Override
    public E call() throws Exception {
      try {
        Log.d(TAG, String.format("started %s %s", mRequest.method(), mRequest.url()));
        Response response = mCall.execute();
        Log.d(TAG, String.format("succeeded %s %s", mRequest.method(), mRequest.url()));
        if (mCall.isCanceled()) {
          return null;
        }
        return mResponseReader.read(response);
      } catch (Exception e) {
        Log.e(TAG, String.format("failed %s %s", mRequest.method(), mRequest.url()), e);
        throw e instanceof ResourceException ? e : new ResourceException(e);
      }
    }

    @Override
    public void cancel() {
      if (mCall != null) {
        mCall.cancel();
      }
    }
  }

  private static interface ResponseReader<E> {
    E read(Response response) throws Exception;
  }

  private class CancellableOkHttpAsync<E> implements Cancellable {
    private Call mCall;

    public CancellableOkHttpAsync(
        final Request request,
        final ResponseReader<E> responseReader,
        final OnSuccessListener<E> successListener,
        final OnErrorListener errorListener) {
      try {
        mCall = mOkHttpClient.newCall(request);
        Log.d(TAG, String.format("started %s %s", request.method(), request.url()));
        //retry 3x, renew token
        mCall.enqueue(new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            notifyFailure(e, request, errorListener);
          }

          @Override
          public void onResponse(Call call, Response response) throws IOException {
            try {
              notifySuccess(response, request, successListener, responseReader);
            } catch (Exception e) {
              notifyFailure(e, request, errorListener);
            }
          }
        });
      } catch (Exception e) {
        notifyFailure(e, request, errorListener);
      }
    }

    @Override
    public void cancel() {
      if (mCall != null) {
        mCall.cancel();
      }
    }

    private void notifySuccess(Response response, Request request,
                               OnSuccessListener<E> successListener, ResponseReader<E> responseReader) throws Exception {
      if (mCall.isCanceled()) {
        Log.d(TAG, String.format("completed, but cancelled %s %s", request.method(), request.url()));
      } else {
        Log.d(TAG, String.format("completed %s %s", request.method(), request.url()));
        successListener.onSuccess(responseReader.read(response));
      }
    }

    private void notifyFailure(Exception e, Request request, OnErrorListener errorListener) {
      if (mCall.isCanceled()) {
        Log.d(TAG, String.format("failed, but cancelled %s %s", request.method(), request.url()));
      } else {
        Log.e(TAG, String.format("failed %s %s", request.method(), request.url()), e);
        //errorListener.onError(e instanceof ResourceException ? e : new ResourceException(e));
      }
    }
  }
}
