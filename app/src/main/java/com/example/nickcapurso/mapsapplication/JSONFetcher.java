package com.example.nickcapurso.mapsapplication;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by nickcapurso on 3/4/15.
 */
public class JSONFetcher extends AsyncTask<String, Void, String>{
    private Handler mClientHandler;
    private NetworkTimeout mTimer;

    public JSONFetcher(Handler client){
        mClientHandler = client;
    }

    @Override
    protected void onPreExecute() {
        mTimer = new NetworkTimeout(mClientHandler);
        mTimer.start();
    }

    @Override
    protected String doInBackground(String... params) {
        Log.d(MainActivity.TAG, "JSONFetcher starting...");
        HttpClient httpclient = HttpClients.createDefault();

        try {
            URIBuilder builder = new URIBuilder(params[0]);

            //Set params
            for(int i = 1; i < params.length; i+=2)
                builder.setParameter(params[i], params[i+1]);


            URI uri = builder.build();
            HttpGet request = new HttpGet(uri);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                return (EntityUtils.toString(entity));
            }
        } catch (ClientProtocolException e) {
            Log.d(MainActivity.TAG, "JSONFetcher - ClientProtocolException " + e.getStackTrace());
            return "err";
        } catch (IOException e) {
            Log.d(MainActivity.TAG, "JSONFetcher - IOException " + e.getStackTrace());
            return "err";
        } catch (URISyntaxException e) {
            Log.d(MainActivity.TAG, "JSONFetcher - URISyntaxException " + e.getStackTrace());
            return "err";
        }
        return "okay";
    }

    @Override
    protected void onPostExecute(String result) {
        mTimer.cancel();
        if(result.equals("err")){
            mClientHandler.sendMessage(mClientHandler.obtainMessage(HandlerCodes.JSON_FETCH_ERR));
        }else {
            Log.d(MainActivity.TAG, "Result: " + result);
            Message message = mClientHandler.obtainMessage(HandlerCodes.JSON_FETCH_DONE);
            message.obj = result;
            mClientHandler.sendMessage(message);
        }
    }
}
