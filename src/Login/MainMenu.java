package Login;

import Play.SnakeGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class MenuListener implements ActionListener,MouseListener
{
    MainMenu frame;
    MySQLDataBase dataBase;

    public void setFrame(MainMenu frame) {
        this.frame=frame;
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource()==frame.bt[0]) {
            SnakeGame.main(new String[]{frame.userName});    //开始游戏：接入贪吃蛇游戏中界面
        }
        if(e.getSource()==frame.bt[1]) {
            showData();
        }
        if(e.getSource()==frame.bt[2]) {
            JOptionPane.showMessageDialog(frame,"制作成员：孙凌龙、疏程飞、张泽旭\n制作时间：2026.8.24-2026.8.28","贪吃蛇游戏-主菜单界面-关于我们",JOptionPane.INFORMATION_MESSAGE);
        }
        if(e.getSource()==frame.bt[3]) {
            int n=JOptionPane.showConfirmDialog(frame,"确认退出游戏吗？","贪吃蛇游戏-主菜单界面-退出游戏",JOptionPane.YES_NO_OPTION);
            if(n==JOptionPane.YES_OPTION) System.exit(0);
        }
    }
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}

    //数据统计
    public void showData() {
        dataBase=new MySQLDataBase();
        if(!dataBase.connectionToDB()) {
            JOptionPane.showMessageDialog(frame,"数据库连接失败，请检查配置或数据库服务！","数据统计",JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(!dataBase.setStatement()) {
            JOptionPane.showMessageDialog(frame,"数据库初始化失败！","数据统计",JOptionPane.ERROR_MESSAGE);
            return;
        }
        Vector<String> columnNames=new Vector<String>();
        columnNames.add("用户名");
        columnNames.add("最大经验得分");
        columnNames.add("用时(秒)");
        columnNames.add("电话号码");
        Vector<Vector<String>> rows=dataBase.queryPlayersForTable("SELECT name,max_exp,time,phonenumber FROM table1");
        JTable table=new JTable(rows,columnNames) {
            public boolean isCellEditable(int row,int column) {
                return false;
            }
        };
        table.setRowHeight(26);
        JScrollPane scrollPane=new JScrollPane(table);
        JFrame dataFrame=new JFrame("贪吃蛇游戏——数据统计");
        dataFrame.setBounds(300,200,600,380);
        dataFrame.add(scrollPane);
        dataFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dataFrame.setVisible(true);
    }
}
public class MainMenu extends JFrame
{
    JLabel []lb=new JLabel[1];
    String []label={"主菜单界面"};
    JButton []bt=new JButton[4];
    String []button={"开始游戏","数据统计","关于我们","退出游戏"};
    JPanel []pn=new JPanel[5];
    MenuListener listener;
    String userName;

    public MainMenu(String userName) {
        this.userName=userName;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        listener=new MenuListener();
        listener.setFrame(this);
        initial();
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    public void initial() {
        setTitle("贪吃蛇游戏——主菜单界面");
        setLayout(new GridLayout(5,1));
        setBounds(250,200,420,400);

        Color bgColor=new Color(245,250,245);
        Color accentColor=new Color(76,175,80);

        for(int i=0;i<lb.length;i++) lb[i]=new JLabel(label[i]);
        for(int i=0;i<bt.length;i++) bt[i]=new JButton(button[i]);
        for(int i=0;i<pn.length;i++) pn[i]=new JPanel();

        //标题标签样式
        lb[0].setFont(new Font("微软雅黑", Font.BOLD, 26));
        lb[0].setForeground(new Color(46,125,50));

        //按钮样式
        for(int i=0;i<bt.length;i++) styleButton(bt[i], accentColor);

        for(int i=0;i<pn.length;i++) pn[i].setBackground(bgColor);

        for(int i=0;i<bt.length;i++) {
            bt[i].addActionListener(listener);
            bt[i].addMouseListener(listener);
        }

        pn[0].add(lb[0]);
        pn[1].add(bt[0]);
        pn[2].add(bt[1]);
        pn[3].add(bt[2]);
        pn[4].add(bt[3]);
        for(int i=0;i<pn.length;i++) add(pn[i]);
    }

    //按钮美化
    private void styleButton(JButton b,Color color) {
        b.setFont(new Font("微软雅黑", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(color);
        b.setContentAreaFilled(false);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(140,38));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(color.darker()); }
            public void mouseExited(MouseEvent e) { b.setBackground(color); }
        });
    }
}