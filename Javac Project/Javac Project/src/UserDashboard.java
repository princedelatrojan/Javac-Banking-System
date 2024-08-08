import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserDashboard extends JFrame {
    private int userId;
    private String fullName;

    public UserDashboard(int userId, String fullName) {
        this.userId = userId;
        this.fullName = fullName;

        setTitle("Bank System - User Dashboard");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 1));
        panel.add(new JLabel("Welcome, " + fullName));

        JButton viewBalanceButton = new JButton("View Balance");
        viewBalanceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                viewBalance();
            }
        });
        panel.add(viewBalanceButton);

        JButton depositMoneyButton = new JButton("Deposit Money");
        depositMoneyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                depositMoney();
            }
        });
        panel.add(depositMoneyButton);

        JButton withdrawMoneyButton = new JButton("Withdraw Money");
        withdrawMoneyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                withdrawMoney();
            }
        });
        panel.add(withdrawMoneyButton);

        JButton transferMoneyButton = new JButton("Transfer Money");
        transferMoneyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                transferMoney();
            }
        });
        panel.add(transferMoneyButton);

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logout();
            }
        });
        panel.add(logoutButton);

        add(panel);
        setVisible(true);
    }

    private void viewBalance() {
        try (Connection con = DatabaseConnection.getConnection()) {
            String query = "SELECT balance FROM users WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                JOptionPane.showMessageDialog(this, "Your balance is: $" + balance);
            } else {
                JOptionPane.showMessageDialog(this, "User not found.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void depositMoney() {
        String amountStr = JOptionPane.showInputDialog(this, "Enter amount to deposit:");
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Invalid amount.");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                con.setAutoCommit(false);

                // Update balance
                String query = "UPDATE users SET balance = balance + ? WHERE id = ?";
                PreparedStatement ps = con.prepareStatement(query);
                ps.setDouble(1, amount);
                ps.setInt(2, userId);
                int result = ps.executeUpdate();

                if (result > 0) {
                    // Insert transaction record
                    query = "INSERT INTO transactions (user_id, type, amount) VALUES (?, ?, ?)";
                    ps = con.prepareStatement(query);
                    ps.setInt(1, userId);
                    ps.setString(2, "deposit");
                    ps.setDouble(3, amount);
                    ps.executeUpdate();

                    con.commit();
                    JOptionPane.showMessageDialog(this, "Deposit successful.");
                } else {
                    con.rollback();
                    JOptionPane.showMessageDialog(this, "Deposit failed.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid number.");
        }
    }

    private void withdrawMoney() {
        String amountStr = JOptionPane.showInputDialog(this, "Enter amount to withdraw:");
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Invalid amount.");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                con.setAutoCommit(false);

                // Check if user has enough balance
                String query = "SELECT balance FROM users WHERE id = ?";
                PreparedStatement ps = con.prepareStatement(query);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double currentBalance = rs.getDouble("balance");
                    if (currentBalance < amount) {
                        JOptionPane.showMessageDialog(this, "Insufficient balance.");
                        return;
                    }
                }

                // Update balance
                query = "UPDATE users SET balance = balance - ? WHERE id = ?";
                ps = con.prepareStatement(query);
                ps.setDouble(1, amount);
                ps.setInt(2, userId);
                int result = ps.executeUpdate();

                if (result > 0) {
                    // Insert transaction record
                    query = "INSERT INTO transactions (user_id, type, amount) VALUES (?, ?, ?)";
                    ps = con.prepareStatement(query);
                    ps.setInt(1, userId);
                    ps.setString(2, "withdraw");
                    ps.setDouble(3, amount);
                    ps.executeUpdate();

                    con.commit();
                    JOptionPane.showMessageDialog(this, "Withdrawal successful.");
                } else {
                    con.rollback();
                    JOptionPane.showMessageDialog(this, "Withdrawal failed.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter a valid number.");
        }
    }

    private void transferMoney() {
        String recipientIdStr = JOptionPane.showInputDialog(this, "Enter recipient user ID:");
        String amountStr = JOptionPane.showInputDialog(this, "Enter amount to transfer:");
        try {
            int recipientId = Integer.parseInt(recipientIdStr);
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                JOptionPane.showMessageDialog(this, "Invalid amount.");
                return;
            }
            try (Connection con = DatabaseConnection.getConnection()) {
                con.setAutoCommit(false);

                // Check if user has enough balance
                String query = "SELECT balance FROM users WHERE id = ?";
                PreparedStatement ps = con.prepareStatement(query);
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double currentBalance = rs.getDouble("balance");
                    if (currentBalance < amount) {
                        JOptionPane.showMessageDialog(this, "Insufficient balance.");
                        return;
                    }
                }

                // Deduct amount from sender
                query = "UPDATE users SET balance = balance - ? WHERE id = ?";
                ps = con.prepareStatement(query);
                ps.setDouble(1, amount);
                ps.setInt(2, userId);
                ps.executeUpdate();

                // Add amount to recipient
                query = "UPDATE users SET balance = balance + ? WHERE id = ?";
                ps = con.prepareStatement(query);
                ps.setDouble(1, amount);
                ps.setInt(2, recipientId);
                ps.executeUpdate();

                // Insert transaction record for sender
                query = "INSERT INTO transactions (user_id, type, amount) VALUES (?, ?, ?)";
                ps = con.prepareStatement(query);
                ps.setInt(1, userId);
                ps.setString(2, "transfer");
                ps.setDouble(3, -amount);  // Negative amount for sender
                ps.executeUpdate();

                // Insert transaction record for recipient
                query = "INSERT INTO transactions (user_id, type, amount) VALUES (?, ?, ?)";
                ps = con.prepareStatement(query);
                ps.setInt(1, recipientId);
                ps.setString(2, "transfer");
                ps.setDouble(3, amount);  // Positive amount for recipient
                ps.executeUpdate();

                con.commit();
                JOptionPane.showMessageDialog(this, "Transfer successful.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid input. Please enter valid numbers.");
        }
    }

    private void logout() {
        JOptionPane.showMessageDialog(this, "Logout clicked.");
        dispose();
        new UserLogin();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserDashboard(1, "John Doe"));
    }
}
