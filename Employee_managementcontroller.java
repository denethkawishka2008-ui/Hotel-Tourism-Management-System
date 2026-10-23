package Hospitality_and_tourism_system;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.sql.*;

public class Employee_managementcontroller {

    @FXML private TextField txtEmpId;
    @FXML private TextField txtEmpName;
    @FXML private ComboBox<String> cmbEmpRole;
    @FXML private TextField txtAddress;
    @FXML private ComboBox<String> cmbGender;
    @FXML private TextField txtAge;
    @FXML private TextField txtEmail;
    @FXML private TextField txtNic;
    @FXML private TextField txtPhone;
    @FXML private Label lblImagePath;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;

    private String selectedImagePath = "";

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hospitality_db?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "deneth";

    @FXML
    public void initialize() {
        cmbEmpRole.getItems().addAll("Clean Staff", "Waiters", "Admin", "Manager", "Cashier");
        cmbGender.getItems().addAll("Male", "Female", "Other");
        autoGenerateEmployeeId();
    }

    private void autoGenerateEmployeeId() {
        String query = "SELECT MAX(employee_id) AS max_id FROM employees";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                String maxId = rs.getString("max_id");
                if (maxId != null && maxId.startsWith("EMP")) {
                    int numericId = Integer.parseInt(maxId.substring(3)) + 1;
                    txtEmpId.setText(String.format("EMP%03d", numericId));
                } else {
                    txtEmpId.setText("EMP001");
                }
            } else {
                txtEmpId.setText("EMP001");
            }
        } catch (Exception e) {
            txtEmpId.setText("EMP001");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBrowseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(txtEmpId.getScene().getWindow());
        if (selectedFile != null) {
            selectedImagePath = selectedFile.getAbsolutePath();
            lblImagePath.setText(selectedFile.getName());
        }
    }

    @FXML
    private void handleSaveEmployee() {
        String empId = txtEmpId.getText().trim();
        String name = txtEmpName.getText().trim();
        String role = cmbEmpRole.getValue();
        String address = txtAddress.getText().trim();
        String gender = cmbGender.getValue();
        String ageText = txtAge.getText().trim();
        String email = txtEmail.getText().trim();
        String nic = txtNic.getText().trim();
        String phone = txtPhone.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();
        String confirmPassword = txtConfirmPassword.getText().trim();

        // Validations
        if (name.isEmpty() || role == null || nic.isEmpty() || phone.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill all required fields!");
            return;
        }

        // Email validation regex
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Email", "Please enter a valid email address!");
            return;
        }

        // Phone validation (Sri Lankan digits check or basic numeric check)
        if (!phone.matches("^[0-9]{10,15}$")) {
            showAlert(Alert.AlertType.WARNING, "Invalid Phone", "Please enter a valid phone number (digits only)!");
            return;
        }

        // Password confirmation check
        if (!password.equals(confirmPassword)) {
            showAlert(Alert.AlertType.WARNING, "Password Mismatch", "Password and Confirm Password do not match!");
            return;
        }

        int age = 0;
        try {
            if (!ageText.isEmpty()) {
                age = Integer.parseInt(ageText);
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Input", "Age must be a valid number!");
            return;
        }

        String query = "INSERT INTO employees (employee_id, employee_name, employee_role, address, gender, age, email_address, nic, phone_number, profile_image, username, password, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Free')";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, empId);
            pstmt.setString(2, name);
            pstmt.setString(3, role);
            pstmt.setString(4, address);
            pstmt.setString(5, gender);
            pstmt.setInt(6, age);
            pstmt.setString(7, email);
            pstmt.setString(8, nic);
            pstmt.setString(9, phone);
            pstmt.setString(10, selectedImagePath);
            pstmt.setString(11, username);
            pstmt.setString(12, password);

            pstmt.executeUpdate();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Employee added successfully!");
            handleClear();
            autoGenerateEmployeeId();

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to add employee: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            // Dashboard FXML එක Load කරගැනීම (ඔබේ ප්‍රොජෙක්ට් එකේ FXML නම මෙතැනට දෙන්න, උදා: dashboard.fxml)
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("Dashbord.fxml"));
            javafx.scene.Parent root = loader.load();
            
            javafx.stage.Stage stage = (javafx.stage.Stage) txtEmpId.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.setTitle("DK Tourism Hotel - Dashboard");
            stage.centerOnScreen();
            stage.show();
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not load dashboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClear() {
        txtEmpName.clear();
        cmbEmpRole.getSelectionModel().clearSelection();
        txtAddress.clear();
        cmbGender.getSelectionModel().clearSelection();
        txtAge.clear();
        txtEmail.clear();
        txtNic.clear();
        txtPhone.clear();
        lblImagePath.setText("No file chosen");
        selectedImagePath = "";
        txtUsername.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}