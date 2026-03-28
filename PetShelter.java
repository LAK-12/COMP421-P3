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
    System.out.println("6. Quit");
    System.out.print("Please select an option: ");

    String input = scanner.nextLine().trim();

    switch (input) {
      case "1": viewAvailableAnimals(); break;
      case "2": bookVisit(); break;
      case "3": submitAdoptionApplication(); break;
      case "4": reviewApplication(); break;
      case "5": showDonorSummary(); break;
      case "6":
          System.out.println("Have a good day!");
          return;
      default:
          System.out.println("Please select the correct option.");
      }
  }
}

//option 1
private static void viewAvailableAnimals() {
  System.out.println("Not implemented yet.");
}
 
//option 2
private static void bookVisit() {
  System.out.println("Not implemented yet.");
}

// option 3 
private static void submitAdoptionApplication() {
  System.out.println("Not implemented yet.");
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



}