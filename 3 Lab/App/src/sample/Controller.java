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
    private ServerSocket ss = null;
    private Socket socket = null;
    public String internal_username = "unknown";
    public String external_username = "unknown";
    private boolean hasconnection = false;
    private int external_P = 0, external_G = 0, external_H = 0, external_R = 0; // Переменные для внешнего открытого ключа
    private int internal_R = (int)(Math.random()*((999-100)+1))+100; // Переменная для рандомного числа 100-999 (любой диапазон)
    private int C1 = 0, C2 = 0, external_C1 = 0, external_C2 = 0;
    private int Session_Key = 0;
    private int P = 0, G = 0, A = 0, H = 0, R = 0;
    /* P - Простое Большое Число
    *  G - Примитивный корень по модулю P
    *  A - Секретный ключ
    *  R - Рандомайзер
    *  H - Вычисляемая функция */

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
    public static int Gen_C1 (int external_G, int R, int external_P){
        int res = power(external_G,R,external_P);
        return res;
    }
    public static int Gen_C2 (int M, int external_H, int R, int external_P){
        int res = (M * power(external_H, R, external_P)) % external_P;
        return res;
    }
    public int Dec_C (int C1, int C2, int P, int A){
        chat_field.appendText("\nC1=" + C1 + " C2=" + C2);
        //chat_field.appendText("\nPower : " + power(C1,A,P));
        //chat_field.appendText("\nInverse : " + modInverse(power(C1,A,P),P));
        int res = (C2 * modInverse(power(C1,A,P),P)) % P;
        return res;
    }
    public static int GetPRoot(int p) {
        // Первообразный корень простого числа
        for (int i = 0; i < p; i++)
            if (IsPRoot(p, i))
                return i;
        return 0;
    }
    public static boolean IsPRoot(long p, long a) {
        // Первообразный корень простого числа
        if (a == 0 || a == 1)
            return false;
        long last = 1;

        Set<Long> set = new HashSet<>();
        for (long i = 0; i < p - 1; i++) {
            last = (last * a) % p;
            if (set.contains(last)) // Если повтор
                return false;
            set.add(last);
        }
        return true;
    }
    public static int modInverse(int a, int m){ // Обратный элемент по модулю
        a = a % m;
        for (int x = 1; x < m; x++)
            if ((a * x) % m == 1)return x;  // Обычным перебором смотрим выполнение условия
        return 1;
    }
    public static int gcd(int a, int b) { // НОД двух чисел, gcd source https://stackoverflow.com/questions/4009198/java-get-greatest-common-divisor
        if (b == 0) return a;
        if (a == 0) return b;

        // А и B чётные
        if ((a & 1) == 0 && (b & 1) == 0) return gcd(a >> 1, b >> 1) << 1;

            // А чётное, B нечётное
        else if ((a & 1) == 0) return gcd(a >> 1, b);

            // A нечётное, B чётное
        else if ((b & 1) == 0) return gcd(a, b >> 1);

            // А и B нечётные, A >= B
        else if (a >= b) return gcd((a-b) >> 1, b);

            // А и B нечётные, A < B
        else return gcd(a, (b-a) >> 1);
    }
    private static boolean TestRabinMiller(int number, int mod){
        if(mod <= 4) return false;
        int rNumber = 2 + (int)(Math.random() % (mod - 4)); // Выбираем случайное число в отрезке [2,n-2]
        int modNumber = power(rNumber, number, mod); // Возводим число в степень по модулю
        if(modNumber == 1 || modNumber == mod - 1) return true;

        while (number != mod - 1){
            modNumber = (modNumber * modNumber) % mod;
            number = number * 2;
            if(modNumber == 1 ) return false; //В итоге получаем 1 из 2 результатов
            if(modNumber == mod - 1 ) return true;
        }
        return false;
    }
    public static int power(int x, int y, int p) {
        // Возведение в степень по модулю
        int res = 1; // Initialize result

        x = x % p; // Update x if it is more than or
        // equal to p

        if (x == 0)
            return 0; // In case x is divisible by p;

        while (y > 0)
        {

            // If y is odd, multiply x with result
            if ((y & 1) != 0)
                res = (res * x) % p;

            // y must be even now
            y = y >> 1; // y = y/2
            x = (x * x) % p;
        }
        return res;
    }
    public static boolean isPrime(int n, int k) {
        //Проверка на простое число
        if (n <= 1 || n == 4) return false;
        if (n <= 3) return true; // если n == 2 или n == 3 - эти числа простые, возвращаем true
        int d = n - 1;
        while (d % 2 == 0) // Последовательное деление n-1 на 2 (Представляем n-1 в виде 2^s * t, где t - нечетное)
            d = d/2;
        for (int i = 0; i < k; i++) //Тест Миллера – Рабина с k итерациями
            if (!TestRabinMiller(d, n)) return false;
        return true;
    }
    public static int generationLargeNumber(){
        // Генерация числа
        int min = 1480, max = 9960, k = 10;
        int i = min + (int)(Math.random() * max);
        for( ; i <= max; i ++) { if(isPrime(i, k)) return i; }
        for( ; i >= min; i --) { if(isPrime(i, k)) return i; }
        return -1;
    }
    //-------------------------------------------------------------------------------------------------------//
    private class ReadMsgServer extends Thread {
        @Override
        public void run() {
            try {
                socket = ss.accept(); // заставляем сервер ждать подключений

                //Генерация открытого ключа
                P = generationLargeNumber();
                chat_field.appendText("\nПростое большое число P: " + P);
                G = GetPRoot(P);
                chat_field.appendText("\nПервообразный корень G: " + G);
                A = (int)(Math.random()*(((P-2)-1)+1))+1; // (Math.random()*((max-min)+1))+min;
                chat_field.appendText("\nСекретный ключ A: " + A);
                H = power(G,A,P); // Возведение в степень по модулю
                chat_field.appendText("\nh = g^a mod p = " + H);
                // Генерируем рандомизатор r - целое число из [1;p-1]
                R = (int)(Math.random()*(((P-1)-1)+1))+1;
                chat_field.appendText("\nRandomizer: " + R);
                chat_field.appendText("\nRa: " + internal_R);

                chat_field.appendText("\nGot a client.");
                InputStream sin = socket.getInputStream();
                // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                // Легче работать со строками
                DataInputStream in = new DataInputStream(sin);

                OutputStream sout = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(sout);

                out.writeInt(P); // Передаем 3 компоненты открытого ключа сервера (Алисы)
                out.writeInt(G);
                out.writeInt(H);
                out.writeUTF(internal_username); // Отправляем никнейм сервера клиенту
                out.flush(); // заставляем поток закончить передачу данных.

                external_P = in.readInt(); // Ждем пока Bob пришлет все 3 компоненты своего открытого ключа
                external_G = in.readInt();
                external_H = in.readInt();
                external_username = in.readUTF(); // Считываем никнейм клиента


                chat_field.appendText("\nПришел открытый ключ: [" + external_P + ";" + external_G + ";" + external_H + "]");
                C1 = Gen_C1(external_G, R, external_P); // Шифрование M={C1,C2}=Ra открытым ключом Bob
                C2 = Gen_C2(internal_R, external_H, R, external_P);
                chat_field.appendText("\nШифротекст: [" + C1 + ";" + C2 + "]");

                // Протокол взаимоблокировки
                out.writeInt(C1); // Отправляем первую часть
                external_C1 = in.readInt(); // Принимаем первую часть
                out.writeInt(C2); // Отправляем вторую часть
                external_C2 = in.readInt(); // Принимаем вторую часть
                chat_field.appendText("\nC1: " + external_C1 + " C2: " + external_C2);
                external_R = Dec_C(external_C1, external_C2, P, A); // Расшифровка двух частей
                chat_field.appendText("\nRb: " + external_R);

                Session_Key = internal_R ^ external_R; // K = Ra xor Rb
                chat_field.appendText("\nSession Key: " + Session_Key);

                String line = null; //Создаем пустую строку "буфер"
                while(true) {
                    line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                    line = Decryption(line,Session_Key);
                    chat_field.appendText("\n[" + external_username + "]: " + line);
                }
            } catch (IOException e) {
                System.out.println("Исключение ReadMsgServer");
            }
        }
    }
    private class ReadMsgClient extends Thread {
        @Override
        public void run() {
            try {

                //Генерация открытого ключа
                P = generationLargeNumber();
                chat_field.appendText("\nПростое большое число P: " + P);
                G = GetPRoot(P);
                chat_field.appendText("\nПервообразный корень G: " + G);
                A = (int)(Math.random()*(((P-2)-1)+1))+1; // (Math.random()*((max-min)+1))+min;
                chat_field.appendText("\nСекретный ключ A: " + A);
                H = power(G,A,P); // Возведение в степень по модулю
                chat_field.appendText("\nh = g^a mod p = " + H);
                // Генерируем рандомизатор r - целое число из [1;p-1]
                R = (int)(Math.random()*(((P-1)-1)+1))+1;
                chat_field.appendText("\nRandomizer: " + R);
                chat_field.appendText("\nRb: " + internal_R);

                InputStream sin = socket.getInputStream();
                // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                // Легче работать со строками
                DataInputStream in = new DataInputStream(sin);

                OutputStream sout = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(sout);

                external_P = in.readInt(); // Ждем пока Alice пришлет все 3 компоненты своего открытого ключа
                external_G = in.readInt();
                external_H = in.readInt();
                external_username = in.readUTF(); // Считываем никнейм сервера

                out.writeInt(P); // Передаем 3 компоненты открытого ключа (Bob)
                out.writeInt(G);
                out.writeInt(H);
                out.writeUTF(internal_username); // Отправляем никнейм клиента серверу
                out.flush(); // заставляем поток закончить передачу данных.

                chat_field.appendText("\nПришел открытый ключ: [" + external_P + ";" + external_G + ";" + external_H + "]");
                C1 = Gen_C1(external_G, R, external_P);
                C2 = Gen_C2(internal_R, external_H, R, external_P);
                chat_field.appendText("\nШифротекст: [" + C1 + ";" + C2 + "]");


                // Протокол взаимоблокировки
                external_C1 = in.readInt(); // Принимаем первую часть
                out.writeInt(C1); // Отправляем первую часть
                external_C2 = in.readInt(); // Принимаем вторую часть
                external_R = Dec_C(external_C1, external_C2, P, A); // Расшифровка двух частей
                chat_field.appendText("\nRa: " + external_R);
                out.writeInt(C2); // Отправляем вторую часть

                Session_Key = internal_R ^ external_R; // K = Ra xor Rb
                chat_field.appendText("\nSession Key: " + Session_Key);

                String line = null; //Создаем пустую строку "буфер"
                while(true) {
                    line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                    line = Decryption(line,Session_Key);
                    chat_field.appendText("\n[" + external_username + "]: " + line);
                }
            } catch (IOException e) {
                System.out.println("Исключение ");
            }
        }
    }
}

