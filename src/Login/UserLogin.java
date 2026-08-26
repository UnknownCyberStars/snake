package Login;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

//class MyListener implements ActionListener,KeyListener
//{
//    UserLogin frame;
//    MySQLDataBase dataBase;
//
//    public void setFrame(UserLogin frame) {
//        this.frame=frame;
//    }
//    public void actionPerformed(ActionEvent e) {
//        if(e.getSource()==frame.bt2)
//            frame.dispose();
//        if(e.getSource()==frame.bt1) {
//            connectToDB();
//            frame.dispose();
//        }
//    }
//    public void connectToDB() {
//        dataBase=new AccessDataBase();
//        String dan="'",dou=",";
//        String sqlQuery="select number,password from ASD_example14.userTable where number=";
//        sqlQuery+=dan+frame.tf[0].getText()+dan+" and "+"password="+dan+frame.tf[1].getText()+dan;
//        dataBase.connectionToDB();
//        dataBase.setStatement();
//        if(!dataBase.query(sqlQuery))
//            JOptionPane.showMessageDialog(frame,"账号和口令不一致，请重写输入！","警告",JOptionPane.INFORMATION_MESSAGE);
//        else {
//            JOptionPane.showMessageDialog(frame,"验证通过，登录成功！","恭喜",JOptionPane.INFORMATION_MESSAGE);
//            DataBaseApplication mainFrame=new DataBaseApplication();
//            for(int i=2;i<5;i++) {
//                mainFrame.menuItem[i].setEnabled(true);
//                mainFrame.enableUserMenus();
//            }
//        }
//    }
//    public void keyPressed(KeyEvent e) {
//        if(e.getSource()==frame.tf[1] && e.getKeyCode()==KeyEvent.VK_ENTER)
//            frame.bt1.doClick();
//    }
//    public void keyTyped(KeyEvent e) {
//    }
//    public void keyReleased(KeyEvent e) {
//    }
//}
public class UserLogin extends JFrame
{
    JLabel []lb=new JLabel[6];
    String []label={"欢迎来到——","贪吃蛇游戏的世界！","欢迎您游玩本游戏，请输入您的用户名与密码进行登录！","用户名：","密码：","还没有账户？点击本行文字立即注册！"};
    JTextField []tf=new JTextField[2];
    JButton []bt=new JButton[2];
    String []button={"登录！","退出~"};
    JPanel []pn=new JPanel[7];
//    MyListener listener;

    public UserLogin() {
//        listener=new MyListener();
//        listener.setFrame(this);
        initial();
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    public void initial() {
        setTitle("贪吃蛇游戏——用户登录页面");
        setLayout(new GridLayout(7,1));
        setBounds(200,200,350,300);

        for(int i=0;i<lb.length;i++) lb[i]=new JLabel(label[i]);
        for(int i=0;i<tf.length;i++) tf[i]=new JTextField(20);
        for(int i=0;i<bt.length;i++) bt[i]=new JButton(button[i]);
        for(int i=0;i<pn.length;i++) pn[i]=new JPanel();

//        for(int i=0;i<bt.length;i++) bt[i].addActionListener(listener);
//        lb[5].addKeyListener(listener);


        for(int i=0;i<5;i++) pn[i].add(lb[i]);
        pn[3].add(tf[0]);
        pn[4].add(tf[1]);
        for(int i=0;i<button.length;i++) pn[5].add(bt[i]);
        pn[6].add(lb[label.length-1]);
        for(int i=0;i<pn.length;i++) add(pn[i]);
    }
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new UserLogin();
    }
}