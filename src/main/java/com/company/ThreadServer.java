package com.company;

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

            while (true) {
                System.out.println("Waiting connection Clients on port: " + port + " someone try to connect"); //нужно ли мне ЭТО?!
                //получив соединение, начинаем работу с сокетом
                Socket fromClientSocket = serverSocket.accept();

                //BufferedReader br = new BufferedReader(new InputStreamReader(fromClientSocket.getInputStream()));
                PrintWriter pw = new PrintWriter(fromClientSocket.getOutputStream(), true);

                //пишем клиенту что выполнен вход на сервер
                pw.println("Соединение с сервером выполнено успешно");
                //открываем новую нить
                new SocketThread(fromClientSocket, userList).start();

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
