package com.example.bluetoothterminal;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


public class HTTPMessages {
    String payload;
    private static String API_KEY = "";
    Context context;


    public HTTPMessages(String payload, Context context) {
        this.payload = payload;
        this.context = context;
    }

    public void setAPIKey(String apiKey){
        this.API_KEY = apiKey;

    }

    public void run() {
        RequestQueue ExampleRequestQueue = Volley.newRequestQueue(context);

        String url = "https://api.thingspeak.com/update?api_key=" + API_KEY + payload;

        StringRequest ExampleStringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                //This code is executed if the server responds, whether or not the response contains data.
                // TODO: add logic for response
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.
            @Override
            public void onErrorResponse(VolleyError error) {
                //This code is executed if there is an error.
            }
        });
        ExampleRequestQueue.add(ExampleStringRequest);
    }
}

