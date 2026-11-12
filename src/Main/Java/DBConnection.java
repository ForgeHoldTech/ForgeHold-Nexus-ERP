import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.nio.file.Path;

public class DBConnection {

    private static final String DB_PATH =
        Path.of("InventoryBillingDB.db")
            .toAbsolutePath()
            .normalize()
            .toString();

    private static final String URL =
        "jdbc:sqlite:" + DB_PATH;

    public static Connection getConnection()
            throws SQLException {

        System.out.println("Database file: " + DB_PATH);

        return DriverManager.getConnection(URL);
    }
}