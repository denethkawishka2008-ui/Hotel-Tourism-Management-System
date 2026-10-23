package Hospitality_and_tourism_system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import javafx.event.ActionEvent;

public class room_management {

    @FXML private TextField txtRoomId;
    @FXML private TextField txtRoomNumber;
    @FXML private ComboBox<String> cmbRoomType;
    @FXML private ComboBox<String> cmbStatus;
    @FXML private TextField txtprice;
    @FXML private TextField txtSearch;

    @FXML private TableView<Room> tblRooms;
    @FXML private TableColumn<Room, Integer> colRoomId;
    @FXML private TableColumn<Room, String> colRoomNumber;
    @FXML private TableColumn<Room, String> colRoomType;
    @FXML private TableColumn<Room, String> colStatus;
    @FXML private TableColumn<Room, String> colprice;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hospitality_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "deneth";

    private ObservableList<Room> roomList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        cmbRoomType.getItems().addAll("Deluxe", "Standard", "Suite", "Single", "Double");
        cmbStatus.getItems().addAll("Available", "Booked", "Maintenance","Dirty");
        
        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
      
        colprice.setCellValueFactory(new PropertyValueFactory<>("price"));  
    
        loadRoomData();
        autoGenerateRoomDetails();
      
        tblRooms.setOnMouseClicked(event -> {
            Room selectedRoom = tblRooms.getSelectionModel().getSelectedItem();
            if (selectedRoom != null) {
                txtRoomId.setText(String.valueOf(selectedRoom.getRoomId()));
                txtRoomNumber.setText(selectedRoom.getRoomNumber());
                cmbRoomType.setValue(selectedRoom.getRoomType());
                cmbStatus.setValue(selectedRoom.getStatus());
                txtprice.setText(selectedRoom.getPrice());
            }
        });
        
        FilteredList<Room> filteredData = new FilteredList<>(roomList, b -> true);
        txtSearch.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(room -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();

                if (room.getRoomNumber().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (room.getRoomType().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (room.getStatus().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });

        SortedList<Room> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tblRooms.comparatorProperty());
        tblRooms.setItems(sortedData);
    }


    private void loadRoomData() {
        roomList.clear();
        String query = "SELECT * FROM rooms";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                while (rs.next()) {
                    roomList.add(new Room(
                            rs.getInt("room_id"),
                            rs.getString("room_number"),
                            rs.getString("room_type"),
                            rs.getString("status"),
                            rs.getString("price")
                    ));
                }
            }
        } catch (ClassNotFoundException e) {
            showAlert(Alert.AlertType.ERROR, "Driver Error", "MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load rooms: " + e.getMessage());
            e.printStackTrace();
        }
    }

   // room id 
    private void autoGenerateRoomDetails() {
        txtRoomId.setEditable(false);
        txtRoomNumber.setEditable(false);
        
        String query = "SELECT MAX(room_id) AS max_id FROM rooms";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                if (rs.next()) {
                    int nextId = rs.getInt("max_id") + 1;
                    txtRoomId.setText(String.valueOf(nextId));
                    txtRoomNumber.setText("Room " + String.format("%02d", nextId)); 
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleAddRoom() {
        String roomNumber = txtRoomNumber.getText();
        String roomType = cmbRoomType.getValue();
        String status = cmbStatus.getValue();
        String price = txtprice.getText();

        if (roomType == null || status == null || price.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill all fields (Room Type, Status, Price)!");
            return;
        }

        String query = "INSERT INTO rooms (room_number, room_type, status, price) VALUES (?, ?, ?, ?)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, roomNumber);
                pstmt.setString(2, roomType);
                pstmt.setString(3, status);
                pstmt.setString(4, price);

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Room Added Successfully!");
                    loadRoomData();
                    handleClear();
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private Button btnDelete;

    @FXML
    private void handleDeleteRoom(ActionEvent event) {
        Room selectedRoom = tblRooms.getSelectionModel().getSelectedItem();
        if (selectedRoom == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a room from the table to delete!");
            return;
        }

        String query = "DELETE FROM rooms WHERE room_id = ?";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, selectedRoom.getRoomId());
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Room Deleted Successfully!");
                    loadRoomData();
                    handleClear();
                }
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        }
    }


    @FXML
    private void handleUpdateRoom() {
        String roomIdStr = txtRoomId.getText();
        String roomType = cmbRoomType.getValue();
        String status = cmbStatus.getValue();
        String price = txtprice.getText();

        if (roomIdStr.isEmpty() || roomType == null || status == null || price.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Selection Error", "Please select a room and fill all fields to update!");
            return;
        }

        String query = "UPDATE rooms SET room_type = ?, status = ?, price = ? WHERE room_id = ?";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, roomType);
                pstmt.setString(2, status);
                pstmt.setString(3, price);
                pstmt.setInt(4, Integer.parseInt(roomIdStr));

                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Room Updated Successfully!");
                    loadRoomData();
                    handleClear();
                } else {
                    showAlert(Alert.AlertType.WARNING, "Update Failed", "No matching room found to update.");
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clear බොත්තම
    @FXML
    private void handleClear() {
        cmbRoomType.getSelectionModel().clearSelection();
        cmbStatus.getSelectionModel().clearSelection();
        txtprice.clear();
        tblRooms.getSelectionModel().clearSelection();
        autoGenerateRoomDetails();
    }

  
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}