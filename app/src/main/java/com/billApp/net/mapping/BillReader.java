package com.billApp.net.mapping;

import android.util.JsonReader;
import android.util.Log;

import com.billApp.content.Bill;

import java.io.IOException;

import static com.billApp.net.mapping.Api.Bill.PRICE;
import static com.billApp.net.mapping.Api.Bill.STATUS;
import static com.billApp.net.mapping.Api.Bill.TEXT;
import static com.billApp.net.mapping.Api.Bill.UPDATED;
import static com.billApp.net.mapping.Api.Bill.USER_ID;
import static com.billApp.net.mapping.Api.Bill.VERSION;
import static com.billApp.net.mapping.Api.Bill._ID;

public class BillReader implements ResourceReader<Bill, JsonReader> {
  private static final String TAG = BillReader.class.getSimpleName();

  @Override
  public Bill read(JsonReader reader) throws IOException {
    Bill bill = new Bill();
    reader.beginObject();
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (name.equals(_ID)) {
        bill.setId(reader.nextString());
      } else if (name.equals(TEXT)) {
        bill.setText(reader.nextString());
      } else if (name.equals(PRICE)) {
        bill.setPrice(reader.nextString());
      } else if (name.equals(STATUS)) {
        bill.setStatus(com.billApp.content.Bill.Status.valueOf(reader.nextString()));
      } else if (name.equals(UPDATED)) {
        bill.setUpdated(reader.nextLong());
      } else if (name.equals(USER_ID)) {
        bill.setUserId(reader.nextString());
      } else if (name.equals(VERSION)) {
        bill.setVersion(reader.nextInt());
      } else {
        reader.skipValue();
        Log.w(TAG, String.format("Bill property '%s' ignored", name));
      }
    }
    reader.endObject();
    return bill;
  }
}
