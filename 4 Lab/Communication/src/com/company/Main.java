package com.company;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.Scanner;
import java.net.*;

public class Main {
    private static ServerSocket ss = null;
    private static ServerSocket ss2 = null;
    private static Socket socket = null;
    private static Socket socket2 = null;
    private static int Port = 8560; // Порт Боба
    private static int Port2 = 8561; // Порт Алисы
    private static int Ka = (int)(Math.random()*((999-100)+1))+100; // Общий ключ Алисы и Трента
    private static int Kb = (int)(Math.random()*((999-100)+1))+100; // Общий ключ Боба и Трента
    private static int Session_Key = (int)(Math.random()*((3300-1300)+1))+1300; // Сеансовый ключ
    public String internal_username = "Trent";


    public static void main(String[] args) {
        try {
            //Производим начальные вычисления
            System.out.println("Ka: " + Ka);
            System.out.println("Kb: " + Kb);
            System.out.println("Session key: " + Session_Key);

            ss = new ServerSocket(Port); // создаем сокет сервера и привязываем его к порту Боба
            ss2 = new ServerSocket(Port2); // создаем сокет сервера и привязываем его к порту Алисы
            System.out.println("Waiting Bob connection...");
            socket = ss.accept(); // заставляем сервер ждать подключений и выводим сообщение когда кто-то связался с сервером
            System.out.println("Bob has been connected to Trent!");

            // Конвертируем потоки в другой тип. Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиенту.
            InputStream sin = socket.getInputStream();
            OutputStream sout = socket.getOutputStream();
            DataInputStream in = new DataInputStream(sin);
            DataOutputStream out = new DataOutputStream(sout);

            out.writeInt(Kb); //Отправляем Бобу общий с ним ключ

            // Принимаем сообщение от Боба
            String Bob_name = in.readUTF(); // Имя Боба
            int Rb = in.readInt(); // Сгенерированное бобом число
            String Alice_name = in.readUTF(); // Имя Алисы (шифр)
            int Ra = in.readInt(); // Сгенерированное Алисой число (шифр)
            int currenttime = in.readInt(); // Метка времени Боба (шифр)
            System.out.println("Получено сообщение от Боба в формате [B,Rb,E(A,Ra,T)]");
            System.out.println(Bob_name + " " + Rb + " (" + Alice_name + "," + Ra + "," + currenttime + ")");
            System.out.println("Расшифровываем...");
            System.out.println(Bob_name + " " + Rb + " (" + Decryption(Alice_name,Kb) + "," + Decryption_int(Ra,Kb) + "," + Decryption_int(currenttime,Kb) + ")");

            //Подключаем Алису
            System.out.println("Waiting Alice connection...");
            socket2 = ss2.accept(); // заставляем сервер ждать подключений и выводим сообщение когда кто-то связался с сервером
            System.out.println("Alice has been connected!");

            // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиенту.
            OutputStream sout2 = socket2.getOutputStream();
            DataOutputStream out2 = new DataOutputStream(sout2);

            // Отправляем общий ключ Алисы и Трента ей
            out2.writeInt(Ka);

            // Формируем первое сообщение для Алисы
            out2.writeUTF(Encryption(Bob_name,Ka));
            out2.writeInt(Encryption_int(Decryption_int(Ra,Kb),Ka));
            out2.writeInt(Encryption_int(Session_Key,Ka));
            out2.writeInt(Encryption_int(Decryption_int(currenttime,Kb),Ka));

            // Формируем Второе сообщение для Алисы (шифр)
            out2.writeUTF(Alice_name); // Уже зашифрованно Бобом (Kb)
            out2.writeInt(Encryption_int(Session_Key,Kb));
            out2.writeInt(currenttime); // Уже зашифрованно Бобом (Kb)

            // Случайное число Боба
            out2.writeInt(Rb);

            out.flush(); // заставляем поток закончить передачу данных.
            out2.flush(); // заставляем поток закончить передачу данных.

            String choice = "";
            while(true) {
                System.out.println("Трент завершил начальную инициализацию, закрыть программу? [Y/N]: ");
                Scanner sc = new Scanner(System.in);
                choice = sc.next();
                if (choice.equals("Y")) { System.exit(9); }
                choice = "";
            }
        } catch(Exception x) { x.printStackTrace(); }
    }

    public static String Encryption (String Text, int Key){
        String code = "";
        char m;
        for(int i = 0; i < Text.length(); i++){
            char t = Text.charAt(i);
            int n = t^Key;
            m = (char)Integer.parseInt(String.valueOf(n));
            code += m;
        }
        return code;
    }
    public static int Encryption_int (int Text, int Key){
        int code = 0;
        code = Text ^ Key;
        return code;
    }
    public static String Decryption (String Text, int Key){
        String code = "";
        char m;
        for(int i = 0; i < Text.length(); i++){
            char t = Text.charAt(i);
            int n = t^Key;
            m = (char)Integer.parseInt(String.valueOf(n));
            code += m;
        }
        return code;
    }
    public static int Decryption_int (int Text, int Key){
        int code = 0;
        code = Text ^ Key;
        return code;
    }
}
