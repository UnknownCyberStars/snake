package Play;

import Login.MySQLDataBase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 登录面板：嵌入 SnakeGame 画布内的登录界面（与游戏深色风格统一）
 * 登录成功后通过回调通知 SnakeGame 开始游戏，错误提示使用对话框或面板内红字
 */
public class LoginPanel extends JPanel {

    /** 登录成功回调 */
    public interface LoginCallback {
        void onLoginSuccess(String name);
    }

    private final LoginCallback callback;
    private final JTextField userField = new JTextField(18);
    private final JPasswordField passField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel(" ", SwingConstants.CENTER);

    private static final Color FIELD_BG = new Color(0x1f, 0x27, 0x35);
    private static final Color BORDER = new Color(0x2a, 0x35, 0x4a);
    private static final Color GREEN = new Color(0x2a, 0x8f, 0x4e);
    private static final Color GREEN_BRIGHT = new Color(0x6e, 0xf2, 0x9a);
    private static final Color GRAY_TEXT = new Color(0x8a, 0x94, 0xa3);

    public LoginPanel(LoginCallback callback) {
        this.callback = callback;
        setOpaque(false);
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(380, 440));
        buildUI();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(0x16, 0x1b, 0x27));
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g2.setColor(GREEN);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        g2.dispose();
    }

    private void buildUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 24, 6, 24);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel("贪吃蛇", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 32));
        title.setForeground(GREEN_BRIGHT);
        gbc.gridy = 0;
        add(title, gbc);

        JLabel subtitle = new JLabel("请输入账号密码登录", SwingConstants.CENTER);
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(GRAY_TEXT);
        gbc.gridy = 1;
        add(subtitle, gbc);

        gbc.gridy = 2;
        add(createFieldRow("用户名", userField), gbc);
        gbc.gridy = 3;
        add(createFieldRow("密码", passField), gbc);

        errorLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        errorLabel.setForeground(Color.RED);
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 24, 0, 24);
        add(errorLabel, gbc);

        JButton loginBtn = createButton("登录", GREEN);
        JButton exitBtn = createButton("退出", new Color(0x8a, 0x94, 0xa3));
        loginBtn.addActionListener(e -> login());
        exitBtn.addActionListener(e -> System.exit(0));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        btnRow.setOpaque(false);
        btnRow.add(loginBtn);
        btnRow.add(exitBtn);
        gbc.gridy = 5;
        gbc.insets = new Insets(10, 24, 6, 24);
        add(btnRow, gbc);

        JLabel register = new JLabel("<html><u>还没有账户？点击这里注册！</u></html>");
        register.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        register.setForeground(new Color(0x4d, 0xa3, 0xff));
        register.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        register.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { register(); }
        });
        gbc.gridy = 6;
        gbc.insets = new Insets(6, 24, 12, 24);
        add(register, gbc);

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) loginBtn.doClick();
            }
        };
        userField.addKeyListener(enter);
        passField.addKeyListener(enter);
    }

    private JPanel createFieldRow(String label, JTextField field) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        row.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        lb.setForeground(Color.WHITE);
        row.add(lb);
        field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setBackground(FIELD_BG);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        field.setPreferredSize(new Dimension(220, 32));
        row.add(field);
        return row;
    }

    private JButton createButton(String text, Color color) {
        JButton b = new JButton(text);
        b.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setOpaque(true);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(110, 34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(color.darker()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(color); }
        });
        return b;
    }

    /** 清空输入并复位错误提示 */
    public void resetFields() {
        userField.setText("");
        passField.setText("");
        errorLabel.setText(" ");
    }

    /** 请求焦点到用户名输入框 */
    public void focusField() {
        userField.requestFocusInWindow();
    }

    /** 登录入口 */
    private void login() {
        String name = userField.getText().trim();
        String password = new String(passField.getPassword());
        errorLabel.setText(" ");
        if (name.isEmpty()) {
            errorLabel.setText("用户名不能为空！");
            return;
        }
        if (password.isEmpty()) {
            errorLabel.setText("密码不能为空！");
            return;
        }
        MySQLDataBase db = new MySQLDataBase();
        if (!db.connectionToDB()) {
            JOptionPane.showMessageDialog(this, "数据库连接失败，请检查配置或数据库服务！", "登录失败", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!db.setStatement()) {
            JOptionPane.showMessageDialog(this, "数据库初始化失败！", "登录失败", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dan = "'";
        String[] result = db.queryAndGetAccount("SELECT password FROM table1 WHERE name=" + dan + name + dan);
        if (result.length == 0) {
            errorLabel.setText("该用户名不存在，请先注册！");
        } else if (result[0].equals(password)) {
            callback.onLoginSuccess(name);
        } else {
            errorLabel.setText("账号和密码不一致，请重新输入！");
        }
    }

    /** 注册（对话框形式，逻辑与原注册界面一致） */
    private void register() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        JTextField[] tf = new JTextField[2];
        JPasswordField[] pf = new JPasswordField[2];
        JLabel[] lb = new JLabel[4];
        String[] label = {"用户名：", "电话号码：", "密码：", "确认密码："};

        for (int i = 0; i < tf.length; i++) tf[i] = new JTextField(10);
        for (int i = 0; i < pf.length; i++) pf[i] = new JPasswordField(10);
        for (int i = 0; i < lb.length; i++) lb[i] = new JLabel(label[i]);

        for (JLabel l : lb) {
            l.setFont(new Font("微软雅黑", Font.BOLD, 16));
            l.setForeground(GREEN);
        }
        for (JTextField f : tf) f.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        for (JPasswordField p : pf) p.setFont(new Font("微软雅黑", Font.PLAIN, 16));

        panel.add(lb[0]); panel.add(tf[0]);
        panel.add(lb[1]); panel.add(tf[1]);
        panel.add(lb[2]); panel.add(pf[0]);
        panel.add(lb[3]); panel.add(pf[1]);
        panel.setPreferredSize(new Dimension(380, 180));

        KeyAdapter enterListener = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    Window window = SwingUtilities.getWindowAncestor(panel);
                    JButton okButton = findOkButton(window);
                    if (okButton != null) okButton.doClick();
                }
            }
        };
        tf[0].addKeyListener(enterListener);
        tf[1].addKeyListener(enterListener);
        pf[0].addKeyListener(enterListener);
        pf[1].addKeyListener(enterListener);

        while (true) {
            int result = JOptionPane.showConfirmDialog(this, panel, "贪吃蛇游戏——用户注册界面", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            String name = tf[0].getText();
            String phone = tf[1].getText();
            String password1 = new String(pf[0].getPassword());
            String password2 = new String(pf[1].getPassword());

            if (name.equals("")) {
                JOptionPane.showMessageDialog(this, "用户名不能为空！", "输入信息不合规范！", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (phone.equals("")) {
                JOptionPane.showMessageDialog(this, "电话号码不能为空！", "输入信息不合规范！", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (password1.equals("")) {
                JOptionPane.showMessageDialog(this, "密码不能为空！", "输入信息不合规范！", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if (!password1.equals(password2)) {
                JOptionPane.showMessageDialog(this, "密码和确认密码不匹配！", "输入信息不合规范！", JOptionPane.WARNING_MESSAGE);
                continue;
            }

            MySQLDataBase dataBase = new MySQLDataBase();
            if (!dataBase.connectionToDB()) {
                JOptionPane.showMessageDialog(this, "数据库连接失败，请检查配置或数据库服务！", "注册失败", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if (!dataBase.setStatement()) {
                JOptionPane.showMessageDialog(this, "数据库初始化失败！", "注册失败", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            String dan = "'";
            if (dataBase.queryAndGetAccount("SELECT name FROM table1 WHERE name=" + dan + name + dan).length > 0) {
                JOptionPane.showMessageDialog(this, "该用户名已被注册，请更换用户名！", "注册失败", JOptionPane.WARNING_MESSAGE);
                continue;
            }
            String sql = "INSERT INTO table1(name, password, max_exp, time, phonenumber) VALUES(" + dan + name + dan + "," + dan + password1 + dan + ",0,0," + dan + phone + dan + ")";
            if (dataBase.insert(sql)) {
                JOptionPane.showMessageDialog(this, "注册成功，请使用新账户登录！", "恭喜", JOptionPane.INFORMATION_MESSAGE);
                return;
            } else {
                JOptionPane.showMessageDialog(this, "注册失败，请稍后重试！", "注册失败", JOptionPane.ERROR_MESSAGE);
                continue;
            }
        }
    }

    public JButton findOkButton(Container container) {
        if (container == null) return null;
        for (Component comp : container.getComponents()) {
            if (comp instanceof JButton) return (JButton) comp;
            if (comp instanceof Container) {
                JButton b = findOkButton((Container) comp);
                if (b != null) return b;
            }
        }
        return null;
    }
}
