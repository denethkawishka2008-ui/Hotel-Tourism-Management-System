package Hospitality_and_tourism_system;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;

public class Customer_ManagementController {

    @FXML private TextField txtCustomerId;
    @FXML private TextField txtCustomerNic;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtCustomerAddress;
    @FXML private TextField txtCustomerPhone;
    @FXML private TextField txtEmail;
     
    @FXML private ComboBox<String> cmbRoomType;
    @FXML private ComboBox<String> cmbRoomNumber;
     
    @FXML private TextField txtNoOfRooms;
    @FXML private TextField txtNoOfNights;
    @FXML private TextField txtNoOfPersons;
     
    @FXML private RadioButton rbCash;
    @FXML private RadioButton rbCard;
    @FXML private ToggleGroup paymentGroup;
     
    @FXML private TextField txtTotal;
    @FXML private TextField txtNetTotal;
    @FXML private TextField txtPayAmount; 
    @FXML private TextField txtBalance;    
    
    @FXML private DatePicker dpInDate;
    @FXML private DatePicker dpOutDate;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hospitality_db?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "deneth"; 

    private double currentRoomPrice = 0.0;

    @FXML
    public void initialize() {
        cmbRoomType.getItems().addAll("Deluxe", "Standard", "Suite", "Single", "Double");
        
        autoUpdateRoomsToDirtyOnOutDate();
         
        txtCustomerNic.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                fetchCustomerDetailsByNic(newValue.trim());
            } else {
                txtCustomerId.clear();
                txtCustomerId.setEditable(true);
            }
        });
         
        cmbRoomType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadRoomsByType(newValue);
            }
        });
         
        cmbRoomNumber.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                String roomNo = newValue.split(" - ")[0];
                fetchRoomPrice(roomNo);
            }
        });

        txtNoOfRooms.textProperty().addListener((observable, oldValue, newValue) -> calculateTotalAmount());
        txtNoOfNights.textProperty().addListener((observable, oldValue, newValue) -> calculateTotalAmount());
         
        if (txtNetTotal != null) {
            txtNetTotal.textProperty().addListener((observable, oldValue, newValue) -> calculateBalance());
        }
        
        if (txtPayAmount != null) {
            txtPayAmount.textProperty().addListener((observable, oldValue, newValue) -> calculateBalance());
        }
    }

    private void fetchCustomerDetailsByNic(String nic) {
        String query = "SELECT customer_id, first_name, last_name, address, phone_number, email FROM customers WHERE customer_nic = ?";
         
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, nic);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    txtCustomerId.setText(rs.getString("customer_id"));
                    txtFirstName.setText(rs.getString("first_name"));
                    txtLastName.setText(rs.getString("last_name"));
                    txtCustomerAddress.setText(rs.getString("address"));
                    txtCustomerPhone.setText(rs.getString("phone_number"));
                    txtEmail.setText(rs.getString("email"));
                     
                    txtCustomerId.setEditable(false);
                } else {
                    txtFirstName.clear();
                    txtLastName.clear();
                    txtCustomerAddress.clear();
                    txtCustomerPhone.clear();
                    txtEmail.clear();
                     
                    autoGenerateNewCustomerId();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void autoGenerateNewCustomerId() {
        txtCustomerId.setEditable(false);
        String query = "SELECT MAX(customer_id) AS max_id FROM customers";
         
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (rs.next()) {
                int nextId = rs.getInt("max_id") + 1;
                txtCustomerId.setText(String.valueOf(nextId)); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRoomsByType(String roomType) {
        cmbRoomNumber.getItems().clear();
        String query = "SELECT room_number, status FROM rooms WHERE room_type = ? AND status = 'Available'";
         
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, roomType);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String roomNo = rs.getString("room_number");
                    String status = rs.getString("status");
                    cmbRoomNumber.getItems().add(roomNo + " - " + status);
                }
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load rooms by type: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void fetchRoomPrice(String roomNumber) {
        String query = "SELECT price FROM rooms WHERE room_number = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
             
            pstmt.setString(1, roomNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String priceStr = rs.getString("price");
                    currentRoomPrice = priceStr != null && !priceStr.isEmpty() ? Double.parseDouble(priceStr) : 0.0;
                    calculateTotalAmount();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void calculateTotalAmount() {
        try {
            int rooms = txtNoOfRooms.getText().isEmpty() ? 1 : Integer.parseInt(txtNoOfRooms.getText());
            int nights = txtNoOfNights.getText().isEmpty() ? 1 : Integer.parseInt(txtNoOfNights.getText());

            double total = currentRoomPrice * rooms * nights;
            txtTotal.setText(String.format("%.2f", total));
            txtNetTotal.setText(String.format("%.2f", total)); 
        } catch (NumberFormatException e) {
            // Ignore
        }
    }

    private void calculateBalance() {
        try {
            double netTotal = 0.0;
            if (txtNetTotal != null && txtNetTotal.getText() != null && !txtNetTotal.getText().trim().isEmpty()) {
                netTotal = Double.parseDouble(txtNetTotal.getText().trim());
            }

            double payAmount = 0.0;
            if (txtPayAmount != null && txtPayAmount.getText() != null && !txtPayAmount.getText().trim().isEmpty()) {
                payAmount = Double.parseDouble(txtPayAmount.getText().trim());
            }

            double balance = payAmount - netTotal;

            if (txtBalance != null) {
                txtBalance.setText(String.format("%.2f", balance));
            }
        } catch (NumberFormatException e) {
            if (txtBalance != null) {
                txtBalance.setText("0.00");
            }
        }
    }

    @FXML
    private void handleSave() {
        String customerId = txtCustomerId.getText();
        String nic = txtCustomerNic.getText();
        String firstName = txtFirstName.getText();
        String lastName = txtLastName.getText();
        String address = txtCustomerAddress.getText();
        String phone = txtCustomerPhone.getText();
        String email = txtEmail.getText();
         
        String inDate = (dpInDate.getValue() != null) ? dpInDate.getValue().toString() : "";
        String outDate = (dpOutDate.getValue() != null) ? dpOutDate.getValue().toString() : "";
         
        String roomType = cmbRoomType.getValue();
        String roomNumberWithStatus = cmbRoomNumber.getValue();
         
        try {
            
            if (txtNoOfRooms.getText().isEmpty() || txtNoOfNights.getText().isEmpty() || txtNoOfPersons.getText().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please enter Rooms, Nights, and Persons counts!");
                return;
            }

            int noOfRooms = Integer.parseInt(txtNoOfRooms.getText().trim());
            int noOfNights = Integer.parseInt(txtNoOfNights.getText().trim());
            int noOfPersons = Integer.parseInt(txtNoOfPersons.getText().trim());
             
            String paymentMethod = rbCash != null && rbCash.isSelected() ? "Cash" : "Credit Card";
             
            double total = txtTotal.getText().isEmpty() ? 0.0 : Double.parseDouble(txtTotal.getText());
            double netTotal = txtNetTotal.getText().isEmpty() ? 0.0 : Double.parseDouble(txtNetTotal.getText());
            double payAmount = (txtPayAmount == null || txtPayAmount.getText().isEmpty()) ? 0.0 : Double.parseDouble(txtPayAmount.getText());
            double balance = payAmount - netTotal;

            if (customerId.isEmpty() || firstName.isEmpty() || roomNumberWithStatus == null || nic.isEmpty() || inDate.isEmpty() || outDate.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "Please fill required fields, NIC, and Dates!");
                return;
            }

            String roomNumber = roomNumberWithStatus.split(" - ")[0];
            String roomStatus = roomNumberWithStatus.split(" - ")[1];

            if ("Booked".equalsIgnoreCase(roomStatus)) {
                showAlert(Alert.AlertType.ERROR, "Room Error", "This room is already booked! Please select an available room.");
                return;
            }

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                 
                boolean customerExists = false;
                String checkQuery = "SELECT COUNT(*) FROM customers WHERE customer_nic = ?";
                try (PreparedStatement pstmtCheck = conn.prepareStatement(checkQuery)) {
                    pstmtCheck.setString(1, nic);
                    try (ResultSet rs = pstmtCheck.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            customerExists = true;
                        }
                    }
                }

                if (!customerExists) {
                    String insertCust = "INSERT INTO customers (customer_id, customer_nic, first_name, last_name, address, phone_number, email, room_type, room_number, no_of_rooms, no_of_nights, no_of_persons, in_date, out_date, payment_method, total_amount, net_total, balance) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement pstmtCust = conn.prepareStatement(insertCust)) {
                        pstmtCust.setString(1, customerId);
                        pstmtCust.setString(2, nic);
                        pstmtCust.setString(3, firstName);
                        pstmtCust.setString(4, lastName);
                        pstmtCust.setString(5, address);
                        pstmtCust.setString(6, phone);
                        pstmtCust.setString(7, email);
                        pstmtCust.setString(8, roomType);
                        pstmtCust.setString(9, roomNumber);
                        pstmtCust.setInt(10, noOfRooms);
                        pstmtCust.setInt(11, noOfNights);
                        pstmtCust.setInt(12, noOfPersons);
                        pstmtCust.setString(13, inDate);
                        pstmtCust.setString(14, outDate);
                        pstmtCust.setString(15, paymentMethod);
                        pstmtCust.setDouble(16, total);
                        pstmtCust.setDouble(17, netTotal);
                        pstmtCust.setDouble(18, balance);
                        pstmtCust.executeUpdate();
                    }
                }

                String insertHistory = "INSERT INTO history (customer_nic, customer_id, first_name, last_name, room_type, room_number, in_date, out_date, total_amount, payment_method) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmtHist = conn.prepareStatement(insertHistory)) {
                    pstmtHist.setString(1, nic);
                    pstmtHist.setString(2, customerId);
                    pstmtHist.setString(3, firstName);
                    pstmtHist.setString(4, lastName);
                    pstmtHist.setString(5, roomType);
                    pstmtHist.setString(6, roomNumber);
                    pstmtHist.setString(7, inDate);
                    pstmtHist.setString(8, outDate);
                    pstmtHist.setDouble(9, netTotal);
                    pstmtHist.setString(10, paymentMethod);
                    pstmtHist.executeUpdate();
                }

                updateRoomStatusToBooked(conn, roomNumber);
                int visitCount = getCustomerVisitCount(conn, nic);

                showBillPopup(customerId, firstName, lastName, roomNumber, noOfRooms, noOfNights, currentRoomPrice, netTotal, paymentMethod, visitCount, payAmount, balance);
                
                // --- HTML EMAIL SENDING LOGIC (Invoice + Tourism Attractions + Food Menu + Feedback) ---
                if (email != null && !email.trim().isEmpty()) {
                    String customerFullName = firstName + " " + lastName;
                    
                    String invoiceSummary = "Customer ID    : " + customerId + "\n" +
                                            "Room Number    : " + roomNumber + "\n" +
                                            "Check-In Date  : " + inDate + "\n" +
                                            "Check-Out Date : " + outDate + "\n" +
                                            "No. of Nights  : " + noOfNights + "\n" +
                                            "Net Total      : LKR " + String.format("%.2f", netTotal) + "\n" +
                                            "Payment Method : " + paymentMethod;

                    new Thread(() -> {
                        EmailSender.sendBookingEmailWithAttractions(email, customerFullName, invoiceSummary);
                    }).start();
                }

                handleClear();

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Error saving data: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Format Error", "Please enter valid numeric values!");
        }
    }
     
    private void autoUpdateRoomsToDirtyOnOutDate() {
        String updateQuery = "UPDATE rooms r " +
                             "JOIN customers c ON r.room_number = c.room_number " +
                             "SET r.status = 'Dirty' " +
                             "WHERE c.out_date REGEXP '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' " +
                             "AND STR_TO_DATE(c.out_date, '%Y-%m-%d') <= CURDATE() AND r.status = 'Booked'";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
             
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println(rowsUpdated + " room(s) status automatically changed to 'Dirty'.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdate() {
        String nic = txtCustomerNic.getText().trim();
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String address = txtCustomerAddress.getText().trim();
        String phone = txtCustomerPhone.getText().trim();
        String email = txtEmail.getText().trim();

        if (nic.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Warning", "Please enter or select a customer NIC to update!");
            return;
        }

        String query = "UPDATE customers SET first_name = ?, last_name = ?, address = ?, phone_number = ?, email = ? WHERE customer_nic = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, address);
            pstmt.setString(4, phone);
            pstmt.setString(5, email);
            pstmt.setString(6, nic);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Customer details updated successfully!");
                handleClear();
            } else {
                showAlert(Alert.AlertType.WARNING, "Not Found", "No customer found with the given NIC: " + nic);
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update customer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        String nic = txtCustomerNic.getText().trim();

        if (nic.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Warning", "Please enter or select a customer NIC to delete!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Customer Record");
        confirmAlert.setContentText("Are you sure you want to delete the customer with NIC: " + nic + "?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM customers WHERE customer_nic = ?";

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, nic);
                int rowsAffected = pstmt.executeUpdate();

                if (rowsAffected > 0) {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Customer deleted successfully!");
                    handleClear();
                } else {
                    showAlert(Alert.AlertType.WARNING, "Not Found", "No customer found with the given NIC: " + nic);
                }

            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to delete customer: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private int getCustomerVisitCount(Connection conn, String nic) {
        String query = "SELECT COUNT(*) FROM history WHERE customer_nic = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, nic);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private void updateRoomStatusToBooked(Connection conn, String roomNumber) throws SQLException {
        String updateQuery = "UPDATE rooms SET status = 'Booked' WHERE room_number = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setString(1, roomNumber);
            pstmt.executeUpdate();
        }
    }

    private void showBillPopup(String custId, String fName, String lName, String roomNo, int rooms, int nights, double unitPrice, double netTotal, String paymentMethod, int visitCount, double payAmount, double balance) {
        String billDetails = "========================================\n" +
                             "            HOTEL INVOICE / BILL         \n" +
                             "========================================\n" +
                             "Customer ID    : " + custId + "\n" +
                             "Customer Name  : " + fName + " " + lName + "\n" +
                             "Total Visits   : " + visitCount + " (Returning Guest)\n" +
                             "----------------------------------------\n" +
                             "Room Number    : " + roomNo + "\n" +
                             "Unit Price     : LKR " + String.format("%.2f", unitPrice) + "\n" +
                             "No. of Rooms   : " + rooms + "\n" +
                             "No. of Nights  : " + nights + "\n" +
                             "----------------------------------------\n" +
                             "NET TOTAL      : LKR " + String.format("%.2f", netTotal) + "\n" +
                             "Pay Amount     : LKR " + String.format("%.2f", payAmount) + "\n" +
                             "BALANCE        : LKR " + String.format("%.2f", balance) + "\n" +
                             "Payment Type   : " + paymentMethod + "\n" +
                             "========================================\n" +
                             "        Thank You For Your Stay!        ";

        String fileName = "Invoice_" + custId + "_" + System.currentTimeMillis() + ".txt";
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(billDetails);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Alert billAlert = new Alert(Alert.AlertType.INFORMATION);
        billAlert.setTitle("Reservation Bill");
        billAlert.setHeaderText("Customer Registered Successfully! (Visits: " + visitCount + ")");
         
        TextArea textArea = new TextArea(billDetails);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefWidth(450);
        textArea.setPrefHeight(350);
        textArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px; -fx-font-weight: bold;");

        billAlert.getDialogPane().setContent(textArea);
        billAlert.showAndWait();
    }

    @FXML
    private void handleClear() {
        txtCustomerId.clear();
        txtCustomerId.setEditable(true);
        txtCustomerNic.clear();
        txtFirstName.clear();
        txtLastName.clear();
        txtCustomerAddress.clear();
        txtCustomerPhone.clear();
        txtEmail.clear();
        cmbRoomType.getSelectionModel().clearSelection();
        cmbRoomNumber.getItems().clear();
        txtNoOfRooms.clear();
        txtNoOfNights.clear();
        txtNoOfPersons.clear();
         
        dpInDate.setValue(null);
        dpOutDate.setValue(null);
         
        txtTotal.clear();
        txtNetTotal.clear();
        if (txtPayAmount != null) txtPayAmount.clear();
        if (txtBalance != null) txtBalance.clear();
        if (rbCash != null) rbCash.setSelected(true);
        currentRoomPrice = 0.0;
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

 

  public static class EmailSender {

    private static final String SENDER_EMAIL = "denethkawishka2008@gmail.com";
    private static final String SENDER_PASSWORD = "tbwaxzasurgpekrv";

    public static void sendBookingEmailWithAttractions(String recipientEmail, String customerName, String invoiceDetails) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        prop.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "DK Tourism Hotel"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject("Booking Confirmation & Travel Guide - DK Tourism Hotel");
            
            MimeMultipart multipart = new MimeMultipart("related");

            // HTML Email Body Setup
            StringBuilder htmlBuilder = new StringBuilder();
            htmlBuilder.append("<div style='font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 10px;'>")
                       .append("<h2 style='color: #d4af37; text-align: center;'>DK Tourism Hotel & Travels</h2>")
                       .append("<p>Dear <b>").append(customerName).append("</b>,</p>")
                       .append("<p>Thank you very much for choosing our hotel! We are committed to providing you with the highest quality service to ensure your Travelling Experience is exceptional. Warm wishes for a pleasant stay!</p>")
                       
                       // Invoice Section
                       .append("<div style='background: #f1f5f9; padding: 15px; border-radius: 8px; margin: 20px 0;'>")
                       .append("<h3 style='margin-top:0; color: #1e293b;'>Booking Invoice Details</h3>")
                       .append("<pre style='font-family: monospace; font-size: 13px;'>").append(invoiceDetails).append("</pre>")
                       .append("</div>")

                       // Food & Dining Request Menu Section
                       .append("<div style='background: #fffbeb; padding: 15px; border-radius: 8px; border: 1px solid #fde68a; margin: 20px 0;'>")
                       .append("<h3 style='color: #d97706; margin-top:0;'>🍽️ Food & Dining Request Menu</h3>")
                       .append("<p style='font-size: 13px; color: #78350f;'>During your stay, you can order or request the following meals:</p>")
                       .append("<ul style='font-size: 13px; color: #78350f;'>")
                       .append("<li><b>Sri Lankan Traditional Rice & Curry</b> (Breakfast / Lunch / Dinner)</li>")
                       .append("<li><b>Western & Continental Breakfast Platters</b></li>")
                       .append("<li><b>Fresh Seafood Specialties & BBQ Nights</b></li>")
                       .append("<li><b>Fresh Tropical Fruit Juices & Desserts</b></li>")
                       .append("</ul>")
                       .append("<p style='font-size: 12px; color: #92400e; margin-bottom: 0;'>To place food orders, please contact our Reception or use our online portal.</p>")
                       .append("</div>")

                       // Dynamic Tourism Places Header
                       .append("<h2 style='color: #2563eb; border-bottom: 2px solid #2563eb; padding-bottom: 5px; margin-top: 30px;'>Explore Nearby Tourist Attractions</h2>");

            // Fetch Places from DB
            String dbUrl = "jdbc:mysql://localhost:3306/hospitality_db?allowPublicKeyRetrieval=true&useSSL=false";
            List<String[]> placesList = new ArrayList<>();

            try (Connection conn = DriverManager.getConnection(dbUrl, "root", "deneth");
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT place_name, description, map_url, image_path FROM tourism_places")) {

                while (rs.next()) {
                    placesList.add(new String[]{
                        rs.getString("place_name"), 
                        rs.getString("description"), 
                        rs.getString("map_url"), 
                        rs.getString("image_path")
                    });
                }
            } catch (Exception dbEx) {
                dbEx.printStackTrace();
            }

            int imgCount = 0;
            for (String[] place : placesList) {
                imgCount++;
                String pName = place[0];
                String desc = place[1];
                String mapUrl = place[2];
                String imgPath = place[3];
                String cid = "img_" + imgCount;

                htmlBuilder.append("<div style='margin-bottom: 25px; border-bottom: 1px solid #e2e8f0; padding-bottom: 20px;'>")
                           .append("<h3 style='color: #1e293b; margin-bottom: 5px;'>").append(pName).append("</h3>")
                           .append("<p style='color: #475569; font-size: 14px; line-height: 1.5;'>").append(desc).append("</p>");

                if (imgPath != null && !imgPath.isEmpty() && new File(imgPath).exists()) {
                    htmlBuilder.append("<img src='cid:").append(cid).append("' style='max-width:100%; height:auto; border-radius:8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); margin-top: 10px;'><br/><br/>");
                }

                if (mapUrl != null && !mapUrl.isEmpty()) {
                    htmlBuilder.append("<a href='").append(mapUrl).append("' style='background: #2563eb; color: white; padding: 10px 16px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;'>📍 View on Google Maps</a>");
                }
                htmlBuilder.append("</div>");
            }

            // Feedback & 5-Star Rating Section with Web Portal Link
            htmlBuilder.append("<div style='background: #f8fafc; padding: 20px; border-radius: 8px; text-align: center; margin-top: 30px; border: 1px solid #cbd5e1;'>")
                       .append("<h3 style='color: #0f172a; margin-top:0;'>⭐ Rate Your Experience & Feedback</h3>")
                       .append("<p style='font-size: 14px; color: #475569;'>Please provide your valuable feedback, food orders, and star rating regarding our service:</p>")
                       .append("<div style='font-size: 28px; letter-spacing: 6px; margin: 10px 0;'>")
                       .append("<span style='color: #eab308;'>★</span>")
                       .append("<span style='color: #eab308;'>★</span>")
                       .append("<span style='color: #eab308;'>★</span>")
                       .append("<span style='color: #eab308;'>★</span>")
                       .append("<span style='color: #eab308;'>★</span>")
                       .append("</div>")
                       .append("<p style='font-size: 13px; color: #64748b; margin-bottom: 20px;'>Click the button below to submit your feedback and orders via our website:</p>")
                       .append("<a href='https://dk-hotel.page.gd/' style='background: linear-gradient(to right, #2563eb, #1d4ed8); color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 14px; display: inline-block; box-shadow: 0 4px 6px rgba(37, 99, 235, 0.3);'>🌐 Open Feedback & Order Portal</a>")
                       .append("</div>");

            htmlBuilder.append("<p style='margin-top: 30px; text-align: center; color: #64748b; font-size: 14px;'>Thank you for choosing our hotel!<br/><b>DK Tourism Hotel Management Team</b></p>")
                       .append("</div>");

            MimeBodyPart htmlBodyPart = new MimeBodyPart();
            htmlBodyPart.setContent(htmlBuilder.toString(), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlBodyPart);

            // Attach Inline Images
            int attachCount = 0;
            for (String[] place : placesList) {
                attachCount++;
                String imgPath = place[3];
                if (imgPath != null && !imgPath.isEmpty() && new File(imgPath).exists()) {
                    MimeBodyPart imagePart = new MimeBodyPart();
                    DataSource fds = new FileDataSource(imgPath);
                    imagePart.setDataHandler(new DataHandler(fds));
                    imagePart.setHeader("Content-ID", "<img_" + attachCount + ">");
                    multipart.addBodyPart(imagePart);
                }
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("HTML Email with attractions, menu & feedback sent successfully to " + recipientEmail);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to send HTML email: " + e.getMessage());
        }
        }
    }
}