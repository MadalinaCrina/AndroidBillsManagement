package com.billApp.net.mapping;

import com.billApp.content.Bill;

import org.json.JSONObject;

import static com.billApp.net.mapping.Api.Bill.PRICE;
import static com.billApp.net.mapping.Api.Bill.STATUS;
import static com.billApp.net.mapping.Api.Bill.TEXT;
import static com.billApp.net.mapping.Api.Bill.UPDATED;
import static com.billApp.net.mapping.Api.Bill.USER_ID;
import static com.billApp.net.mapping.Api.Bill.VERSION;
import static com.billApp.net.mapping.Api.Bill._ID;

public class BillJsonObjectReader implements ResourceReader<Bill, JSONObject> {
  private static final String TAG = BillJsonObjectReader.class.getSimpleName();

  @Override
  public Bill read(JSONObject obj) throws Exception {
    Bill bill = new Bill();
    bill.setId(obj.getString(_ID));
    bill.setText(obj.getString(TEXT));
    bill.setText(obj.getString(PRICE));
    bill.setUpdated(obj.getLong(UPDATED));
    bill.setStatus(Bill.Status.valueOf(obj.getString(STATUS)));
    bill.setUserId(obj.getString(USER_ID));
    bill.setVersion(obj.getInt(VERSION));
    return bill;
  }
}
