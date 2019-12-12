package com.company;

import com.sun.security.ntlm.Server;

//код многопоточного сервера, первая часть
import java.io.*;
import java.net.*;
import java.util.LinkedList;

public class ThreadServer {
    final LinkedList<SocketThread> userList = new LinkedList<SocketThread>();

    public void startServer() {
        //определяем номер порта, который будет слушать сервер
        int port = 1777;
        try {
            //открываем серверный сокет
            ServerSocket serverSocket = new ServerSocket(port);
            //старый код
            /*while (true) {
                System.out.println("Waiting connection on port: " + port);
                //получив соединение, начинаем работу с сокетом
                Socket fromClientSocket = serverSocket.accept();
                //Стартуем новый поток для обработки запроса клиента
                new SocketThread(fromClientSocket,userList).start();
            }*/
            while (true) {
                System.out.println("Waiting connection Clients on port: " + port + " someone try to connect"); //нужно ли мне ЭТО?!
                //получив соединение, начинаем работу с сокетом
                Socket fromClientSocket = serverSocket.accept();
                BufferedReader br = new BufferedReader(new InputStreamReader(fromClientSocket.getInputStream()));
                PrintWriter pw = new PrintWriter(fromClientSocket.getOutputStream(), true);
                //строка содержащая имя пользователя

                String userName = null;

                try {
                    userName = br.readLine();
                } catch (Exception e) {
                    e.fillInStackTrace();
                }
                //ошибка со стороны сервера вылетала здесь (строка 44, обработка строки 38-42)
                //if ((userName = br.readLine()) != null && (!userName.equals("bye"))) {
                if (userName != null && (!userName.equals("bye"))) {
                    //подключение успешно
                    pw.println("Вход на сервер выполнен успешно");
                    //отладка и информация
                    System.out.println("Соединение с клиентом выполнено успешно");
                    //открываем новую нить
                    new SocketThread(fromClientSocket, userList, userName).start();
                } else {
                    //подключение сорвалось
                    pw.println("Вход не выполнен");
                    //закрываем сокет
                    fromClientSocket.close();
                    //отладка и информация
                    System.out.println("Соединение прервано");
                    //закрываем буфер и writer
                    br.close();
                    pw.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ThreadServer TS = new ThreadServer();
        TS.startServer();
    }
}
