package Hospitality_and_tourism_system;

import javafx.beans.property.*;

public class CustomerHistory {
    private final IntegerProperty historyId;
    private final StringProperty customerNic;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty roomType;
    private final StringProperty roomNumber;
    private final StringProperty inDate;
    private final StringProperty outDate;
    private final DoubleProperty totalAmount;
    private final StringProperty paymentMethod;

    public CustomerHistory(int historyId, String customerNic, String firstName, String lastName, 
                           String roomType, String roomNumber, String inDate, String outDate, 
                           double totalAmount, String paymentMethod) {
        this.historyId = new SimpleIntegerProperty(historyId);
        this.customerNic = new SimpleStringProperty(customerNic);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.roomType = new SimpleStringProperty(roomType);
        this.roomNumber = new SimpleStringProperty(roomNumber);
        this.inDate = new SimpleStringProperty(inDate);
        this.outDate = new SimpleStringProperty(outDate);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.paymentMethod = new SimpleStringProperty(paymentMethod);
    }

    // Getters and Property methods for JavaFX TableView
    public int getHistoryId() { return historyId.get(); }
    public IntegerProperty historyIdProperty() { return historyId; }

    public String getCustomerNic() { return customerNic.get(); }
    public StringProperty customerNicProperty() { return customerNic; }

    public String getFirstName() { return firstName.get(); }
    public StringProperty firstNameProperty() { return firstName; }

    public String getLastName() { return lastName.get(); }
    public StringProperty lastNameProperty() { return lastName; }

    public String getRoomType() { return roomType.get(); }
    public StringProperty roomTypeProperty() { return roomType; }

    public String getRoomNumber() { return roomNumber.get(); }
    public StringProperty roomNumberProperty() { return roomNumber; }

    public String getInDate() { return inDate.get(); }
    public StringProperty inDateProperty() { return inDate; }

    public String getOutDate() { return outDate.get(); }
    public StringProperty outDateProperty() { return outDate; }

    public double getTotalAmount() { return totalAmount.get(); }
    public DoubleProperty totalAmountProperty() { return totalAmount; }

    public String getPaymentMethod() { return paymentMethod.get(); }
    public StringProperty paymentMethodProperty() { return paymentMethod; }
}