package Hospitality_and_tourism_system;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class Tourism_ManagementController implements Initializable {

    @FXML private TextField txtPlaceName, txtMapUrl;
    @FXML private TextArea txtDescription;
    @FXML private Label lblImagePath;
    @FXML private ImageView imgPreview;

    @FXML private TableView<TourismPlace> tblPlaces;
    @FXML private TableColumn<TourismPlace, Integer> colId;
    @FXML private TableColumn<TourismPlace, String> colName, colDescription, colMap;

    private String selectedImagePath = "";
    private int selectedPlaceId = -1;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hospitality_db?allowPublicKeyRetrieval=true&useSSL=false";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "deneth";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colId.setCellValueFactory(new PropertyValueFactory<>("placeId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("placeName"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colMap.setCellValueFactory(new PropertyValueFactory<>("mapUrl"));

        loadPlaces();

        tblPlaces.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedPlaceId = newSel.getPlaceId();
                txtPlaceName.setText(newSel.getPlaceName());
                txtDescription.setText(newSel.getDescription());
                txtMapUrl.setText(newSel.getMapUrl());
                selectedImagePath = newSel.getImagePath();
                lblImagePath.setText(selectedImagePath);

                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    File file = new File(selectedImagePath);
                    if (file.exists()) {
                        imgPreview.setImage(new Image(file.toURI().toString()));
                    }
                }
            }
        });
    }

    private void loadPlaces() {
        ObservableList<TourismPlace> list = FXCollections.observableArrayList();
        String sql = "SELECT * FROM tourism_places";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new TourismPlace(
                        rs.getInt("place_id"),
                        rs.getString("place_name"),
                        rs.getString("description"),
                        rs.getString("map_url"),
                        rs.getString("image_path")
                ));
            }
            tblPlaces.setItems(list);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            lblImagePath.setText(file.getName());
            imgPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave() {
        String sql = "INSERT INTO tourism_places (place_name, description, map_url, image_path) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, txtPlaceName.getText());
            pst.setString(2, txtDescription.getText());
            pst.setString(3, txtMapUrl.getText());
            pst.setString(4, selectedImagePath);
            pst.executeUpdate();

            loadPlaces();
            handleClear();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedPlaceId == -1) return;
        String sql = "UPDATE tourism_places SET place_name=?, description=?, map_url=?, image_path=? WHERE place_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, txtPlaceName.getText());
            pst.setString(2, txtDescription.getText());
            pst.setString(3, txtMapUrl.getText());
            pst.setString(4, selectedImagePath);
            pst.setInt(5, selectedPlaceId);
            pst.executeUpdate();

            loadPlaces();
            handleClear();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedPlaceId == -1) return;
        String sql = "DELETE FROM tourism_places WHERE place_id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, selectedPlaceId);
            pst.executeUpdate();

            loadPlaces();
            handleClear();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClear() {
        selectedPlaceId = -1;
        txtPlaceName.clear();
        txtDescription.clear();
        txtMapUrl.clear();
        lblImagePath.setText("No image selected");
        selectedImagePath = "";
        imgPreview.setImage(null);
    }
}