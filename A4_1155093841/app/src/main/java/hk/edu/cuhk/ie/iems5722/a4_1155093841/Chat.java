package hk.edu.cuhk.ie.iems5722.a4_1155093841;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import android.os.Message;


@RequiresApi(api = Build.VERSION_CODES.O)
public class Chat extends AppCompatActivity {

    private static final String CHANNEL_ID = "message_arrive";
    private int app_user_id = 1155093841;
    private String app_user_name = "Chow Tsz Kui, Alex";

    private ArrayList<Message_s> message_array;
    private String room_name;
    private String room_id;
    private ProgressDialog wait;
    private String source_link;
    private int current_page;
    private int total_pages;
    private Adapter adapter;
    private ListView g_listview;
    private String message_input;

    private int visibleItemCount_g;
    private int totalItemCount_g;
    private int firstVisibleItem_g;
    private int initial = 0;

    private Socket socket;
    private static final int ACTION_CONNECTED = 1;
    private static final int ACTION_UPDATE = 2;
    //private MainHandler handler = new MainHandler(this);
    private NotificationChannel new_message_arrive_channel;
    private NotificationManager notificationManager;
    private NotificationManager old_notificationManager;

    private int send = 0;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        wait_block();
        createNotificationChannel();

        room_name = getIntent().getStringExtra("chatroom_name");
        room_id = getIntent().getStringExtra("chatroom_id");

        try{
            socket = IO.socket("http://3.20.28.255:8001");
            socket.on("new_message", onReceiveMessage);

            String tmp = String.valueOf(room_id);
            //socket.on("join", tmp);

            socket.connect();
            socket.emit("join", room_id);

        }
        catch(URISyntaxException e) {
            e.printStackTrace();
        }

        TextView chatroom_name = (TextView) findViewById(R.id.appbar_app_name);
        chatroom_name.setText(room_name);

        source_link = "http://3.20.28.255/api/a3/get_messages?chatroom_id="+room_id+"&page=1";

        new Chat.get_chatroom_message().execute(source_link);

        //Toast.makeText(getApplicationContext(), "room_id = " + room_id, Toast.LENGTH_SHORT).show();
        //Log.v("link", source_link);

        ArrayList<Message_s> message_array_create = new ArrayList<Message_s>();
        message_array = message_array_create;
        ListView listView = findViewById(R.id.listview_message);
        adapter = new Adapter(this, message_array);
        listView.setAdapter(adapter);

        g_listview = listView;
        g_listview.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                //System.out.println("ON_firstVisibleItem = "+firstVisibleItem);
                //System.out.println("ON_visibleItemCount = "+visibleItemCount);
                //System.out.println("ON_totalItemCount = "+totalItemCount);

