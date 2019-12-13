package com.company;

import javax.print.DocFlavor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

//клиент
//


import java.io.*;
import java.net.Socket;


public class Client {
    private String name;
    private String password;

    //тут блок работы с БД
    //меняем на True булевое значение в БД у юзера
    public void setUserOnline() throws SQLException, IOException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "UPDATE myusers set useron = true where username like ? and userpassword like ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        preparedStatement.setString(1, this.name);
        preparedStatement.setString(2, this.password);
        preparedStatement.executeUpdate();
    }

    //меняем на False булевое значение в БД у юзера
    public void setUserOffline() throws SQLException, IOException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "UPDATE myusers set useron = false where username like ? and userpassword like ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        preparedStatement.setString(1, this.name);
        preparedStatement.setString(2, this.password);
        preparedStatement.executeUpdate();
    }


    //проверяем есть ли такой юзер с таким паролем, если нет предлагаем создать нового
    public boolean userExistence() throws SQLException, IOException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "select * from MYUSERS where username like ? and userpassword like ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);

        Scanner scanner = new Scanner(System.in);
        String login, password, anwser;
        System.out.println("Введите логин");
        // просит вводить логин пока он не станет меньше 19 символов или пока он будет равет пустому
        // в случае если логин будет равен нулю или bye, вернет false и выйдет из системы
        login = scanner.nextLine();
        if (login == null || login.equals("bye")) {
            return false;
        }
        while (login.equals("") || login.length() > 19) {
            System.out.println("Логин не должен содержать пустое место или быть больше 19 символов");
            login = scanner.nextLine();
            if (login == null || login.equals("bye")) {
                return false;
            }
        }

        System.out.println("Введите пароль");
        password = scanner.nextLine();

        //проверяем есть ли юзер с такими параметрами
        preparedStatement.setString(1, login);
        preparedStatement.setString(2, password);
        preparedStatement.execute();
        //возвращаем список результатов с нашими параметрами
        ResultSet rs = preparedStatement.getResultSet();
        //идем до первого (подразумевается что пара логин/пароль уникальна)
        rs.next();
        //смотрим количество строк (если есть хоть одна запись - будет 1)
        if (rs.getRow() == 0) {
            //если 0 записей - говорим что пользователь или ошибся или такого нет, предлагаем создать юзера
            System.out.println("Пользователь с указанными данными не найден. \nВозможно вы ошиблись при вводе пары Логин/Пароль или такого Пользователя не существует.");
            System.out.println("Попробовать ввести снова пару Логин/Пароль или добавить нового Пользователя с укзанными параметрами?");
            System.out.println("Try - попробовать ввод снова, New - добавить нового, Иное - выход");
            anwser = scanner.nextLine();

            if (anwser.equals("Try")) {
                System.out.println("повторная попытка");
                return userExistence();
            } else if (anwser.equals("New")) {
                System.out.println("данные верны");
                this.name = login;
                this.password = password;
                DBC.addNewUser(this.name, this.password);
                setUserOnline();
                return true;
            } else {
                System.out.println("некорректные данные");
                return false;
            }

        } else {
            //если такой пользователь есть, но он уже онлайн - тогда выход
            if (rs.getBoolean(4)) {
                System.out.println("такой пользователь уже в сети");
                return false;
            } else {
                System.out.println("данные верны");
                this.name = login;
                this.password = password;
                setUserOnline();
                return true;
            }

        }

    }

    //возврат имени
    public String getName() {
        return name;
    }


    ///////до этого, работа с БД
    private Scanner scanner = new Scanner(System.in);
    private PrintWriter pw = null;
    private BufferedReader br = null;

    //нить (поток) отвечает за отправку сообещний на сервер, при написании bye - прерывается
    public void typewritingThread() {
        Thread keyThread = new Thread() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    String message = scanner.nextLine();
                    try {
                        if (message.equals("bye")) {
                            pw.println(message);
                            interrupt();
                            break;
                        } else {
                            pw.println(message);

                        }
                    } catch (Exception x) {
                        break;
                    }
                }
            }
        };
        keyThread.start();
    }

    public void startClient() throws IOException, SQLException {

        int portNumber = 1777;
        String str;
        System.out.println("Client is started");
        Socket socket = new Socket("127.0.0.1", portNumber);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);

        //подключаемся к БД
        DataBase DBC = DataBase.getMyDBObject();
        //берем Стейтмент от БД для быстрого обращения
        Statement statement = DBC.getStatement();

        //проверяем возможность входа, если все нормально - запускаем на сервер, иначе пишем серверу bye
        //пробегаем по таблице БД, ищем есть ли такой пользователь
        //если таких вообще в таблице нет, предлагаем создать нового
        if (userExistence()) {
            pw.println(this.getName());
        } else {
            System.out.println("Принято bye, null или пустая строка при вводе имени - Клиент отключен от сервера");
            br.close();
            pw.close();
            socket.close();
            return;
        }

        //запускаем нить для написания на сервер
        typewritingThread();

        //чтение сообщений в обычном режиме
        while (true) {

            //читаем с потока
            str = br.readLine();

            //Если пришел ответ Bye, оканчиваем цикл
            if (str.equals("bye")) {
                this.setUserOffline();
                break;
            }
            //печатаем ответ от сервера на консоль для проверки
            System.out.println(str);

        }

        br.close();
        pw.close();
        socket.close();

    }

    public static void main(String[] args) throws IOException, SQLException {
        Client client = new Client();
        client.startClient();

    }
}
