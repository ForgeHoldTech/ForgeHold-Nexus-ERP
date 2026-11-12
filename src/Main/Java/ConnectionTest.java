import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

public class ConnectionTest {

    public static void main(String[] args) {

        try (Connection con = DBConnection.getConnection()) {

            System.out.println("SQLite connection successful!");

            try (Statement st = con.createStatement()) {
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Products (
                        ProductID INTEGER PRIMARY KEY AUTOINCREMENT,
                        ProductName TEXT NOT NULL,
                        PurchasePrice REAL NOT NULL,
                        SalePrice REAL NOT NULL,
                        Quantity INTEGER NOT NULL DEFAULT 0,
                        ReorderLevel INTEGER NOT NULL DEFAULT 5
                    )
                    """);
            }

            try (
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(
                    "SELECT COUNT(*) AS TotalProducts FROM Products"
                )
            ) {
                if (rs.next()) {
                    System.out.println(
                        "Products table ready. Total rows: "
                        + rs.getInt("TotalProducts")
                    );
                }
            }

        } catch (Exception e) {
            System.out.println("Connection or table error!");
            System.out.println(e.getMessage());
        }
    }
}
