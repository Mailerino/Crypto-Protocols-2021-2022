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
import java.math.*;

public class Controller {
    private static ServerSocket ss = null;
    private static ServerSocket ss2 = null;
    private static Socket socket = null;
    private static Socket socket2 = null;
    public String internal_username = "unknown"; // Своё имя
    public String external_username = "unknown"; // Чужое имя
    private boolean hasconnection = false; // Флаг подключения
    private int Session_Key = 0; // Главный сессионный ключ, через который будут шифроваться сообщения
    //private int TrentSessionKey = 0; // Общий ключ с трентом Ka or Kb
    private int internal_R = (int)(Math.random()*((999-100)+1))+100; // Переменная для рандомного числа 100-999 (любой диапазон) Ra or Rb
    private int C1 = 0, C2 = 0, external_C1 = 0, external_C2 = 0;
    private static int P = 0, G = 0, A = 0, H = 0, R = 0; // Свои компоненты
    private static int t_P = 0, t_G = 0, t_A = 0, t_H = 0, t_R = 0; // Компоненты Трента
    private static int ex_P = 0, ex_G = 0, ex_A = 0, ex_H = 0, ex_R = 0; // Внешние компоненты собеседника
    /*  P - Простое Большое Число
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
    public static int random(int from, int to){
        return from + (int) (Math.random() * to);
    }
    public static int Gen_C1 (int external_G, int R, int external_P){
        int res = power(external_G,R,external_P);
        return res;
    }
    public static int Gen_C2 (int M, int external_H, int R, int external_P){
        int res = (M * power(external_H, R, external_P)) % external_P;
        return res;
    }
    public static int Dec_C (int C1, int C2, int P, int A){
        //chat_field.appendText("\nC1=" + C1 + " C2=" + C2);
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
    public static int [] Signature(String msg, int primitiveRoot, int primeNumber, int closeKey ){
        // msg - Исходная строка primitiveRoot = G primeNumber = P closeKey = A
        int[] arraySignatureElGamal  = new int[2];
        int hashMessage = Math.abs(msg.hashCode()); // Используем модуль т.к. хеш hashCode() может быть отрицательный
        int randomNumber = random(2, primeNumber - 1);
        int reverseRandomNumber = modInverse(randomNumber, primeNumber - 1);
        while(gcd(randomNumber,primeNumber - 1) != 1 || (randomNumber * reverseRandomNumber) % (primeNumber - 1)  != 1)
        {
            randomNumber = random(2, primeNumber - 1);
            reverseRandomNumber = modInverse(randomNumber,primeNumber - 1);
        }
        arraySignatureElGamal[0] = power(primitiveRoot,randomNumber, primeNumber);
        int numberU = Math.floorMod((hashMessage - (closeKey * arraySignatureElGamal[0])), primeNumber - 1);
        arraySignatureElGamal[1] = (reverseRandomNumber * numberU) % (primeNumber - 1);
        return arraySignatureElGamal;
    }
    public static boolean isSignature(String msg, int calculationResult, int primitiveRoot, int primeNumber, int signatureR, int signatureS ){
        // msg - сообщение calculationResult - H primitiveRoot - G primeNumber - P signatureR = a signatureS = b
        if(signatureR < 0 || signatureR >= primeNumber || signatureS < 0 || signatureS >= (primeNumber - 1) ) return false;
        int hashMessage = Math.abs(msg.hashCode());
        int leftComposition = ( power(calculationResult,signatureR,primeNumber) *
                power(signatureR,signatureS,primeNumber)) % primeNumber;
        int rightComposition =  power(primitiveRoot, hashMessage, primeNumber);
        return leftComposition == rightComposition;
    }
    //-------------------------------------------------------------------------------------------------------//
    private class ReadMsgServer extends Thread {
        @Override
        public void run() {
            try {
                socket = ss.accept(); // заставляем сервер ждать подключений
                chat_field.appendText("\nBob has been connected to Alice!");
                InputStream sin = socket.getInputStream(); // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                OutputStream sout = socket.getOutputStream();
                DataInputStream in = new DataInputStream(sin);
                DataOutputStream out = new DataOutputStream(sout);

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

                external_username = in.readUTF(); // Принимаем имя Боба
                chat_field.appendText("\nПришло имя: " + external_username);
                chat_field.appendText("\nПодключение к Тренту: ");


                socket2 = new Socket("127.0.0.1", 8561); // Подключаемся к тренту по специальному для Алисы и Трента порту
                InputStream sin2 = socket2.getInputStream();
                OutputStream sout2 = socket2.getOutputStream(); // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                DataInputStream in2 = new DataInputStream(sin2);
                DataOutputStream out2 = new DataOutputStream(sout2);
                chat_field.appendText("Успешно");

                out2.writeInt(P); // Отправляем 3 компоненты своего открытого ключа тренту
                out2.writeInt(G);
                out2.writeInt(H);
                chat_field.appendText("\n Открытый ключ был отправлен Тренту");

                t_P = in2.readInt(); // Получаем открытый ключ Трента
                t_G = in2.readInt();
                t_H = in2.readInt();
                chat_field.appendText("\n Был получен открытый ключ Трента: P=" + t_P + "  G=" + t_G + "  H=" + t_H);
                // Получаем H открытого ключа Трента для проверки подлинности сообщения

                out2.writeUTF(internal_username); // Отсылаем Тренту имя Алисы и Боба
                out2.writeUTF(external_username);

                ex_P = in2.readInt();
                ex_G = in2.readInt();
                ex_H = in2.readInt(); // Получили открытый ключ Боба от Трента
                chat_field.appendText("\nПришел открытый ключ Боба [P.G.H]: [" + ex_P + "." + ex_G + "." + ex_H + "]");

                int t_ex_P1 = in2.readInt(); // 1 часть подписи
                int t_ex_P2 = in2.readInt(); // 2 часть подписи
                int t_ex_G1 = in2.readInt();
                int t_ex_G2 = in2.readInt();
                int t_ex_H1 = in2.readInt();
                int t_ex_H2 = in2.readInt();// Получили 3  компоненты открытого ключа в подписанном виде
                chat_field.appendText("\nПроверяем подлинность P: " + isSignature(Integer.toString(ex_P),t_H,t_G,t_P,t_ex_P1,t_ex_P2));
                chat_field.appendText("\nПроверяем подлинность G: " + isSignature(Integer.toString(ex_G),t_H,t_G,t_P,t_ex_G1,t_ex_G2));
                chat_field.appendText("\nПроверяем подлинность H: " + isSignature(Integer.toString(ex_H),t_H,t_G,t_P,t_ex_H1,t_ex_H2));
                if((isSignature(Integer.toString(ex_H),t_H,t_G,t_P,t_ex_H1,t_ex_H2)) == false){System.out.println("Подпись Трента не совпала [status: 4]"); System.exit(4);}


                out.writeUTF(internal_username); // Алиса отправляет Бобу своё имя
                C1 = Gen_C1(ex_G, R, ex_P);
                C2 = Gen_C2(internal_R, ex_H, R, ex_P); // Шифрование M={C1,C2}=Ra открытым ключом Боба
                System.out.println("Ra Шифротекст от Алисы для Боба: [" + C1 + ";" + C2 + "]");
                out.writeInt(C1);
                out.writeInt(C2); // Отправили шифротекст ЗАКОНЧИЛИ ШАГ 3

                // Шаг 7
                //------DEFAULT------//
                int myNewRa = in.readInt();
                Session_Key = in.readInt();
                int myNewNameCode = in.readInt();
                int exNewNameCode = in.readInt();
                ex_R = in.readInt(); // Rb
                chat_field.appendText("\nПришло [Ra.Session Key.Rb]: [" +myNewRa + "."+Session_Key+ "." + ex_R + "]");
                //------Encode + Signature------//
                int en_sign_1 = in.readInt();
                int en_sign_Ra1 = in.readInt();
                int en_sign_Ra2 = in.readInt();
                int en_sign_SK1 = in.readInt();
                int en_sign_SK2 = in.readInt();
                int en_sign_AN1 = in.readInt();
                int en_sign_AN2 = in.readInt();
                int en_sign_BN1 = in.readInt();
                int en_sign_BN2 = in.readInt();
                int en_Rb = in.readInt();
                //------Decryption------//
                int sign_Ra1 = Dec_C(en_sign_1,en_sign_Ra1,P,A);
                int sign_Ra2 = Dec_C(en_sign_1,en_sign_Ra2,P,A);
                int sign_SK1 = Dec_C(en_sign_1,en_sign_SK1,P,A);
                int sign_SK2 = Dec_C(en_sign_1,en_sign_SK2,P,A);
                int sign_AN1 = Dec_C(en_sign_1,en_sign_AN1,P,A);
                int sign_AN2 = Dec_C(en_sign_1,en_sign_AN2,P,A);
                int sign_BN1 = Dec_C(en_sign_1,en_sign_BN1,P,A);
                int sign_BN2 = Dec_C(en_sign_1,en_sign_BN2,P,A);
                int newRb = Dec_C(en_sign_1,en_Rb,P,A);
                //------Check------//
                chat_field.appendText("\nПроверяем подлинность Ra: " + isSignature(Integer.toString(myNewRa),t_H,t_G,t_P,sign_Ra1,sign_Ra2));
                chat_field.appendText("\nПроверяем подлинность Session Key: " + isSignature(Integer.toString(Session_Key),t_H,t_G,t_P,sign_SK1,sign_SK2));
                chat_field.appendText("\nПроверяем подлинность Alice Name: " + isSignature(Integer.toString(myNewNameCode),t_H,t_G,t_P,sign_AN1,sign_AN2));
                chat_field.appendText("\nПроверяем подлинность Bob Name: " + isSignature(Integer.toString(exNewNameCode),t_H,t_G,t_P,sign_BN1,sign_BN2));
                if((isSignature(Integer.toString(Session_Key),t_H,t_G,t_P,sign_SK1,sign_SK2)) == false) {
                    System.out.println("Подпись Трента Session Key не совпала [status: 6]");
                    System.exit(6);
                }
                if(internal_R == myNewRa){chat_field.appendText("\nRa совпало!");}else{chat_field.appendText("\nRa НЕ совпало!");}

                //------7 to 8------//
                out.writeInt(Encryption_int(ex_R,Session_Key));




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

                out.writeUTF(internal_username); // Отправляем Алисе имя Боба
                chat_field.appendText("\nАлисе было отправлено имя: " + internal_username);

                Thread.sleep(3000);
                chat_field.appendText("\nПодключение к Тренту: ");
                socket2 = new Socket("127.0.0.1", 8560); // Подключаемся к специальному порту Трента (предназначенному Бобу)
                OutputStream sout2 = socket2.getOutputStream();
                InputStream sin2 = socket2.getInputStream();
                DataOutputStream out2 = new DataOutputStream(sout2);
                DataInputStream in2 = new DataInputStream(sin2);
                chat_field.appendText("Успешно");

                out2.writeInt(P); // Отправляем 3 компоненты своего открытого ключа тренту
                out2.writeInt(G);
                out2.writeInt(H);
                chat_field.appendText("\n Открытый ключ был отправлен Тренту");

                t_P = in2.readInt(); // Получаем открытый ключ Трента
                t_G = in2.readInt();
                t_H = in2.readInt();
                chat_field.appendText("\n Был получен открытый ключ Трента: P=" + t_P + "  G=" + t_G + "  H=" + t_H);


                external_username = in.readUTF();
                external_C1 = in.readInt();
                external_C2 = in.readInt();
                System.out.println("Ra шифротекст от Алисы: [" + external_C1 + ";" + external_C2 + "]");
                int external_R = Dec_C(external_C1,external_C2,P,A);
                chat_field.appendText("\nRa Алисы: " + external_R);

                // Шаг 4 Отправляем Тренту
                out2.writeUTF(internal_username);
                out2.writeUTF(external_username);
                C1 = Gen_C1(t_G, R, t_P);
                C2 = Gen_C2(external_R, t_H, R, t_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
                System.out.println("Ra Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
                out2.writeInt(C1);
                out2.writeInt(C2); // Отправили шифротекст
                System.out.println("Шифротекст был отправлен");

                //-------------------------------------------------------------------------------------------------------//
                // Шаг 5 принимаем от Трента 1 сообщение
                ex_P = in2.readInt();
                ex_G = in2.readInt();
                ex_H = in2.readInt(); // Открытый ключ Алисы в обычном виде
                chat_field.appendText("\nПришло [P.G.H]: [" +ex_P + "."+ex_G+ "."+ ex_H +"]");
                int sign_P1 = in2.readInt(); // Получаем компоненты подписей
                int sign_P2 = in2.readInt(); //
                int sign_G1 = in2.readInt(); //
                int sign_G2 = in2.readInt(); //
                int sign_H1 = in2.readInt(); //
                int sign_H2 = in2.readInt(); // Проверяем подписи
                chat_field.appendText("\nПроверяем подлинность P: " + isSignature(Integer.toString(ex_P),t_H,t_G,t_P,sign_P1,sign_P2));
                chat_field.appendText("\nПроверяем подлинность G: " + isSignature(Integer.toString(ex_G),t_H,t_G,t_P,sign_G1,sign_G2));
                chat_field.appendText("\nПроверяем подлинность H: " + isSignature(Integer.toString(ex_H),t_H,t_G,t_P,sign_H1,sign_H2));
                if((isSignature(Integer.toString(ex_H),t_H,t_G,t_P,sign_H1,sign_H2)) == false){
                    System.out.println("Подпись Трента H не совпала [status: 4]");
                    System.exit(4);
                }
                //-------------------------------------------------------------------------------------------------------//
                //------DEFAULT------// 2 Сообщение
                int newRa = in2.readInt();
                Session_Key = in2.readInt();
                int newAliceNameCode = in2.readInt();
                int newBobNameCode = in2.readInt();
                chat_field.appendText("\nПришло [Ra.Session Key]: [" +newRa + "."+Session_Key+"]");
                //------Encode + Signature------//
                int en_sign_1 = in2.readInt();
                int en_sign_Ra1 = in2.readInt();
                int en_sign_Ra2 = in2.readInt();
                int en_sign_SK1 = in2.readInt();
                int en_sign_SK2 = in2.readInt();
                int en_sign_AN1 = in2.readInt();
                int en_sign_AN2 = in2.readInt();
                int en_sign_BN1 = in2.readInt();
                int en_sign_BN2 = in2.readInt();
                //------Decryption------//
                int sign_Ra1 = Dec_C(en_sign_1,en_sign_Ra1,P,A);
                int sign_Ra2 = Dec_C(en_sign_1,en_sign_Ra2,P,A);
                int sign_SK1 = Dec_C(en_sign_1,en_sign_SK1,P,A);
                int sign_SK2 = Dec_C(en_sign_1,en_sign_SK2,P,A);
                int sign_AN1 = Dec_C(en_sign_1,en_sign_AN1,P,A);
                int sign_AN2 = Dec_C(en_sign_1,en_sign_AN2,P,A);
                int sign_BN1 = Dec_C(en_sign_1,en_sign_BN1,P,A);
                int sign_BN2 = Dec_C(en_sign_1,en_sign_BN2,P,A);
                //------Check------//
                chat_field.appendText("\nПроверяем подлинность Ra: " + isSignature(Integer.toString(newRa),t_H,t_G,t_P,sign_Ra1,sign_Ra2));
                chat_field.appendText("\nПроверяем подлинность Session Key: " + isSignature(Integer.toString(Session_Key),t_H,t_G,t_P,sign_SK1,sign_SK2));
                chat_field.appendText("\nПроверяем подлинность Alice Name: " + isSignature(Integer.toString(newAliceNameCode),t_H,t_G,t_P,sign_AN1,sign_AN2));
                chat_field.appendText("\nПроверяем подлинность Bob Name: " + isSignature(Integer.toString(newBobNameCode),t_H,t_G,t_P,sign_BN1,sign_BN2));
                if((isSignature(Integer.toString(newBobNameCode),t_H,t_G,t_P,sign_BN1,sign_BN2)) == false) {
                    System.out.println("Подпись Трента Bob Name не совпала [status: 5]");
                    System.exit(5);
                }
                //------6 to 7------////------DEFAULT------//
                out.writeInt(newRa);
                out.writeInt(Session_Key);
                out.writeInt(newAliceNameCode);
                out.writeInt(newBobNameCode);
                out.writeInt(internal_R); // Rb
                //------Encryption------//
                C1 = Gen_C1(ex_G, R, ex_P);
                C2 = Gen_C2(sign_Ra1, ex_H, R, ex_P);
                System.out.println("Ra1 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C1);
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_Ra2, ex_H, R, ex_P);
                System.out.println("Ra2 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_SK1, ex_H, R, ex_P);
                System.out.println("SK1 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_SK2, ex_H, R, ex_P);
                System.out.println("SK2 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_AN1, ex_H, R, ex_P);
                System.out.println("AN1 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_AN2, ex_H, R, ex_P);
                System.out.println("AN2 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_BN1, ex_H, R, ex_P);
                System.out.println("BN1 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(sign_BN2, ex_H, R, ex_P);
                System.out.println("BN2 Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст
                C2 = Gen_C2(internal_R, ex_H, R, ex_P);
                System.out.println("Rb Шифротекст от Боба для Алисы: [" + C1 + ";" + C2 + "]");
                out.writeInt(C2); // Отправили шифротекст Rb
                
                // Шаг 8
                int en_myNewRb = in.readInt();
                int myNewRb = Decryption_int(en_myNewRb,Session_Key);
                if(myNewRb == internal_R){chat_field.appendText("\n Rb совпало!");}else{chat_field.appendText("\n Rb НЕ совпало!");}


                String line = null; //Создаем пустую строку "буфер"
                while(true) {
                    line = in.readUTF(); // ожидаем пока клиент пришлет строку текста.
                    line = Decryption(line,Session_Key);
                    chat_field.appendText("\n[" + external_username + "]: " + line);
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("Исключение ReadMsgClient (Боб)");
            }
        }
    }
}

