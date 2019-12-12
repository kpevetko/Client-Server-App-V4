package com.company;

import java.sql.*;
import java.util.Scanner;

public class DataBase {
    //сканер не факт что нужен
    Scanner scanner = new Scanner(System.in);
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
    public static DataBase getMyDBObject() throws SQLException {
        if (myDBObject == null) {
            myDBObject = new DataBase();
        }
        return myDBObject;
    }

    public DataBase() throws SQLException {
        DB_URL = "jdbc:postgresql://localhost:5432/postgres";
        DB_USER = "postgres";
        DB_PASS = "0000";
        ConnectToDB();
    }

    //добавление нового юзера в таблицу (включая автоинкремент номера)
    //кстати автоинкремент можно было сделать поинтереснее, но такой способ тоже работает
    public void addNewUser(String name, String password) throws SQLException {
        String SQL = "INSERT INTO MYUSERS (userid,USERNAME,USERPASSWORD) VALUES (?,?,?)";
        PreparedStatement preparedStatement = connection.prepareStatement(SQL);
        Statement statement = myDBObject.getStatement();
        ResultSet rs = statement.executeQuery("select * from myusers");
        int rst = 0;
        while (rs.next()) {
            if (rs.isLast()) {
                rst = rs.getRow();
            }
        }
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
