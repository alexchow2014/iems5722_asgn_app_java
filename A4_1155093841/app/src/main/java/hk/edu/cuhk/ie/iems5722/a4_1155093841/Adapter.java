package hk.edu.cuhk.ie.iems5722.a4_1155093841;

import android.widget.ArrayAdapter;
import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class Adapter extends ArrayAdapter<Message_s> {
    public Adapter(Context context, ArrayList<Message_s> message) {
        super(context, 0, message);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Message_s message = getItem(position);

        if (message.get_message_source() == 0) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_presention, parent, false);
        }
        else if (message.get_message_source() == 1){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_presention_left, parent, false);
        }


        //convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_presention_left, parent, false);
        TextView name = convertView.findViewById(R.id.message_name);
        TextView info = convertView.findViewById(R.id.message_info);
        TextView time = convertView.findViewById(R.id.message_time);

        name.setText(message.get_message_name());
        info.setText(message.get_message_info());
        time.setText(message.get_message_time());

        return convertView;
    }


}
