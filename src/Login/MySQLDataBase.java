package Login;

import java.sql.*;
import java.io.*;
import java.util.*;

public class MySQLDataBase
{
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private String url;            // 数据库连接地址
    private String user;           // 用户名
    private String password;       // 密码

    //构造方法的作用是调用加载 src/db.properties 配置文件的loadConfig()方法
    //loadConfig()方法从 src/db.properties 读取MySQL数据库的连接参数
    public MySQLDataBase() {
        loadConfig();
    }
    private void loadConfig() {
        try (InputStream input = getClass().getResourceAsStream("/db.properties")) {
            Properties props = new Properties();
            props.load(input);

            // 读取配置项（如果缺失则使用默认值）
            String host = props.getProperty("db.host");
            String port = props.getProperty("db.port");
            String dbName = props.getProperty("db.name");
            this.user = props.getProperty("db.user");
            this.password = props.getProperty("db.password");

            //拼接 MySQL 连接 URL（带时区和安全参数）
            this.url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
            System.out.println("配置文件加载成功，目标库：" + dbName);
        } catch (Exception e) {
            System.out.println("加载 db.properties 失败，请检查 src 目录下是否存在该文件");
            e.printStackTrace();
        }
    }

    //以下的内容和Access数据库相关类里的内容相似
    public boolean connectionToDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection=DriverManager.getConnection(url,user,password);
            if(connection!=null) {
                System.out.println("connectionToDB成功");
                return true;
            } else {
                System.out.println("connectionToDB失败");
                return false;
            }
        } catch (Exception e) {
            System.out.println("connectionToDB异常");
            e.printStackTrace();
            return false;
        }
    }

    public boolean setStatement() {
        try {
            statement=connection.createStatement();
            if(statement!=null) {
                System.out.println("setStatement成功");
                return true;
            } else {
                System.out.println("setStatement失败");
                return false;
            }
        } catch(Exception e) {
            System.out.println("setStatement异常");
            e.printStackTrace();
            return false;
        }
    }

    public boolean query(String sql) {
        try {
            resultSet=statement.executeQuery(sql);
            if(resultSet.next()) {
                System.out.println("查询操作成功");
                return true;
            } else {
                System.out.println("查询操作失败："+sql);
                return false;
            }
        } catch (Exception e) {
            System.out.println("查询操作异常："+sql);
            return false;
        }
    }

    public String[] queryAndGetAccount(String sql) {
        StringBuilder sb=new StringBuilder();
        try {
            resultSet=statement.executeQuery(sql);
            while(resultSet.next()) {
                sb.append(resultSet.getString(1)).append(";");
            }
            if(sb.length()==0) {
                return new String[0];
            }
            return sb.toString().split(";");
        } catch (Exception e) {
            System.out.println("查询操作异常："+sql);
            return new String[0];
        }
    }

    public String[] queryAndGetData(String sql) {
        StringBuilder sb=new StringBuilder();
        try {
            resultSet=statement.executeQuery(sql);
            ResultSetMetaData metaData=resultSet.getMetaData();
            int columnCount=metaData.getColumnCount();
            while(resultSet.next()) {
                for (int i=2;i<=columnCount;i++) {
                    sb.append(resultSet.getString(i)).append(";");
                }
            }
            if(sb.length()==0) {
                return new String[0];
            }
            return sb.toString().split(";");
        } catch (Exception e) {
            System.out.println("查询操作异常：" + sql);
            return new String[0];
        }
    }

    public String queryData(String sql) {
        StringBuilder sb=new StringBuilder();
        try {
            resultSet=statement.executeQuery(sql);
            //表头
            sb.append(String.format("%-8s\t%-12s\t%-12s\t%-10s\t%-10s\t%-20s\t%-15s\n", "序号", "用户名", "密码", "最高经验", "颜色", "得分时间", "电话号码"));
            while (resultSet.next()) {
                int number=resultSet.getInt("number");
                String name=resultSet.getString("name");
                String password=resultSet.getString("password");
                int maxExp=resultSet.getInt("max_exp");
                String maxColor=resultSet.getString("max_color");
                String time=resultSet.getString("time");
                String phonenumber=resultSet.getString("phonenumber");
                sb.append(String.format("%-8s\t%-12s\t%-12s\t%-10s\t%-10s\t%-20s\t%-15s\n", number, name, password, maxExp, maxColor, time, phonenumber));
            }
        } catch(Exception e) {
            System.out.println("查询操作异常："+sql);
            return "查询出现异常";
        }
        return sb.toString();
    }

    public void delete(String sql) {
        try {
            int n=statement.executeUpdate(sql);
            if(n!=0) {
                System.out.println("删除操作成功");
            } else {
                System.out.println("删除操作失败：" + sql);
            }
        } catch (Exception e) {
            System.out.println("删除操作异常：" + sql);
        }
    }

    public boolean update(String sql) {
        try{
            int n=statement.executeUpdate(sql);
            if(n!=0) {
                System.out.println("更新操作成功");
                return true;
            } else {
                System.out.println("更新操作失败："+sql);
                return false;
            }
        } catch(Exception e){
            System.out.println("更新操作异常："+sql);
            return false;
        }
    }

    public boolean insert(String sql) {
        try{
            int n=statement.executeUpdate(sql);
            if(n!=0) {
                System.out.println("insert成功");
                return true;
            } else {
                System.out.println("insert失败");
                return false;
            }
        } catch(Exception e){
            System.out.println("插入操作异常："+sql);
            e.printStackTrace();
            return false;
        }
    }
}