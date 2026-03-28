import java.sql.*; 

public class PetShelter {
  public static void main(String[] args) {
    String url = "jdbc:db2://winter2026-comp421.cs.mcgill.ca:50000/COMP421";
    String user =  System.getenv("SOCSUSER");
    String password = System.getenv("SOCSPASSWD");

    try {
      Connection conn = DriverManager.getConnection(url, user, password);
      System.out.println("Connected successfully!");
      conn.close();
        
    } catch (Exception e) {
      System.out.println("Connection failed: " + e.getMessage());
    }


  }
  
}
