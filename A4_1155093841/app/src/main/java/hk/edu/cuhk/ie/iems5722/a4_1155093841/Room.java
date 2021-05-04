package hk.edu.cuhk.ie.iems5722.a4_1155093841;

public class Room {
    public int id;
    public String room_name;

    public Room (int id, String room_name) {
        this.id = id;
        this.room_name =  room_name;
    }

    public int get_id(){ return id; }
    public String get_room(){
        return room_name;
    }

}
