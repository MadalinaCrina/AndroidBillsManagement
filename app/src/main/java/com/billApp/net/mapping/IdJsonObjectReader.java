package com.billApp.net.mapping;

import org.json.JSONObject;

import static com.billApp.net.mapping.Api.Bill._ID;

public class IdJsonObjectReader implements ResourceReader<String, JSONObject> {
  private static final String TAG = IdJsonObjectReader.class.getSimpleName();

  @Override
  public String read(JSONObject obj) throws Exception {
    return obj.getString(_ID);
  }
}
