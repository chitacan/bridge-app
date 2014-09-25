package com.chitacan.bridge;

import java.util.List;

import retrofit.Callback;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.PUT;

/**
 * Created by chitacan on 2014. 9. 25..
 */
public class API {

    static class Bridge {
        public String client;
        public String daemon;
    }

    static class Client {
        public String name;
        public String value;
    }

    static class Response {
        public String result;
    }

    public interface BridgeService {

        @GET("/api/bridge/client")
        void listClients(Callback<List<Client>> cb);

        @GET("/api/bridge")
        List<Bridge> listBridge();

        @PUT("/api/bridge")
        Response createBridge(@Field("client") String client, @Field("daemon") String daemon);

        @DELETE("/api/bridge")
        List<Client> removeBridge(@Field("client") String client, @Field("daemon") String daemon);

    }

}
