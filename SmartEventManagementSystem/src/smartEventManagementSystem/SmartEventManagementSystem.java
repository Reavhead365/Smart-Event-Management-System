package smartEventManagementSystem;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.collections.*;
import javafx.geometry.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// ----------------------------
// Custom Exceptions
// ----------------------------
class EventFullException extends Exception {
    public EventFullException(String message) {
        super(message);
    }
}

class DuplicateParticipantException extends Exception {
    public DuplicateParticipantException(String message) {
        super(message);
    }
}

class EventNotFoundException extends Exception {
    public EventNotFoundException(String message) {
        super(message);
    }
}

// ----------------------------
// Participant Class
// ----------------------------
class Participant implements Serializable {
    String name, email;
    public Participant(String name, String email) {
        this.name = name;
        this.email = email;
    }
    public String toString() {
        return name + " (" + email + ")";
    }
}

// ----------------------------
// Event Class
// ----------------------------
class Event implements Serializable {
    String name, organizer;
    LocalDate date;
    int capacity;
    List<Participant> participants = new ArrayList<>();

    public Event(String name, LocalDate date, int capacity, String organizer) {
        this.name = name;
        this.date = date;
        this.capacity = capacity;
        this.organizer = organizer;
    }

    public void addParticipant(Participant p) throws EventFullException, DuplicateParticipantException {
        if (participants.contains(p)) throw new DuplicateParticipantException("Participant already registered.");
        if (participants.size() >= capacity) throw new EventFullException("Event is full.");
        participants.add(p);
    }

    public String toString() {
        return name + " - " + date + " - " + participants.size() + "/" + capacity;
    }
}

// ----------------------------
// Main JavaFX Application
// ----------------------------
public class SmartEventManagementSystem extends Application {
    private final ObservableList<Event> eventList = FXCollections.observableArrayList();
    private final String DATA_FILE = "events.dat";

    @Override
    public void start(Stage primaryStage) {
        // Load data
        loadData();

        // Event form
        TextField nameField = new TextField();
        DatePicker datePicker = new DatePicker();
        TextField capacityField = new TextField();
        TextField organizerField = new TextField();
        Button addButton = new Button("Add Event");

        addButton.setOnAction(e -> {
            try {
                String name = nameField.getText();
                LocalDate date = datePicker.getValue();
                int capacity = Integer.parseInt(capacityField.getText());
                String organizer = organizerField.getText();
                Event ev = new Event(name, date, capacity, organizer);
                eventList.add(ev);
                saveData();
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        VBox eventForm = new VBox(10, new Label("Event Name"), nameField,
                new Label("Date"), datePicker,
                new Label("Capacity"), capacityField,
                new Label("Organizer"), organizerField,
                addButton);

        // Event List and Filter
        ListView<Event> eventListView = new ListView<>(eventList);
        DatePicker filterDate = new DatePicker();
        filterDate.setOnAction(e -> {
            LocalDate selectedDate = filterDate.getValue();
            List<Event> filtered = eventList.stream().filter(ev -> ev.date.equals(selectedDate)).collect(Collectors.toList());
            eventListView.setItems(FXCollections.observableArrayList(filtered));
        });

        // Participant registration
        TextField partNameField = new TextField();
        TextField partEmailField = new TextField();
        Button regButton = new Button("Register Participant");

        regButton.setOnAction(e -> {
            try {
                Event selected = eventListView.getSelectionModel().getSelectedItem();
                if (selected == null) throw new EventNotFoundException("No event selected.");
                Participant p = new Participant(partNameField.getText(), partEmailField.getText());
                selected.addParticipant(p);
                saveData();
                showAlert("Success", "Participant added.");
            } catch (Exception ex) {
                showAlert("Error", ex.getMessage());
            }
        });

        VBox regForm = new VBox(10, new Label("Name"), partNameField,
                new Label("Email"), partEmailField, regButton);

        HBox root = new HBox(20, eventForm, new VBox(10, new Label("Events"), filterDate, eventListView), regForm);
        root.setPadding(new Insets(10));

        primaryStage.setScene(new Scene(root, 800, 400));
        primaryStage.setTitle("Smart Event Management System");
        primaryStage.show();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void saveData() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(new ArrayList<>(eventList));
        } catch (IOException e) {
            showAlert("Save Error", "Failed to save data.");
        }
    }

    private void loadData() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
            List<Event> loaded = (List<Event>) in.readObject();
            eventList.setAll(loaded);
        } catch (Exception e) {
            System.out.println("No previous data found or data is corrupted.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