                // initial part
                if ((visibleItemCount == totalItemCount) && (visibleItemCount > 0) && (totalItemCount > 0) && initial == 0) {
                    if (current_page == total_pages) {return;}
                    else if (totalItemCount_g == totalItemCount && visibleItemCount_g == visibleItemCount) {return;}
                    else {
                        visibleItemCount_g = visibleItemCount;
                        totalItemCount_g = totalItemCount;

                        int input_page_no = current_page + 1;
                        source_link = "http://3.20.28.255/api/a3/get_messages?chatroom_id=" + room_id + "&page=" + input_page_no;
                        //Object run_result = get_chatroom_more_message.execute().get();
                        new get_chatroom_more_message().execute(source_link);
                    }


                }
                // in the progress
                else if ((visibleItemCount > 0)) {

                    visibleItemCount_g = visibleItemCount;
                    totalItemCount_g = totalItemCount;
                    firstVisibleItem_g = firstVisibleItem;


                    if (initial == 0 && firstVisibleItem == 0) {
                        initial = 1;
                    }
                    else if (initial == 1 && firstVisibleItem == 0) {

                        //System.out.println("called");

                        if (current_page != total_pages) {
                            int input_page_no = current_page + 1;

                            source_link = "http://3.20.28.255/api/a3/get_messages?chatroom_id=" + room_id + "&page=" + input_page_no;

                            new get_chatroom_more_message().execute(source_link);
                            //wait.dismiss();
                        }


                    }
                }
            }
        });

        back_button();
        refresh_button();
        input_button(); //message input

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    //button event
    private void back_button(){
        final ImageButton back_button = findViewById(R.id.back_button);
        back_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                socket.emit("leave", room_id);
                socket.disconnect();
                socket.off();
                Chat.this.finish();
            }
        });
    }
    private void refresh_button() {
        final ImageButton refresh_button = findViewById(R.id.refresh_button);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                wait_block();

                message_array.clear();
                adapter.notifyDataSetChanged();

                totalItemCount_g = 0;
                visibleItemCount_g = 0;
                firstVisibleItem_g = 0;
                initial = 0;
                current_page = 1;

                source_link = "http://3.20.28.255/api/a3/get_messages?chatroom_id="+room_id+"&page="+current_page;

                new Chat.get_chatroom_message().execute(source_link);
            }
        });
    }
    private void input_button() {
        final ImageButton button = findViewById(R.id.execute_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText input = findViewById(R.id.Input);
                String input_text = input.getText().toString();

                if (input_text.matches("")) {
                    Toast.makeText(Chat.this, "Message empty", Toast.LENGTH_LONG).show();
                    return;
                }
                else if (input_text.length() > 200) {
                    Toast.makeText(Chat.this, "Chatacters of message cannot more than 200", Toast.LENGTH_LONG).show();
                    return;
                }
                else {
                    message_input = input_text; //message

                    String send_link = "http://3.20.28.255/api/a3/send_message";
                    new send_message().execute(send_link);

                    input.getText().clear();
                }

            }
        });
    }

    private void listview_to_bottom() {
        g_listview.post(new Runnable() {
            @Override
            public void run() {
                g_listview.setSelection(g_listview.getCount() - 1);
            }
        });
    }
    private void listview_to_bottom_after_update(int visibleItemCount, int totalItemCount) {
        g_listview.post(new Runnable() {
            @Override
            public void run() { g_listview.setSelection(visibleItemCount); }
        });
    }

    private void wait_block() {
        wait = ProgressDialog.show(Chat.this,"Please wait", "Waiting for chatroom details", true, false);
    }

    //socket.io event
    private Emitter.Listener onReceiveMessage = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    System.out.printf(String.valueOf(data));
                    try {

                        String input;
                        String chatroom_id_str = data.getString("chatroom_id");
                        int chatroom_id_take = Integer.parseInt(chatroom_id_str);
                        int room_id_general = Integer.parseInt(room_id);
                        String message_str = data.getString("message");


                        if (chatroom_id_take == room_id_general) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(Chat.this, CHANNEL_ID)
                                        .setSmallIcon(R.drawable.message_icon)
                                        .setContentTitle(room_name)
                                        .setContentText(message_str)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                int notificationId = 1;
                                notificationManager.notify(notificationId, builder.build());
                            }
                            else {
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(Chat.this)
                                        .setSmallIcon(R.drawable.message_icon)
                                        .setContentTitle(room_name)
                                        .setContentText(message_str)
                                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(Chat.this);

                                int notificationId = 1;
                                notificationManager.notify(notificationId, builder.build());

                            }

                            if (send == 1) {
                                send = 0;
                            }
                            else {
                                wait_block();

                                message_array.clear();
                                adapter.notifyDataSetChanged();

                                totalItemCount_g = 0;
                                visibleItemCount_g = 0;
                                firstVisibleItem_g = 0;
                                initial = 0;
                                current_page = 1;

                                source_link = "http://3.20.28.255/api/a3/get_messages?chatroom_id="+room_id+"&page="+current_page;

                                new Chat.get_chatroom_message().execute(source_link);
                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    };

    //handle the action of message send and receive
    private class get_chatroom_message extends AsyncTask<String, Integer, JSONObject> {
        InputStream chatroom_message = null;
        JSONObject json = new JSONObject();
        @Override
        protected JSONObject doInBackground(String... source_link) {
            try{
                HttpURLConnection conn = null;
                URL url = new URL(source_link[0]);
                System.out.println(source_link[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.connect();

                int response = conn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {

                    chatroom_message = conn.getInputStream();
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(chatroom_message, Charset.forName("UTF-8")));
                        StringBuilder sb = new StringBuilder();
                        int cp;
                        while ((cp = rd.read()) != -1) {
                            sb.append((char) cp);
                        }
                        json = new JSONObject(sb.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        chatroom_message.close();
                    }
                }

            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return json;
        }
        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }
        @Override
        protected void onPostExecute(JSONObject result) {

            int id;
            int user_id;
            String user_name;
            String message;
            String message_time;
            int current_page_get;
            int total_pages_get;

            String data = null;

            try {
                data = result.getString("data");
                JSONObject data_object = new JSONObject(data);

                String current_page_str = data_object.getString("current_page");
                String total_pages_str = data_object.getString("total_pages");

                current_page_get = Integer.parseInt(current_page_str);
                total_pages_get = Integer.parseInt(total_pages_str);

                current_page = current_page_get;
                total_pages = total_pages_get;

                JSONArray messages_array = data_object.getJSONArray("messages");

                //System.out.println(messages_array);

                for (int i = 0; i < messages_array.length(); i++) {
                    // get id
                    String id_string = messages_array.getJSONObject(i).getString("id");
                    id = Integer.parseInt(id_string);

                    // get user id
                    String user_id_string = messages_array.getJSONObject(i).getString("user_id");
                    user_id = Integer.parseInt(user_id_string);

                    // get user name
                    user_name = messages_array.getJSONObject(i).getString("name");

                    // get message
                    message = messages_array.getJSONObject(i).getString("message");

                    // get message time
                    message_time = messages_array.getJSONObject(i).getString("message_time");

                    /*
                    System.out.println("id = "+id);
                    System.out.println("user_id = "+user_id);
                    System.out.println("user_name = "+user_name);
                    System.out.println("message = "+message);
                    System.out.println("message_time = "+message_time);


                    */
                    if (user_id == app_user_id) {
                        message_array.add(0, new Message_s(user_name, message, message_time, 0));
                    }
                    else {
                        message_array.add(0, new Message_s(user_name , message, message_time, 1));
                    }
                    adapter.notifyDataSetChanged();
                }


            } catch (JSONException e) {
                e.printStackTrace();
            }
            listview_to_bottom();
            wait.hide();
        }
    }
    private class get_chatroom_more_message extends AsyncTask<String, Integer, JSONObject> {
        InputStream chatroom_message = null;
        JSONObject json = new JSONObject();
        ProgressDialog wait_in;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            wait_in = ProgressDialog.show(Chat.this,"Please wait", "Waiting for chatroom details", true, false);
        }

        @Override
        protected JSONObject doInBackground(String... source_link) {
            try{

                HttpURLConnection conn = null;
                URL url = new URL(source_link[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.connect();

                int response = conn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {

                    chatroom_message = conn.getInputStream();
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(chatroom_message, Charset.forName("UTF-8")));
                        StringBuilder sb = new StringBuilder();
                        int cp;
                        while ((cp = rd.read()) != -1) {
                            sb.append((char) cp);
                        }
                        json = new JSONObject(sb.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        chatroom_message.close();
                    }
                }

            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return json;
        }
        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }
        @Override
        protected void onPostExecute(JSONObject result) {

            int id;
            int user_id;
            String user_name;
            String message;
            String message_time;
            int current_page_get;
            int total_pages_get;

            String data = null;

            wait_in.dismiss();

            try {
                data = result.getString("data");
                JSONObject data_object = new JSONObject(data);

                System.out.println(data);

                String current_page_str = data_object.getString("current_page");
                String total_pages_str = data_object.getString("total_pages");

                current_page_get = Integer.parseInt(current_page_str);
                total_pages_get = Integer.parseInt(total_pages_str);

                if (current_page_get == current_page) { System.out.println("here"); wait.dismiss(); return; }

                current_page = current_page_get;
                total_pages = total_pages_get;

                //System.out.println("current_page = "+current_page);
                //System.out.println("total_pages = "+total_pages);

                JSONArray messages_array = data_object.getJSONArray("messages");

                //System.out.println(messages_array);

                for (int i = 0; i < messages_array.length(); i++) {
                    // get id
                    String id_string = messages_array.getJSONObject(i).getString("id");
                    id = Integer.parseInt(id_string);

                    // get user id
                    String user_id_string = messages_array.getJSONObject(i).getString("user_id");
                    user_id = Integer.parseInt(user_id_string);

                    // get user name
                    user_name = messages_array.getJSONObject(i).getString("name");

                    // get message
                    message = messages_array.getJSONObject(i).getString("message");

                    // get message time
                    message_time = messages_array.getJSONObject(i).getString("message_time");

                    if (user_id == app_user_id) {
                        message_array.add(0, new Message_s(user_name, message, message_time, 0));
                    }
                    else {
                        message_array.add(0, new Message_s(user_name , message, message_time, 1));
                    }
                    adapter.notifyDataSetChanged();
                }

                listview_to_bottom_after_update(visibleItemCount_g, totalItemCount_g);


            }
            catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
    private class send_message extends AsyncTask<String, Integer, JSONObject> {
        InputStream return_message = null;
        JSONObject json = new JSONObject();
        ProgressDialog wait_send;


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            wait_send = ProgressDialog.show(Chat.this,"Please wait", "Waiting for sending message to chatroom", true, false);
        }

        @Override
        protected JSONObject doInBackground(String... source_link) {
            try{
                send = 1;
                HttpURLConnection conn = null;
                URL url = new URL(source_link[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.connect();

                OutputStream output_parameter = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output_parameter, "UTF-8"));
                Uri.Builder builder = new Uri.Builder();

                String app_user_id_string = String.valueOf(app_user_id);

                builder.appendQueryParameter("chatroom_id", room_id);
                builder.appendQueryParameter("user_id", app_user_id_string);
                builder.appendQueryParameter("name", app_user_name);
                builder.appendQueryParameter("message", message_input);

                String send_query = builder.build().getEncodedQuery();
                writer.write(send_query);
                writer.flush();
                writer.close();
                output_parameter.close();

                int response = conn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {

                    return_message = conn.getInputStream();
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(return_message, Charset.forName("UTF-8")));
                        StringBuilder sb = new StringBuilder();
                        int cp;
                        while ((cp = rd.read()) != -1) {
                            sb.append((char) cp);
                        }
                        json = new JSONObject(sb.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        return_message.close();
                    }
                }
                /*
                int response = conn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {

                    return_message = conn.getInputStream();
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(return_message, Charset.forName("UTF-8")));
                        StringBuilder sb = new StringBuilder();
                        int cp;
                        while ((cp = rd.read()) != -1) {
                            sb.append((char) cp);
                        }
                        json = new JSONObject(sb.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        return_message.close();
                    }
                }
                */
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return json;
        }
        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            String status = null;

            wait_send.dismiss();

            try {
                status = result.getString("status");

                System.out.println("status = "+status);

                if (status.equals("OK")) {
                    message_array.clear();
                    adapter.notifyDataSetChanged();

                    totalItemCount_g = 0;
                    visibleItemCount_g = 0;
                    firstVisibleItem_g = 0;
                    initial = 0;
                    current_page = 1;

                    message_input = "";
                    source_link = "http://3.20.28.255/api/a3/get_messages?chatroom_id="+room_id+"&page=1";
                    new Chat.get_chatroom_message().execute(source_link);
                }
                else {
                    Toast.makeText(Chat.this, "Fail to send the message, please check your connection.", Toast.LENGTH_LONG).show();
                }

            }
            catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    //createNotificationChannel
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            System.out.printf(">=8.0");

            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            new_message_arrive_channel = channel;
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new_message_arrive_channel);

        }
        else {

        }
    }
}