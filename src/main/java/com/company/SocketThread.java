package com.company;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

public class SocketThread extends Thread {
    private Socket localSocket = null;
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private String myName;
    private Socket fromClientSocket;
    public LinkedList<SocketThread> userList = null;

    public SocketThread(Socket fromClietnSocket, LinkedList<SocketThread> userlist) {
        this.fromClientSocket = fromClietnSocket;
        this.userList = userlist;
    }

    @Override
    public void run() {
        //автоматом закрывает ресурсы
        try {
            localSocket = fromClientSocket;
            pw = new PrintWriter(localSocket.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(localSocket.getInputStream()));

            //это обработка ошибки со стороны сокета
            //при входе на сервер клиента
            try {
                if (userExistence()) {
                    pw.println("Подключено");

                    userList.add(this);
                } else {
                    System.out.println("Принято bye, null или пустая строка при вводе имени - Клиент отключен от сервера");
                    pw.println("bye");
                    br.close();
                    pw.close();
                    localSocket.close();
                    return;
                }
            } catch (Exception e) {
                pw.println("bye");
                pw.close();
                br.close();
                localSocket.close();
                return;
            }
            //пишем всем что такой-то вошел в чат
            messageUserEnter();

            //читаем сообщения от клиента пока он не скажет bye
            String userMessage = null;

            while (true) {
                //это обработка ошибки со стороны сокета
                //во время работы с клиентом
                try {
                    userMessage = br.readLine();
                } catch (Exception e) {
                    messageUserExit();
                    closeAll();
                    break;
                }

                //сравниваем с bye, если так то выходим из цикла и закрываем соединение
                if (userMessage.equals("bye")) {

                    //тоже говорим клиенту
                    pw.println("bye");
                    //рассылаем что такой-то отключился
                    messageUserExit();
                    setUserOffline();
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

    //тут блок работы с БД
    //меняем на True булевое значение в БД у юзера
    public void setUserOnline() throws SQLException, IOException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "UPDATE myusers set useron = true where username = ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        preparedStatement.setString(1, this.myName);
        preparedStatement.executeUpdate();
    }

    //проверяем существует ли пользователь с введенными данными
    //так же проверяет есть ли пользователь с таким логином для входа
    //параллельно, в случае если не существует предлагает добавить нового
    //в случае если пользователь есть, но он уже онлайн запрещает вход
    public boolean userExistence() throws SQLException, IOException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "select * from MYUSERS where username = ? and userpassword = ?";
        String SQLuser = "select * from MYUSERS where username = ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        PreparedStatement preparedStatementUser = DBC.connection.prepareStatement(SQLuser);

        String login, password, anwser;
        boolean userExist = false;
        pw.println("Введите логин");
        // просит вводить логин пока он не станет меньше или не будет равен 20 символам или пока он будет равет пустому
        // в случае если логин будет равен нулю или bye, вернет false и выйдет из системы
        login = br.readLine();
        if (login == null || login.equals("bye")) {
            return false;
        }

        while (login.equals("") || login.length() >= 20) {
            pw.println("Логин не должен содержать пустое место или быть больше 20 символов\nПовторите ввод логина");
            login = br.readLine();
            if (login == null || login.equals("bye")) {
                return false;
            }
        }

        //блок проверки оригинальности
        preparedStatementUser.setString(1, login);
        preparedStatementUser.execute();
        ResultSet rsUser = preparedStatementUser.getResultSet();
        rsUser.next();

        if (rsUser.getRow() != 0) {
            userExist = true;
        } else {
            userExist = false;
        }
        //окончание блока проверки оригинальности

        pw.println("Введите пароль");
        password = br.readLine();

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
            pw.println("Пользователь с указанными данными не найден. \nВозможно вы ошиблись при вводе пары Логин/Пароль или такого Пользователя не существует.");
            pw.println("Попробовать ввести снова пару Логин/Пароль или добавить нового Пользователя с укзанными параметрами?");
            pw.println("Try - попробовать ввод снова, New - добавить нового, Иное - выход");
            anwser = br.readLine();

            if (anwser.equals("Try")) {
                pw.println("повторная попытка");
                return userExistence();
            } else if (anwser.equals("New")) {
                if (userExist) {
                    pw.println("Пользователь с таким именем уже существует");
                    return false;
                } else {
                    pw.println("новые данные");
                    this.myName = login;
                    DBC.addNewUser(login, password);
                    setUserOnline();
                    return true;
                }

            } else {
                pw.println("некорректные данные");
                pw.println("bye");

                return false;
            }

        } else {
            //если такой пользователь есть, но он уже онлайн - тогда выход
            if (rs.getBoolean(4)) {
                pw.println("такой пользователь уже в сети");
                return false;
            } else {
                pw.println("данные верны");
                this.myName = login;
                setUserOnline();
                return true;
            }

        }

    }

    //меняем на False булевое значение в БД у юзера
    //5 ветка - переработал, теперь отключение в БД будет только по имени (запретить создавать пользователей с одинаковыми именами)
    public void setUserOffline() throws SQLException, IOException {
        DataBase DBC = DataBase.getMyDBObject();
        String SQL = "UPDATE myusers set useron = false where username = ?";
        PreparedStatement preparedStatement = DBC.connection.prepareStatement(SQL);
        preparedStatement.setString(1, this.myName);
        preparedStatement.executeUpdate();
    }

    //закрывает все каналы связанные с этой нитью
    public void closeAll() throws IOException, SQLException {
        //setUserOfflineFromSocket();
        setUserOffline();
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
    public void writheMessageToDB(String message) throws SQLException, IOException {
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

    //метод отправляющий ВСЕХ кто в онлайне в личный чат переписку
    //нужно учитывать как он работает
    //в нашей системе есть List содержащий данные о занятых сокетах, как объектах (с именами и т.д.)
    //фактически мы берем и перечисляем все сокеты которые есть в userListE и отправляем на них сообщения
    public void sendMessageToChat(String message) {
        for (SocketThread socket : userList) {
            try {
                PrintWriter print = socket.getPrint();
                writheMessageToDB(getTime() + " [" + myName + "] пишет в чат: " + message); //запись в БД
                print.println("[" + getTime() + " " + myName + "] " + message);
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    //оповещаем о том, что от сервера отключился такой-то
    //так же записывает это в БД
    public void messageUserExit() throws SQLException, IOException {
        writheMessageToDB(getTime() + " [" + myName + "] отключился от сервера");
        for (SocketThread socket : userList) {
            try {
                PrintWriter print = socket.getPrint();
                print.println("[" + getTime() + " Сервер] " + myName + " отключился от сервера");
            } catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
    }

    //оповещаем о том, что от сервера отключился такой-то
    //так же записывает это в БД
    public void messageUserEnter() throws SQLException, IOException {
        writheMessageToDB(getTime() + " [" + myName + "] вошел на сервер");
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

    //Получаем список всех людей кто онлайн на сервере
    public void getUserList() throws SQLException, IOException {
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
