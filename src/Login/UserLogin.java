package Login;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class MyListener implements ActionListener,KeyListener,MouseListener
{
    UserLogin frame;
    MySQLDataBase dataBase;

    public void setFrame(UserLogin frame) {
        this.frame=frame;
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==frame.bt[1]) {
            System.exit(0);
        }
        if(e.getSource()==frame.bt[0]) {
            String name=frame.tf[0].getText();
            String password=new String(frame.pf.getPassword());
            checkTextfield(name,3);
            checkTextfield(password,4);
            if(!name.equals("") && !password.equals("")) {
                login(name,password);
            }
        }
    }
    public void keyPressed(KeyEvent e) {
        if((e.getSource()==frame.tf[0] || e.getSource()==frame.pf) && e.getKeyCode()==KeyEvent.VK_ENTER) {
            frame.bt[0].doClick();
        }
    }
    public void keyTyped(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}
    public void mouseClicked(MouseEvent e) {
        if(e.getSource()==frame.lb[5]) {
            register();
        }
    }
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    public void checkTextfield(String s,int x) {
        JLabel lb;
        if(x==3) {
            lb=new JLabel("用户名不能为空！");
        } else {
            lb=new JLabel("密码不能为空！");
        }
        lb.setFont(new Font("微软雅黑", Font.BOLD, 13));
        lb.setForeground(Color.RED);
        checkErrorLabel(x);    //如果之前有错误标签，则先删除
        if(s.equals("")) {
            GridBagConstraints g=new GridBagConstraints();
            g.gridx=2;
            g.gridy=0;
            frame.pn[x].add(lb,g);
        }
        frame.pn[x].revalidate();
        frame.pn[x].repaint();
    }

    public void checkErrorLabel(int j) {
        for (int i=frame.pn[j].getComponentCount()-1;i>=0;i--) {    //遍历组件
            Component comp=frame.pn[j].getComponent(i);
            if (comp instanceof JLabel && comp.getForeground().equals(Color.RED)) {
                frame.pn[j].remove(i);
            }
        }
    }

    public void register() {
        JPanel panel=new JPanel(new GridLayout(4,2,8,8));
        JTextField []tf=new JTextField[2];
        JPasswordField []pf=new JPasswordField[2];
        JLabel []lb=new JLabel[4];
        String []label=new String[]{"用户名：","电话号码：","密码：","确认密码："};

        for(int i=0;i<tf.length;i++) tf[i]=new JTextField(10);
        for(int i=0;i<pf.length;i++) pf[i]=new JPasswordField(10);
        for(int i=0;i<lb.length;i++) lb[i]=new JLabel(label[i]);

        for(JLabel l:lb) {
            l.setFont(new Font("微软雅黑", Font.BOLD, 16));
            l.setForeground(new Color(46,125,50));
        }
        for(JTextField f:tf) f.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        for(JPasswordField p:pf) p.setFont(new Font("微软雅黑", Font.PLAIN, 16));

        panel.add(lb[0]);panel.add(tf[0]);
        panel.add(lb[1]);panel.add(tf[1]);
        panel.add(lb[2]);panel.add(pf[0]);
        panel.add(lb[3]);panel.add(pf[1]);
        panel.setPreferredSize(new Dimension(380, 180));

        //循环出现注册对话框
        while(true) {
            int result=JOptionPane.showConfirmDialog(frame,panel,"贪吃蛇游戏——用户注册界面",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(result!=JOptionPane.OK_OPTION) {
                return;
            }
            String name=tf[0].getText();
            String phone=tf[1].getText();
            String password1=new String(pf[0].getPassword());
            String password2=new String(pf[1].getPassword());

            if(name.equals("")) {
                JOptionPane.showMessageDialog(frame,"用户名不能为空！","输入信息不合规范！",JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if(phone.equals("")) {
                JOptionPane.showMessageDialog(frame,"电话号码不能为空！","输入信息不合规范！",JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if(password1.equals("")) {
                JOptionPane.showMessageDialog(frame,"密码不能为空！","输入信息不合规范！",JOptionPane.WARNING_MESSAGE);
                continue;
            }
            if(!password1.equals(password2)) {
                JOptionPane.showMessageDialog(frame,"密码和确认密码不匹配！","输入信息不合规范！",JOptionPane.WARNING_MESSAGE);
                continue;
            }

            dataBase=new MySQLDataBase();
            if(!dataBase.connectionToDB()) {
                JOptionPane.showMessageDialog(frame,"数据库连接失败，请检查配置或数据库服务！","注册失败",JOptionPane.ERROR_MESSAGE);
                continue;
            }
            if(!dataBase.setStatement()) {
                JOptionPane.showMessageDialog(frame,"数据库初始化失败！","注册失败",JOptionPane.ERROR_MESSAGE);
                continue;
            }
            String dan="'";
            if(dataBase.queryAndGetAccount("SELECT name FROM table1 WHERE name="+dan+name+dan).length>0) {
                JOptionPane.showMessageDialog(frame,"该用户名已被注册，请更换用户名！","注册失败",JOptionPane.WARNING_MESSAGE);
                continue;
            }
            String sql="INSERT INTO table1(name, password, max_exp, time, phonenumber) VALUES(" +dan+name+dan+","+dan+password1+dan+",0,0,"+dan+phone+dan+")";
            if(dataBase.insert(sql)) {
                JOptionPane.showMessageDialog(frame,"注册成功，请使用新账户登录！","恭喜",JOptionPane.INFORMATION_MESSAGE);
                return;
            } else JOptionPane.showMessageDialog(frame,"注册失败，请稍后重试！","注册失败",JOptionPane.ERROR_MESSAGE);
        }
    }

    public void login(String name,String password) {
        dataBase=new MySQLDataBase();
        if(!dataBase.connectionToDB()) {
            JOptionPane.showMessageDialog(frame,"数据库连接失败，请检查配置或数据库服务！","登录失败",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(!dataBase.setStatement()) {
            JOptionPane.showMessageDialog(frame,"数据库初始化失败！","登录失败",JOptionPane.ERROR_MESSAGE);
            return;
        }
        String dan="'";
        String sql="SELECT password FROM table1 WHERE name="+dan+name+dan;
        String[] result=dataBase.queryAndGetAccount(sql);
        if(result.length==0) {
            JOptionPane.showMessageDialog(frame,"该用户名不存在，请先注册！","登录失败",JOptionPane.WARNING_MESSAGE);
        } else if(result[0].equals(password)) {
            JOptionPane.showMessageDialog(frame,"验证通过，登录成功！","恭喜",JOptionPane.INFORMATION_MESSAGE);
            frame.dispose();
            new MainMenu(name);
        } else {
            JOptionPane.showMessageDialog(frame,"账号和密码不一致，请重新输入！","登录失败",JOptionPane.WARNING_MESSAGE);
        }
    }
}
public class UserLogin extends JFrame
{
    JLabel []lb=new JLabel[6];
    String []label={"欢迎来到——","贪吃蛇游戏的世界！","欢迎您游玩本游戏，请输入您的用户名与密码进行登录！","用户名：","密码：","还没有账户？点击这里立即注册！"};
    JTextField []tf=new JTextField[1];
    JPasswordField pf;
    JButton []bt=new JButton[2];
    String []button={"登录！","退出~"};
    JPanel []pn=new JPanel[7];
    MyListener listener;

    public UserLogin() {
        listener=new MyListener();
        listener.setFrame(this);
        initial();
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    public void initial() {
        setTitle("贪吃蛇游戏——用户登录页面");
        setLayout(new GridLayout(7,1));
        setBounds(250,200,460,400);

        Color bgColor=new Color(40,44,52);
        Color titleColor=new Color(46,125,50);
        Color accentColor=new Color(76,175,80);
        Color textColor=new Color(90,90,90);

        for(int i=0;i<lb.length;i++) lb[i]=new JLabel(label[i]);
        for(int i=0;i<tf.length;i++) tf[i]=new JTextField(20);
        pf=new JPasswordField(20);
        for(int i=0;i<bt.length;i++) bt[i]=new JButton(button[i]);
        for(int i=0;i<pn.length;i++) pn[i]=new JPanel();

        //各标签
        lb[0].setFont(new Font("微软雅黑", Font.BOLD, 26));
        lb[0].setForeground(titleColor);
        lb[1].setFont(new Font("微软雅黑", Font.BOLD, 22));
        lb[1].setForeground(accentColor);
        lb[2].setFont(new Font("微软雅黑", Font.PLAIN, 14));
        lb[2].setForeground(textColor);
        lb[3].setFont(new Font("微软雅黑", Font.BOLD, 15));
        lb[3].setForeground(textColor);
        lb[4].setFont(new Font("微软雅黑", Font.BOLD, 15));
        lb[4].setForeground(textColor);
        lb[5].setText("<html><u>"+label[5]+"</u></html>");
        lb[5].setFont(new Font("微软雅黑", Font.BOLD, 13));
        lb[5].setForeground(new Color(21,101,192));
        lb[5].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lb[5].setToolTipText("点击注册新账户");

        //输入框
        for(int i=0;i<tf.length;i++) {
            tf[i].setFont(new Font("微软雅黑", Font.PLAIN, 14));
            tf[i].setPreferredSize(new Dimension(180,30));
            tf[i].setMinimumSize(new Dimension(80,30));
        }
        pf.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        pf.setPreferredSize(new Dimension(180,30));
        pf.setMinimumSize(new Dimension(80,30));

        //按钮
        styleButton(bt[0], accentColor);
        styleButton(bt[1], new Color(158,158,158));

        //面板
        for(int i=0;i<pn.length;i++) pn[i].setBackground(bgColor);
        pn[0].setLayout(new FlowLayout(FlowLayout.LEFT, 50, 8));
        pn[1].setLayout(new FlowLayout(FlowLayout.RIGHT, 40, 8));

        for(int i=0;i<bt.length;i++) bt[i].addActionListener(listener);
        lb[5].addMouseListener(listener);
        tf[0].addKeyListener(listener);
        pf.addKeyListener(listener);
        pn[0].add(lb[0]);
        pn[1].add(lb[1]);
        pn[2].add(lb[2]);

        GridBagConstraints gbc=new GridBagConstraints();
        gbc.gridy=0;
        pn[3].setLayout(new GridBagLayout());
        gbc.gridx=0; gbc.fill=GridBagConstraints.NONE; gbc.weightx=0; gbc.insets=new Insets(0,0,0,0);
        pn[3].add(lb[3],gbc);
        gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1.0; gbc.insets=new Insets(0,5,0,5);
        pn[3].add(tf[0],gbc);
        pn[4].setLayout(new GridBagLayout());
        gbc.gridx=0; gbc.fill=GridBagConstraints.NONE; gbc.weightx=0; gbc.insets=new Insets(0,0,0,0);
        pn[4].add(lb[4],gbc);
        gbc.gridx=1; gbc.fill=GridBagConstraints.HORIZONTAL; gbc.weightx=1.0; gbc.insets=new Insets(0,5,0,5);
        pn[4].add(pf,gbc);
        for(int i=0;i<button.length;i++) pn[5].add(bt[i]);
        pn[6].add(lb[label.length-1]);
        for(int i=0;i<pn.length;i++) add(pn[i]);
    }

    private void styleButton(JButton b,Color color) {
        b.setFont(new Font("微软雅黑", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(110,34));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { b.setBackground(color); }
        });
    }
    public static void main(String[] args) {
        //登录界面已整合进游戏主界面(单窗口闭环)，此处直接启动游戏
        Play.SnakeGame.main(new String[0]);
    }
}