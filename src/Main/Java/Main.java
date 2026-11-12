import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

public class Main extends JFrame {

    private static String loggedInUsername = "admin";
    private static String loginPassword = "admin123";
    private JLabel dashboardUsernameLabel;
    private static final Path LOGIN_SETTINGS_PATH = Path.of(
        System.getProperty("user.home"), ".inventory-billing-login.properties"
    );
    private static final SecureRandom LOGIN_RANDOM = new SecureRandom();

    private JTable productTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JTable stockTable;
    private DefaultTableModel stockTableModel;
    private JTable historyTable;
    private DefaultTableModel historyTableModel;

    private JTable customerTable;
    private DefaultTableModel customerTableModel;
    private JTextField customerSearchField;

    private JComboBox<CustomerOption> billingCustomerCombo;
    private JComboBox<ProductOption> billingProductCombo;
    private JSpinner billingQuantitySpinner;
    private DefaultTableModel cartTableModel;
    private JTable cartTable;
    private JLabel billTotalLabel;
    private java.util.List<CartItem> cartItems = new java.util.ArrayList<>();

    private JLabel dashboardProductsLabel;
    private JLabel dashboardCustomersLabel;
    private JLabel dashboardSalesLabel;
    private JLabel dashboardLowStockLabel;
    private DefaultTableModel dashboardBillsModel;
    private DefaultTableModel dashboardLowStockModel;

    /** Creates the ForgeHold FH icon shown in the Windows title bar/taskbar. */
    /** Creates the same gold tile, navy diamond and white checkmark used by the login logo. */
    private Image createWindowIcon() {
        int size = 64;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(new Color(215, 184, 117));
        g.fillRoundRect(2, 2, size - 4, size - 4, 12, 12);
        int[] x = {size / 2, size - 10, size / 2, 10};
        int[] y = {8, size / 2, size - 8, size / 2};
        g.setColor(new Color(24, 43, 70));
        g.fillPolygon(x, y, x.length);
        g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(Color.WHITE);
        g.drawLine(20, 32, 28, 41);
        g.drawLine(28, 41, 45, 21);
        g.dispose();
        return icon;
    }

    public Main() {
        applyProfessionalTheme();
        setTitle("ForgeHold Nexus ERP");
        setIconImage(createWindowIcon());
        setSize(1100, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(245, 248, 252));

        JPanel heading = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 12));
        heading.setBackground(new Color(24, 43, 70));
        heading.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        heading.add(new ForgeHoldLogo(52));
        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));
        JLabel brandName = new JLabel("ForgeHold Nexus ERP");
        brandName.setFont(new Font("Segoe UI", Font.BOLD, 25));
        brandName.setForeground(Color.WHITE);
        JLabel brandTagline = new JLabel("INTELLIGENT BUSINESS MANAGEMENT");
        brandTagline.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        brandTagline.setForeground(new Color(215, 194, 145));
        brandText.add(brandName);
        brandText.add(brandTagline);
        heading.add(brandText);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Dashboard", createDashboardPanel(tabs));
        tabs.addTab("Products", createProductsPanel());
        tabs.addTab("Stock", createStockPanel());
        tabs.addTab("Customers", createCustomersPanel());
        tabs.addTab("Billing", createBillingPanel());
        tabs.addTab("Reports", createReportsPanel());
        tabs.addTab("About", createAboutPanel());

        setLayout(new BorderLayout(5, 5));
        add(heading, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        // Create all database tables before any panel performs its initial data load.
        createProductsTable();
        createCustomersTable();
        createBillingTables();

        loadProducts("");
        loadCustomers("");
        loadBillingCustomers();
        loadBillingProducts();
        loadSalesReport(true);
        loadStockProducts(false);
        loadStockHistory();
        refreshDashboard();
        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 0) refreshDashboard();
        });
        styleComponentTree(getContentPane());
    }

    private void applyProfessionalTheme() {
        Color navy = new Color(24, 43, 70);
        Color blue = new Color(43, 96, 150);
        Color background = new Color(245, 248, 252);
        Color foreground = new Color(39, 52, 70);
        UIManager.put("Panel.background", background);
        UIManager.put("OptionPane.background", background);
        UIManager.put("Label.foreground", foreground);
        UIManager.put("Button.background", blue);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("Button.focus", new Color(180, 205, 230));
        UIManager.put("TabbedPane.background", background);
        UIManager.put("TabbedPane.foreground", foreground);
        UIManager.put("TabbedPane.selected", Color.WHITE);
        UIManager.put("TabbedPane.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 12));
        UIManager.put("Table.foreground", foreground);
        UIManager.put("Table.background", Color.WHITE);
        UIManager.put("Table.selectionBackground", new Color(210, 229, 248));
        UIManager.put("Table.selectionForeground", navy);
        UIManager.put("TableHeader.background", navy);
        UIManager.put("TableHeader.foreground", Color.WHITE);
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TextArea.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Spinner.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("ScrollPane.background", Color.WHITE);
    }

    private void styleComponentTree(Component component) {
        if (component instanceof JTable table) {
            table.setRowHeight(Math.max(table.getRowHeight(), 26));
            table.setShowGrid(true);
            table.setGridColor(new Color(225, 231, 239));
            table.setFillsViewportHeight(true);
            table.setSelectionBackground(new Color(210, 229, 248));
            table.setSelectionForeground(new Color(24, 43, 70));
            if (table.getTableHeader() != null) {
                table.getTableHeader().setBackground(new Color(24, 43, 70));
                table.getTableHeader().setForeground(Color.WHITE);
                table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
                table.getTableHeader().setPreferredSize(new Dimension(0, 32));
            }
        } else if (component instanceof JButton button) {
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setBackground(new Color(43, 96, 150));
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(35, 79, 125)),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
            button.setOpaque(true);
        } else if (component instanceof JTextField field) {
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(205, 215, 228)),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        } else if (component instanceof JPanel panel && panel.getBackground().equals(Color.WHITE)) {
            panel.setBackground(Color.WHITE);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleComponentTree(child);
            }
        }
    }

    
private JPanel createStockPanel() {
    JPanel panel = new JPanel(new BorderLayout(8, 8));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    stockTableModel = new DefaultTableModel(
        new String[] {"Product ID", "Product Name", "Quantity", "Reorder Level"},
        0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    stockTable = new JTable(stockTableModel);
    stockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    stockTable.setRowHeight(24);

    historyTableModel = new DefaultTableModel(
        new String[] {
            "Movement ID", "Product ID", "Type", "Quantity",
            "Previous", "New", "Notes", "Date"
        }, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    historyTable = new JTable(historyTableModel);
    historyTable.setRowHeight(22);

    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

    JButton inButton = new JButton("Stock In");
    JButton outButton = new JButton("Stock Out");
    JButton lowButton = new JButton("Low Stock");
    JButton allButton = new JButton("Show All");
    JButton refreshButton = new JButton("Refresh");
    JButton deleteHistoryButton = new JButton("Delete Selected");
    JButton clearHistoryButton = new JButton("Clear History");

    inButton.addActionListener(e -> performStockMovement("IN"));
    outButton.addActionListener(e -> performStockMovement("OUT"));
    lowButton.addActionListener(e -> loadStockProducts(true));
    allButton.addActionListener(e -> loadStockProducts(false));

    refreshButton.addActionListener(e -> {
        loadStockProducts(false);
        loadStockHistory();
    });
    deleteHistoryButton.addActionListener(e -> deleteSelectedHistory());
    clearHistoryButton.addActionListener(e -> clearStockHistory());

    buttons.add(inButton);
    buttons.add(outButton);
    buttons.add(lowButton);
    buttons.add(allButton);
    buttons.add(refreshButton);
    buttons.add(deleteHistoryButton);
    buttons.add(clearHistoryButton);

    JPanel stockPanel = new JPanel(new BorderLayout(5, 5));
    stockPanel.add(new JLabel("Current Stock"), BorderLayout.NORTH);
    stockPanel.add(new JScrollPane(stockTable), BorderLayout.CENTER);

    JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
    historyPanel.add(new JLabel("Recent Stock History"), BorderLayout.NORTH);
    historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

    JSplitPane split = new JSplitPane(
        JSplitPane.VERTICAL_SPLIT, stockPanel, historyPanel
    );
    split.setResizeWeight(0.55);

    panel.add(buttons, BorderLayout.NORTH);
    panel.add(split, BorderLayout.CENTER);

    return panel;
}

private void loadStockProducts(boolean lowStockOnly) {
    if (stockTableModel == null) {
        return;
    }

    stockTableModel.setRowCount(0);

    String sql = """
        SELECT ProductID, ProductName, Quantity, ReorderLevel
        FROM Products
        """ + (lowStockOnly ? " WHERE Quantity <= ReorderLevel " : "")
          + " ORDER BY ProductName COLLATE NOCASE";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            stockTableModel.addRow(new Object[] {
                rs.getInt("ProductID"),
                rs.getString("ProductName"),
                rs.getInt("Quantity"),
                rs.getInt("ReorderLevel")
            });
        }

    } catch (SQLException e) {
        showError("Unable to load the stock list.", e);
    }
}

private void loadStockHistory() {
    if (historyTableModel == null) {
        return;
    }

    historyTableModel.setRowCount(0);

    String sql = """
        SELECT MovementID, ProductID, MovementType, Quantity,
               PreviousQuantity, NewQuantity, Notes, MovementDate
        FROM StockMovements
        ORDER BY MovementID DESC
        LIMIT 100
        """;

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

        while (rs.next()) {
            historyTableModel.addRow(new Object[] {
                rs.getInt("MovementID"),
                rs.getInt("ProductID"),
                rs.getString("MovementType"),
                rs.getInt("Quantity"),
                rs.getInt("PreviousQuantity"),
                rs.getInt("NewQuantity"),
                rs.getString("Notes"),
                rs.getString("MovementDate")
            });
        }

    } catch (SQLException e) {
        showError("Unable to load stock history.", e);
    }
}

