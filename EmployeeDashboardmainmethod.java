package Hospitality_and_tourism_system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EmployeeDashboardmainmethod extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
       
            Parent root = FXMLLoader.load(getClass().getResource("EmployeeDashboard.fxml"));
            primaryStage.setTitle("DK-Hotel's and Tourism - Employee & Staff Dashboard");
            primaryStage.setScene(new Scene(root, 1250, 750));
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}