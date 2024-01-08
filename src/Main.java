import java.sql.*;
import java.util.Scanner;

public class Main {

	public static Integer menu(Scanner sc) {
        System.out.println("Welcome to LaperAh Restaurant Reservation");
        System.out.println("1. Display Menu");
        System.out.println("2. Make a Reservation");
        System.out.println("3. Place an Order");
        System.out.println("4. Check Out");
        System.out.println("5. Manage Menu");
        System.out.println("6. Exit");
        System.out.println("Enter your choice");
        return sc.nextInt();
    }

    public static Connection init() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/RestoManagement", "root", "");
            if (conn != null) {
                System.out.println("Connection established");
                return conn;
            } else {
                System.out.println("Cannot connect to the database");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        Connection conn = init();
        if (conn != null) {
            Scanner sc = new Scanner(System.in);
            int choice;
            do {
                choice = menu(sc);
                switch (choice) {
                    case 1:
                        showMenu(conn);
                        break;
                    case 2:
                        makeReservation(conn, sc);
                        break;
                    case 3:
                        orderFood(conn, sc);
                        break;
                    case 4:
                        checkOut(conn, sc);
                        break;
                    case 5:
                        manageMenu(conn, sc);
                        break;
                    case 6:
                        System.out.println("Exiting...");
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            } while (choice != 6);

            sc.close();
        }
    }

    private static void showMenu(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Menu");
            System.out.println("Menu:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") +
                        ", Name: " + rs.getString("name") +
                        ", Price: " + rs.getDouble("price"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void makeReservation(Connection conn, Scanner sc) {
        try {
            System.out.println("Enter customer name:");
            String customerName = sc.next();
            System.out.println("Enter number of tables:");
            int numTables = sc.nextInt();
            System.out.println("Enter table type (Romantic/General/Family):");
            String tableType = sc.next();

            if (!tableType.equals("Romantic") && !tableType.equals("General") && !tableType.equals("Family")) {
                System.out.println("Invalid table type");
                return;
            }

            int maxPeople = 0;
            switch (tableType) {
                case "Romantic":
                    maxPeople = 2;
                    break;
                case "General":
                    maxPeople = 4;
                    break;
                case "Family":
                    maxPeople = 10;
                    break;
            }

            if (numTables > maxPeople) {
                System.out.println("Number of people exceeds the maximum limit for " + tableType + " tables");
                return;
            }

            PreparedStatement pst = conn.prepareStatement("INSERT INTO Reservation (customer_name, num_tables, table_type, status) VALUES (?, ?, ?, ?)");
            pst.setString(1, customerName);
            pst.setInt(2, numTables);
            pst.setString(3, tableType);
            pst.setString(4, "in reserve");
            
            System.out.println("SQL Statement: " + pst.toString());
            System.out.println("Is connection closed? " + conn.isClosed());

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Reservation made successfully");
            } else {
                System.out.println("Failed to make reservation");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("SQL Exception Message: " + e.getMessage());
        }
    }

    private static void orderFood(Connection conn, Scanner sc) {
        try {
            System.out.println("Enter reservation ID:");
            int reservationId = sc.nextInt();
            System.out.println("Enter menu ID:");
            int menuId = sc.nextInt();
            System.out.println("Enter quantity:");
            int quantity = sc.nextInt();

            if (!checkReservationExists(conn, reservationId)) {
                System.out.println("Invalid reservation ID");
                return;
            }

            if (!checkMenuExists(conn, menuId)) {
                System.out.println("Invalid menu ID");
                return;
            }

            updateReservationStatus(conn, reservationId, "in order");
            System.out.println("Reservation status after orderFood: " + checkReservationInOrder(conn, reservationId));

            PreparedStatement pst = conn.prepareStatement("INSERT INTO OrderItem (reservation_id, menu_id, quantity) VALUES (?, ?, ?)");
            pst.setInt(1, reservationId);
            pst.setInt(2, menuId);
            pst.setInt(3, quantity);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Order placed successfully");
            } else {
                System.out.println("Failed to place order");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void checkOut(Connection conn, Scanner sc) {
        try {
            System.out.println("Enter reservation ID:");
            int reservationId = sc.nextInt();

            if (!checkReservationExists(conn, reservationId)) {
                System.out.println("Invalid reservation ID");
                return;
            }
            System.out.println("Before update - Reservation status: " + checkReservationInOrder(conn, reservationId));

            System.out.println("Checking reservation status...");
            if (!checkReservationInOrder(conn, reservationId)) {
                System.out.println("Reservation is not in order status");
                return;
            }

            try (PreparedStatement pst = conn.prepareStatement("UPDATE Reservation SET status = 'finalized' WHERE id = ?")) {
                pst.setInt(1, reservationId);

                int rowsAffected = pst.executeUpdate();

                if (rowsAffected > 0) {
                    double totalBill = calculateTotalBill(conn, reservationId);
                    System.out.println("Bill for reservation ID " + reservationId + ": " + totalBill);
                    System.out.println("Total payment: " + totalBill);
                    
                    System.out.println("After update - Reservation status: " + checkReservationInOrder(conn, reservationId));

                } else {
                    System.out.println("Failed to check out");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void manageMenu(Connection conn, Scanner sc) {
        try {
            System.out.println("1. Add Menu");
            System.out.println("2. Update Menu");
            System.out.println("3. Delete Menu");
            int choice = sc.nextInt();

            switch (choice) {
                case 1:
                    System.out.println("Enter menu name:");
                    String menuName = sc.next();
                    System.out.println("Enter menu price:");
                    double menuPrice = sc.nextDouble();

                    PreparedStatement addMenuPst = conn.prepareStatement("INSERT INTO Menu (name, price) VALUES (?, ?)");
                    addMenuPst.setString(1, menuName);
                    addMenuPst.setDouble(2, menuPrice);

                    int addMenuRowsAffected = addMenuPst.executeUpdate();

                    if (addMenuRowsAffected > 0) {
                        System.out.println("Menu added successfully");
                    } else {
                        System.out.println("Failed to add menu");
                    }
                    break;

                case 2:
                    System.out.println("Enter menu ID:");
                    int menuId = sc.nextInt();
                    System.out.println("Enter new menu name:");
                    String newMenuName = sc.next();
                    System.out.println("Enter new menu price:");
                    double newMenuPrice = sc.nextDouble();

                    if (!checkMenuExists(conn, menuId)) {
                        System.out.println("Invalid menu ID");
                        return;
                    }

                    if (!checkMenuCanBeUpdated(conn, menuId)) {
                        System.out.println("Menu cannot be updated as it is already ordered by a reservation");
                        return;
                    }

                    PreparedStatement updateMenuPst = conn.prepareStatement("UPDATE Menu SET name = ?, price = ? WHERE id = ?");
                    updateMenuPst.setString(1, newMenuName);
                    updateMenuPst.setDouble(2, newMenuPrice);
                    updateMenuPst.setInt(3, menuId);

                    int updateMenuRowsAffected = updateMenuPst.executeUpdate();

                    if (updateMenuRowsAffected > 0) {
                        System.out.println("Menu updated successfully");
                    } else {
                        System.out.println("Failed to update menu");
                    }
                    break;

                case 3:
                    System.out.println("Enter menu ID:");
                    int deleteMenuId = sc.nextInt();

                    if (!checkMenuExists(conn, deleteMenuId)) {
                        System.out.println("Invalid menu ID");
                        return;
                    }

                    if (!checkMenuCanBeDeleted(conn, deleteMenuId)) {
                        System.out.println("Menu cannot be deleted as it is already ordered by a reservation");
                        return;
                    }

                    PreparedStatement deleteMenuPst = conn.prepareStatement("DELETE FROM Menu WHERE id = ?");
                    deleteMenuPst.setInt(1, deleteMenuId);

                    int deleteMenuRowsAffected = deleteMenuPst.executeUpdate();

                    if (deleteMenuRowsAffected > 0) {
                        System.out.println("Menu deleted successfully");
                    } else {
                        System.out.println("Failed to delete menu");
                    }
                    break;

                default:
                    System.out.println("Invalid choice");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkReservationExists(Connection conn, int reservationId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT * FROM Reservation WHERE id = ?");
        pst.setInt(1, reservationId);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }

    private static boolean checkMenuExists(Connection conn, int menuId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT * FROM Menu WHERE id = ?");
        pst.setInt(1, menuId);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }

    private static boolean checkReservationInOrder(Connection conn, int reservationId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT * FROM Reservation WHERE id = ? AND status = 'in order'");
        pst.setInt(1, reservationId);
        ResultSet rs = pst.executeQuery();
        return rs.next();
    }
    private static void updateReservationStatus(Connection conn, int reservationId, String status) throws SQLException {
        System.out.println("Updating reservation status. Reservation ID: " + reservationId + ", New status: " + status);
        PreparedStatement pst = conn.prepareStatement("UPDATE Reservation SET status = ? WHERE id = ?");
        pst.setString(1, status);
        pst.setInt(2, reservationId);
        int rowsAffected = pst.executeUpdate();
        System.out.println("Rows affected after updating reservation status: " + rowsAffected);
    }


    private static boolean checkMenuCanBeUpdated(Connection conn, int menuId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT * FROM OrderItem WHERE menu_id = ?");
        pst.setInt(1, menuId);
        ResultSet rs = pst.executeQuery();
        return !rs.next(); 
    }

    private static boolean checkMenuCanBeDeleted(Connection conn, int menuId) throws SQLException {
        return checkMenuCanBeUpdated(conn, menuId);
    }

    private static double calculateTotalBill(Connection conn, int reservationId) throws SQLException {
        PreparedStatement pst = conn.prepareStatement("SELECT SUM(Menu.price * OrderItem.quantity) AS total FROM OrderItem INNER JOIN Menu ON OrderItem.menu_id = Menu.id WHERE OrderItem.reservation_id = ?");
        pst.setInt(1, reservationId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return rs.getDouble("total");
        }
        return 0.0;
    }
}