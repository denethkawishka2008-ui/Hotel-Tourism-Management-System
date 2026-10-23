package Hospitality_and_tourism_system;

public class Room {
    private int roomId;
    private String roomNumber;
    private String roomType;
    private String status;
    private String price;

    // Constructor
    public Room(int roomId, String roomNumber, String roomType, String status, String price) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.status = status;
        this.price = price;
    }

    // Getters
    public int getRoomId() {
        return roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getStatus() {
        return status;
    }

    public String getPrice() {
        return price; // මෙන්න මෙතැන හරියට price එක return වෙන්න දමා ඇත
    }

    // Setters
    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}