package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.Arrays;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;
    private static int appID = 0;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE pUsername = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // format is yyyy-mm-dd
        // Still need check date

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            if (currentCaregiver == null && currentPatient == null) {
                System.out.println("Please login first!");
            } else if (currentPatient != null || currentCaregiver != null) {
                System.out.println("Caregiver Name     Vaccines Name     Doses   ");
                String show = "SELECT Username, Name, Doses FROM Availabilities, Vaccines WHERE Time = ? ORDER BY Username";
                try {
                    Date d = Date.valueOf(date);
                    PreparedStatement statement = con.prepareStatement(show);
                    statement.setDate(1, d);
                    ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String cUsername = resultSet.getString("Username");
                        String vName = resultSet.getString("Name");
                        int doses = resultSet.getInt("Doses");

                        System.out.println(cUsername + " " + vName + " " + doses);
                    }
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                } finally {
                    cm.closeConnection();
                }

            } else {
                System.out.println("Please try again!");
            }
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd format.");
        }
    }

    private static void reserve(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];
        Date d = Date.valueOf(date);
        String vaccine = tokens[2];
        String cUsername = "";
        int doses = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            if (currentCaregiver == null && currentPatient == null) {
                System.out.println("Please login first!");
            } else if (currentCaregiver != null) {
                System.out.println("Please login as a patient!");
            } else if (currentPatient != null) {
                // get Availabilities
                String show = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
                try {
                    PreparedStatement statement = con.prepareStatement(show);
                    statement.setDate(1, d);
                    ResultSet resultSet = statement.executeQuery();
                    if (!resultSet.next()) {
                        System.out.println("No Caregiver is available!");
                    } else {
                        cUsername = resultSet.getString("Username");
                    }
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }

                // get Vaccines
                show = "SELECT Doses FROM Vaccines WHERE Name = ?";
                try {
                    PreparedStatement statement = con.prepareStatement(show);
                    statement.setString(1, vaccine);
                    ResultSet resultSet = statement.executeQuery();
                    if (!resultSet.next()) {
                        System.out.println("No such vaccine!");
                    } else {
                        doses = resultSet.getInt("Doses");
                        if (doses == 0) {
                            System.out.println("Not enough available doses!");
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("Please try again!");
                    e.printStackTrace();
                }

                if (!(cUsername.equals("")) && doses != 0) {
                    // Update Availabilities
                    show = "DELETE FROM Availabilities WHERE Time = ? AND Username = ?";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setDate(1, d);
                        statement.setString(2, cUsername);
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Update Vaccine
                    show = "UPDATE Vaccines SET Doses = ? WHERE Name = ?";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setInt(1, doses - 1);
                        statement.setString(2, vaccine);
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // get appointment_ID
                    show = "SELECT * FROM Appointment";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        ResultSet resultSet = statement.executeQuery();
                        while (resultSet.next()) {
                            appID = resultSet.getInt("Appointment_ID");
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Update Appointment (Time, Appointment_ID, Username, pUsername, Name)
                    show = "INSERT INTO Appointment VALUES (? , ?, ?, ?, ?)";
                    appID++;
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setDate(1, d);
                        statement.setInt(2, appID);
                        statement.setString(3, cUsername);
                        statement.setString(4, currentPatient.getUsername());
                        statement.setString(5, vaccine);
                        statement.executeUpdate();

                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        cm.closeConnection();
                    }
                }
                System.out.println("Appointment_ID: " + appID  + " Caregiver username: " + cUsername);
            } else {
                System.out.println("Please try again!");
            }
        } catch(ParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd format.");
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // format is yyyy-mm-dd
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (tokens.length != 2) {
            System.out.println("Please try again!");
        }

        int appID = Integer.valueOf(tokens[1]), validFlag = 0, doses = 0;
        String vaccine = "", cUsername = "", pUsername = "";
        Date time = new Date(0000, 00, 00);

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        }
        else  {
            // check appointment is valid
            String show = "SELECT * FROM Appointment WHERE Appointment_ID = ?";
            try {
                PreparedStatement statement = con.prepareStatement(show);
                statement.setInt(1, appID);
                ResultSet resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    System.out.println("No such appointment!");
                }
                else {
                    time = resultSet.getDate("Time");
                    vaccine = resultSet.getString("Name");
                    cUsername = resultSet.getString("Username");
                    pUsername = resultSet.getString("pUsername");
                    validFlag = 1;
                }
            } catch (SQLException e) {
                System.out.println("Please try again!");
                e.printStackTrace();
            }

            if(validFlag == 1) {
                if((currentCaregiver != null && cUsername.equals(currentCaregiver.getUsername())) || (currentPatient != null && pUsername.equals(currentPatient.getUsername()))) {
                    // delete appointment
                    show = "DELETE FROM Appointment WHERE Appointment_ID = ?";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setInt(1, appID);
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        System.out.println("Please try again!");
                        e.printStackTrace();
                    }

                    // Update Availability
                    show = "INSERT INTO Availabilities VALUES (? ,?)";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setDate(1, time);
                        statement.setString(2, cUsername);
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Update Vaccines
                    // get doses of vaccine first
                    show = "SELECT * FROM Vaccines WHERE Name = ?";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setString(1, vaccine);
                        ResultSet resultSet = statement.executeQuery();
                        if(resultSet.next()) {
                            doses = resultSet.getInt("Doses");
                        }
                        else {
                        }
                    } catch (SQLException e) {
                        System.out.println("Please try again!");
                        e.printStackTrace();
                    }

                    // update doses of vaccines
                    show = "UPDATE Vaccines SET Doses = ? WHERE Name = ?";
                    try {
                        PreparedStatement statement = con.prepareStatement(show);
                        statement.setInt(1, doses + 1);
                        statement.setString(2, vaccine);
                        statement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Cancel successful!");
                }
                else {
                    System.out.println("You're not the Patient or Caregiver of this appointment. Please login the correct user!");
                }
            }

        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }

        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        }
        else if(currentPatient != null) {
            System.out.println("Appointment ID    Vaccine Name    Date    Caregiver Name    ");
            String show = "SELECT Appointment_ID, Name, Time, Username FROM Appointment WHERE pUsername = ? ORDER BY Appointment_ID";
            try {
                PreparedStatement statement = con.prepareStatement(show);
                statement.setString(1, currentPatient.getUsername());
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int appointmentID = resultSet.getInt("Appointment_ID");
                    Date time = resultSet.getDate("Time");
                    String vName = resultSet.getString("Name");
                    String cUsername = resultSet.getString("Username");
                    System.out.println(appointmentID + " " + time + " " + vName + " " + cUsername);
                }
            }
            catch (SQLException e) {
                System.out.println("Show appointment failed.");
                e.printStackTrace();
            }
            finally {
                cm.closeConnection();
            }
        }
        else if(currentCaregiver != null) {
            System.out.println("Appointment ID    Vaccine Name    Date    Patient Name    ");
            String show = "SELECT Appointment_ID, Name, Time, pUsername FROM Appointment WHERE Username = ? ORDER BY Appointment_ID";
            try {
                PreparedStatement statement = con.prepareStatement(show);
                statement.setString(1, currentCaregiver.getUsername());
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    int appointmentID = resultSet.getInt("Appointment_ID");
                    Date time = resultSet.getDate("Time");
                    String vName = resultSet.getString("Name");
                    String pUsername = resultSet.getString("pUsername");
                    System.out.println(appointmentID + " " + time + " " + vName + " " + pUsername);
                }
            }
            catch (SQLException e) {
                System.out.println("Show appointment failed.");
                e.printStackTrace();
            }
            finally {
                cm.closeConnection();
            }
        }
        else {
            System.out.println("Please try again!");
        }
    }

    private static void logout(String[] tokens) {
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        else if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
        }
        else if(currentCaregiver != null) {
            currentCaregiver = null;
            System.out.println("Successfully logged out!");
        }
        else if(currentPatient != null){
            currentPatient = null;
            System.out.println("Successfully logged out!");
        }
        else {
            System.out.println("Please try again!");
        }
    }
}
