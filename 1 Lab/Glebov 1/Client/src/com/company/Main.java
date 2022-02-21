package com.company;
import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) throws IOException {
        int serverPort = 6996; // здесь обязательно нужно указать порт к которому привязывается сервер.
        String address = "127.0.0.1"; // это IP-адрес компьютера, где исполняется наша серверная программа.
        //localhost ip
        // Здесь указан адрес того самого компьютера где будет исполняться и клиент.

        try {
            InetAddress ipAddress = InetAddress.getByName(address); // создаем объект который отображает вышеописанный IP-адрес.
            //Конвертируем IP
            System.out.println("Подключение к серверу с адресом: " + address + ":" + serverPort);
            Socket socket = new Socket(ipAddress, serverPort); // создаем сокет используя IP-адрес и порт сервера.
            System.out.println("Подключение успешно!");

            // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиентом.
            InputStream sin = socket.getInputStream();
            OutputStream sout = socket.getOutputStream();

            // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
            DataInputStream in = new DataInputStream(sin);
            DataOutputStream out = new DataOutputStream(sout);

            // Создаем поток для чтения с клавиатуры.
            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
            String line = null;
            System.out.println("Теперь возможен обмен сообщениями с сервером, сообщение: ");
            System.out.println("--------------------------------------------------------");

            while (true) {
                line = keyboard.readLine(); // ждем пока пользователь введет что-то и нажмет кнопку Enter.
                System.out.println("Отправка на сервер...");
                out.writeUTF(line); // отсылаем введенную строку текста серверу.
                out.flush(); // заставляем поток закончить передачу данных.
                line = in.readUTF(); // ждем пока сервер отошлет строку текста.
                System.out.println("С сервера получено сообщение: " + line);
                System.out.println("Теперь возможен обмен сообщениями с сервером, сообщение: ");
                System.out.println("--------------------------------------------------------");
            }
        } catch (Exception x) {
            x.printStackTrace();
        }
    }
}
