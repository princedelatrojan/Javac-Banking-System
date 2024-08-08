import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class AdminPanel extends JFrame {
    private JButton viewUsersButton, viewTransactionsButton, logoutButton;
    private JTable table;
    private JScrollPane scrollPane;

    public AdminPanel() {
        setTitle("Bank System - Admin Panel");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        viewUsersButton = new JButton("View Users");
        viewTransactionsButton = new JButton("View Transactions");
        logoutButton = new JButton("Logout");

        JPanel panel = new JPanel();
        panel.add(viewUsersButton);
        panel.add(viewTransactionsButton);
        panel.add(logoutButton);

        table = new JTable();
        scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(750, 400));

        viewUsersButton.addActionListener(e -> viewUsers());
        viewTransactionsButton.addActionListener(e -> viewTransactions());
        logoutButton.addActionListener(e -> logout());

        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private void viewUsers() {
        String query = "SELECT id, full_name, username, balance FROM users";
        updateTable(query, new String[]{"ID", "Full Name", "Username", "Balance"});
    }

    private void viewTransactions() {
        String query = "SELECT transaction_id, user_id, type, amount, transaction_date FROM transactions";
        updateTable(query, new String[]{"Transaction ID", "User ID", "Type", "Amount", "Transaction Date"});
    }

    private void updateTable(String query, String[] columnNames) {
        try (Connection con = DatabaseConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            DefaultTableModel model = new DefaultTableModel();
            for (String columnName : columnNames) {
                model.addColumn(columnName);
            }

            while (rs.next()) {
                if (query.contains("transactions")) {
                    Timestamp transactionDate = rs.getTimestamp("transaction_date");
                    model.addRow(new Object[]{rs.getInt("transaction_id"), rs.getInt("user_id"), rs.getString("type"), rs.getDouble("amount"), transactionDate});
                } else {
                    model.addRow(new Object[]{rs.getInt("id"), rs.getString("full_name"), rs.getString("username"), rs.getDouble("balance")});
                }
            }

            table.setModel(model);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void logout() {
        new UserLogin().setVisible(true);
        dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdminPanel::new);
    }
}
