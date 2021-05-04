package hk.edu.cuhk.ie.iems5722.a4_1155093841;

public class Message_s {
    public String message_name;
    public String message_info;
    public String message_time;
    public int message_source; // 0 -> you, 1 -> others

    public Message_s(String message_name, String message_info, String message_time, int message_source) {
        this.message_name = message_name;
        this.message_info = message_info;
        this.message_time = message_time;
        this.message_source = message_source;
    }


    //Method of getting the information
    public String get_message_name(){
        return message_name;
    }
    public String get_message_info(){
        return message_info;
    }
    public String get_message_time(){
        return message_time;
    }
    public int get_message_source(){
        return message_source;
    }

}

