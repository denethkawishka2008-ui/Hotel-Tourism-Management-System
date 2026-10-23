package Hospitality_and_tourism_system;

import javafx.beans.property.*;

public class Employee {
    private final IntegerProperty taskId;
    private final StringProperty roomNumber;
    private final StringProperty roomStatus;
    private final StringProperty startTime;
    private final StringProperty endTime;
    private final StringProperty taskDate;
    private final StringProperty workStatus;

    public Employee(int taskId, String roomNumber, String roomStatus, String startTime, String endTime, String taskDate, String workStatus) {
        this.taskId = new SimpleIntegerProperty(taskId);
        this.roomNumber = new SimpleStringProperty(roomNumber);
        this.roomStatus = new SimpleStringProperty(roomStatus);
        this.startTime = new SimpleStringProperty(startTime);
        this.endTime = new SimpleStringProperty(endTime);
        this.taskDate = new SimpleStringProperty(taskDate);
        this.workStatus = new SimpleStringProperty(workStatus);
    }

    public int getTaskId() { return taskId.get(); }
    public IntegerProperty taskIdProperty() { return taskId; }

    public String getRoomNumber() { return roomNumber.get(); }
    public StringProperty roomNumberProperty() { return roomNumber; }

    public String getRoomStatus() { return roomStatus.get(); }
    public StringProperty roomStatusProperty() { return roomStatus; }

    public String getStartTime() { return startTime.get(); }
    public StringProperty startTimeProperty() { return startTime; }

    public String getEndTime() { return endTime.get(); }
    public StringProperty endTimeProperty() { return endTime; }

    public String getTaskDate() { return taskDate.get(); }
    public StringProperty taskDateProperty() { return taskDate; }

    public String getWorkStatus() { return workStatus.get(); }
    public StringProperty workStatusProperty() { return workStatus; }
}