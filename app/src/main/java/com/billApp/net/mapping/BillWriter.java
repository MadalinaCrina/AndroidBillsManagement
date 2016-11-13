package com.billApp.net.mapping;

import android.util.JsonWriter;

import com.billApp.content.Bill;

import java.io.IOException;

import static com.billApp.net.mapping.Api.Bill.PRICE;
import static com.billApp.net.mapping.Api.Bill.STATUS;
import static com.billApp.net.mapping.Api.Bill.TEXT;
import static com.billApp.net.mapping.Api.Bill.UPDATED;
import static com.billApp.net.mapping.Api.Bill.USER_ID;
import static com.billApp.net.mapping.Api.Bill.VERSION;
import static com.billApp.net.mapping.Api.Bill._ID;

public class BillWriter implements ResourceWriter<Bill, JsonWriter>{
  @Override
  public void write(Bill bill, JsonWriter writer) throws IOException {
    writer.beginObject();
    {
      if (bill.getId() != null) {
        writer.name(_ID).value(bill.getId());
      }
      writer.name(TEXT).value(bill.getText());
      writer.name(PRICE).value(bill.getText());
      writer.name(STATUS).value(bill.getStatus().name());
      if (bill.getUpdated() > 0) {
        writer.name(UPDATED).value(bill.getUpdated());
      }
      if (bill.getUserId() != null) {
        writer.name(USER_ID).value(bill.getUserId());
      }
      if (bill.getVersion() > 0) {
        writer.name(VERSION).value(bill.getVersion());
      }
    }
    writer.endObject();
  }
}
