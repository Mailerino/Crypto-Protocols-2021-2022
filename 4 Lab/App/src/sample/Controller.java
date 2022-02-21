package sample;

import java.io.*;
import java.net.*;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class Controller {
    private static ServerSocket ss = null;
    private static ServerSocket ss2 = null;
    private static Socket socket = null;
    private static Socket socket2 = null;
    public String internal_username = "unknown"; // Своё имя
    public String external_username = "unknown"; // Чужое имя
    private boolean hasconnection = false; // Флаг подключения
    private int Session_Key = 0; // Главный сессионный ключ, через который будут шифроваться сообщения
    private int Ka = 0;
    private long currentTime = System.currentTimeMillis() / 1000L; // Вычисляем текущее время

    @FXML
    private ResourceBundle resources;
    @FXML
    private URL location;
    @FXML
    private TextArea chat_field;
    @FXML
    private TextField message_field;
    @FXML
    private Button SendButton;
    @FXML
    private TextField ip_field;
    @FXML
    private TextField port_field;
    @FXML
    private Button ConnectButton;
    @FXML
    private Button CreateButton;

    @FXML
    void initialize() {
        chat_field.appendText("Порт должен быть: 1025..65535 ");
        CreateButton.setOnAction(event -> {
            String s_port = port_field.getText().trim();
            int port = Integer.parseInt(s_port); // (может быть любое число от 1025 до 65535)
            System.out.println("Port in field: " + port);
            hasconnection = true;
            try {
                ss = new ServerSocket(port); // создаем сокет сервера и привязываем его к вышеуказанному порту
                chat_field.appendText("\nПорт для подключения: " + port + "\nОжидание подключения...");
                new ReadMsgServer().start(); // нить читающая сообщения из сокета в бесконечном цикле
            } catch(Exception x) { x.printStackTrace(); }
        });
        ConnectButton.setOnAction(event -> {
            String s_port = port_field.getText().trim();
            int port = Integer.parseInt (s_port); // здесь обязательно нужно указать порт к которому привязывается сервер.
            String s_ip = ip_field.getText().trim(); // это IP-адрес компьютера, где исполняется наша серверная программа.
            //localhost ip
            // Здесь указан адрес того самого компьютера где будет исполняться и клиент.
            try {
                InetAddress ipAddress = InetAddress.getByName(s_ip); // создаем объект который отображает вышеописанный IP-адрес.
                //Конвертируем IP
                chat_field.appendText("\nПодключение к серверу с адресом: " + s_ip + ":" + port);
                socket = new Socket(ipAddress, port); // создаем сокет используя IP-адрес и порт сервера.
                new ReadMsgClient().start(); // нить читающая сообщения из сокета в бесконечном цикле
                chat_field.appendText("\nПодключение успешно!");
                hasconnection = true;
            } catch (Exception x) {
                System.out.println("\nПодключение не удалось!");
            }
        });
        SendButton.setOnAction(event -> {
            if (hasconnection == false) {
                internal_username = message_field.getText();
                chat_field.appendText("\nUsername has been changed to '" + internal_username + "'");
                message_field.setText(null);
            }

            if (hasconnection == true) {
            try {
                OutputStream sout = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(sout);
                String line = message_field.getText();
                chat_field.appendText("\n[" + internal_username + "]: " + line);
                line = Encryption(line,Session_Key);
                out.writeUTF(line); // отсылаем клиенту строку текста.
                message_field.setText(null); // Очищаем строку для следующего ввода
                out.flush(); // заставляем поток закончить передачу данных.
            } catch (IOException e) {
                System.out.println("Проверьте подключение к сокету");
            }}
        });
    }

    //-------------------------------------------------------------------------------------------------------//
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
    //-------------------------------------------------------------------------------------------------------//
    private class ReadMsgServer extends Thread {
        @Override
        public void run() {
            try {
                socket = ss.accept(); // заставляем сервер ждать подключений
                chat_field.appendText("\nBob has been connected to Alice!");
                InputStream sin = socket.getInputStream(); // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                DataInputStream in = new DataInputStream(sin);
                OutputStream sout = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(sout);

                int Ra = (int)(Math.random()*((999-100)+1))+100;
                chat_field.appendText("\nСгенерированное Ra: " + Ra);
                out.writeUTF(internal_username); // Отсылаем бобу Имя и Ra
                out.writeInt(Ra);

                socket2 = new Socket("127.0.0.1", 8561); // Подключаемся к тренту по специальному для Алисы и Трента порту
                InputStream sin2 = socket2.getInputStream();
                OutputStream sout2 = socket2.getOutputStream(); // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                DataInputStream in2 = new DataInputStream(sin2);
                DataOutputStream out2 = new DataOutputStream(sout2);

                Ka = in2.readInt(); // Считываем общий ключ Алисы и Трента
                chat_field.appendText("\nОбщий ключ с Трентом: " + Ka);

                //Первое сообщение от трента для Алисы
                external_username = in2.readUTF();
                int newRa = in2.readInt();
                Session_Key = in2.readInt();
                int time_mark = in2.readInt();

                //Второе сообщение от трента для Боба (через Алису)
                String Ekb_A = in2.readUTF();
                int Ekb_K = in2.readInt();
                int Ekb_Tb = in2.readInt();
                int Rb = in2.readInt();

                //Расшифровываем первое сообщение
                external_username = Decryption(external_username,Ka);
                newRa = Decryption_int(newRa,Ka);
                Session_Key = Decryption_int(Session_Key,Ka);
                time_mark = Decryption_int(time_mark,Ka);

                // Проверяем совпадение new Ra с Ra на первом шаге
                if (Ra == newRa) {System.out.println("Ra Совпали!");}
                chat_field.appendText("\nnew Ra: " + newRa + " Ra: " + Ra);
                chat_field.appendText("\nSession Key: " + Session_Key);

                // Отправляем Бобу первое сообщение
                out.writeUTF(Ekb_A);
                out.writeInt(Ekb_K);
                out.writeInt(Ekb_Tb);

                // Отправляем Бобу второе сообщение
                out.writeInt(Encryption_int(Rb,Session_Key));


                String line = null; //Создаем пустую строку "буфер"
                while(true) {
                    line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                    line = Decryption(line,Session_Key);
                    chat_field.appendText("\n[" + external_username + "]: " + line);
                }
            } catch (IOException e) {
                System.out.println("Исключение ReadMsgServer (Алисы)");
            }
        }
    }
    private class ReadMsgClient extends Thread {
        @Override
        public void run() {
            try {
                InputStream sin = socket.getInputStream(); // Конвертируем потоки в другой тип.
                DataInputStream in = new DataInputStream(sin);
                OutputStream sout = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(sout);

                long unixTime = System.currentTimeMillis() / 1000L; // Вычисляем текущее время
                int Kb = 0, Ra = 0; // Подготавливаем переменные
                int Rb = (int)(Math.random()*((999-100)+1))+100; // Генерируем Rb
                chat_field.appendText("\nCurrent Time: " + unixTime + "\nСгенерированное Rb: " + Rb);

                external_username = in.readUTF(); // Принимаем от Алисы её имя и Ra
                Ra = in.readInt();

                chat_field.appendText("\nПришло имя: " + external_username + "\nПришло Ra: " + Ra);

                socket2 = new Socket("127.0.0.1", 8560); // Подключаемся к специальному порту Трента (предназначенному Бобу)
                OutputStream sout2 = socket2.getOutputStream();
                InputStream sin2 = socket2.getInputStream();
                DataOutputStream out2 = new DataOutputStream(sout2);
                DataInputStream in2 = new DataInputStream(sin2);

                Kb = in2.readInt(); // Считываем общий ключ Трента и Боба
                chat_field.appendText("\nОбщий ключ с Трентом: " + Kb);

                String shAlice = Encryption(external_username,Kb);
                int shRa = Encryption_int(Ra,Kb);
                int shTime = Encryption_int(((int)unixTime),Kb);

                System.out.println("myName: " + internal_username);
                System.out.println("Rb: " + Rb);
                System.out.println("shAlice: " + shAlice);
                System.out.println("shRa: " + shRa);
                System.out.println("shCurTime: " + shTime);

                out2.writeUTF(internal_username); // Имя Боба
                out2.writeInt(Rb); // Число Боба
                out2.writeUTF(shAlice); // Имя алисы (шифр)
                out2.writeInt(shRa); // Ra (шифр)
                out2.writeInt(shTime); // Метка времени (шифр)

                //Второе сообщение от трента для Боба отправленное Алисой
                String Ekb_A = in.readUTF();
                int Ekb_K = in.readInt();
                int Ekb_Tb = in.readInt();

                // Rb от Алисы, закодированное сессионным ключом
                int newRb = in.readInt();

                // Расшифровываем
                Ekb_A = Decryption(Ekb_A, Kb);
                Ekb_K = Decryption_int(Ekb_K, Kb);
                Ekb_Tb = Decryption_int(Ekb_Tb, Kb);

                Session_Key = Ekb_K;
                newRb = Decryption_int(newRb,Session_Key);
                chat_field.appendText("\nSession Key: " + Session_Key);

                // Проверяем совпадение new Rb с Rb
                if (Rb == newRb) {chat_field.appendText("Rb Совпали!");}
                chat_field.appendText("\nnew Rb: " + newRb + " Rb: " + Rb);
                if (Ekb_A.equals(external_username)) {chat_field.appendText("\nИмя Алисы совпало!");}
                if (Ekb_Tb == (int)unixTime) {chat_field.appendText("\nМетка времени совпала!");}
                if (Ekb_Tb < ((int)currentTime - 300)) {chat_field.appendText("\nМетка времени ПРОСРОЧЕНА МИНИМУМ НА 5 МИНУТ!");}
                if (Ekb_Tb > ((int)currentTime + 300)) {chat_field.appendText("\nМетка времени СПЕШИТ МИНИМУМ НА 5 МИНУТ!");}

                String line = null; //Создаем пустую строку "буфер"
                while(true) {
                    line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                    line = Decryption(line,Session_Key);
                    chat_field.appendText("\n[" + external_username + "]: " + line);
                }
            } catch (IOException e) {
                System.out.println("Исключение ReadMsgClient (Боб)");
            }
        }
    }
}

