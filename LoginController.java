package Hospitality_and_tourism_system;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private ImageView imgLogo;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private ProgressBar progressBar;

    @FXML
    public void initialize() {
        try {
            Image logoImg = new Image(getClass().getResourceAsStream("/Hospitality_and_tourism_system/resources/Login.png"));
            if (imgLogo != null) {
                imgLogo.setImage(logoImg);
            }
        } catch (Exception e) {
            System.out.println("Logo image path could not be loaded automatically: " + e.getMessage());
        }
        
        progressBar.setProgress(0.0);
    }

    @FXML
    private void handleLogin() {
        String user = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter both username and password!");
            return;
        }

        // Disable UI elements during progress animation
        btnLogin.setDisable(true);
        txtUsername.setDisable(true);
        txtPassword.setDisable(true);

        // Progress bar smooth animation sequence
        Timeline timeline = new Timeline();
        final double[] progress = {0.0};

        KeyFrame keyFrame = new KeyFrame(Duration.millis(30), event -> {
            progress[0] += 0.03;
            progressBar.setProgress(progress[0]);

            if (progress[0] >= 1.0) {
                timeline.stop();
                checkUserRoleAndLogin(user, pass);
            }
        });

        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkUserRoleAndLogin(String username, String password) {
        String query = "SELECT employee_id, employee_name, employee_role FROM employees WHERE username = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, username);
            pst.setString(2, password);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String empId = rs.getString("employee_id");
                String empName = rs.getString("employee_name");
                String role = rs.getString("employee_role");

                if (role == null || role.trim().isEmpty()) {
                    Platform.runLater(() -> {
                        resetLoginUI();
                        showAlert(Alert.AlertType.ERROR, "Error", "Role is not assigned for this user!");
                    });
                    return;
                }

                String trimmedRole = role.trim();
                boolean opened;

                if (trimmedRole.equalsIgnoreCase("Admin") || trimmedRole.equalsIgnoreCase("Manager")) {
                    UserSession.login(empId, username, trimmedRole);
                    opened = openWindow("Dashbord.fxml", "Admin Dashboard - Welcome " + empName, 1100, 700);
                } else if (trimmedRole.equalsIgnoreCase("Housekeeping Staff")
                        || trimmedRole.equalsIgnoreCase("Receptionist")
                        || trimmedRole.equalsIgnoreCase("Maintenance")
                        || trimmedRole.equalsIgnoreCase("Kitchen Staff")
                        || trimmedRole.equalsIgnoreCase("Staff")) {
                    UserSession.login(empId, username, trimmedRole);
                    opened = openWindow("EmployeeDashboard.fxml", "Employee Dashboard - Welcome " + empName, 1200, 750);
                } else {
                    Platform.runLater(() -> {
                        resetLoginUI();
                        showAlert(Alert.AlertType.ERROR, "Access Denied", "Invalid user role!");
                    });
                    return;
                }

                if (opened) {
                    Platform.runLater(() -> {
                        ((Stage) btnLogin.getScene().getWindow()).close();
                    });
                } else {
                    UserSession.logout();
                    Platform.runLater(() -> {
                        resetLoginUI();
                        showAlert(Alert.AlertType.ERROR, "Error", "Could not open the dashboard. Check the console for details.");
                    });
                }
            } else {
                Platform.runLater(() -> {
                    resetLoginUI();
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid Username or Password!");
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                resetLoginUI();
                showAlert(Alert.AlertType.ERROR, "Database Error", "Database connection error!");
            });
        }
    }

    private boolean openWindow(String fxmlFile, String title, double width, double height) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, width, height));
            stage.show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void resetLoginUI() {
        progressBar.setProgress(0.0);
        btnLogin.setDisable(false);
        txtUsername.setDisable(false);
        txtPassword.setDisable(false);
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}