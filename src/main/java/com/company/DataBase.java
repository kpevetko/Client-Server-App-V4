package com.company;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;
import java.util.Properties;



public class DataBase {
    FileInputStream fileInputStream;
    //инициализируем специальный объект Properties
    Properties prop = new Properties();
    //поле коннекта
    Connection connection = null;
    //поля содержащие параметры входа (адрес, имя и пароль) для базы
    String DB_URL, DB_USER, DB_PASS;
    //
    Statement pstmt= null;
    //список результатов запроса
    ResultSet rs= null;

    //приватная статическая ссылка класса на его объект
    private static DataBase myDBObject;

    //открытый статический метод используемый для получения объекта нашего класса
    public static DataBase getMyDBObject() throws SQLException, IOException {
        if (myDBObject == null) {
            myDBObject = new DataBase();
        }
        return myDBObject;
    }

    public DataBase() throws SQLException, IOException {
        fileInputStream = new FileInputStream("src/main/resources/ServerConfig.properties");
        prop.load(fileInputStream);
        DB_URL = prop.getProperty("URL");
        DB_USER = prop.getProperty("LOGIN");
        DB_PASS = prop.getProperty("PASSWORD");
        ConnectToDB();
    }

    //добавление нового юзера в таблицу (включая автоинкремент номера)
    //кстати автоинкремент можно было сделать поинтереснее, но такой способ тоже работает
    public void addNewUser(String name, String password) throws SQLException {
        String SQL = "INSERT INTO MYUSERS (userid,USERNAME,USERPASSWORD) VALUES (?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(SQL);
        Statement statement = myDBObject.getStatement();
        ResultSet rs = statement.executeQuery("select max(userid) from myusers");
        rs.next();
        int rst = rs.getInt(1);;

        preparedStatement.setInt(1, rst + 1);
        preparedStatement.setString(2, name);
        preparedStatement.setString(3, password);
        preparedStatement.executeUpdate();
    }

    //коннектимся к БД
    public void ConnectToDB() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("NO");
            e.printStackTrace();
            return;
        }
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            pstmt = connection.createStatement();
            System.out.println("Connect to DB is done.");
        } catch (SQLException e) {
            System.out.println("NOT CONNECT");
            e.printStackTrace();
            return;
        }
    }

    //быстро получаем стейтмент для формирования запросов SQL
    public Statement getStatement() throws SQLException {
        return connection.createStatement();
    }

    //смотрим список юзеров (всех)
    public void getUserListDB() throws SQLException {
        rs = pstmt.executeQuery("Select * from myUsers");
        while (rs.next()) {
            System.out.printf(rs.getString(1) + " " + rs.getString(2));

        }
    }



}
