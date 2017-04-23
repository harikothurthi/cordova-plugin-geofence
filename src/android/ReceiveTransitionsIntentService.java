package com.cowbell.cordova.geofence;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import cz.msebera.android.httpclient.Consts;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpDelete;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;

public class ReceiveTransitionsIntentService extends IntentService {
    protected static final String GeofenceTransitionIntent = "com.cowbell.cordova.geofence.TRANSITION";
    protected BeepHelper beepHelper;
    protected GeoNotificationNotifier notifier;
    protected GeoNotificationStore store;
    public static final String REST_URL = "https://lit-basin-60588.herokuapp.com/api/sisoweb";
    protected Profile profile;

    /**
     * Sets an identifier for the service
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
        beepHelper = new BeepHelper();
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
    }

    /**
     * Handles incoming intents
     *
     * @param intent The Intent sent by Location Services. This Intent is provided
     *               to Location Services (inside a PendingIntent) when you call
     *               addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Logger logger = Logger.getLogger();
        logger.log(Log.DEBUG, "ReceiveTransitionsIntentService - onHandleIntent");
        Intent broadcastIntent = new Intent(GeofenceTransitionIntent);
        notifier = new GeoNotificationNotifier(
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE),
                this
        );

        // TODO: refactor this, too long
        // First check for errors
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            // Get the error code with a static method
            int errorCode = geofencingEvent.getErrorCode();
            String error = "Location Services error: " + Integer.toString(errorCode);
            // Log the error
            logger.log(Log.ERROR, error);
            broadcastIntent.putExtra("error", error);
        } else {
            // Get the type of transition (entry or exit)
            int transitionType = geofencingEvent.getGeofenceTransition();
            this.profile = store.getProfile();

            if (this.profile == null) {
                String error = "Error: Profile does not exists";
                logger.log(Log.ERROR, error);
                broadcastIntent.putExtra("error", error);
            } else if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) ||
                    (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)) {

                logger.log(Log.DEBUG, "Geofence transition detected");

                List<Geofence> triggerList = geofencingEvent.getTriggeringGeofences();
                List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();

                logger.log(Log.DEBUG, "Preparing profile");


                if (this.profile != null) {
                    logger.log(Log.DEBUG, "Showing profile store on localstorage");
                    logger.log(Log.DEBUG, this.profile.toString());
                } else {
                    logger.log(Log.DEBUG, "profile is null");
                }


                /*
                UserInfo userInfo = new UserInfo();
                userInfo.setFname("Wilf");
                userInfo.setMname("");
                userInfo.setLname("Vill");
                userInfo.setMfname("Kathy");
                userInfo.setMlname("Casey");
                userInfo.setContact("1234567890");
                userInfo.setLocation("7152 Sign In");
                */


                for (Geofence fence : triggerList) {
                    String fenceId = fence.getRequestId();
                    GeoNotification geoNotification = store
                            .getGeoNotification(fenceId);

                    if (geoNotification != null) {
                        if (geoNotification.notification != null) {
                            notifier.notify(geoNotification.notification);
                            profile.preferredLocation = geoNotification.notification.text;
                        }
                        geoNotification.transitionType = transitionType;
                        geoNotifications.add(geoNotification);
                    }
                }

