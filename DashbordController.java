package Hospitality_and_tourism_system;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class DashbordController implements Initializable {

    @FXML private StackPane rootStackPane;
    @FXML private ImageView hotelLogo, imgSidebarAvatar, imgModalAvatar;
    @FXML private Label lblTime;
    @FXML private Label lblAvailableRooms;
    @FXML private Label lblBookedRooms;
    @FXML private Label lblTotalCustomers;

    @FXML private Button btnCustomer, btnRoom, btnRoomkeeping, btnEmployee, btnTourism, btnHistory, btnBack ;
    @FXML private Button btnNotifications, btnAdminProfile;

    // Modals & Profile Fields
    @FXML private AnchorPane paneProfileModal, paneNotificationModal;
    @FXML private TextArea txtNotificationArea;
    @FXML private TextField txtProfileEmpId, txtProfileName, txtProfileNic, txtProfilePhone, txtProfileEmail, txtProfileAddress, txtProfileUsername;
    @FXML private PasswordField txtProfilePassword;

    private String currentAdminId = null; // UserSession eken dynamically gannawa

    private static final String CUSTOMER_FXML      = "Customer_Management.fxml";
    private static final String ROOM_FXML          = "room_management.fxml";
    private static final String HOUSEKEEPING_FXML  = "Room_Housekeeping.fxml";
    private static final String EMPLOYEE_FXML      = "Employee_management.fxml";
    private static final String HISTORY_FXML       = "Customer_History.fxml";
    private static final String LOGIN_FXML         = "Login.fxml";
    private static final String TOURISM_FXML       = "Tourism_Management.fxml";

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd | HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentAdminId = UserSession.getEmployeeId(); // Login wela inna kenage ID eka gannawa
        startClock();
        loadDashboardStats();
        if (currentAdminId != null) {
            loadAdminAvatar();
        }
    }

    private void startClock() {
        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> lblTime.setText(LocalDateTime.now().format(TIME_FORMAT))),
                new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void loadDashboardStats() {
        int available = getCount("SELECT COUNT(*) FROM rooms WHERE status = 'Available'", null);
        int booked = getCount("SELECT COUNT(*) FROM rooms WHERE status = 'Booked'", null);
        
      
        String today = LocalDate.now().toString();
        
     
        int customersToday = getCount("SELECT COUNT(*) FROM customers WHERE in_date = ?", today);

        lblAvailableRooms.setText(String.valueOf(available));
        lblBookedRooms.setText(String.valueOf(booked));
        lblTotalCustomers.setText(String.valueOf(customersToday));
    }

    private int getCount(String sql, String param) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Circular Image Helper
    private void makeImageViewCircular(ImageView imageView) {
        double width = imageView.getFitWidth();
        double height = imageView.getFitHeight();
        double radius = Math.min(width, height) / 2;
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(width / 2, height / 2, radius);
        imageView.setClip(clip);
    }

    private void loadAdminAvatar() {
        String query = "SELECT profile_image FROM employees WHERE employee_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, currentAdminId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String imgPath = rs.getString("profile_image");
                    if (imgPath != null && !imgPath.isEmpty()) {
                        File file = new File(imgPath);
                        if (file.exists()) {
                            Image img = new Image(file.toURI().toString());
                            imgSidebarAvatar.setImage(img);
                            imgModalAvatar.setImage(img);
                            makeImageViewCircular(imgSidebarAvatar);
                            makeImageViewCircular(imgModalAvatar);
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Notifications Modal Handler =====
    @FXML
    private void handleOpenNotifications(ActionEvent event) {
        StringBuilder msgList = new StringBuilder();
        String query = "SELECT employee_id, employee_name, message_content, sent_date FROM messages ORDER BY message_id DESC LIMIT 15";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                msgList.append("👤 From: ").append(rs.getString("employee_name"))
                       .append(" (ID: ").append(rs.getString("employee_id")).append(")\n")
                       .append("💬 Message: ").append(rs.getString("message_content")).append("\n")
                       .append("🕒 Date: ").append(rs.getString("sent_date"))
                       .append("\n--------------------------------------------------\n\n");
            }

            txtNotificationArea.setText(msgList.length() > 0 ? msgList.toString() : "No new messages from employees.");
            paneNotificationModal.setVisible(true);
            playModalAnimation(paneNotificationModal);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load employee messages.");
        }
    }

    @FXML
    private void handleCloseNotificationModal(ActionEvent event) {
        paneNotificationModal.setVisible(false);
    }

    // ===== Admin Profile Modal Handler =====
    @FXML
    private void handleOpenAdminProfile(ActionEvent event) {
        if (currentAdminId == null) {
            currentAdminId = UserSession.getEmployeeId();
        }
        loadProfileDataIntoModal(currentAdminId);
        paneProfileModal.setVisible(true);
        playModalAnimation(paneProfileModal);
    }

    @FXML
    private void handleCloseProfileModal(ActionEvent event) {
        paneProfileModal.setVisible(false);
    }

    private void loadProfileDataIntoModal(String empId) {
        String query = "SELECT * FROM employees WHERE employee_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, empId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    txtProfileEmpId.setText(rs.getString("employee_id"));
                    txtProfileName.setText(rs.getString("employee_name"));
                    txtProfileNic.setText(rs.getString("nic"));
                    txtProfilePhone.setText(rs.getString("phone_number"));
                    txtProfileEmail.setText(rs.getString("email_address"));
                    txtProfileAddress.setText(rs.getString("address"));
                    txtProfileUsername.setText(rs.getString("username"));
                    txtProfilePassword.setText(rs.getString("password"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUploadProfileImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fileChooser.showOpenDialog(paneProfileModal.getScene().getWindow());

        if (selectedFile != null) {
            String imagePath = selectedFile.getAbsolutePath();
            String query = "UPDATE employees SET profile_image = ? WHERE employee_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, imagePath);
                pst.setString(2, currentAdminId);
                pst.executeUpdate();

                loadAdminAvatar();
                showAlert("Success", "Profile picture updated successfully!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSaveProfile(ActionEvent event) {
        String query = "UPDATE employees SET employee_name = ?, nic = ?, phone_number = ?, email_address = ?, address = ?, username = ?, password = ? WHERE employee_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, txtProfileName.getText());
            pst.setString(2, txtProfileNic.getText());
            pst.setString(3, txtProfilePhone.getText());
            pst.setString(4, txtProfileEmail.getText());
            pst.setString(5, txtProfileAddress.getText());
            pst.setString(6, txtProfileUsername.getText());
            pst.setString(7, txtProfilePassword.getText());
            pst.setString(8, txtProfileEmpId.getText());
            pst.executeUpdate();

            loadAdminAvatar();
            showAlert("Success", "Profile updated successfully!");
            paneProfileModal.setVisible(false);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to update profile.");
        }
    }

    private void playModalAnimation(AnchorPane pane) {
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(Duration.seconds(0.4), pane);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(Duration.seconds(0.4), pane);
        scale.setFromX(0.85);
        scale.setToX(1.0);
        scale.setFromY(0.85);
        scale.setToY(1.0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(fade, scale);
        pt.play();
    }

    // ===== Navigation =====
    @FXML private void handleCustomer(ActionEvent event) { openWindow(CUSTOMER_FXML, "Customer Management", event); }
    @FXML private void handleRoom(ActionEvent event) { openWindow(ROOM_FXML, "Room Management", event); }
    @FXML private void handleHousekeeping(ActionEvent event) { openWindow(HOUSEKEEPING_FXML, "Housekeeping Management", event); }
    @FXML private void handleEmployee(ActionEvent event) { openWindow(EMPLOYEE_FXML, "Employee Management", event); }
    @FXML private void handleHistory(ActionEvent event) { openWindow(HISTORY_FXML, "Customer History", event); }
    @FXML private void handleTourismManagement(ActionEvent event) { openWindow(TOURISM_FXML, "Tourism Attractions & Email Management", event); }
    @FXML private void handleBack(ActionEvent event) { UserSession.logout(); openWindow(LOGIN_FXML, "System Login", event); }

    private void openWindow(String fxml, String title, ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", fxml + " file not found.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}