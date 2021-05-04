package hk.edu.cuhk.ie.iems5722.a4_1155093841;

import android.widget.ArrayAdapter;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Room_adapter extends ArrayAdapter<Room> {
    public Room_adapter(Context context, ArrayList<Room> room) {
        super(context, 0, room);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Room room = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.room_presention, parent, false);
        }

        //TextView id = convertView.findViewById(R.id.room_id);
        TextView name = convertView.findViewById(R.id.room_name);

        //id.setText(""+room.get_id());
        name.setText(room.get_room());

        return convertView;
    }
}