                if (geoNotifications.size() > 0 && profile != null) {
                    try {
                        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
                            URI createURI = new URIBuilder(REST_URL)
                                    .build();
                            OperationParams operationParams = new OperationParams(OperationType.SAVE, createURI);
                            operationParams.setUserProfile(profile);
                            new RestOperation(triggerList, geoNotifications, broadcastIntent).execute(operationParams);
                        } else if (profile._id != null && transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
                            URI deleteURI = new URIBuilder(REST_URL)
                                    .setPath("/api/sisoweb/" + profile._id)
                                    .build();
                            new RestOperation(triggerList, geoNotifications, broadcastIntent).execute(new OperationParams(OperationType.DELETE, deleteURI));
                        }
                    } catch (URISyntaxException e) {
                        logger.log(Log.DEBUG, e.getMessage());
                        e.printStackTrace();
                    }
                }


                /*
                if (geoNotifications.size() > 0) {
                    broadcastIntent.putExtra("transitionData", Gson.get().toJson(geoNotifications));
                    GeofencePlugin.onTransitionReceived(geoNotifications);
                }
                */
            } else {
                String error = "Geofence transition error: " + transitionType;
                logger.log(Log.ERROR, error);
                broadcastIntent.putExtra("error", error);
            }
        }
        sendBroadcast(broadcastIntent);
    }


    private class RestOperation extends AsyncTask<OperationParams, Void, Void> {

        private static final String TAG = "RestOperation";
        String content;
        String error;
        String data = "";
        List<Geofence> triggerList;
        List<GeoNotification> geoNotifications;
        Intent broadcastIntent;

        public RestOperation() {
        }

        public RestOperation(List<Geofence> aTriggerList, List<GeoNotification> aGeoNotifications, Intent aBroadcastIntent) {
            this.triggerList = aTriggerList;
            this.geoNotifications = aGeoNotifications;
            this.broadcastIntent = aBroadcastIntent;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Log.d(TAG, "data received: " + data);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Log.d(TAG, "onPostExecute()");


            if (error != null) {
                // TODO send error notification
                Log.e(TAG, "onPostExecute() - ERROR " + error);
            } else {
                //serverDataReceived.setText(content);
                    /*Pattern p = Pattern.compile("^\\[.*\\]$");
                    Matcher m = p.matcher(content);
                    boolean isArray = m.matches();
                    Log.d(TAG, "is an Array?:  " + isArray);

                    if (isArray) {
                        parseJSONArray();
                    } else {
                        parseJSONObject();
                    }*/

                //if (this.geoNotifications.size() > 0) { Task is already executed if geoNotifications.size() > 0
                broadcastIntent.putExtra("transitionData", Gson.get().toJson(this.geoNotifications));
                GeofencePlugin.onTransitionReceived(this.geoNotifications);
                //}

            }

        }

        private void parseJSONObject() throws JSONException {
            JSONObject jsonObject = new JSONObject(content);
            profile._id = jsonObject.getString("_id");
            store.setProfile(profile);
        }

        @Override
        protected Void doInBackground(OperationParams... params) {
            Log.d(TAG, "param[0] received: " + params[0].getURL());

            try {

                OperationParams op = params[0];

                if (OperationType.REQUEST.equals(op.getOperationType())) {
                    //getCurrentRecord(op);
                } else if (OperationType.DELETE.equals(op.getOperationType())) {
                    deleteCurrentRecord(op);
                } else if (OperationType.SAVE.equals(op.getOperationType())) {
                    createRecord(op);
                }


            } catch (IOException e) {
                error = e.getMessage();
                Log.e(TAG, "doInBackground ", e);
                e.printStackTrace();
            }

            return null;
        }

        private void createRecord(OperationParams op) throws IOException {

            if (op.getUserProfile() == null)
                throw new IllegalArgumentException("User information does not exists");

            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            HttpPost httpPost = new HttpPost(op.getURL());

            try {
                UrlEncodedFormEntity entity = createFormParams(op.getUserProfile());
                httpPost.setEntity(entity);
                response = httpClient.execute(httpPost);
                Log.d(TAG, "Post Status code: " + response.getStatusLine().getStatusCode());
                handleResponse(response);

                Pattern p = Pattern.compile("^\\[.*\\]$");
                Matcher m = p.matcher(content);
                boolean isArray = m.matches();
                Log.d(TAG, "is an Array?:  " + isArray);

                if (isArray) {
                    //parseJSONArray();
                } else {
                    JSONObject jsonObject = new JSONObject(content);
                    profile._id = jsonObject.getString("_id");
                    store.setProfile(profile);
                }
            } catch (JSONException e) {
                Log.e(TAG, "onPostExecute()", e);
                error = e.getMessage();
                e.printStackTrace();

            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }

        private UrlEncodedFormEntity createFormParams(Profile userInfo) {
            return new UrlEncodedFormEntity(userInfo.getParameterList(), Consts.UTF_8);
        }

        private void deleteCurrentRecord(OperationParams op) throws IOException {

            CloseableHttpClient httpClient = HttpClients.createDefault();
            CloseableHttpResponse response = null;
            HttpDelete httpDelete = new HttpDelete(op.getURL());

            try {
                response = httpClient.execute(httpDelete);
                Log.d(TAG, "Delete Status code: " + response.getStatusLine().getStatusCode());
                handleResponse(response);
            } finally {
                if (response != null) {
                    response.close();
                }
            }

        }

        private void handleResponse(CloseableHttpResponse response) throws IOException {
            BufferedReader br = null;
            HttpEntity entity = response.getEntity();
            try {
                if (entity != null) {
                    br = new BufferedReader(new InputStreamReader(entity.getContent()));
                    StringBuilder sb = new StringBuilder();
                    String line;

                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    Log.d(TAG, sb.toString());
                    content = sb.toString();
                }
            } finally {
                response.close();
                if (br != null) br.close();
            }
        }

    }
}
