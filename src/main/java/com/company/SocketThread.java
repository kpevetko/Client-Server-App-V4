package com.company;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;


//сделать переадресацию клиенту

public class SocketThread extends Thread {
    private Socket localSocket = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private String myName;
    private Socket fromClientSocket;
    public LinkedList<SocketThread> userList = null;


    public SocketThread(Socket fromClietnSocket, LinkedList<SocketThread> userlist, String name) {
        this.fromClientSocket = fromClietnSocket;
        this.userList = userlist;
        this.myName = name;
    }

    @Override
    public void run() {
        //автоматом закрывает ресурсы
        try {
            localSocket = fromClientSocket;
            pw = new PrintWriter(localSocket.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));
            userList.add(this);

            //пишем всем что такой-то вошел в чат
            messageUserEnter();

            //читаем сообщения от клиента пока он не скажет bye
            String userMessage = null;

            //ошибка со стороны сокета вылетала здесь (строка 44, обработка строки 47-53)
            //while ((userMessage = br.readLine()) != null) {
            while (true) {
                //это обработка ошибки со стороны сокета
                try {
                    userMessage = br.readLine();
                } catch (Exception e) {
                    closeAll();
                    break;
                }

                //сравниваем с bye, если так то выходим из цикла и закрываем соединение
                if (userMessage.equals("bye")) {

                    //тоже говорим клиенту
                    pw.println("bye");
                    //рассылаем что такой-то отключился
                    messageUserExit();
                    break;

                } else if (userMessage.equals("online")) { //выводит инфу о количестве человек в онлайне тому кто ввел Online
                    getUserList();
                } else if (userMessage.equals("users")) { //выводит список всех клиентов, даже тех кто не в онлайне (согласно БД)
                    //sendMessageToChat(userMessage);
                    System.out.println(userList.size());
                } else if (userMessage.indexOf("::") > 1) { //отправляем личное сообщение конкретному пользователю
                    sendTransMessage(userMessage);
                } else if (!userMessage.equals("")) { //отправляем месседж в чат (не через сервер, а каждому кто в онлайне)
                    sendMessageToChat(userMessage);
                } else {
                    //если пустое сообщение - ничего не пишем
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //закрывает все каналы связанные с этой нитью
    public void closeAll() throws IOException, SQLException {
        setUserOfflineFromSocket();
        userList.remove(this);
        pw.close();
        br.close();
        localSocket.close();
    }

    //получаем исходящий поток для сокетов/сокета
    public PrintWriter getPrint() {
        return pw;
    }

    //записывает все диалоги в БД
    public void writheMessageToDB(String message) throws SQLException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "INSERT INTO mylogs (messageid, messagestring) VALUES (?,?)";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        Statement statement = DBC.getStatement();
        ResultSet rs = statement.executeQuery("select * from mylogs");
        int rst = 0;
        while (rs.next()) {
            if (rs.isLast()) {
                rst = rs.getRow();
            }
        }
        preparedStatement.setInt(1, rst + 1);
        preparedStatement.setString(2, message);
        preparedStatement.execute();
    }

    //получаем время текущее
    public String getTime() {
        Date date = new Date();
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
        String time = DATE_FORMAT.format(date);
        return time;
    }

    //переадресовывает сообщение конкретному человеку в чате
    //в начале отделяем имя от фразы (подразумевается что в начале, перед :: стоит имя юзера которому хотим отправить)
    //потом, уже зная имя - пробегаемся по списку людей, находим его и передаем ему фразу
    public void sendTransMessage(String retMessage) {
        synchronized (userList) {
            int i = 1;
            String userTo = null;
            for (String retval : retMessage.split("::", 2)) {
                if (i == 1) {
                    userTo = retval;
                    i++;
                } else {
                    //добавляем проверку на не пустое сообщение
                    if (retval.equals("")) {
                        break;
                    } else {
                        for (SocketThread socket : userList) {
                            try {
                                if (socket.myName.equals(userTo)) {
                                    PrintWriter print = socket.getPrint();
                                    writheMessageToDB(getTime() + "[" + myName + "] шлет личное сообщение [" + userTo + "] :" + retval); //запись в БД
                                    print.println("[" + getTime() + " " + myName + "] шлет вам личное сообщение: " + retval);
                                    return;
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace(System.out);
                            }
                        }
                    }

                }

            }
        }
    }

    //меняем на False булевое значение в БД у юзера, но со стороны потока (для случаем аварийного отключения)
    public void setUserOfflineFromSocket() throws SQLException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "UPDATE myusers set useron = false where username like ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        synchronized (userList) {
            for (SocketThread socket : userList) {
                try {
                    preparedStatement.setString(1, socket.myName);
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }
        preparedStatement.executeUpdate();
    }

    //метод отправляющий ВСЕХ кто в онлайне в личный чат переписку
    //нужно учитывать как он работает
    //в нашей системе есть List содержащий данные о занятых сокетах, как объектах (с именами и т.д.)
    //фактически мы берем и перечисляем все сокеты которые есть в userListE и отправляем на них сообщения
    public void sendMessageToChat(String message) {
        synchronized (userList) {
            for (SocketThread socket : userList) {
                try {
                    PrintWriter print = socket.getPrint();
                    writheMessageToDB(getTime() + "[ " + myName + "] пишет в чат: " + message); //запись в БД
                    print.println("[" + getTime() + " " + myName + "] " + message);
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }
    }

    //оповещаем о том, что от сервера отключился такой-то
    //так же записывает это в БД
    public void messageUserExit() throws SQLException {
        writheMessageToDB(getTime() + " [" + myName + "] отключился от сервера");
        synchronized (userList) {
            for (SocketThread socket : userList) {
                try {
                    PrintWriter print = socket.getPrint();
                    print.println("[" + getTime() + " Сервер] " + myName + " отключился от сервера");
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }
    }

    //оповещаем о том, что от сервера отключился такой-то
    //так же записывает это в БД
    public void messageUserEnter() throws SQLException {
        writheMessageToDB(getTime() + " [" + myName + "] вошел на сервер");
        synchronized (userList) {
            for (SocketThread socket : userList) {
                try {
                    if (socket.localSocket != this.localSocket) {
                        PrintWriter print = socket.getPrint();
                        print.println("[" + getTime() + " Сервер] " + myName + " вошел на сервер");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }
    }

    //Получаем список всех людей кто онлайн на сервере
    public void getUserList() throws SQLException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "Select * from myusers where useron=true";
        Statement preparedStatement = DBC.getStatement();
        ResultSet rs = preparedStatement.executeQuery(SQL);
        pw.println("[" + getTime() + " Сервер] Пользователи онлайн:");
        while (rs.next()) {
            pw.println("[" + getTime() + " Сервер] Имя: " + rs.getString(2));
        }
    }
}
