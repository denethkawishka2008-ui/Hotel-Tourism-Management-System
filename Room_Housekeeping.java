package Hospitality_and_tourism_system;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class Room_Housekeeping {

    @FXML private ComboBox<String> cmbDirtyRooms;         // Dirty rooms (to assign)
    @FXML private ComboBox<String> cmbMaintenanceRooms;   // Maintenance rooms (to finish)
    @FXML private Label lblLastCleanedBy;
    @FXML private Label lblLastCleanedTime;

    @FXML private ComboBox<String> cmbEmpName;
    @FXML private TextField txtEmpId;
    @FXML private TextField txtEmpNic;
    @FXML private TextField txtEmpPhone;

    @FXML private DatePicker dpCleaningDate;
    @FXML private TextField txtStartTime;
    @FXML private TextField txtEndTime;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hospitality_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "deneth";

    // Employee ID of the selected maintenance room (used when finishing manually)
    private String currentEmpIdForFinish = "";

    @FXML
    public void initialize() {
        loadDirtyRoomNumbers();
        loadMaintenanceRooms();
        loadFreeEmployees();
        dpCleaningDate.setValue(LocalDate.now());

        startAutoTimeChecker();

        cmbDirtyRooms.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                fetchLastCleanedDetails(newV);
            }
        });

        if (cmbMaintenanceRooms != null) {
            cmbMaintenanceRooms.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                if (newV != null) {
                    fetchEmployeeIdByMaintenanceRoom(newV);
                }
            });
        }

        cmbEmpName.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null && !newV.trim().isEmpty()) {
                fetchEmployeeDetailsByName(newV.trim());
            } else {
                clearEmployeeFields();
            }
        });
    }

    // ============================================================
    //  AUTO FINISH (runs every minute)
    // ============================================================
    private void startAutoTimeChecker() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(1), event -> checkAndCompleteExpiredTasks()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkAndCompleteExpiredTasks() {
        String currentTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        LocalDate currentDate = LocalDate.now();

        String selectQuery = "SELECT room_number, employee_id FROM room_housekeeping "
                + "WHERE room_status = 'In Progress' AND cleaning_date = ? AND end_time <= ?";

        boolean changed = false;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            List<String[]> expired = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(selectQuery)) {
                pstmt.setDate(1, java.sql.Date.valueOf(currentDate));
                pstmt.setString(2, currentTimeStr);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        expired.add(new String[]{rs.getString("room_number"), rs.getString("employee_id")});
                    }
                }
            }

            if (expired.isEmpty()) {
                return;
            }

            conn.setAutoCommit(false);
            try {
                for (String[] task : expired) {
                    completeTask(conn, task[0], task[1]);
                    System.out.println("Auto Finished Task -> Room: " + task[0] + ", Employee: " + task[1]);
                }
                conn.commit();
                changed = true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (changed) {
            loadDirtyRoomNumbers();
            loadMaintenanceRooms();
            loadFreeEmployees();
        }
    }

    private void completeTask(Connection conn, String roomNo, String empId) throws SQLException {
        String roomQuery = "UPDATE rooms SET status = 'Available' WHERE room_number = ?";
        String empQuery = "UPDATE employees SET status = 'Free' WHERE employee_id = ?";
        String housekeepingQuery = "UPDATE room_housekeeping SET room_status = 'Completed' "
                + "WHERE room_number = ? AND employee_id = ? AND room_status = 'In Progress'";
        String empTaskQuery = "UPDATE employee_tasks SET room_status = 'Completed', work_status = 'Free' "
                + "WHERE room_number = ? AND employee_id = ? AND room_status = 'In Progress'";

        try (PreparedStatement p1 = conn.prepareStatement(roomQuery);
             PreparedStatement p2 = conn.prepareStatement(empQuery);
             PreparedStatement p3 = conn.prepareStatement(housekeepingQuery);
             PreparedStatement p4 = conn.prepareStatement(empTaskQuery)) {

            p1.setString(1, roomNo);
            p1.executeUpdate();

            p2.setString(1, empId);
            p2.executeUpdate();

            p3.setString(1, roomNo);
            p3.setString(2, empId);
            p3.executeUpdate();

            p4.setString(1, roomNo);
            p4.setString(2, empId);
            p4.executeUpdate();
        }
    }

    // ============================================================
    //  LOADERS
    // ============================================================
    private void loadDirtyRoomNumbers() {
        if (cmbDirtyRooms == null) return;
        cmbDirtyRooms.getItems().clear();
        String query = "SELECT room_number FROM rooms WHERE status = 'Dirty' OR status = 'dust'";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                cmbDirtyRooms.getItems().add(rs.getString("room_number"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMaintenanceRooms() {
        if (cmbMaintenanceRooms == null) return;
        cmbMaintenanceRooms.getItems().clear();
        String query = "SELECT room_number FROM rooms WHERE status = 'In Progress'";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                cmbMaintenanceRooms.getItems().add(rs.getString("room_number"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

   
    private void loadFreeEmployees() {
        cmbEmpName.getItems().clear();
        
      
        String query = "SELECT employee_name FROM employees WHERE status = 'Free' OR status = 'free'";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String name = rs.getString("employee_name");
                if (name != null && !name.trim().isEmpty()) {
                    cmbEmpName.getItems().add(name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fetchEmployeeIdByMaintenanceRoom(String roomNumber) {
        String query = "SELECT employee_id FROM room_housekeeping "
                + "WHERE room_number = ? AND room_status = 'In Progress' ORDER BY task_id DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                currentEmpIdForFinish = rs.next() ? rs.getString("employee_id") : "";
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fetchLastCleanedDetails(String roomNumber) {
        String query = "SELECT employee_name, cleaning_date, end_time FROM room_housekeeping "
                + "WHERE room_number = ? ORDER BY task_id DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    lblLastCleanedBy.setText(rs.getString("employee_name"));
                    lblLastCleanedTime.setText(rs.getDate("cleaning_date") + " | " + rs.getTime("end_time"));
                } else {
                    lblLastCleanedBy.setText("No previous records");
                    lblLastCleanedTime.setText("N/A");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fetchEmployeeDetailsByName(String name) {
        String query = "SELECT employee_id, nic, phone_number FROM employees WHERE employee_name = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    txtEmpId.setText(rs.getString("employee_id"));
                    txtEmpNic.setText(rs.getString("nic"));
                    txtEmpPhone.setText(rs.getString("phone_number"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearEmployeeFields() {
        txtEmpId.clear();
        txtEmpNic.clear();
        txtEmpPhone.clear();
    }

    // ============================================================
    //  ASSIGN TASK  -> room_housekeeping + rooms + employees + employee_tasks
    // ============================================================
    @FXML
    private void handleSendTask() {
        String roomNo = cmbDirtyRooms.getValue();
        String empName = cmbEmpName.getValue();
        String empId = txtEmpId.getText().trim();
        String empNic = txtEmpNic.getText().trim();
        String empPhone = txtEmpPhone.getText().trim();
        LocalDate cleanDate = dpCleaningDate.getValue();
        String startText = txtStartTime.getText().trim();
        String endText = txtEndTime.getText().trim();

        if (roomNo == null || empName == null || empId.isEmpty() || cleanDate == null) {
            showAlert(Alert.AlertType.WARNING, "Validation Error",
                    "Please select a room, a free employee and a date!");
            return;
        }

        LocalTime startTime;
        LocalTime endTime;
        try {
            startTime = LocalTime.parse(startText);
            endTime = LocalTime.parse(endText);
        } catch (DateTimeParseException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Time",
                    "Enter times in 24-hour format HH:mm (example: 08:30 and 10:15).");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Invalid Time", "End time must be after start time!");
            return;
        }

        String insertHousekeeping = "INSERT INTO room_housekeeping "
                + "(room_number, room_status, employee_id, employee_name, employee_nic, employee_phone, cleaning_date, start_time, end_time) "
                + "VALUES (?, 'In Progress', ?, ?, ?, ?, ?, ?, ?)";
        String updateRoomQuery = "UPDATE rooms SET status = 'In Progress' WHERE room_number = ?";
        String updateEmpQuery = "UPDATE employees SET status = 'Working' WHERE employee_id = ?";
        
        String insertEmployeeTask = "INSERT INTO employee_tasks "
                + "(employee_id, employee_name, room_number, room_status, work_status, work_start_time, work_end_time, task_date) "
                + "VALUES (?, ?, ?, 'In Progress', 'Working', ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            try (PreparedStatement p1 = conn.prepareStatement(insertHousekeeping);
                 PreparedStatement p2 = conn.prepareStatement(updateRoomQuery);
                 PreparedStatement p3 = conn.prepareStatement(updateEmpQuery);
                 PreparedStatement p4 = conn.prepareStatement(insertEmployeeTask)) {

                p1.setString(1, roomNo);
                p1.setString(2, empId);
                p1.setString(3, empName);
                p1.setString(4, empNic);
                p1.setString(5, empPhone);
                p1.setDate(6, java.sql.Date.valueOf(cleanDate));
                p1.setString(7, startText);
                p1.setString(8, endText);
                p1.executeUpdate();

                p2.setString(1, roomNo);
                p2.executeUpdate();

                p3.setString(1, empId);
                p3.executeUpdate();

                p4.setString(1, empId);
                p4.setString(2, empName);
                p4.setString(3, roomNo);
                p4.setTime(4, Time.valueOf(startTime));
                p4.setTime(5, Time.valueOf(endTime));
                p4.setDate(6, java.sql.Date.valueOf(cleanDate));
                p4.executeUpdate();

                conn.commit();
                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Task assigned! Room is now 'In Progress' and the employee is now 'Working'.");

                handleClear();
                loadDirtyRoomNumbers();
                loadMaintenanceRooms();
                loadFreeEmployees();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to assign task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============================================================
    //  MANUAL FINISH
    // ============================================================
    @FXML
    private void handleFinishTask() {
        if (cmbMaintenanceRooms == null) return;
        String roomNo = cmbMaintenanceRooms.getValue();
        String empId = currentEmpIdForFinish;

        if (roomNo == null || roomNo.isEmpty() || empId == null || empId.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please select a maintenance room to finish!");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);

            try {
                completeTask(conn, roomNo, empId);
                conn.commit();

                showAlert(Alert.AlertType.INFORMATION, "Success",
                        "Task completed! Room is now Available and the employee is Free.");

                handleClear();
                loadDirtyRoomNumbers();
                loadMaintenanceRooms();
                loadFreeEmployees();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to finish task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClear() {
        if (cmbDirtyRooms != null) cmbDirtyRooms.getSelectionModel().clearSelection();
        if (cmbMaintenanceRooms != null) cmbMaintenanceRooms.getSelectionModel().clearSelection();
        cmbEmpName.getSelectionModel().clearSelection();
        clearEmployeeFields();
        dpCleaningDate.setValue(LocalDate.now());
        txtStartTime.clear();
        txtEndTime.clear();
        if (lblLastCleanedBy != null) lblLastCleanedBy.setText("N/A");
        if (lblLastCleanedTime != null) lblLastCleanedTime.setText("N/A");
        currentEmpIdForFinish = "";
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}