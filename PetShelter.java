import java.sql.*;
import java.util.Scanner;

public class PetShelter {

static Connection conn;
static Scanner scanner = new Scanner(System.in);

public static void main(String[] args) {
  
  String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/COMP421";
  String user = System.getenv("SOCSUSER");
  String password = System.getenv("SOCSPASSWD");

  try {
      conn = DriverManager.getConnection(url, user, password);
      System.out.println("Connected successfully");

      mainMenu();
      conn.close();

  } catch (Exception e) {
      System.out.println("Connection failed: " + e.getMessage());
  }
}

private static void mainMenu() {
  while (true) {
    System.out.println("\n========== Pet Shelter Main Menu ==========");
    System.out.println("1. View Available Animals");
    System.out.println("2. Book a Visit");
    System.out.println("3. Submit an Adoption Application");
    System.out.println("4. Admin: Review an application");
    System.out.println("5. Show donor summary");
    System.out.println("6. View all bookings");
    System.out.println("7. Quit");
    System.out.print("Please select an option: ");

    String input = scanner.nextLine().trim();

    switch (input) {
      case "1": viewAvailableAnimals(); break;
      case "2": bookVisit(); break;
      case "3": submitAdoptionApplication(); break;
      case "4": reviewApplication(); break;
      case "5": showDonorSummary(); break;
      case "6": viewAllBookings(); break;
      case "7":
          System.out.println("Have a good day!");
          return;
      default:
          System.out.println("Please select the correct option.");
      }
  }
}

//option 1
private static void viewAvailableAnimals() {
  try {
    // fetch species of available animals from DB
    String speciesQuery = "SELECT DISTINCT species FROM Animal WHERE status = 'Available' ORDER BY species";
    Statement speciesStatement = conn.createStatement();
    ResultSet speciesResult = speciesStatement.executeQuery(speciesQuery);

    System.out.println("\n--- Filter by Species ---");
    System.out.println("0. All species");

    int i = 1;
    java.util.List<String> speciesList = new java.util.ArrayList<>();
    while (speciesResult.next()) {
      String s = speciesResult.getString("species");
      speciesList.add(s);
      System.out.println(i + ". " + s);
      i++;
    }

    speciesResult.close();
    speciesStatement.close();

    System.out.print("Select an option: ");
    String choice = scanner.nextLine().trim();

    //based on the user coice, we will query the database for that specific specie(s). 
    String animalQuery;
    PreparedStatement animalStatement;

    int choiceInt = -1;
    try { 
      choiceInt = Integer.parseInt(choice); 
    } catch (NumberFormatException e) {
      System.out.println("Invalid input. Returning to main menu.");
      return;
    }

    if (choiceInt == 0) {
      // Show all available animals
      animalQuery = "SELECT animal_id, name, species, breed, date_of_birth, size " +
                    "FROM Animal WHERE status = 'Available' ORDER BY species, name";
      animalStatement = conn.prepareStatement(animalQuery);
    } else if (choiceInt >= 1 && choiceInt <= 2) {
      // Filter by chosen species after validating input
      animalQuery = "SELECT animal_id, name, species, breed, date_of_birth, size " +
                    "FROM Animal WHERE status = 'Available' AND species = ? ORDER BY name";
      animalStatement = conn.prepareStatement(animalQuery);
      animalStatement.setString(1, speciesList.get(choiceInt - 1));
    } else {
      System.out.println("Invalid option. Returning to main menu.");
      return;
    }

    // execute query and display the results based on the animal choice
    ResultSet animals = animalStatement.executeQuery();

    System.out.println("\nAvailable Animals:");
    System.out.println("ID  | Name       | Species | Breed          | Date of Birth | Size");
    System.out.println("------------------------------------------------------------------------");

    boolean found = false;
    while (animals.next()) {
      // Return formatted answer
      found = true;
      System.out.printf("%-4d| %-11s| %-8s| %-15s| %-14s| %s%n",
        animals.getInt("animal_id"),
        animals.getString("name"),
        animals.getString("species"),
        animals.getString("breed"),
        animals.getDate("date_of_birth"),
        animals.getString("size")
      );
    }

    if (!found) {
      System.out.println("No available animals found for this selection.");
    }

    animals.close();
    animalStatement.close();

    // return to main menu after displaying results

  } catch (Exception e) {
    System.out.println("Error retrieving animals. Please try again: " + e.getMessage());
  }
}
 
//option 2
private static void bookVisit() {
  try {
    // first show available animals pulled from the database
    String animalQuery = "SELECT animal_id, name, species FROM Animal WHERE status = 'Available' ORDER BY animal_id";
    Statement animalStatement = conn.createStatement();
    ResultSet animalResult = animalStatement.executeQuery(animalQuery);

    System.out.println("\n--- Available Animals for a Visit ---");
    System.out.println("ID  | Name       | Species");
    System.out.println("------------------------------");

    java.util.List<Integer> animalIds = new java.util.ArrayList<>();
    boolean anyAvailable = false;

    while (animalResult.next()) {
      anyAvailable = true;
      int id = animalResult.getInt("animal_id");

      // store animal id for later validation when user selects an animal to visit
      animalIds.add(id);

      // print out all available animals with formatting
      System.out.printf("%-4d| %-11s| %s%n",
        id,
        animalResult.getString("name"),
        animalResult.getString("species")
      );
    }

    animalResult.close();
    animalStatement.close();

    if (!anyAvailable) {
      System.out.println("No animals are currently available for a visit.");
      return;
    }

    // Prompt user for email and validate that it exists in the User table before proceeding with booking
    System.out.print("\nEnter your email: ");
    String userEmail = scanner.nextLine().trim();

    // Validate that the email exists in the User table
    PreparedStatement userCheckStatement = conn.prepareStatement(
      "SELECT email FROM Users WHERE email = ?");
    userCheckStatement.setString(1, userEmail);
    ResultSet userCheckResult = userCheckStatement.executeQuery();


    if (!userCheckResult.next()) {
      userCheckResult.close();
      userCheckStatement.close();

      // Attempt to create a new user account with the provided email. If creation fails, return to main menu.
      if (!createNewUser(userEmail)) return;
    } else {

      // if user exists, just close the result and continue with booking process
      userCheckResult.close();
      userCheckStatement.close();
    }

    // Prompt user to select an animal ID from the available list and validate input
    System.out.print("Enter animal ID to visit: ");
    int animalId;
    try {
      animalId = Integer.parseInt(scanner.nextLine().trim());
    } catch (NumberFormatException e) {
      System.out.println("Invalid animal ID.");
      return;
    }

    // Validate input animal ID is in the list of available animal IDs retrieved from the database. If not, return to main menu.
    if (!animalIds.contains(animalId)) {
      System.out.println("That animal ID is not in the available list.");
      return;
    }

    // Prompt user for visit start and end times, and validate as usual 
    System.out.print("Enter visit start time (YYYY-MM-DD HH:MM:SS): ");
    String startTimeStr = scanner.nextLine().trim();

    System.out.print("Enter visit end time   (YYYY-MM-DD HH:MM:SS): ");
    String endTimeStr = scanner.nextLine().trim();

    Timestamp startTime = Timestamp.valueOf(startTimeStr);
    Timestamp endTime   = Timestamp.valueOf(endTimeStr);

    // Check to make sure the start time is before the end time.
    if (!endTime.after(startTime)) {
      System.out.println("End time must be after start time.");
      return;
    }

    // Generate next booking_id - we get the max current id and then add one
    Statement idStatement = conn.createStatement();
    // get max booking id from db 
    ResultSet idResult = idStatement.executeQuery("SELECT MAX(booking_id) AS max_id FROM VisitBooking");
    int newBookingId = 1;
    if (idResult.next() && idResult.getObject("max_id") != null) {
      newBookingId = idResult.getInt("max_id") + 1;
    }
    idResult.close();
    idStatement.close();

    // Insert new booking (status = 'Pending', is_completed = 0)
    String insertQuery = "INSERT INTO VisitBooking (booking_id, status, is_completed, start_time, end_time, email, animal_id) " +
                         "VALUES (?, 'Pending', 'No', ?, ?, ?, ?)";
    PreparedStatement insertStatement = conn.prepareStatement(insertQuery);

    // Insert all details
    insertStatement.setInt(1, newBookingId);
    insertStatement.setTimestamp(2, startTime);
    insertStatement.setTimestamp(3, endTime);
    insertStatement.setString(4, userEmail);
    insertStatement.setInt(5, animalId);
    insertStatement.executeUpdate();
    insertStatement.close();

    // Confirm with the user that the booking was successful 
    System.out.println("Visit booked successfully! Your booking ID is: " + newBookingId);

  } catch (IllegalArgumentException e) {
    System.out.println("Invalid date/time format. Please use YYYY-MM-DD HH:MM:SS.");
  } catch (Exception e) {
    System.out.println("Error booking visit. Please try again: " + e.getMessage());
  }
}

// option 3 
private static void submitAdoptionApplication() {
  try {
    // prompt user for email and validate that it exists in the User table before proceeding with application submission
    System.out.print("Enter your email: ");
    String userEmail = scanner.nextLine().trim();

    // Verify the user exists
    String userCheckQuery = "SELECT email FROM Users WHERE email = ?";
    PreparedStatement userCheckStatement = conn.prepareStatement(userCheckQuery);
    userCheckStatement.setString(1, userEmail);
    ResultSet userResult = userCheckStatement.executeQuery();

    if (!userResult.next()) {
      userResult.close();
      userCheckStatement.close();
      // attempt to create a new user account 
      if (!createNewUser(userEmail)) return;
    } else {
      userResult.close();
      userCheckStatement.close();
    }

    // Show available animals as a sub-menu
    String animalQuery = "SELECT animal_id, name, species, breed FROM Animal WHERE status = 'Available' ORDER BY animal_id";
    Statement animalStatement = conn.createStatement();
    ResultSet animalResult = animalStatement.executeQuery(animalQuery);

    System.out.println("\n--- Animals Available for Adoption ---");
    System.out.println("ID  | Name       | Species | Breed");
    System.out.println("----------------------------------------------");

    java.util.List<Integer> animalIds = new java.util.ArrayList<>();
    boolean anyAvailable = false;

    // print all available animals 
    while (animalResult.next()) {
      anyAvailable = true;
      int id = animalResult.getInt("animal_id");
      animalIds.add(id);
      System.out.printf("%-4d| %-11s| %-8s| %s%n",
        id,
        animalResult.getString("name"),
        animalResult.getString("species"),
        animalResult.getString("breed")
      );
    }

    animalResult.close();
    animalStatement.close();

    if (!anyAvailable) {
      System.out.println("No animals are currently available for adoption.");
      return;
    }

    // continue with application process
    System.out.print("\nEnter the animal ID you wish to adopt: ");
    // parse int
    int animalId;
    try {
      animalId = Integer.parseInt(scanner.nextLine().trim());
    } catch (NumberFormatException e) {
      System.out.println("Invalid animal ID.");
      return;
    }

    // verify animal exists
    if (!animalIds.contains(animalId)) {
      System.out.println("That animal ID is not in the available list.");
      return;
    }

    //cehck for an existing applications
    String existingApplication = "SELECT app_id FROM AdoptionApplication " +
                           "WHERE email = ? AND animal_id = ? AND status NOT IN ('Rejected')";
    PreparedStatement existingAppStatement = conn.prepareStatement(existingApplication);
    existingAppStatement.setString(1, userEmail);
    existingAppStatement.setInt(2, animalId);
    ResultSet duplicateResult = existingAppStatement.executeQuery();

    if (duplicateResult.next()) {
      System.out.println("You already have an active application for this animal (App ID: " +
                          duplicateResult.getInt("app_id") + ").");
      duplicateResult.close();
      existingAppStatement.close();
      return;
    }
    duplicateResult.close();
    existingAppStatement.close();

    // Generate next by taking max and adding 1
    Statement idStatement = conn.createStatement();
    ResultSet idResult = idStatement.executeQuery("SELECT MAX(app_id) AS max_id FROM AdoptionApplication");
    int newAppId = 1;
    if (idResult.next() && idResult.getObject("max_id") != null) {
      newAppId = idResult.getInt("max_id") + 1;
    }
    idResult.close();
    idStatement.close();

    // Insert the new application with today's date and status 'Submitted'
    String insertQuery = "INSERT INTO AdoptionApplication (app_id, submission_date, status, email, animal_id) " +
                         "VALUES (?, CURRENT DATE, 'Submitted', ?, ?)";
    PreparedStatement insertStatement = conn.prepareStatement(insertQuery);
    insertStatement.setInt(1, newAppId);
    insertStatement.setString(2, userEmail);
    insertStatement.setInt(3, animalId);
    insertStatement.executeUpdate();
    insertStatement.close();

    System.out.println("Adoption application submitted successfully! Your application ID is: " + newAppId);

  } catch (Exception e) {
    System.out.println("Error submitting adoption application. Please try again: " + e.getMessage());
  }
}

//option 4
private static void reviewApplication() {
  try {
    
    // Select all adoption applications
    String query = "SELECT app_id, submission_date, status, email, animal_id " +
    "FROM AdoptionApplication ORDER BY app_id";

    // to run SQl
    Statement statement = conn.createStatement();

    // to store the results of query execution
    ResultSet queryOutput = statement.executeQuery(query);

    System.out.println("\nApplications:");
    System.out.println("App ID | Submission Date | Status | User Email | Animal ID ");
    System.out.println("----------------------------------------------------------");
    
    boolean applicationFound = false;

    // loop through each row in the output 
    while(queryOutput.next()) {
      applicationFound = true;

      System.out.println(
        queryOutput.getInt("app_id") + " | " +
        queryOutput.getDate("submission_date") + " | " +
        queryOutput.getString("status") + " | " +
        queryOutput.getString("email") + " | " +
        queryOutput.getInt("animal_id")
    );
  }

    // if no applications found in system
    if (!applicationFound) {
      System.out.println("No applications found.");
      return;
    }

    //Getting insertion data for ReviewsApplication
    System.out.print("\nEnter application ID to review: ");
    int appId = Integer.parseInt(scanner.nextLine());

    System.out.print("Enter admin email: ");
    String adminEmail = scanner.nextLine();

    System.out.print("Enter review date (YYYY-MM-DD): ");
    String reviewDate = scanner.nextLine();

    System.out.print("Enter new status (Submitted / Under Review / Approved / Rejected): ");
    String updatedStatus = scanner.nextLine();

    String insertionQuery = "INSERT INTO ReviewsApplication (app_id, email, review_date) VALUES (?, ?, ?)";

    //Modification: Animal Status
    String updateQuery = "UPDATE AdoptionApplication SET status = ? WHERE app_id = ?";

    //Placeholders replacement

    PreparedStatement p1 = conn.prepareStatement(insertionQuery);
    PreparedStatement p2 = conn.prepareStatement(updateQuery);

    //Insert data into ReviewsApplication
    p1.setInt(1, appId);
    p1.setString(2, adminEmail);
    p1.setDate(3, Date.valueOf(reviewDate));

    p1.executeUpdate();

    //Update status in adoption applications
    p2.setString(1, updatedStatus);
    p2.setInt(2, appId);

    p2.executeUpdate();

    System.out.println("Application has been reviewed and status has been updated successfully.");

    queryOutput.close();
    statement.close();
    p1.close();
    p2.close();

  } catch (Exception e) {
    System.out.println("Error in database system. Please try again.: " + e.getMessage());
  }
}

//option 5
private static void showDonorSummary() {
  try {
    // Query to get the total donation amount per person
    String query = "SELECT P.full_name, P.email, SUM(D.amount) as total_donated " +
    "FROM Person P " +
    "JOIN Donations D ON P.email = D.email " +
    "GROUP BY P.full_name, P.email " +
    "ORDER BY total_donated DESC" ;

    Statement statement = conn.createStatement();
    ResultSet queryOutput = statement.executeQuery(query); 

    System.out.println("\nDonor Summary:");
    System.out.println("Name | Email | Total Donated");
    System.out.println("--------------------------------");
    
    boolean donationsFound = false;

    // loop through each row of the output from query exection
    while(queryOutput.next()) {

      donationsFound = true;

      System.out.println(
        queryOutput.getString("full_name") + " | " +
        queryOutput.getString("email") + " | " +
        queryOutput.getDouble("total_donated")
      );

      // if no donations found
  
    }

    if(!donationsFound) {
      System.out.println("No donations found");
    }

    queryOutput.close();
    statement.close();

  } catch (Exception e) {
    System.out.println("Error retrieving donor summary. Please try again.: " + e.getMessage());

  }
}

// option 6
private static void viewAllBookings() {
  try {
    String query = "SELECT booking_id, status, is_completed, start_time, end_time, email, animal_id " +
                   "FROM VisitBooking ORDER BY booking_id";
    Statement statement = conn.createStatement();
    ResultSet rs = statement.executeQuery(query);

    System.out.println("\nAll Visit Bookings:");
    System.out.println("ID  | Status     | Completed | Start Time          | End Time            | Email                | Animal ID");
    System.out.println("---------------------------------------------------------------------------------------------------------------");

    boolean found = false;
    while (rs.next()) {
      found = true;
      String start = rs.getTimestamp("start_time").toString().substring(0, 19);
      String end   = rs.getTimestamp("end_time").toString().substring(0, 19);
      System.out.printf("%-4d| %-11s| %-10s| %-20s| %-20s| %-21s| %d%n",
        rs.getInt("booking_id"),
        rs.getString("status"),
        rs.getString("is_completed"),
        start,
        end,
        rs.getString("email"),
        rs.getInt("animal_id")
      );
    }

    if (!found) {
      System.out.println("No bookings found.");
    }

    rs.close();
    statement.close();

  } catch (Exception e) {
    System.out.println("Error retrieving bookings: " + e.getMessage());
  }
}

// create a new person and user
private static boolean createNewUser(String email) {
  try {
    System.out.println("No account found for " + email);

    System.out.print("Full name: ");
    String fullName = scanner.nextLine().trim();

    System.out.print("Address: ");
    String address = scanner.nextLine().trim();

    System.out.print("Phone number: ");
    String phone = scanner.nextLine().trim();

    System.out.print("Password: ");
    String password = scanner.nextLine().trim();

    System.out.print("Do you have pets? (yes/no): ");
    int hasPets = scanner.nextLine().trim().equalsIgnoreCase("yes") ? 1 : 0;

    System.out.print("Years of pet experience: ");
    int yearsExp = 0;
    try { yearsExp = Integer.parseInt(scanner.nextLine().trim()); } catch (NumberFormatException e) {}

    PreparedStatement personStatement = conn.prepareStatement(
      "INSERT INTO Person (email, full_name, address, phone_number, password_hash) VALUES (?, ?, ?, ?, ?)");
    personStatement.setString(1, email);
    personStatement.setString(2, fullName);
    personStatement.setString(3, address);
    personStatement.setString(4, phone);
    personStatement.setString(5, password);
    personStatement.executeUpdate();
    personStatement.close();

    PreparedStatement userStatement = conn.prepareStatement(
      "INSERT INTO Users (email, has_pets, years_of_pet_experience) VALUES (?, ?, ?)");
    userStatement.setString(1, email);
    userStatement.setInt(2, hasPets);
    userStatement.setInt(3, yearsExp);
    userStatement.executeUpdate();
    userStatement.close();

    System.out.println("Account created successfully!");
    return true;

  } catch (Exception e) {
    System.out.println("Failed to create account: " + e.getMessage());
    return false;
  }
}

}