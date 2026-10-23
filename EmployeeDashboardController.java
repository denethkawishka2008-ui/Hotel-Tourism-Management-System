package Hospitality_and_tourism_system;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class EmployeeDashboardController implements Initializable {

    @FXML private StackPane rootStackPane;
    @FXML private AnchorPane mainAnchorPane;
    @FXML private Label lblEmployeeWelcome, lblLiveDate, lblLiveTime;
    @FXML private Label lblTaskRoomNo, lblTaskStartTime, lblTaskEndTime, lblTaskRoomStatus;
    @FXML private Label lblBigWelcome;
    @FXML private Button btnThemeToggle, btnProfileDrawer, btnFinishTask, btnMsgAdmin;
    @FXML private DatePicker calendarPicker;
    @FXML private ImageView imgEmployeeAvatar, imgModalAvatar, imgTeamBanner;
    @FXML private TextArea txtAdminNotices, txtAdminMessageBody;

    @FXML private AnchorPane paneProfileModal, paneMessageModal;
    @FXML private TextField txtProfileEmpId, txtProfileName, txtProfileNic, txtProfilePhone, txtProfileEmail, txtProfileAddress, txtProfileUsername;
    @FXML private PasswordField txtProfilePassword;

    private boolean isDarkMode = false;
    private String currentLoggedInEmpId = null;
    private String currentEmpName = "Employee";

    private int currentTaskId = -1;
    private String currentTaskRoomNo = null;
    private LocalDate currentTaskDate = null;
    private LocalTime taskEndTime = null;

    private Timeline liveRefresh;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
      
        currentLoggedInEmpId = UserSession.getEmployeeId();

        initLiveClock();
        calendarPicker.setValue(LocalDate.now());

        loadTeamBannerImage();

        if (currentLoggedInEmpId == null || currentLoggedInEmpId.trim().isEmpty()) {
            lblEmployeeWelcome.setText("No user logged in. Please log in again.");
            showNoTask();
            loadAdminNotices();
            return;
        }

       
        loadEmployeeInfo(currentLoggedInEmpId);
        loadTodayTask(currentLoggedInEmpId);
        loadAdminNotices();

        calendarPicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null && currentLoggedInEmpId != null) {
                loadTodayTask(currentLoggedInEmpId);
            }
        });

        startLiveTaskRefresh();
    }

    private void loadTeamBannerImage() {
        try {
            if (imgTeamBanner != null) {
                Image bannerImg = new Image(getClass().getResourceAsStream("imgEmployeeAvatar.png"));
                imgTeamBanner.setImage(bannerImg);
            }
        } catch (Exception e) {
            System.out.println("Team banner image loading warning: " + e.getMessage());
        }
    }

    public void setLoggedInEmployee(String empId) {
        this.currentLoggedInEmpId = empId;
        loadEmployeeInfo(empId);
        loadTodayTask(empId);
    }

    private void initLiveClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            lblLiveDate.setText(LocalDate.now().toString());
            lblLiveTime.setText(LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
            checkTaskUnlockStatus();
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void startLiveTaskRefresh() {
        liveRefresh = new Timeline(new KeyFrame(Duration.seconds(5), e -> {
            if (rootStackPane.getScene() == null
                    || rootStackPane.getScene().getWindow() == null
                    || !rootStackPane.getScene().getWindow().isShowing()) {
                liveRefresh.stop();
                return;
            }
            if (currentLoggedInEmpId == null) {
                return;
            }
            loadTodayTask(currentLoggedInEmpId);
        }));
        liveRefresh.setCycleCount(Animation.INDEFINITE);
        liveRefresh.play();
    }

    private void makeImageViewCircular(ImageView imageView) {
        if (imageView == null) return;
        double width = imageView.getFitWidth();
        double height = imageView.getFitHeight();
        double radius = Math.min(width, height) / 2;
        
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(width / 2, height / 2, radius);
        imageView.setClip(clip);
    }

    private void setDefaultOrCustomImage(String imgPath) {
        try {
            if (imgPath != null && !imgPath.isEmpty()) {
                File file = new File(imgPath);
                if (file.exists()) {
                    Image img = new Image(file.toURI().toString());
                    if (imgEmployeeAvatar != null) imgEmployeeAvatar.setImage(img);
                    if (imgModalAvatar != null) imgModalAvatar.setImage(img);
                    
                    makeImageViewCircular(imgEmployeeAvatar);
                    makeImageViewCircular(imgModalAvatar);
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Image defaultImg = new Image(getClass().getResourceAsStream("imgEmployeeAvatar.png"));
            if (imgEmployeeAvatar != null) imgEmployeeAvatar.setImage(defaultImg);
            if (imgModalAvatar != null) imgModalAvatar.setImage(defaultImg);
            
            makeImageViewCircular(imgEmployeeAvatar);
            makeImageViewCircular(imgModalAvatar);
        } catch (Exception ignored) {
        }
    }

    private void loadEmployeeInfo(String empId) {
        String query = "SELECT employee_name, profile_image FROM employees WHERE employee_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, empId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    currentEmpName = rs.getString("employee_name");
                    lblEmployeeWelcome.setText("Welcome back, " + currentEmpName + "!");
                    
                    if (lblBigWelcome != null) {
                        lblBigWelcome.setText("Welcome to DK Hotel Management System - " + currentEmpName + " ✨");
                        playWelcomeAnimation();
                    }

                    setDefaultOrCustomImage(rs.getString("profile_image"));
                } else {
                    lblEmployeeWelcome.setText("Employee " + empId + " was not found in database.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void playWelcomeAnimation() {
        if (lblBigWelcome == null) return;
        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(1.5), lblBigWelcome);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(javafx.util.Duration.seconds(1.2), lblBigWelcome);
        scale.setFromX(0.85);
        scale.setToX(1.0);
        scale.setFromY(0.85);
        scale.setToY(1.0);

        javafx.animation.ParallelTransition pt = new javafx.animation.ParallelTransition(fade, scale);
        pt.play();
    }

 
    private void loadTodayTask(String empId) {
        String query = "SELECT * FROM employee_tasks "
                + "WHERE employee_id = ? AND room_status = 'In Progress' "
                + "ORDER BY task_id DESC LIMIT 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, empId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    currentTaskId = rs.getInt("task_id");
                    currentTaskRoomNo = rs.getString("room_number");
                    Date tDate = rs.getDate("task_date");
                    currentTaskDate = tDate != null ? tDate.toLocalDate() : LocalDate.now();

                    lblTaskRoomNo.setText(currentTaskRoomNo);
                    lblTaskStartTime.setText(rs.getString("work_start_time"));
                    String endTimeStr = rs.getString("work_end_time");
                    lblTaskEndTime.setText(endTimeStr);
                    lblTaskRoomStatus.setText(rs.getString("room_status"));

                    taskEndTime = (endTimeStr != null && !endTimeStr.isEmpty()) ? LocalTime.parse(endTimeStr) : null;
                    checkTaskUnlockStatus();
                } else {
                    showNoTask();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showNoTask() {
        currentTaskId = -1;
        currentTaskRoomNo = null;
        currentTaskDate = null;
        taskEndTime = null;

        lblTaskRoomNo.setText("No Task Assigned");
        lblTaskStartTime.setText("--:--");
        lblTaskEndTime.setText("--:--");
        lblTaskRoomStatus.setText("Free");
        btnFinishTask.setDisable(true);
        btnFinishTask.setText("No Active Task Today");
    }

    private boolean isTaskUnlocked() {
        if (currentTaskId == -1 || taskEndTime == null || currentTaskDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return currentTaskDate.isBefore(today)
                || (currentTaskDate.equals(today) && !LocalTime.now().isBefore(taskEndTime));
    }

    private void checkTaskUnlockStatus() {
        if (currentTaskId == -1 || taskEndTime == null) {
            return;
        }
        if (isTaskUnlocked()) {
            btnFinishTask.setDisable(false);
            btnFinishTask.setText("Finish Task & Set Status to Free");
        } else {
            btnFinishTask.setDisable(true);
            if (currentTaskDate != null && currentTaskDate.isAfter(LocalDate.now())) {
                btnFinishTask.setText("Locked until " + currentTaskDate + " " + taskEndTime);
            } else {
                btnFinishTask.setText("Locked until End Time (" + taskEndTime + ")");
            }
        }
    }

    @FXML
    private void handleFinishTask() {
        if (currentTaskId == -1 || currentTaskRoomNo == null) return;

        if (!isTaskUnlocked()) {
            showAlert("Locked", "You can finish this task only after the end time.");
            return;
        }

        String roomQuery = "UPDATE rooms SET status = 'Available' WHERE room_number = ?";
        String empQuery = "UPDATE employees SET status = 'Free' WHERE employee_id = ?";
        String historyQuery = "UPDATE room_housekeeping SET room_status = 'Completed' "
                + "WHERE room_number = ? AND employee_id = ? AND room_status = 'In Progress'";
        String deleteTaskQuery = "DELETE FROM employee_tasks WHERE task_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement p1 = conn.prepareStatement(roomQuery);
                 PreparedStatement p2 = conn.prepareStatement(empQuery);
                 PreparedStatement p3 = conn.prepareStatement(historyQuery);
                 PreparedStatement p4 = conn.prepareStatement(deleteTaskQuery)) {

                p1.setString(1, currentTaskRoomNo);
                p1.executeUpdate();

                p2.setString(1, currentLoggedInEmpId);
                p2.executeUpdate();

                p3.setString(1, currentTaskRoomNo);
                p3.setString(2, currentLoggedInEmpId);
                p3.executeUpdate();

                p4.setInt(1, currentTaskId);
                p4.executeUpdate();

                conn.commit();

                showAlert("Success", "Task finished! Room is now Available and you are Free.");
                loadTodayTask(currentLoggedInEmpId);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to finish task: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleTheme() {
        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            mainAnchorPane.getStyleClass().remove("light-mode");
            btnThemeToggle.setText("☀️");
        } else {
            mainAnchorPane.getStyleClass().add("light-mode");
            btnThemeToggle.setText("🌙");
        }
    }

    @FXML
    private void handleOpenProfileModal() {
        if (currentLoggedInEmpId == null) {
            showAlert("Error", "No user is logged in.");
            return;
        }
        loadProfileDataIntoModal(currentLoggedInEmpId);
        paneProfileModal.setVisible(true);
    }

    @FXML
    private void handleCloseProfileModal() {
        paneProfileModal.setVisible(false);
    }

    @FXML
    private void handleUploadProfileImage() {
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
                pst.setString(2, currentLoggedInEmpId);
                pst.executeUpdate();

                setDefaultOrCustomImage(imagePath);
                showAlert("Success", "Profile picture updated successfully!");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSaveProfile() {
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

            UserSession.login(currentLoggedInEmpId, txtProfileUsername.getText(), UserSession.getRole());
            loadEmployeeInfo(currentLoggedInEmpId);
            showAlert("Success", "Profile details saved successfully!");
            paneProfileModal.setVisible(false);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save profile: " + e.getMessage());
        }
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

                    setDefaultOrCustomImage(rs.getString("profile_image"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleOpenMessageAdmin() {
        if (currentLoggedInEmpId == null) {
            showAlert("Error", "No user is logged in.");
            return;
        }
        txtAdminMessageBody.clear();
        paneMessageModal.setVisible(true);
    }

    @FXML
    private void handleCloseMessageModal() {
        paneMessageModal.setVisible(false);
    }

    @FXML
    private void handleSendAdminMessage() {
        String msg = txtAdminMessageBody.getText().trim();
        if (msg.isEmpty()) {
            showAlert("Error", "Please type a message before sending.");
            return;
        }

        String query = "INSERT INTO employee_messages (employee_id, employee_name, message_content) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, currentLoggedInEmpId);
            pst.setString(2, currentEmpName);
            pst.setString(3, msg);
            pst.executeUpdate();

            showAlert("Success", "Message sent to Admin successfully!");
            paneMessageModal.setVisible(false);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to send message to admin.");
        }
    }

    private void loadAdminNotices() {
        StringBuilder notices = new StringBuilder();
        String query = "SELECT notice_title, notice_message, posted_date FROM admin_notices ORDER BY notice_id DESC LIMIT 5";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                notices.append("• ").append(rs.getString("notice_title")).append("\n")
                       .append(rs.getString("notice_message")).append("\n")
                       .append("🕒 ").append(rs.getString("posted_date")).append("\n\n------------------------------------\n\n");
            }
            txtAdminNotices.setText(notices.length() > 0 ? notices.toString() : "No new notices from admin today.");
        } catch (SQLException e) {
            txtAdminNotices.setText("Failed to load notices.");
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