private void deleteSelectedHistory() {
    int selectedRow = historyTable.getSelectedRow();

    if (selectedRow == -1) {
        showMessage("Please select a stock history entry to delete.");
        return;
    }

    int row = historyTable.convertRowIndexToModel(selectedRow);
    int movementId = ((Number) historyTableModel.getValueAt(row, 0)).intValue();

    int choice = JOptionPane.showConfirmDialog(
        this,
        "Are you sure you want to delete the selected stock history entry?",
        "Confirm Delete",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (choice != JOptionPane.YES_OPTION) {
        return;
    }

    String sql = "DELETE FROM StockMovements WHERE MovementID = ?";

    try (Connection con = DBConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, movementId);
        ps.executeUpdate();
        loadStockHistory();
        showMessage("The selected history entry was deleted successfully.");

    } catch (SQLException e) {
        showError("Unable to delete the history entry.", e);
    }
}

private void clearStockHistory() {
    int choice = JOptionPane.showConfirmDialog(
        this,
        "Are you sure you want to permanently delete all stock history?\n"
            + "Products and their current quantities will not be affected.",
        "Clear All Stock History",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (choice != JOptionPane.YES_OPTION) {
        return;
    }

    try (Connection con = DBConnection.getConnection();
         Statement st = con.createStatement()) {

        st.executeUpdate("DELETE FROM StockMovements");
        loadStockHistory();
        showMessage("All stock history was cleared successfully.");

    } catch (SQLException e) {
        showError("Unable to clear stock history.", e);
    }
}

private void performStockMovement(String type) {
    int selectedRow = stockTable.getSelectedRow();

    if (selectedRow == -1) {
        showMessage("Please select a product from the Current Stock table first.");
        return;
    }

    int row = stockTable.convertRowIndexToModel(selectedRow);
    int productId = (int) stockTableModel.getValueAt(row, 0);
    String productName = stockTableModel.getValueAt(row, 1).toString();

    String quantityText = JOptionPane.showInputDialog(
        this,
        "Product: " + productName + "\nEnter quantity:",
        type.equals("IN") ? "Stock In" : "Stock Out",
        JOptionPane.QUESTION_MESSAGE
    );

    if (quantityText == null) {
        return;
    }

    final int movementQuantity;

    try {
        movementQuantity = Integer.parseInt(quantityText.trim());

        if (movementQuantity <= 0) {
            showMessage("Quantity must be greater than zero.");
            return;
        }
    } catch (NumberFormatException e) {
        showMessage("Please enter a valid whole number for quantity.");
        return;
    }

    String notes = JOptionPane.showInputDialog(
        this, "Notes (optional):", "Stock Notes",
        JOptionPane.QUESTION_MESSAGE
    );

    if (notes == null) {
        return;
    }

    String selectSql =
        "SELECT Quantity FROM Products WHERE ProductID = ?";

    String updateSql =
        "UPDATE Products SET Quantity = ? WHERE ProductID = ?";

    String historySql = """
        INSERT INTO StockMovements
        (ProductID, MovementType, Quantity,
         PreviousQuantity, NewQuantity, Notes)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    try (Connection con = DBConnection.getConnection()) {
        con.setAutoCommit(false);

        try {
            int previousQuantity;

            try (PreparedStatement ps = con.prepareStatement(selectSql)) {
                ps.setInt(1, productId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalArgumentException(
                            "Product was not found in the database."
                        );
                    }

                    previousQuantity = rs.getInt("Quantity");
                }
            }

            int newQuantity;

            if (type.equals("IN")) {
                newQuantity = Math.addExact(
                    previousQuantity, movementQuantity
                );
            } else {
                if (movementQuantity > previousQuantity) {
                    throw new IllegalArgumentException(
                        "Insufficient stock. Available quantity: "
                            + previousQuantity
                    );
                }

                newQuantity = previousQuantity - movementQuantity;
            }

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setInt(1, newQuantity);
                ps.setInt(2, productId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Product quantity could not be updated.");
                }
            }

            try (PreparedStatement ps = con.prepareStatement(historySql)) {
                ps.setInt(1, productId);
                ps.setString(2, type);
                ps.setInt(3, movementQuantity);
                ps.setInt(4, previousQuantity);
                ps.setInt(5, newQuantity);
                ps.setString(6, notes.trim().isEmpty() ? null : notes.trim());
                ps.executeUpdate();
            }

            con.commit();

        } catch (Exception e) {
            con.rollback();

            if (e instanceof IllegalArgumentException
                    || e instanceof ArithmeticException) {
                showMessage(e.getMessage());
                return;
            }

            throw e;
        } finally {
            con.setAutoCommit(true);
        }

        loadStockProducts(false);
        loadStockHistory();
        loadProducts(searchField.getText().trim());

        JOptionPane.showMessageDialog(
            this,
            "Stock movement saved successfully."
        );

    } catch (Exception e) {
        showError("Unable to save the stock movement.", e);
    }
}

    private JPanel createBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel customerRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        customerRow.add(new JLabel("Customer:"));
        billingCustomerCombo = new JComboBox<>();
        billingCustomerCombo.setPreferredSize(new Dimension(300, 28));
        JButton refreshCustomers = new JButton("Refresh Customers");
        refreshCustomers.addActionListener(e -> loadBillingCustomers());
        customerRow.add(billingCustomerCombo);
        customerRow.add(refreshCustomers);

        JPanel productRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        productRow.add(new JLabel("Product:"));
        billingProductCombo = new JComboBox<>();
        billingProductCombo.setPreferredSize(new Dimension(340, 28));
        billingQuantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        productRow.add(billingProductCombo);
        productRow.add(new JLabel("Quantity:"));
        productRow.add(billingQuantitySpinner);
        JButton addToCartButton = new JButton("Add to Bill");
        addToCartButton.addActionListener(e -> addBillingItem());
        JButton refreshProducts = new JButton("Refresh Products");
        refreshProducts.addActionListener(e -> loadBillingProducts());
        productRow.add(addToCartButton);
        productRow.add(refreshProducts);
        top.add(customerRow);
        top.add(productRow);
        panel.add(top, BorderLayout.NORTH);

        cartTableModel = new DefaultTableModel(
            new String[] {"Product ID", "Product", "Unit Price", "Quantity", "Line Total"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        cartTable = new JTable(cartTableModel);
        cartTable.setRowHeight(24);
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton removeButton = new JButton("Remove Selected Item");
        removeButton.addActionListener(e -> removeBillingItem());
        JButton clearButton = new JButton("Clear Bill");
        clearButton.addActionListener(e -> clearBillingCart());
        JButton checkoutButton = new JButton("Save Bill / Checkout");
        checkoutButton.addActionListener(e -> checkoutBill());
        JButton viewBillsButton = new JButton("View Saved Bills / PDF");
        viewBillsButton.addActionListener(e -> showSavedBills());
        actions.add(removeButton);
        actions.add(clearButton);
        actions.add(checkoutButton);
        actions.add(viewBillsButton);
        billTotalLabel = new JLabel("Total: 0.00");
        billTotalLabel.setFont(new Font("Arial", Font.BOLD, 18));
        bottom.add(actions, BorderLayout.WEST);
        bottom.add(billTotalLabel, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        // Initial database loading is performed by the constructor after table creation.
        return panel;
    }

    private void createBillingTables() {
        String salesSql = """
            CREATE TABLE IF NOT EXISTS Sales (
                SaleID INTEGER PRIMARY KEY AUTOINCREMENT,
                CustomerID INTEGER,
                CustomerName TEXT NOT NULL,
                TotalAmount REAL NOT NULL,
                SaleDate TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """;
        String itemsSql = """
            CREATE TABLE IF NOT EXISTS SaleItems (
                SaleItemID INTEGER PRIMARY KEY AUTOINCREMENT,
                SaleID INTEGER NOT NULL,
                ProductID INTEGER NOT NULL,
                ProductName TEXT NOT NULL,
                UnitPrice REAL NOT NULL,
                Quantity INTEGER NOT NULL,
                LineTotal REAL NOT NULL
            )
            """;
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate(salesSql);
            st.executeUpdate(itemsSql);
        } catch (SQLException e) {
            showError("Unable to create billing tables.", e);
        }
    }

    private void loadBillingCustomers() {
        if (billingCustomerCombo == null) return;
        billingCustomerCombo.removeAllItems();
        String sql = "SELECT CustomerID, CustomerName FROM Customers ORDER BY CustomerName COLLATE NOCASE";
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) billingCustomerCombo.addItem(new CustomerOption(rs.getInt(1), rs.getString(2)));
        } catch (SQLException e) {
            showError("Unable to load customers for billing.", e);
        }
    }

    private void loadBillingProducts() {
        if (billingProductCombo == null) return;
        billingProductCombo.removeAllItems();
        String sql = "SELECT ProductID, ProductName, SalePrice, Quantity FROM Products ORDER BY ProductName COLLATE NOCASE";
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                billingProductCombo.addItem(new ProductOption(rs.getInt(1), rs.getString(2), rs.getDouble(3), rs.getInt(4)));
            }
        } catch (SQLException e) {
            showError("Unable to load products for billing.", e);
        }
    }

    private void addBillingItem() {
        ProductOption product = (ProductOption) billingProductCombo.getSelectedItem();
        if (product == null) { showMessage("Please add a product first."); return; }
        int quantity = (Integer) billingQuantitySpinner.getValue();
        int alreadyInCart = 0;
        for (CartItem item : cartItems) if (item.productId == product.id) alreadyInCart += item.quantity;
        if (quantity + alreadyInCart > product.available) {
            showMessage("Insufficient stock. Available quantity: " + product.available + "."); return;
        }
        CartItem existing = null;
        for (CartItem item : cartItems) if (item.productId == product.id) { existing = item; break; }
        if (existing == null) cartItems.add(new CartItem(product.id, product.name, product.price, quantity));
        else existing.quantity += quantity;
        refreshCartTable();
    }

    private void refreshCartTable() {
        cartTableModel.setRowCount(0);
        double total = 0;
        for (CartItem item : cartItems) {
            double lineTotal = item.price * item.quantity;
            total += lineTotal;
            cartTableModel.addRow(new Object[] {item.productId, item.name, item.price, item.quantity, lineTotal});
        }
        billTotalLabel.setText(String.format(java.util.Locale.US, "Total: %.2f", total));
    }

    private void removeBillingItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) { showMessage("Please select an item in the bill to remove it."); return; }
        cartItems.remove(row);
        refreshCartTable();
    }

    private void clearBillingCart() {
        if (cartItems.isEmpty()) return;
        int choice = JOptionPane.showConfirmDialog(this, "Are you sure you want to clear the current bill?", "Clear Bill", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) { cartItems.clear(); refreshCartTable(); }
    }

    private void checkoutBill() {
        if (cartItems.isEmpty()) { showMessage("Please add at least one product to the bill."); return; }
        CustomerOption customer = (CustomerOption) billingCustomerCombo.getSelectedItem();
        if (customer == null) { showMessage("Please add or select a customer in the Customers tab first."); return; }
        double total = 0;
        for (CartItem item : cartItems) total += item.price * item.quantity;
        int choice = JOptionPane.showConfirmDialog(this,
            "Customer: " + customer.name + "\nTotal: " + String.format(java.util.Locale.US, "%.2f", total) + "\nSave this bill and deduct the stock?",
            "Confirm Checkout", JOptionPane.YES_NO_OPTION);
        if (choice != JOptionPane.YES_OPTION) return;

        String saleSql = "INSERT INTO Sales (CustomerID, CustomerName, TotalAmount) VALUES (?, ?, ?)";
        String stockSql = "SELECT ProductName, Quantity FROM Products WHERE ProductID = ?";
        String updateSql = "UPDATE Products SET Quantity = Quantity - ? WHERE ProductID = ? AND Quantity >= ?";
        String itemSql = "INSERT INTO SaleItems (SaleID, ProductID, ProductName, UnitPrice, Quantity, LineTotal) VALUES (?, ?, ?, ?, ?, ?)";
        String historySql = "INSERT INTO StockMovements (ProductID, MovementType, Quantity, PreviousQuantity, NewQuantity, Notes) VALUES (?, 'OUT', ?, ?, ?, ?)";
        int saleId;
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(saleSql)) {
                    ps.setInt(1, customer.id); ps.setString(2, customer.name); ps.setDouble(3, total); ps.executeUpdate();
                }
                try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                    if (!rs.next()) throw new SQLException("Sale ID was not found.");
                    saleId = rs.getInt(1);
                }
                for (CartItem item : cartItems) {
                    int previous;
                    try (PreparedStatement ps = con.prepareStatement(stockSql)) {
                        ps.setInt(1, item.productId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) throw new SQLException("Product has already been deleted: " + item.name);
                            previous = rs.getInt("Quantity");
                        }
                    }
                    if (previous < item.quantity) throw new SQLException("" + item.name + " has insufficient stock. Available: " + previous);
                    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                        ps.setInt(1, item.quantity); ps.setInt(2, item.productId); ps.setInt(3, item.quantity);
                        if (ps.executeUpdate() != 1) throw new SQLException("Stock could not be updated: " + item.name);
                    }
                    try (PreparedStatement ps = con.prepareStatement(itemSql)) {
                        ps.setInt(1, saleId); ps.setInt(2, item.productId); ps.setString(3, item.name);
                        ps.setDouble(4, item.price); ps.setInt(5, item.quantity); ps.setDouble(6, item.price * item.quantity); ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement(historySql)) {
                        ps.setInt(1, item.productId); ps.setInt(2, item.quantity); ps.setInt(3, previous);
                        ps.setInt(4, previous - item.quantity); ps.setString(5, "Sale ID: " + saleId); ps.executeUpdate();
                    }
                }
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            showError("Unable to save the bill. Stock changes have been rolled back.", e);
            return;
        }
        JOptionPane.showMessageDialog(this, "Bill saved successfully!\nBill ID: " + saleId + "\nTotal: " + String.format(java.util.Locale.US, "%.2f", total));
        cartItems.clear(); refreshCartTable();
        loadBillingProducts(); loadProducts(searchField.getText().trim());
        loadStockProducts(false); loadStockHistory();
    }

    private void showSavedBills() {
        DefaultTableModel billsModel = new DefaultTableModel(
            new String[] {"Bill ID", "Customer", "Total", "Date"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable billsTable = new JTable(billsModel);
        billsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        billsTable.setRowHeight(24);

        String sql = "SELECT SaleID, CustomerName, TotalAmount, SaleDate FROM Sales ORDER BY SaleID DESC";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                billsModel.addRow(new Object[] {rs.getInt("SaleID"), rs.getString("CustomerName"),
                    String.format(java.util.Locale.US, "%.2f", rs.getDouble("TotalAmount")), rs.getString("SaleDate")});
            }
        } catch (SQLException e) {
            showError("Unable to load saved bills.", e);
            return;
        }
        if (billsModel.getRowCount() == 0) {
            showMessage("There are no saved bills in the database.");
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.add(new JScrollPane(billsTable), BorderLayout.CENTER);
        JLabel hint = new JLabel("Select a bill, then generate or print its PDF.");
        panel.add(hint, BorderLayout.NORTH);
        JButton pdfButton = new JButton("Generate PDF");
        JButton printButton = new JButton("Open / Print PDF");
        JButton deleteBillButton = new JButton("Delete Selected Bill");
        JButton clearBillsButton = new JButton("Delete All Bills");
        JButton closeButton = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(pdfButton);
        buttons.add(printButton);
        buttons.add(deleteBillButton);
        buttons.add(clearBillsButton);
        buttons.add(closeButton);
        panel.add(buttons, BorderLayout.SOUTH);
        JDialog dialog = new JDialog(this, "Saved Bills", true);
        dialog.setContentPane(panel);
        dialog.setSize(760, 420);
        dialog.setLocationRelativeTo(this);

        pdfButton.addActionListener(e -> {
            int row = billsTable.getSelectedRow();
            if (row < 0) { showMessage("Please select a bill first."); return; }
            int saleId = ((Number) billsModel.getValueAt(billsTable.convertRowIndexToModel(row), 0)).intValue();
            createInvoicePdf(saleId, false);
        });
        printButton.addActionListener(e -> {
            int row = billsTable.getSelectedRow();
            if (row < 0) { showMessage("Please select a bill first."); return; }
            int saleId = ((Number) billsModel.getValueAt(billsTable.convertRowIndexToModel(row), 0)).intValue();
            createInvoicePdf(saleId, true);
        });
        deleteBillButton.addActionListener(e -> {
            int row = billsTable.getSelectedRow();
            if (row < 0) { showMessage("Please select a bill first."); return; }
            int saleId = ((Number) billsModel.getValueAt(billsTable.convertRowIndexToModel(row), 0)).intValue();
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "Are you sure you want to permanently delete bill #" + saleId + " and its PDF?\n"
                + "Note: Stock deducted previously will not be restored.",
                "Confirm Bill Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            if (deleteBillById(saleId)) {
                billsModel.removeRow(billsTable.convertRowIndexToModel(row));
                if (billsModel.getRowCount() == 0) dialog.dispose();
            }
        });
        clearBillsButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                "ALL saved bills, bill items, and generated invoice PDFs will be deleted.\n"
                + "Stock quantities will not be restored. This action cannot be undone.\n\nContinue?",
                "Confirm Delete All Bills", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            if (deleteAllBills()) {
                billsModel.setRowCount(0);
                dialog.dispose();
            }
        });
        closeButton.addActionListener(e -> dialog.dispose());
        dialog.setVisible(true);
    }

    private void createInvoicePdf(int saleId, boolean openForPrint) {
        String customer = "";
        String date = "";
        double total = 0;
        java.util.List<String> lines = new java.util.ArrayList<>();
        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT CustomerName, TotalAmount, SaleDate FROM Sales WHERE SaleID = ?")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { showMessage("Bill was not found in the database."); return; }
                    customer = rs.getString("CustomerName");
                    total = rs.getDouble("TotalAmount");
                    date = rs.getString("SaleDate");
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT ProductName, UnitPrice, Quantity, LineTotal FROM SaleItems WHERE SaleID = ? ORDER BY SaleItemID")) {
                ps.setInt(1, saleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lines.add(rs.getString("ProductName") + " | Qty: " + rs.getInt("Quantity")
                            + " | Unit: " + "PKR " + String.format(java.util.Locale.US, "%,.2f", rs.getDouble("UnitPrice"))
                            + " | Total: " + "PKR " + String.format(java.util.Locale.US, "%,.2f", rs.getDouble("LineTotal")));
                    }
                }
            }
        } catch (SQLException e) {
            showError("Unable to load invoice data.", e);
            return;
        }
        java.util.List<String> pdfLines = new java.util.ArrayList<>();
        pdfLines.add("ForgeHold Nexus ERP");
        pdfLines.add("INVOICE / RECEIPT");
        pdfLines.add("------------------------------------------------------------");
        pdfLines.add("Invoice No: " + saleId);
        pdfLines.add("Date: " + date);
        pdfLines.add("Customer: " + customer);
        pdfLines.add("------------------------------------------------------------");
        pdfLines.add("Product | Quantity | Unit Price | Line Total");
        pdfLines.addAll(lines);
        pdfLines.add("------------------------------------------------------------");
        pdfLines.add("GRAND TOTAL: PKR " + String.format(java.util.Locale.US, "%,.2f", total));
        pdfLines.add("");
        pdfLines.add("Thank you for your business!");

        try {
            Path folder = Path.of("Invoices").toAbsolutePath().normalize();
            Files.createDirectories(folder);
            Path file = folder.resolve("Invoice_" + saleId + ".pdf");
            writeSimplePdf(file, pdfLines);
            if (openForPrint && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file.toFile());
                showMessage("The PDF has opened. Use the Print option in your PDF viewer.\n" + file);
            } else {
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(file.toFile());
                showMessage("Invoice PDF saved to:\n" + file);
            }
        } catch (Exception e) {
            showError("Unable to create or open the PDF.", e);
        }
    }

    private boolean deleteBillById(int saleId) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement items = con.prepareStatement("DELETE FROM SaleItems WHERE SaleID = ?");
                 PreparedStatement sale = con.prepareStatement("DELETE FROM Sales WHERE SaleID = ?")) {
                items.setInt(1, saleId);
                items.executeUpdate();
                sale.setInt(1, saleId);
                sale.executeUpdate();
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
            Path pdf = Path.of("Invoices", "Invoice_" + saleId + ".pdf").toAbsolutePath().normalize();
            Files.deleteIfExists(pdf);
            showMessage("Bill #" + saleId + " and its PDF, if present, were deleted successfully.");
            return true;
        } catch (Exception e) {
            showError("Unable to delete the bill.", e);
            return false;
        }
    }

    private boolean deleteAllBills() {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (Statement st = con.createStatement()) {
                st.executeUpdate("DELETE FROM SaleItems");
                st.executeUpdate("DELETE FROM Sales");
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
            Path folder = Path.of("Invoices").toAbsolutePath().normalize();
            if (Files.isDirectory(folder)) {
                try (var files = Files.list(folder)) {
                    files.filter(f -> f.getFileName().toString().matches("Invoice_\\d+\\.pdf"))
                         .forEach(f -> { try { Files.deleteIfExists(f); } catch (IOException ex) { throw new java.io.UncheckedIOException(ex); } });
                }
            }
            showMessage("All saved bills and generated invoice PDFs were deleted. Stock quantities were not changed.");
            return true;
        } catch (Exception e) {
            showError("Unable to clear saved bills.", e);
            return false;
        }
    }

    // Corporate navy invoice: dark navy is used only with white text; body stays white.
    private void writeSimplePdf(Path file, java.util.List<String> lines) throws IOException {
        StringBuilder c = new StringBuilder();
        final float x = 40, width = 515, rowH = 25;
        float y = 0;

        // White page and navy title banner with high-contrast white text.
        c.append("1 1 1 rg 0 0 595 842 re f\n");
        c.append("0.12 0.20 0.32 rg 40 785 515 35 re f\n");
        textColor(c, 52, 797, 15, "ForgeHold Nexus ERP", true, "1 1 1");
        text(c, 40, 758, 16, "SALES INVOICE", true);

        // Invoice metadata is intentionally black on white.
        float metaY = 730;
        int tableHeader = -1;
        int tableEnd = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("Product | Quantity | Unit Price | Line Total")) tableHeader = i;
            if (tableHeader >= 0 && i > tableHeader && lines.get(i).startsWith("-----")) { tableEnd = i; break; }
        }
        for (int i = 3; i < Math.min(tableHeader, lines.size()); i++) {
            String line = lines.get(i);
            if (!line.startsWith("---")) {
                text(c, 45, metaY, 10, line, false);
                metaY -= 17;
            }
        }

        y = metaY - 12;
        float[] col = {x, x + 210, x + 265, x + 390, x + width};
        // Table header: navy background, white bold text.
        c.append("0.12 0.20 0.32 rg ").append(x).append(" ").append(y-rowH).append(" ").append(width).append(" ").append(rowH).append(" re f\n");
        textColor(c, col[0]+8, y-16, 9, "Product", true, "1 1 1");
        textColor(c, col[1]+8, y-16, 9, "Qty", true, "1 1 1");
        textColor(c, col[2]+8, y-16, 9, "Unit Price (PKR)", true, "1 1 1");
        textColor(c, col[3]+8, y-16, 9, "Line Total (PKR)", true, "1 1 1");
        y -= rowH;

        if (tableHeader >= 0) {
            for (int i = tableHeader + 1; i < tableEnd; i++) {
                String line = lines.get(i);
                String[] parts = line.split(" \\| ");
                String name = parts.length > 0 ? parts[0] : line;
                String qty = parts.length > 1 ? parts[1].replace("Qty: ", "") : "";
                String unit = parts.length > 2 ? parts[2].replace("Unit: ", "") : "";
                String total = parts.length > 3 ? parts[3].replace("Total: ", "") : "";
                if (y < 110) break;
                // Alternating white / pale-blue row fill, dark text, light grid.
                if ((i - tableHeader) % 2 == 0) {
                    c.append("0.96 0.975 0.99 rg ").append(x).append(" ").append(y-rowH).append(" ").append(width).append(" ").append(rowH).append(" re f\n");
                } else {
                    c.append("1 1 1 rg ").append(x).append(" ").append(y-rowH).append(" ").append(width).append(" ").append(rowH).append(" re f\n");
                }
                c.append("0.72 0.76 0.81 RG 0.6 w ").append(x).append(" ").append(y-rowH).append(" ").append(width).append(" ").append(rowH).append(" re S\n");
                for (float vx : col) c.append(vx).append(" ").append(y).append(" m ").append(vx).append(" ").append(y-rowH).append(" l S\n");
                text(c, col[0]+7, y-16, 9, name, false);
                text(c, col[1]+8, y-16, 9, qty, false);
                text(c, col[2]+7, y-16, 9, unit, false);
                text(c, col[3]+7, y-16, 9, total, false);
                y -= rowH;
            }
        }

        y -= 20;
        String grand = lines.stream().filter(l -> l.startsWith("GRAND TOTAL:")).findFirst().orElse("GRAND TOTAL: PKR 0.00");
        c.append("0.91 0.94 0.97 rg 300 ").append(y-14).append(" 255 34 re f\n");
        c.append("0.12 0.20 0.32 RG 0.8 w 300 ").append(y-14).append(" 255 34 re S\n");
        text(c, 310, y+1, 11, grand, true);
        text(c, 40, 55, 9, "Thank you for your business!", false);

        byte[] stream = c.append("\n").toString().getBytes(StandardCharsets.US_ASCII);
        java.util.List<byte[]> objects = new java.util.ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.US_ASCII));
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>".getBytes(StandardCharsets.US_ASCII));
        java.io.ByteArrayOutputStream streamObj = new java.io.ByteArrayOutputStream();
        streamObj.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
        streamObj.write(stream);
        streamObj.write("\nendstream".getBytes(StandardCharsets.US_ASCII));
        objects.add(streamObj.toByteArray());

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        out.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        java.util.List<Integer> offsets = new java.util.ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            out.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            out.write(objects.get(i));
            out.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }
        int xref = out.size();
        out.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i < offsets.size(); i++) {
            out.write(String.format(java.util.Locale.US, "%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        }
        out.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
        Files.write(file, out.toByteArray());
    }

    private void text(StringBuilder c, float x, float y, int size, String value, boolean bold) {
        textColor(c, x, y, size, value, bold, "0 0 0");
    }

    private void textColor(StringBuilder c, float x, float y, int size, String value, boolean bold, String rgb) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)").replaceAll("[^\\x20-\\x7E]", "?");
        c.append(rgb).append(" rg BT /").append(bold ? "F2" : "F1").append(" ").append(size).append(" Tf ")
         .append(x).append(" ").append(y).append(" Td (").append(safe).append(") Tj ET\n");
    }

    private static class CustomerOption {
        final int id; final String name;
        CustomerOption(int id, String name) { this.id = id; this.name = name; }
        @Override public String toString() { return id + " - " + name; }
    }

    private static class ProductOption {
        final int id; final String name; final double price; final int available;
        ProductOption(int id, String name, double price, int available) { this.id = id; this.name = name; this.price = price; this.available = available; }
        @Override public String toString() { return id + " - " + name + " | Price: " + price + " | Stock: " + available; }
    }

    private static class CartItem {
        final int productId; final String name; final double price; int quantity;
        CartItem(int productId, String name, double price, int quantity) { this.productId = productId; this.name = name; this.price = price; this.quantity = quantity; }
    }

    private JTextField reportStartDateField;
    private JTextField reportEndDateField;
    private JLabel reportSalesLabel;
    private JLabel reportBillsLabel;
    private JLabel reportProfitLabel;
    private DefaultTableModel reportTableModel;

    private JPanel createAboutPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel aboutHeader = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 4));
        aboutHeader.setOpaque(false);
        aboutHeader.add(new ForgeHoldLogo(48));
        JLabel title = new JLabel("ForgeHold Nexus ERP");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(24, 43, 70));
        aboutHeader.add(title);
        panel.add(aboutHeader, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel softwareHeading = new JLabel("About the Software");
        softwareHeading.setFont(new Font("Arial", Font.BOLD, 17));
        softwareHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(softwareHeading);
        content.add(Box.createVerticalStrut(8));

        JTextArea softwareDescription = new JTextArea(
            "ForgeHold Nexus ERP is a desktop application " +
            "designed to help manage products, stock, customers, billing, and reports " +
            "in one place. It provides product and customer records, stock-in/stock-out " +
            "tracking, billing tools, and sales and inventory summaries. The application " +
            "uses a SQLite database to store its records."
        );
        softwareDescription.setFont(new Font("Arial", Font.PLAIN, 14));
        softwareDescription.setLineWrap(true);
        softwareDescription.setWrapStyleWord(true);
        softwareDescription.setEditable(false);
        softwareDescription.setOpaque(false);
        softwareDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(softwareDescription);

        content.add(Box.createVerticalStrut(22));
        JLabel creatorHeading = new JLabel("About the Developer");
        creatorHeading.setFont(new Font("Arial", Font.BOLD, 17));
        creatorHeading.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(creatorHeading);
        content.add(Box.createVerticalStrut(8));

        JTextArea creatorDescription = new JTextArea(
            "Developed by: Usman Ahmad\n\n" +
            "Contact: +92 334 6395715\n\n" +
            "This software project was created to make everyday inventory, stock, " +
            "customer, and billing tasks easier to organize and manage.\n\n" +
            "© 2026 All Rights Reserved. Developed by Usman Ahmad\n\n"
        );
        creatorDescription.setFont(new Font("Arial", Font.PLAIN, 14));
        creatorDescription.setLineWrap(true);
        creatorDescription.setWrapStyleWord(true);
        creatorDescription.setEditable(false);
        creatorDescription.setOpaque(false);
        creatorDescription.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(creatorDescription);

        panel.add(content, BorderLayout.CENTER);

        JLabel footer = new JLabel("ForgeHold Nexus ERP", SwingConstants.CENTER);
        footer.setFont(new Font("Arial", Font.ITALIC, 12));
        panel.add(footer, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Sales & Profit Reports");
        title.setFont(new Font("Arial", Font.BOLD, 22));

        reportStartDateField = new JTextField(10);
        reportEndDateField = new JTextField(10);
        java.time.LocalDate today = java.time.LocalDate.now();
        reportStartDateField.setText(today.withDayOfMonth(1).toString());
        reportEndDateField.setText(today.toString());

        JButton applyButton = new JButton("Generate Report");
        JButton allButton = new JButton("All Dates");
        JButton exportButton = new JButton("Export CSV");
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filters.add(new JLabel("From (YYYY-MM-DD):"));
        filters.add(reportStartDateField);
        filters.add(new JLabel("To (YYYY-MM-DD):"));
        filters.add(reportEndDateField);
        filters.add(applyButton);
        filters.add(allButton);
        filters.add(exportButton);

        JPanel top = new JPanel(new BorderLayout(5, 8));
        top.add(title, BorderLayout.NORTH);
        top.add(filters, BorderLayout.SOUTH);

        reportSalesLabel = new JLabel("Total Sales: PKR 0.00");
        reportBillsLabel = new JLabel("Bills: 0");
        reportProfitLabel = new JLabel("Estimated Profit: PKR 0.00");
        for (JLabel label : new JLabel[]{reportSalesLabel, reportBillsLabel, reportProfitLabel}) {
            label.setFont(new Font("Arial", Font.BOLD, 16));
            label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 220, 232)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
            label.setOpaque(true);
            label.setBackground(new Color(240, 245, 250));
        }
        JPanel summary = new JPanel(new GridLayout(1, 3, 10, 0));
        summary.add(reportSalesLabel);
        summary.add(reportBillsLabel);
        summary.add(reportProfitLabel);

        reportTableModel = new DefaultTableModel(
            new String[]{"Bill ID", "Date", "Customer", "Item Names", "Items", "Total (PKR)"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable reportTable = new JTable(reportTableModel);
        reportTable.setRowHeight(25);
        reportTable.setAutoCreateRowSorter(true);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.add(summary, BorderLayout.NORTH);
        center.add(new JScrollPane(reportTable), BorderLayout.CENTER);

        applyButton.addActionListener(e -> loadSalesReport(true));
        allButton.addActionListener(e -> {
            reportStartDateField.setText("");
            reportEndDateField.setText("");
            loadSalesReport(false);
        });
        exportButton.addActionListener(e -> exportSalesReport());

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        // Initial report loading is performed after the database schema is initialized.
        return panel;
    }

    private void loadSalesReport(boolean useDates) {
        if (reportTableModel == null) return;
        String start = reportStartDateField.getText().trim();
        String end = reportEndDateField.getText().trim();
        if (useDates) {
            try {
                if (!start.isEmpty()) java.time.LocalDate.parse(start);
                if (!end.isEmpty()) java.time.LocalDate.parse(end);
                if (!start.isEmpty() && !end.isEmpty() && start.compareTo(end) > 0) {
                    showMessage("The start date cannot be later than the end date.");
                    return;
                }
            } catch (java.time.format.DateTimeParseException ex) {
                showMessage("Enter the date in YYYY-MM-DD format, for example: 2026-10-23");
                return;
            }
        } else { start = ""; end = ""; }

        reportTableModel.setRowCount(0);
        String where = " WHERE (? = '' OR date(s.SaleDate) >= date(?)) AND (? = '' OR date(s.SaleDate) <= date(?)) ";
        String sql = "SELECT s.SaleID, s.SaleDate, s.CustomerName, " +
            "COALESCE((SELECT GROUP_CONCAT(si.ProductName, '; ') " +
            "FROM SaleItems si WHERE si.SaleID=s.SaleID), '') AS ItemNames, " +
            "COALESCE((SELECT SUM(si.Quantity) FROM SaleItems si WHERE si.SaleID=s.SaleID),0) AS Items, " +
            "s.TotalAmount FROM Sales s " + where + " ORDER BY s.SaleID DESC";
        String summarySql = "SELECT COUNT(*) AS Bills, COALESCE(SUM(s.TotalAmount),0) AS Sales, " +
            "COALESCE((SELECT SUM((si.UnitPrice - COALESCE(p.PurchasePrice, si.UnitPrice)) * si.Quantity) " +
            "FROM SaleItems si JOIN Sales sx ON sx.SaleID=si.SaleID LEFT JOIN Products p ON p.ProductID=si.ProductID " +
            "WHERE (? = '' OR date(sx.SaleDate) >= date(?)) AND (? = '' OR date(sx.SaleDate) <= date(?))),0) AS Profit " +
            "FROM Sales s " + where;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, start); ps.setString(2, start); ps.setString(3, end); ps.setString(4, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) reportTableModel.addRow(new Object[]{rs.getInt("SaleID"), rs.getString("SaleDate"),
                    rs.getString("CustomerName"), rs.getString("ItemNames"), rs.getInt("Items"),
                    String.format(java.util.Locale.US, "PKR %,.2f", rs.getDouble("TotalAmount"))});
            }
            try (PreparedStatement sumPs = con.prepareStatement(summarySql)) {
                sumPs.setString(1, start); sumPs.setString(2, start); sumPs.setString(3, end); sumPs.setString(4, end);
                sumPs.setString(5, start); sumPs.setString(6, start); sumPs.setString(7, end); sumPs.setString(8, end);
                try (ResultSet rs = sumPs.executeQuery()) {
                    if (rs.next()) {
                        reportBillsLabel.setText("Bills: " + rs.getInt("Bills"));
                        reportSalesLabel.setText(String.format(java.util.Locale.US, "Total Sales: PKR %,.2f", rs.getDouble("Sales")));
                        reportProfitLabel.setText(String.format(java.util.Locale.US, "Estimated Profit: PKR %,.2f", rs.getDouble("Profit")));
                    }
                }
            }
        } catch (SQLException e) {
            showError("Unable to load the sales report.", e);
        }
    }

    private void exportSalesReport() {
        if (reportTableModel == null || reportTableModel.getRowCount() == 0) {
            showMessage("There is no report data to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("Sales_Report.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        StringBuilder csv = new StringBuilder("Bill ID,Date,Customer,Item Names,Items,Total (PKR)\r\n");
        for (int r = 0; r < reportTableModel.getRowCount(); r++) {
            for (int c = 0; c < reportTableModel.getColumnCount(); c++) {
                if (c > 0) csv.append(',');
                String value = String.valueOf(reportTableModel.getValueAt(r, c));
                if (c == 1) {
                    // Export date only (yyyy-MM-dd), with no time.
                    String dateOnly = value == null ? "" : value.trim();
                    if (dateOnly.length() >= 10) {
                        dateOnly = dateOnly.substring(0, 10);
                    }
                    // Excel formula returns text, avoiding ####### and date auto-formatting.
                    String safeDate = dateOnly.replace("\"", "\"\"");
                    csv.append("\"=\"\"").append(safeDate).append("\"\"\"");
                } else {
                    csv.append('"').append(value.replace("\"", "\"\"")).append('"');
                }
            }
            csv.append("\r\n");
        }
        try {
            Files.writeString(chooser.getSelectedFile().toPath(), csv.toString(), StandardCharsets.UTF_8);
            showMessage("Report exported to CSV successfully.");
        } catch (IOException e) {
            showError("Unable to export the CSV file.", e);
        }
    }

    private JPanel createDashboardPanel(JTabbedPane tabs) {
        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.setBackground(new Color(245, 247, 251));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(new Color(29, 45, 68));
        JLabel subtitle = new JLabel("Inventory overview and business activity");
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(new Color(100, 112, 130));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);
        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.addActionListener(e -> refreshDashboard());
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        dashboardUsernameLabel = new JLabel("Logged in as: " + loggedInUsername);
        dashboardUsernameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        dashboardUsernameLabel.setForeground(new Color(44, 95, 160));
        JButton changeUsernameButton = new JButton("Change Username");
        changeUsernameButton.addActionListener(e -> changeUsername());
        JButton changePasswordButton = new JButton("Change Password");
        changePasswordButton.addActionListener(e -> changePassword());
        headerActions.add(dashboardUsernameLabel);
        headerActions.add(changeUsernameButton);
        headerActions.add(changePasswordButton);
        headerActions.add(refreshButton);
        header.add(heading, BorderLayout.WEST);
        header.add(headerActions, BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        dashboardProductsLabel = new JLabel("0", SwingConstants.LEFT);
        dashboardCustomersLabel = new JLabel("0", SwingConstants.LEFT);
        dashboardSalesLabel = new JLabel("PKR 0.00", SwingConstants.LEFT);
        dashboardLowStockLabel = new JLabel("0", SwingConstants.LEFT);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);
        cards.add(createDashboardCard("TOTAL PRODUCTS", dashboardProductsLabel, new Color(44, 95, 160)));
        cards.add(createDashboardCard("TOTAL CUSTOMERS", dashboardCustomersLabel, new Color(31, 128, 115)));
        cards.add(createDashboardCard("TODAY'S SALES", dashboardSalesLabel, new Color(126, 83, 166)));
        cards.add(createDashboardCard("LOW STOCK ITEMS", dashboardLowStockLabel, new Color(190, 102, 48)));

        dashboardBillsModel = new DefaultTableModel(
            new String[]{"Bill ID", "Date", "Customer", "Total (PKR)"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable recentBillsTable = new JTable(dashboardBillsModel);
        recentBillsTable.setRowHeight(27);
        recentBillsTable.setFillsViewportHeight(true);
        recentBillsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        recentBillsTable.setFont(new Font("Arial", Font.PLAIN, 13));
        JPanel recentPanel = new JPanel(new BorderLayout(8, 8));
        recentPanel.setBackground(Color.WHITE);
        recentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 228, 237)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JLabel recentTitle = new JLabel("Recent Bills");
        recentTitle.setFont(new Font("Arial", Font.BOLD, 18));
        recentPanel.add(recentTitle, BorderLayout.NORTH);
        recentPanel.add(new JScrollPane(recentBillsTable), BorderLayout.CENTER);

        // Replace the generic Quick Actions list with a useful live inventory watchlist.
        dashboardLowStockModel = new DefaultTableModel(
            new String[]{"Product", "In Stock"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable lowStockTable = new JTable(dashboardLowStockModel);
        lowStockTable.setRowHeight(27);
        lowStockTable.setFillsViewportHeight(true);
        lowStockTable.setFont(new Font("Arial", Font.PLAIN, 12));
        lowStockTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        lowStockTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel watchlist = new JPanel(new BorderLayout(8, 8));
        watchlist.setBackground(Color.WHITE);
        watchlist.setPreferredSize(new Dimension(300, 260));
        watchlist.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(222, 228, 237)),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        JPanel watchHeading = new JPanel(new BorderLayout(4, 4));
        watchHeading.setOpaque(false);
        JLabel watchTitle = new JLabel("Inventory Watchlist");
        watchTitle.setFont(new Font("Arial", Font.BOLD, 18));
        JLabel watchSubtitle = new JLabel("Items that may need restocking");
        watchSubtitle.setFont(new Font("Arial", Font.PLAIN, 11));
        watchSubtitle.setForeground(new Color(100, 112, 130));
        JPanel watchText = new JPanel(new GridLayout(2, 1, 0, 3));
        watchText.setOpaque(false);
        watchText.add(watchTitle);
        watchText.add(watchSubtitle);
        watchHeading.add(watchText, BorderLayout.CENTER);
        JButton viewStockButton = new JButton("View Stock");
        viewStockButton.addActionListener(e -> tabs.setSelectedIndex(2));
        watchHeading.add(viewStockButton, BorderLayout.SOUTH);
        watchlist.add(watchHeading, BorderLayout.NORTH);
        watchlist.add(new JScrollPane(lowStockTable), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(12, 12));
        center.setOpaque(false);
        center.add(cards, BorderLayout.NORTH);
        JPanel lower = new JPanel(new BorderLayout(12, 0));
        lower.setOpaque(false);
        lower.add(recentPanel, BorderLayout.CENTER);
        lower.add(watchlist, BorderLayout.EAST);
        center.add(lower, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDashboardCard(String caption, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(6, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 238)),
                BorderFactory.createEmptyBorder(15, 14, 15, 12))));
        JLabel label = new JLabel(caption);
        label.setFont(new Font("Arial", Font.BOLD, 11));
        label.setForeground(new Color(102, 113, 130));
        value.setFont(new Font("Arial", Font.BOLD, 23));
        value.setForeground(new Color(35, 48, 67));
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JButton dashboardActionButton(String caption, JTabbedPane tabs, int tabIndex) {
        JButton button = new JButton(caption);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.addActionListener(e -> tabs.setSelectedIndex(tabIndex));
        return button;
    }

    private void refreshDashboard() {
        if (dashboardProductsLabel == null) return;
        String countsSql = "SELECT " +
            "(SELECT COUNT(*) FROM Products) AS ProductsCount, " +
            "(SELECT COUNT(*) FROM Customers) AS CustomersCount, " +
            "(SELECT COALESCE(SUM(TotalAmount), 0) FROM Sales WHERE date(SaleDate) = date('now','localtime')) AS TodaySales, " +
            "(SELECT COUNT(*) FROM Products WHERE Quantity <= ReorderLevel) AS LowStockCount";
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(countsSql)) {
            if (rs.next()) {
                dashboardProductsLabel.setText(String.valueOf(rs.getInt("ProductsCount")));
                dashboardCustomersLabel.setText(String.valueOf(rs.getInt("CustomersCount")));
                dashboardSalesLabel.setText(String.format(java.util.Locale.US, "PKR %,.2f", rs.getDouble("TodaySales")));
                dashboardLowStockLabel.setText(String.valueOf(rs.getInt("LowStockCount")));
            }
        } catch (SQLException e) {
            showError("Unable to load the dashboard summary.", e);
        }

        if (dashboardLowStockModel != null) {
            dashboardLowStockModel.setRowCount(0);
            String lowStockSql = "SELECT ProductName, Quantity FROM Products " +
                "WHERE Quantity <= ReorderLevel ORDER BY Quantity ASC, ProductName ASC LIMIT 10";
            try (Connection con = DBConnection.getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(lowStockSql)) {
                while (rs.next()) {
                    dashboardLowStockModel.addRow(new Object[]{rs.getString("ProductName"),
                        rs.getInt("Quantity")});
                }
                if (dashboardLowStockModel.getRowCount() == 0) {
                    dashboardLowStockModel.addRow(new Object[]{"All stock levels look good", "—"});
                }
            } catch (SQLException e) {
                showError("Unable to load the inventory watchlist.", e);
            }
        }

        if (dashboardBillsModel != null) {
            dashboardBillsModel.setRowCount(0);
            String billsSql = "SELECT SaleID, SaleDate, CustomerName, TotalAmount FROM Sales ORDER BY SaleID DESC LIMIT 8";
            try (Connection con = DBConnection.getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(billsSql)) {
                while (rs.next()) {
                    String date = rs.getString("SaleDate");
                    if (date != null && date.length() >= 10) date = date.substring(0, 10);
                    dashboardBillsModel.addRow(new Object[]{rs.getInt("SaleID"), date,
                        rs.getString("CustomerName"),
                        String.format(java.util.Locale.US, "%,.2f", rs.getDouble("TotalAmount"))});
                }
            } catch (SQLException e) {
                showError("Unable to load recent bills.", e);
            }
        }
    }

    private JPanel createPanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(new JLabel(message));
        return panel;
    }


    private JPanel createCustomersPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Customer Management");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        customerSearchField = new JTextField(22);
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search by name or phone:"));
        searchPanel.add(customerSearchField);
        searchPanel.add(searchButton);
        searchPanel.add(showAllButton);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(title, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        customerTableModel = new DefaultTableModel(
            new String[] {"Customer ID", "Customer Name", "Phone", "Address"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(customerTableModel);
        customerTable.setRowHeight(25);
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        panel.add(new JScrollPane(customerTable), BorderLayout.CENTER);

        JButton addButton = new JButton("Add Customer");
        JButton updateButton = new JButton("Update Customer");
        JButton deleteButton = new JButton("Delete Customer");
        JButton refreshButton = new JButton("Refresh");

        searchButton.addActionListener(e -> loadCustomers(customerSearchField.getText().trim()));
        customerSearchField.addActionListener(e -> loadCustomers(customerSearchField.getText().trim()));
        showAllButton.addActionListener(e -> {
            customerSearchField.setText("");
            loadCustomers("");
        });
        addButton.addActionListener(e -> addCustomer());
        updateButton.addActionListener(e -> updateCustomer());
        deleteButton.addActionListener(e -> deleteCustomer());
        refreshButton.addActionListener(e -> loadCustomers(customerSearchField.getText().trim()));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void createCustomersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS Customers (
                CustomerID INTEGER PRIMARY KEY AUTOINCREMENT,
                CustomerName TEXT NOT NULL,
                Phone TEXT,
                Address TEXT
            )
            """;

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate(sql);
        } catch (SQLException e) {
            showError("Unable to create the Customers table.", e);
        }
    }

    private void loadCustomers(String search) {
        if (customerTableModel == null) {
            return;
        }
        customerTableModel.setRowCount(0);

        String sql = """
            SELECT CustomerID, CustomerName, Phone, Address
            FROM Customers
            WHERE CustomerName LIKE ? COLLATE NOCASE
               OR Phone LIKE ?
            ORDER BY CustomerID DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String keyword = "%" + search + "%";
            ps.setString(1, keyword);
            ps.setString(2, keyword);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    customerTableModel.addRow(new Object[] {
                        rs.getInt("CustomerID"),
                        rs.getString("CustomerName"),
                        rs.getString("Phone"),
                        rs.getString("Address")
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load customers.", e);
        }
    }

    private void addCustomer() {
        JTextField nameField = new JTextField();
        JTextField phoneField = new JTextField();
        JTextField addressField = new JTextField();
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Customer Name:*"));
        form.add(nameField);
        form.add(new JLabel("Phone:"));
        form.add(phoneField);
        form.add(new JLabel("Address:"));
        form.add(addressField);

        int choice = JOptionPane.showConfirmDialog(this, form, "Add Customer",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showMessage("Please enter the customer name.");
            return;
        }

        String sql = "INSERT INTO Customers (CustomerName, Phone, Address) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phoneField.getText().trim());
            ps.setString(3, addressField.getText().trim());
            ps.executeUpdate();
            loadCustomers(customerSearchField.getText().trim());
            showMessage("Customer saved successfully!");
        } catch (SQLException e) {
            showError("Unable to save the customer.", e);
        }
    }

    private void updateCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            showMessage("Please select a customer to update.");
            return;
        }
        int row = customerTable.convertRowIndexToModel(selectedRow);
        int customerId = ((Number) customerTableModel.getValueAt(row, 0)).intValue();
        JTextField nameField = new JTextField(String.valueOf(customerTableModel.getValueAt(row, 1)));
        Object phoneValue = customerTableModel.getValueAt(row, 2);
        Object addressValue = customerTableModel.getValueAt(row, 3);
        JTextField phoneField = new JTextField(phoneValue == null ? "" : phoneValue.toString());
        JTextField addressField = new JTextField(addressValue == null ? "" : addressValue.toString());

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Customer Name:*"));
        form.add(nameField);
        form.add(new JLabel("Phone:"));
        form.add(phoneField);
        form.add(new JLabel("Address:"));
        form.add(addressField);

        int choice = JOptionPane.showConfirmDialog(this, form, "Update Customer ID: " + customerId,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showMessage("Please enter the customer name.");
            return;
        }

        String sql = "UPDATE Customers SET CustomerName = ?, Phone = ?, Address = ? WHERE CustomerID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phoneField.getText().trim());
            ps.setString(3, addressField.getText().trim());
            ps.setInt(4, customerId);
            ps.executeUpdate();
            loadCustomers(customerSearchField.getText().trim());
            showMessage("Customer updated successfully!");
        } catch (SQLException e) {
            showError("Unable to update the customer.", e);
        }
    }

    private void deleteCustomer() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow == -1) {
            showMessage("Please select a customer to delete.");
            return;
        }
        int row = customerTable.convertRowIndexToModel(selectedRow);
        int customerId = ((Number) customerTableModel.getValueAt(row, 0)).intValue();
        String customerName = String.valueOf(customerTableModel.getValueAt(row, 1));

        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete customer '" + customerName + "'?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        String sql = "DELETE FROM Customers WHERE CustomerID = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            ps.executeUpdate();
            loadCustomers(customerSearchField.getText().trim());
            showMessage("Customer deleted successfully.");
        } catch (SQLException e) {
            showError("Unable to delete the customer.", e);
        }
    }

    private JPanel createProductsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        JLabel title = new JLabel("Product Management");
        title.setFont(new Font("Arial", Font.BOLD, 18));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(title, BorderLayout.NORTH);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchField = new JTextField(22);

        JButton searchButton = new JButton("Search");
        JButton resetButton = new JButton("Show All");

        searchButton.addActionListener(e ->
            loadProducts(searchField.getText().trim())
        );

        searchField.addActionListener(e ->
            loadProducts(searchField.getText().trim())
        );

        resetButton.addActionListener(e -> {
            searchField.setText("");
            loadProducts("");
        });

        searchPanel.add(new JLabel("Search by ID or Name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        topPanel.add(searchPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
            new String[] {
                "Product ID",
                "Product Name",
                "Price (PKR)",
                "Quantity"
            }, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(tableModel);
        productTable.setRowHeight(25);
        productTable.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        panel.add(new JScrollPane(productTable), BorderLayout.CENTER);

        JButton addButton = new JButton("Add Product");
        JButton updateButton = new JButton("Update Product");
        JButton deleteButton = new JButton("Delete Product");
        JButton refreshButton = new JButton("Refresh");

        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());

        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadProducts("");
        });

        JPanel buttonPanel = new JPanel(
            new FlowLayout(FlowLayout.LEFT)
        );

        buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void createProductsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS Products (
                ProductID INTEGER PRIMARY KEY AUTOINCREMENT,
                ProductName TEXT NOT NULL,
                PurchasePrice REAL NOT NULL,
                SalePrice REAL NOT NULL,
                Quantity INTEGER NOT NULL DEFAULT 0,
                ReorderLevel INTEGER NOT NULL DEFAULT 5
            )
            """;

        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(sql);

            String historySql = """
                CREATE TABLE IF NOT EXISTS StockMovements (
                    MovementID INTEGER PRIMARY KEY AUTOINCREMENT,
                    ProductID INTEGER NOT NULL,
                    MovementType TEXT NOT NULL
                        CHECK (MovementType IN ('IN', 'OUT')),
                    Quantity INTEGER NOT NULL CHECK (Quantity > 0),
                    PreviousQuantity INTEGER NOT NULL,
                    NewQuantity INTEGER NOT NULL,
                    Notes TEXT,
                    MovementDate TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

            st.executeUpdate(historySql);

        } catch (SQLException e) {
            showError("Unable to create the Products table.", e);
        }
    }

    private void loadProducts(String search) {
        tableModel.setRowCount(0);

        String sql = """
            SELECT ProductID, ProductName, SalePrice, Quantity
            FROM Products
            WHERE CAST(ProductID AS TEXT) LIKE ?
               OR ProductName LIKE ? COLLATE NOCASE
            ORDER BY ProductID DESC
            """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String keyword = "%" + search + "%";

            ps.setString(1, keyword);
            ps.setString(2, keyword);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[] {
                        rs.getInt("ProductID"),
                        rs.getString("ProductName"),
                        rs.getDouble("SalePrice"),
                        rs.getInt("Quantity")
                    });
                }
            }

        } catch (SQLException e) {
            showError("Unable to load products.", e);
        }
    }

    private void addProduct() {
        JTextField nameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField quantityField = new JTextField("0");

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Product Name:"));
        form.add(nameField);
        form.add(new JLabel("Price (PKR):"));
        form.add(priceField);
        form.add(new JLabel("Quantity:"));
        form.add(quantityField);

        int choice = JOptionPane.showConfirmDialog(
            this, form, "Add New Product",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showMessage("Please enter the product name.");
            return;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (!Double.isFinite(price) || price < 0 || quantity < 0) {
                showMessage("Price and quantity cannot be negative.");
                return;
            }

            String sql = """
                INSERT INTO Products
                (ProductName, PurchasePrice, SalePrice, Quantity)
                VALUES (?, ?, ?, ?)
                """;
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                // New products use the entered price for both stored costs and sale price.
                ps.setDouble(2, price);
                ps.setDouble(3, price);
                ps.setInt(4, quantity);
                ps.executeUpdate();
            }
            loadProducts(searchField.getText().trim());
            JOptionPane.showMessageDialog(this, "Product saved successfully!");
        } catch (NumberFormatException e) {
            showMessage("Please enter a valid number for price and a whole number for quantity.");
        } catch (SQLException e) {
            showError("Unable to save the product.", e);
        }
    }

    private void updateProduct() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            showMessage("Please select a product from the table to update it.");
            return;
        }

        int row = productTable.convertRowIndexToModel(selectedRow);
        int productId = ((Number) tableModel.getValueAt(row, 0)).intValue();
        JTextField nameField = new JTextField(tableModel.getValueAt(row, 1).toString());
        JTextField priceField = new JTextField(tableModel.getValueAt(row, 2).toString());
        JTextField quantityField = new JTextField(tableModel.getValueAt(row, 3).toString());

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("Product Name:"));
        form.add(nameField);
        form.add(new JLabel("Price (PKR):"));
        form.add(priceField);
        form.add(new JLabel("Quantity:"));
        form.add(quantityField);

        int choice = JOptionPane.showConfirmDialog(
            this, form, "Update Product ID: " + productId,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        );
        if (choice != JOptionPane.OK_OPTION) return;

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showMessage("Please enter the product name.");
            return;
        }

        try {
            double price = Double.parseDouble(priceField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());
            if (!Double.isFinite(price) || price < 0 || quantity < 0) {
                showMessage("Price and quantity cannot be negative.");
                return;
            }
            // PurchasePrice is intentionally preserved for historical profit reporting.
            String sql = "UPDATE Products SET ProductName = ?, SalePrice = ?, Quantity = ? WHERE ProductID = ?";
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setDouble(2, price);
                ps.setInt(3, quantity);
                ps.setInt(4, productId);
                ps.executeUpdate();
            }
            loadProducts(searchField.getText().trim());
            JOptionPane.showMessageDialog(this, "Product updated successfully!");
        } catch (NumberFormatException e) {
            showMessage("Please enter a valid number for price and a whole number for quantity.");
        } catch (SQLException e) {
            showError("Unable to update the product.", e);
        }
    }

    private void deleteProduct() {
        int selectedRow = productTable.getSelectedRow();

        if (selectedRow == -1) {
            showMessage("Please select a product to delete.");
            return;
        }

        int row = productTable.convertRowIndexToModel(selectedRow);
        int productId = (int) tableModel.getValueAt(row, 0);
        String productName = tableModel.getValueAt(row, 1).toString();

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this product?\n"
                + productName + " (ID: " + productId + ")",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM Products WHERE ProductID = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            int deleted = ps.executeUpdate();

            loadProducts(searchField.getText().trim());

            if (deleted > 0) {
                JOptionPane.showMessageDialog(
                    this, "Product deleted successfully!"
                );
            } else {
                showMessage("Product was not found. Please refresh the list.");
            }

        } catch (SQLException e) {
            showError("Unable to delete the product.", e);
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(
            this,
            message + "\n" + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static void loadLoginSettings() {
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement()) {
            boolean tableExists = false;
            try (ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='Users'")) {
                tableExists = rs.next();
            }

            if (tableExists) {
                boolean hasPassword = false;
                boolean hasHash = false;
                try (ResultSet rs = st.executeQuery("PRAGMA table_info(Users)")) {
                    while (rs.next()) {
                        String column = rs.getString("name");
                        if ("Password".equalsIgnoreCase(column)) hasPassword = true;
                        if ("PasswordHash".equalsIgnoreCase(column)) hasHash = true;
                    }
                }
                if (hasHash && !hasPassword) {
                    JPasswordField newPasswordField = new JPasswordField(18);
                    JPasswordField confirmPasswordField = new JPasswordField(18);
                    JPanel resetPanel = new JPanel(new GridLayout(0, 1, 5, 5));
                    resetPanel.add(new JLabel("Your database currently stores a password hash."));
                    resetPanel.add(new JLabel("Set a new password to finish converting the Users table:"));
                    resetPanel.add(new JLabel("New Password (at least 2 characters)"));
                    resetPanel.add(newPasswordField);
                    resetPanel.add(new JLabel("Confirm New Password"));
                    resetPanel.add(confirmPasswordField);
                    while (true) {
                        int choice = JOptionPane.showConfirmDialog(null, resetPanel,
                            "Convert Login Storage", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (choice != JOptionPane.OK_OPTION) { loginStorageReady = false; return; }
                        String np = new String(newPasswordField.getPassword());
                        String cp = new String(confirmPasswordField.getPassword());
                        if (np.length() < 2) {
                            JOptionPane.showMessageDialog(null, "Password must be at least 2 characters long.");
                        } else if (!np.equals(cp)) {
                            JOptionPane.showMessageDialog(null, "Passwords do not match.");
                        } else {
                            con.setAutoCommit(false);
                            try {
                                String oldUsername = "admin";
                                try (ResultSet rs = st.executeQuery("SELECT Username FROM Users WHERE UserID = 1")) {
                                    if (rs.next()) oldUsername = rs.getString("Username");
                                }
                                st.executeUpdate("ALTER TABLE Users RENAME TO Users_HashedBackup");
                                st.executeUpdate("CREATE TABLE Users (UserID INTEGER PRIMARY KEY CHECK (UserID = 1), Username TEXT NOT NULL, Password TEXT NOT NULL)");
                                try (PreparedStatement ps = con.prepareStatement("INSERT INTO Users (UserID, Username, Password) VALUES (1, ?, ?)")) {
                                    ps.setString(1, oldUsername);
                                    ps.setString(2, np);
                                    ps.executeUpdate();
                                }
                                st.executeUpdate("DROP TABLE Users_HashedBackup");
                                con.commit();
                                con.setAutoCommit(true);
                                loggedInUsername = oldUsername;
                                loginPassword = np;
                                break;
                            } catch (Exception ex) {
                                con.rollback();
                                con.setAutoCommit(true);
                                throw ex;
                            }
                        }
                    }
                } else if (!hasPassword) {
                    throw new SQLException("Users table has an unsupported structure.");
                }
            } else {
                st.executeUpdate("CREATE TABLE Users (UserID INTEGER PRIMARY KEY CHECK (UserID = 1), Username TEXT NOT NULL, Password TEXT NOT NULL)");
            }

            try (PreparedStatement ps = con.prepareStatement("SELECT Username, Password FROM Users WHERE UserID = 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    loggedInUsername = rs.getString("Username");
                    loginPassword = rs.getString("Password");
                    return;
                }
            }

            String initialUsername = "admin";
            String initialPassword = "admin123";
            if (Files.exists(LOGIN_SETTINGS_PATH)) {
                Properties old = new Properties();
                try (java.io.InputStream input = Files.newInputStream(LOGIN_SETTINGS_PATH)) {
                    old.load(input);
                    initialUsername = old.getProperty("username", "admin");
                    initialPassword = old.getProperty("password", "admin123");
                }
            }
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO Users (UserID, Username, Password) VALUES (1, ?, ?)")) {
                ps.setString(1, initialUsername);
                ps.setString(2, initialPassword);
                ps.executeUpdate();
            }
            loggedInUsername = initialUsername;
            loginPassword = initialPassword;
            Files.deleteIfExists(LOGIN_SETTINGS_PATH);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Unable to initialize login storage in the database: " + e.getMessage(),
                "Login Database Error", JOptionPane.ERROR_MESSAGE);
            loginStorageReady = false;
        }
    }

    private static boolean loginStorageReady = true;

    private static boolean verifyPassword(String password) {
        return loginPassword != null && loginPassword.equals(password);
    }

    private static boolean saveLoginSettings(String username, String password) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE Users SET Username = ?, Password = ? WHERE UserID = 1")) {
            ps.setString(1, username);
            ps.setString(2, password);
            if (ps.executeUpdate() != 1) {
                JOptionPane.showMessageDialog(null, "Login account was not found in the database.",
                    "Account Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            loggedInUsername = username;
            loginPassword = password;
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Unable to save login settings in the database: " + e.getMessage(),
                "Settings Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private static boolean saveLoginUsername(String username) {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "UPDATE Users SET Username = ? WHERE UserID = 1")) {
            ps.setString(1, username);
            if (ps.executeUpdate() != 1) return false;
            loggedInUsername = username;
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Unable to update username in the database: " + e.getMessage(),
                "Settings Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void changeUsername() {
        JPasswordField currentPasswordField = new JPasswordField(18);
        JTextField newUsernameField = new JTextField(loggedInUsername, 18);

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(new JLabel("Current Password"));
        form.add(currentPasswordField);
        form.add(new JLabel("New Username"));
        form.add(newUsernameField);

        int choice = JOptionPane.showConfirmDialog(this, form,
            "Change Username", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String currentPassword = new String(currentPasswordField.getPassword());
        String newUsername = newUsernameField.getText().trim();

        if (!verifyPassword(currentPassword)) {
            showMessage("Current password is incorrect.");
            return;
        }
        if (newUsername.isEmpty()) {
            showMessage("Username cannot be empty.");
            return;
        }

        if (saveLoginUsername(newUsername)) {
            loggedInUsername = newUsername;
            if (dashboardUsernameLabel != null) {
                dashboardUsernameLabel.setText("Logged in as: " + loggedInUsername);
            }
            showMessage("Username changed successfully.");
        }
    }

    private void changePassword() {
        JPasswordField currentPasswordField = new JPasswordField(18);
        JPasswordField newPasswordField = new JPasswordField(18);
        JPasswordField confirmPasswordField = new JPasswordField(18);

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        form.add(new JLabel("Current Password"));
        form.add(currentPasswordField);
        form.add(new JLabel("New Password (at least 2 characters)"));
        form.add(newPasswordField);
        form.add(new JLabel("Confirm New Password"));
        form.add(confirmPasswordField);

        int choice = JOptionPane.showConfirmDialog(this, form,
            "Change Password", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        String currentPassword = new String(currentPasswordField.getPassword());
        String newPassword = new String(newPasswordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!verifyPassword(currentPassword)) {
            showMessage("Current password is incorrect.");
            return;
        }
        if (newPassword.length() < 2) {
            showMessage("New password must be at least 2 characters long.");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showMessage("New password and confirmation do not match.");
            return;
        }

        if (saveLoginSettings(loggedInUsername, newPassword)) {
            showMessage("Password changed successfully. Use the new password next time you log in.");
        }
    }

    /** Small vector-drawn ForgeHold brand mark; no external image file is required. */
    private static class ForgeHoldLogo extends JPanel {
        private final int markSize;
        ForgeHoldLogo(int size) {
            markSize = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int pad = 2;
            g.setColor(new Color(215, 184, 117));
            g.fillRoundRect(pad, pad, markSize - pad * 2, markSize - pad * 2, 13, 13);
            int[] x = {markSize/2, markSize-12, markSize-17, markSize/2, 17, 12};
            int[] y = {10, markSize/2-5, markSize/2+5, markSize-10, markSize/2+5, markSize/2-5};
            g.setColor(new Color(24, 43, 70));
            g.fillPolygon(x, y, x.length);
            g.setStroke(new BasicStroke(Math.max(2f, markSize/18f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255));
            g.drawLine(markSize/2-12, markSize/2-1, markSize/2-3, markSize/2+10);
            g.drawLine(markSize/2-3, markSize/2+10, markSize/2+14, markSize/2-12);
            g.dispose();
        }
    }

    private static boolean showLoginDialog() {
        loginStorageReady = true;
        loadLoginSettings();
        if (!loginStorageReady) return false;
        JTextField usernameField = new JTextField(18);
        JPasswordField passwordField = new JPasswordField(18);

        JPanel loginPanel = new JPanel(new BorderLayout(12, 18));
        loginPanel.setBackground(new Color(245, 248, 252));
        loginPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(215, 223, 234)),
            BorderFactory.createEmptyBorder(18, 22, 20, 22)));
        JPanel branding = new JPanel();
        branding.setOpaque(false);
        branding.setLayout(new BoxLayout(branding, BoxLayout.Y_AXIS));
        ForgeHoldLogo loginLogo = new ForgeHoldLogo(76);
        loginLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel loginTitle = new JLabel("ForgeHold Nexus ERP");
        loginTitle.setFont(new Font("Segoe UI", Font.BOLD, 25));
        loginTitle.setForeground(new Color(24, 43, 70));
        loginTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tagline = new JLabel("INTELLIGENT BUSINESS MANAGEMENT");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        tagline.setForeground(new Color(153, 119, 49));
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);
        branding.add(loginLogo);
        branding.add(Box.createVerticalStrut(7));
        branding.add(loginTitle);
        branding.add(Box.createVerticalStrut(3));
        branding.add(tagline);
        JPanel fields = new JPanel(new GridLayout(0, 1, 6, 6));
        fields.setOpaque(false);
        fields.add(new JLabel("Username"));
        fields.add(usernameField);
        fields.add(new JLabel("Password"));
        fields.add(passwordField);
        loginPanel.add(branding, BorderLayout.NORTH);
        loginPanel.add(fields, BorderLayout.CENTER);
        loginPanel.setPreferredSize(new Dimension(340, 300));

        while (true) {
            int choice = JOptionPane.showConfirmDialog(null, loginPanel,
                "Login - ForgeHold Nexus ERP",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) return false;

            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if (loggedInUsername.equals(username) && verifyPassword(password)) {
                loggedInUsername = username;
                return true;
            }
            JOptionPane.showMessageDialog(null,
                "Invalid username or password. Please try again.",
                "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (!showLoginDialog()) {
                System.exit(0);
                return;
            }

            Main app = new Main();
            app.setVisible(true);
        });
    }
}
