package Hospitality_and_tourism_system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;

public class CustomerHistoryController implements Initializable {

    @FXML private Button btnBack;
    @FXML private Button btnDateFilter;
    @FXML private Button btnReset;
    @FXML private Button btnSearch;
    @FXML private Button btnToday;
    @FXML private Button btnPrintReport;
    @FXML private DatePicker datePicker;
    
    @FXML private Label lblNicVisitCount;
    @FXML private Label lblYearRevenue;
    @FXML private Label lblYearVisitors;
    
    @FXML private TableView<CustomerHistory> tableHistory;
    @FXML private TableColumn<CustomerHistory, Integer> colId;
    @FXML private TableColumn<CustomerHistory, String> colNic;
    @FXML private TableColumn<CustomerHistory, String> colName;
    @FXML private TableColumn<CustomerHistory, String> colLastName;
    @FXML private TableColumn<CustomerHistory, String> colRoomType;
    @FXML private TableColumn<CustomerHistory, String> colRoomNo;
    @FXML private TableColumn<CustomerHistory, String> colInDate;
    @FXML private TableColumn<CustomerHistory, String> colOutDate;
    @FXML private TableColumn<CustomerHistory, Double> colAmount;
    @FXML private TableColumn<CustomerHistory, String> colPayment;
    
    @FXML private TextField txtSearch;

    private ObservableList<CustomerHistory> historyList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Table Columns mapping with Model properties
        colId.setCellValueFactory(cellData -> cellData.getValue().historyIdProperty().asObject());
        colNic.setCellValueFactory(cellData -> cellData.getValue().customerNicProperty());
        colName.setCellValueFactory(cellData -> cellData.getValue().firstNameProperty());
        colLastName.setCellValueFactory(cellData -> cellData.getValue().lastNameProperty());
        colRoomType.setCellValueFactory(cellData -> cellData.getValue().roomTypeProperty());
        colRoomNo.setCellValueFactory(cellData -> cellData.getValue().roomNumberProperty());
        colInDate.setCellValueFactory(cellData -> cellData.getValue().inDateProperty());
        colOutDate.setCellValueFactory(cellData -> cellData.getValue().outDateProperty());
        colAmount.setCellValueFactory(cellData -> cellData.getValue().totalAmountProperty().asObject());
        colPayment.setCellValueFactory(cellData -> cellData.getValue().paymentMethodProperty());

        // Load initial data
        loadHistoryData("SELECT * FROM history");
        loadSummaryCards();

        // Button Actions
        btnSearch.setOnAction(event -> handleSearch());
        btnReset.setOnAction(event -> handleReset());
        btnDateFilter.setOnAction(event -> handleDateFilter());
        btnToday.setOnAction(event -> handleTodayFilter());
        btnBack.setOnAction(event -> handleBack());
        if (btnPrintReport != null) {
            btnPrintReport.setOnAction(event -> handlePrintReport());
        }

        // Table Row Selection Listener (for NIC Check card update)
        tableHistory.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                fetchCustomerVisitStats(newValue.getCustomerNic());
            }
        });
    }

    private void loadHistoryData(String query) {
        historyList.clear();
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                historyList.add(new CustomerHistory(
                        rs.getInt("history_id"),
                        rs.getString("customer_nic"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("room_type"),
                        rs.getString("room_number"),
                        rs.getString("in_date"),
                        rs.getString("out_date"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_method")
                ));
            }
            tableHistory.setItems(historyList);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load history data.");
        }
    }

    private void loadSummaryCards() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String query = "SELECT COUNT(*), SUM(total_amount) FROM history WHERE YEAR(booking_date) = 2026";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                int visitors = rs.getInt(1);
                double revenue = rs.getDouble(2);
                lblYearVisitors.setText(String.valueOf(visitors));
                lblYearRevenue.setText(String.format("Rs. %.2f", revenue));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void fetchCustomerVisitStats(String nic) {
        String query = "SELECT COUNT(*) FROM history WHERE customer_nic = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, nic);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                lblNicVisitCount.setText("NIC: " + nic + " (" + count + " visits)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleSearch() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) {
            loadHistoryData("SELECT * FROM history");
            return;
        }
        String query = "SELECT * FROM history WHERE customer_nic LIKE ? OR first_name LIKE ? OR last_name LIKE ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "%" + keyword + "%");
            pstmt.setString(2, "%" + keyword + "%");
            pstmt.setString(3, "%" + keyword + "%");
            
            ResultSet rs = pstmt.executeQuery();
            historyList.clear();
            while (rs.next()) {
                historyList.add(new CustomerHistory(
                        rs.getInt("history_id"),
                        rs.getString("customer_nic"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("room_type"),
                        rs.getString("room_number"),
                        rs.getString("in_date"),
                        rs.getString("out_date"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_method")
                ));
            }
            tableHistory.setItems(historyList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleDateFilter() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            showAlert("Selection Error", "Please select a date to filter.");
            return;
        }
        String query = "SELECT * FROM history WHERE in_date = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, selectedDate.toString());
            ResultSet rs = pstmt.executeQuery();
            historyList.clear();
            while (rs.next()) {
                historyList.add(new CustomerHistory(
                        rs.getInt("history_id"),
                        rs.getString("customer_nic"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        rs.getString("room_type"),
                        rs.getString("room_number"),
                        rs.getString("in_date"),
                        rs.getString("out_date"),
                        rs.getDouble("total_amount"),
                        rs.getString("payment_method")
                ));
            }
            tableHistory.setItems(historyList);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void handleTodayFilter() {
        String today = LocalDate.now().toString();
        String query = "SELECT * FROM history WHERE in_date = '" + today + "'";
        loadHistoryData(query);
    }

    private void handleReset() {
        txtSearch.clear();
        datePicker.setValue(null);
        lblNicVisitCount.setText("Select a row / Search NIC");
        loadHistoryData("SELECT * FROM history");
        loadSummaryCards();
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Dashbord.fxml"));
            Parent root = loader.load();

            Stage currentStage = (Stage) btnBack.getScene().getWindow();

            Stage dashboardStage = new Stage();
            dashboardStage.setTitle("DK Tourism Hotel - Dashboard");
            dashboardStage.setScene(new Scene(root));
            dashboardStage.show();

            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to load Dashboard: " + e.getMessage());
            alert.showAndWait();
        }
    }
    
    @FXML
    private void handlePrintReport() {
        try {
            String dbUrl = "jdbc:mysql://localhost:3306/hospitality_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            String dbUser = "root";
            String dbPassword = "deneth";

            Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            
            // 1. .jrxml file එක InputStream එකක් ලෙස load කරගැනීම
            InputStream jrxmlStream = getClass().getResourceAsStream("/Hospitality_and_tourism_system/customer_history_report.jrxml");

            if (jrxmlStream == null) {
                showAlert("Report Error", "customer_history_report.jrxml file එක සොයාගැනීමට නොහැකි විය!");
                connection.close();
                return;
            }

            
            JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, connection);
            
           
            JasperViewer.viewReport(jasperPrint, false);

            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Report Generation Error: " + e.getMessage());
            showAlert("Report Error", "Failed to generate report: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}