package hk.edu.cuhk.ie.iems5722.a4_1155093841;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private String source_link = "http://3.20.28.255/api/a3/get_chatrooms";
    private Context context;
    private ListView listview;
    private ArrayList<Room> room;
    private Room_adapter room_adapter;
    private ProgressDialog wait;


    private Socket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this.context;
        listview = findViewById(R.id.listview_chatroom);

        wait_block(context); //This is used to lock the screen until the loading on chatroom details is finished.
        new get_chatroom_details().execute(source_link);

        ArrayList<Room> array_create = new ArrayList<Room>();
        room = array_create;

        room_adapter = new Room_adapter(this, room);
        listview.setAdapter(room_adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //TextView textView = (TextView) view.findViewById(R.id.room_name);
                String room_name = room.get(position).get_room();
                int room_id_int = room.get(position).get_id();
                String room_id = String.valueOf(room_id_int);

                Intent intent = new Intent(MainActivity.this, Chat.class);
                intent.putExtra("chatroom_name", room_name);
                intent.putExtra("chatroom_id", room_id);
                startActivity(intent);

            }
        });

        if (!check_internet()) {
            Retry_Dialog(this);
            return;
        }

    }

    private void wait_block(Context context) {
        wait = new ProgressDialog(this);
        wait.setMessage("Waiting for chatroom details");
        wait.setCancelable(false);
        wait.setInverseBackgroundForced(false);
        wait.show();
    }

    private boolean check_internet() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
    private void Retry_Dialog(Context context){
        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle("No network now.");
        normalDialog.setMessage("Please check your network connection.");
        normalDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishAndRemoveTask();
                }
        });
        normalDialog.show();
    }
    private void Fail_Dialog(Context context){
        final AlertDialog.Builder normalDialog = new AlertDialog.Builder(MainActivity.this);
        normalDialog.setTitle("Cannot get the details of chatroom.");
        normalDialog.setMessage("Please check your network connection.");
        normalDialog.setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishAndRemoveTask();
                    }
                });
        normalDialog.show();
    }

    private class get_chatroom_details extends AsyncTask<String, Integer, JSONObject> {
        InputStream chatroom_details = null;
        JSONObject json = new JSONObject();

        @Override
        protected JSONObject doInBackground(String... source_link) {
            try{
                HttpURLConnection conn = null;
                URL url = new URL(source_link[0]);
                conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.connect();

                int response = conn.getResponseCode();
                if (response == HttpURLConnection.HTTP_OK) {

                    chatroom_details = conn.getInputStream();
                    try {
                        BufferedReader rd = new BufferedReader(new InputStreamReader(chatroom_details, Charset.forName("UTF-8")));
                        StringBuilder sb = new StringBuilder();
                        int cp;
                        while ((cp = rd.read()) != -1) {
                            sb.append((char) cp);
                        }
                        json = new JSONObject(sb.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        chatroom_details.close();
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
            //System.out.println(json);
            return json;
        }

        protected void onProgressUpdate(Integer... progress) {
            //setProgressPercent(progress[0]);
        }
        @Override
        protected void onPostExecute(JSONObject result) {
            String id;
            String name;

            try {
                JSONArray array = result.getJSONArray("data");

                for (int i = 0; i < array.length(); i++) {
                    id = array.getJSONObject(i).getString("id");
                    name = array.getJSONObject(i).getString("name");

                    int id_input = Integer.parseInt(id);
                    room.add(new Room(id_input, name));
                }
                room_adapter.notifyDataSetChanged();
                wait.hide();

            } catch (JSONException e) {
                e.printStackTrace();
                wait.hide();
                Fail_Dialog(context);
            }
        }

    }


}