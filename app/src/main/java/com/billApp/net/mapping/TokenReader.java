package com.billApp.net.mapping;

import android.util.JsonReader;

import java.io.IOException;

import static com.billApp.net.mapping.Api.Auth.TOKEN;

public class TokenReader implements ResourceReader<String, JsonReader> {
  @Override
  public String read(JsonReader reader) throws IOException {
    reader.beginObject();
    String token = null;
    while (reader.hasNext()) {
      String name = reader.nextName();
      if (name.equals(TOKEN)) {
        token = reader.nextString();
      }
    }
    reader.endObject();
    return token;
  }
}
