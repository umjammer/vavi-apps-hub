/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.apps.hub.ws;

import java.io.InputStream;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;


/**
 * TODO temporary.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2020/07/02 umjammer initial version <br>
 */
public class JsonCodec {

    private static Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return false;
        }
        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return InputStream.class.equals(clazz);
        }
    }).create();

    public static class JsonEncoder implements Encoder.Text<ClientData> {
        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public String encode(ClientData notification) throws EncodeException {
            return gson.toJson(notification);
        }

        @Override
        public void destroy() {
        }
    }

    public static class JsonDecoder implements Decoder.Text<ClientData> {

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public boolean willDecode(String s) {
            try {
                gson.fromJson(s, ClientData.class);
                return true;
            } catch (JsonSyntaxException e) {
                return false;
            }
        }

        @Override
        public ClientData decode(String s) throws DecodeException {
            try {
                return gson.fromJson(s, ClientData.class);
            } catch (JsonSyntaxException e) {
                throw new DecodeException(s, e.getMessage(), e);
            }
        }

        @Override
        public void destroy() {
        }
    }
}
