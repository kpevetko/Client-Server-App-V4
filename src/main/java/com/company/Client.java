package com.company;

import com.sun.org.apache.bcel.internal.generic.InstructionConstants;

import javax.print.DocFlavor;
import java.net.SocketException;
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
    private Scanner scanner = new Scanner(System.in);
    private PrintWriter pw = null;
    private BufferedReader br = null;
    private Socket socket = null;

    //нить (поток) отвечает за отправку сообещний на сервер, при написании bye - прерывается
    /*public void typewritingThread() {
        Thread keyThread = new Thread() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        String message = scanner.nextLine();
                        pw.println(message);
                    } catch (Exception x) {
                        break;
                    }
                }
            }
        };
        keyThread.start();
    }*/

    public void startClient() throws IOException, SQLException {

        int portNumber = 1777;
        String str;
        System.out.println("Client is started");
        socket = new Socket("127.0.0.1", portNumber);
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pw = new PrintWriter(socket.getOutputStream(), true);

        //запускаем нить для написания на сервер
        //typewritingThread();
        Thread keyThread = new Thread() {
            @Override
            public void run() {
                //while (!Thread.interrupted()) {
                String message;
                while (!Thread.interrupted()) {
                    //while (!socket.isClosed()) {
                    message = scanner.nextLine();

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

        //чтение сообщений в обычном режиме
        while (true) {
            try {
                //читаем с потока
                str = br.readLine();
                //Если пришел ответ Bye, оканчиваем цикл
                if (str.equals("bye")) {
                    break;
                }
                //печатаем ответ от сервера на консоль для проверки
                System.out.println(str);
            } catch (Exception e) {
                break;
            }
        }

        keyThread.interrupt();
        br.close();
        pw.close();
        socket.close();
        //добавлено т.к. поток требует ввода еще 1 слова для завершения, возможно это можно убрать
        System.exit(0);

    }

    public static void main(String[] args) throws IOException, SQLException {
        Client client = new Client();
        client.startClient();

    }
}

