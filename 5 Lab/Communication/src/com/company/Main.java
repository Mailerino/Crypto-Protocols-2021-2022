package com.company;

import javax.annotation.processing.SupportedSourceVersion;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.HashSet;
import java.util.Scanner;
import java.net.*;
import java.util.Set;

public class Main {
    private static ServerSocket ss = null;
    private static ServerSocket ss2 = null;
    private static Socket socket = null;
    private static Socket socket2 = null;
    private static int Port = 8560; // Порт Боба
    private static int Port2 = 8561; // Порт Алисы
    private static int Session_Key = (int)(Math.random()*((3300-1300)+1))+1300; // Сеансовый ключ
    public String internal_username = "Trent";
    private static String aliceName = "";
    private static String bobName = "";
    private static int K = 0; // Переменная для цифровой подписи
    private static int P = 0, G = 0, A = 0, H = 0, R = 0; // Компоненты Трента
    private static int a_P = 0, a_G = 0, a_A = 0, a_H = 0, a_R = 0; // Компоненты Алисы
    private static int b_P = 0, b_G = 0, b_A = 0, b_H = 0, b_R = 0; // Компоненты Боба
    /*  P - Простое Большое Число
     *  G - Примитивный корень по модулю P
     *  A - Секретный ключ
     *  R - Рандомайзер
     *  H - Вычисляемая функция */


    public static void main(String[] args) {
        try {
            //Производим начальные вычисления
            System.out.println("Session key: " + Session_Key);
            //Генерация открытого ключа
            P = generationLargeNumber();
            System.out.println("Простое большое число P: " + P);
            G = GetPRoot(P);
            System.out.println("Первообразный корень G: " + G);
            A = (int)(Math.random()*(((P-2)-1)+1))+1; // (Math.random()*((max-min)+1))+min;
            System.out.println("Секретный ключ A: " + A);
            H = power(G,A,P); // Возведение в степень по модулю
            System.out.println("h = g^a mod p = " + H);
            // Генерируем рандомизатор r - целое число из [1;p-1]
            R = (int)(Math.random()*(((P-1)-1)+1))+1;
            System.out.println("Randomizer: " + R);
            for(int k=100; k < P-1; k++){
                if((gcd(k,(P-1))) == 1){
                    K = k; // Записываем взаимно простое число в K
                    k = P-1; // Выходим из for
                }
            }
            System.out.println("Случайное целое K: " + K);

            ss = new ServerSocket(Port); // создаем сокет сервера и привязываем его к порту Боба
            ss2 = new ServerSocket(Port2); // создаем сокет сервера и привязываем его к порту Алисы
            System.out.println("Waiting Alice connection...");
            socket2 = ss2.accept(); // заставляем сервер ждать подключений и выводим сообщение когда кто-то связался с сервером
            System.out.println("Alice has been connected to Trent!");

            // Конвертируем потоки в другой тип. Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиенту.
            InputStream sin2 = socket2.getInputStream();
            OutputStream sout2 = socket2.getOutputStream();
            DataInputStream in2 = new DataInputStream(sin2);
            DataOutputStream out2 = new DataOutputStream(sout2);

            a_P = in2.readInt(); // Получаем открытый ключ Алисы
            a_G = in2.readInt();
            a_H = in2.readInt();
            System.out.println("Был получен открытый ключ Алисы: P=" + a_P + "  G=" + a_G + "  H=" + a_H);

            out2.writeInt(P); // Отправляем 3 компоненты своего открытого ключа Алисе
            out2.writeInt(G);
            out2.writeInt(H);
            System.out.println("\nОткрытый ключ был отправлен Алисе");

            //Подключаем Боба
            System.out.println("Waiting Bob connection...");
            socket = ss.accept(); // заставляем сервер ждать подключений и выводим сообщение когда кто-то связался с сервером
            System.out.println("Bob has been connected!");

            // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиенту.
            InputStream sin = socket.getInputStream();
            OutputStream sout = socket.getOutputStream();
            DataInputStream in = new DataInputStream(sin);
            DataOutputStream out = new DataOutputStream(sout);

            b_P = in.readInt(); // Получаем открытый ключ Боба
            b_G = in.readInt();
            b_H = in.readInt();
            System.out.println("Был получен открытый ключ Боба: P=" + b_P + "  G=" + b_G + "  H=" + b_H);

            out.writeInt(P); // Отправляем 3 компоненты своего открытого ключа Бобу
            out.writeInt(G);
            out.writeInt(H); // Отправляем H открытого ключа Трента Бобу для проверки подлинности сообщения
            System.out.println("Открытый ключ был отправлен Бобу");

            aliceName = in2.readUTF(); // Получаем сообщение от Алисы
            bobName = in2.readUTF();
            System.out.println("\nПришли имена: " + aliceName + " и " + bobName); // Шаг 1 закончен

            System.out.println("bob_P: " + b_P + " hash is " + Integer.toString(b_P).hashCode());
            System.out.println("bob_G: " + b_G + " hash is " + Integer.toString(b_G).hashCode());
            System.out.println("bob_H: " + b_H + " hash is " + Integer.toString(b_H).hashCode());

            out2.writeInt(b_P); // 1/3
            out2.writeInt(b_G); // 2/3
            out2.writeInt(b_H); // Отправили открытый ключ Боба 3/3 Алисе

            int temp[] = new int[2];
            temp = Signature(Integer.toString(b_P),G,P,A); // hashcode возвращает int, поэтому еще раз оборачиваем в string
            System.out.println("Bob P after signature:");
            for(int n = 0; n < temp.length; n++){System.out.println(" " + temp[n]);}
            out2.writeInt(temp[0]);
            out2.writeInt(temp[1]); // Отправляем 1/3 подписанный компонент открытого ключа Боба

            temp = Signature(Integer.toString(b_G),G,P,A);
            System.out.println("Bob G after signature:");
            for(int n = 0; n < temp.length; n++){System.out.println(" " + temp[n]);}
            out2.writeInt(temp[0]);
            out2.writeInt(temp[1]); // Отправляем 2/3 подписанный компонент открытого ключа Боба

            temp = Signature(Integer.toString(b_H),G,P,A);
            System.out.println("Bob H after signature:");
            for(int n = 0; n < temp.length; n++){System.out.println(" " + temp[n]);}
            out2.writeInt(temp[0]);
            out2.writeInt(temp[1]); // Отправляем 3/3 подписанный компонент открытого ключа Боба

            // Шаг 4 получаем от Боба
            String ex_B_name = in.readUTF(); // Имя Боба
            String ex_A_name = in.readUTF(); // Имя Алисы
            int ex_C1 = in.readInt();
            int ex_C2 = in.readInt(); // Получили сообщение от Боба
            if(aliceName.equals(ex_A_name)){System.out.println("Имя Алисы совпало с 1 шагом: " + ex_A_name);}
            if(bobName.equals(ex_B_name)){System.out.println("Имя Боба совпало с 1 шагом: " + ex_B_name);}
            System.out.println("Был получен шифротекст: [" + ex_C1 + ";" + ex_C2 + "]");
            int external_R = Dec_C(ex_C1,ex_C2,P,A);
            System.out.println("Ra: " + external_R);

            //-------------------------------------------------------------------------------------------------------//
            // Шаг 5 [1 Message]
            System.out.println("Alice_P: " + a_P + " hash is " + Integer.toString(a_P).hashCode());
            System.out.println("Alice_G: " + a_G + " hash is " + Integer.toString(a_G).hashCode());
            System.out.println("Alice_H: " + a_H + " hash is " + Integer.toString(a_H).hashCode());

            out.writeInt(a_P); // 1/3
            out.writeInt(a_G); // 2/3
            out.writeInt(a_H); // Отправили открытый ключ Алисы 3/3 Бобу

            temp = Signature(Integer.toString(a_P),G,P,A);
            System.out.println("Alice P after signature:");
            for(int n = 0; n < temp.length; n++){System.out.println(" " + temp[n]);}
            out.writeInt(temp[0]);
            out.writeInt(temp[1]); // Отправляем 1/3 подписанный и зашифрованный компонент открытого ключа Алисы Бобу

            temp = Signature(Integer.toString(a_G),G,P,A);
            System.out.println("Alice G after signature:");
            for(int n = 0; n < temp.length; n++){System.out.println(" " + temp[n]);}
            out.writeInt(temp[0]);
            out.writeInt(temp[1]); // Отправляем 2/3 подписанный и зашифрованный компонент открытого ключа Алисы Бобу

            temp = Signature(Integer.toString(a_H),G,P,A);
            System.out.println("Alice H after signature:");
            for(int n = 0; n < temp.length; n++){System.out.println(" " + temp[n]);}
            out.writeInt(temp[0]);
            out.writeInt(temp[1]); // Отправляем 3/3 подписанный и зашифрованный компонент открытого ключа Алисы Бобу
            //-------------------------------------------------------------------------------------------------------//
            // Шаг 5 [2 Message] //------DEFAULT------//
            out.writeInt(external_R); // Ra
            out.writeInt(Session_Key); // SK
            out.writeInt((aliceName.hashCode())%P); // Отправили Alice name hashcode mod P
            out.writeInt((bobName.hashCode())%P); // Отправили Bob name hashcode mod P
            //------Signature + Encode------//
            temp = Signature(Integer.toString(external_R),G,P,A);
            System.out.println("Ra after signature: ");
            for(int n = 0; n < temp.length; n++){System.out.println(temp[n]);}
            int C1 = Gen_C1(b_G, R, b_P);
            int C2 = Gen_C2(temp[0], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Ra1 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст Ra1
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[1], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Ra2 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст Ra2
            System.out.println("Шифротекст был отправлен"); // Отправляем Ra подписанный и зашифрованный компонент Трента Бобу

            temp = Signature(Integer.toString(Session_Key),G,P,A);
            System.out.println("Session Key after signature: ");
            for(int n = 0; n < temp.length; n++){System.out.println(temp[n]);}
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[0], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Ra1 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст SK1
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[1], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Ra2 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст SK2
            System.out.println("Шифротекст был отправлен"); // Отправляем Session Key подписанный и зашифрованный компонент Трента Бобу

            temp = Signature(Integer.toString((String.valueOf(aliceName).hashCode())%P),G,P,A);
            System.out.println("Alice hash name after signature: ");
            for(int n = 0; n < temp.length; n++){System.out.println(temp[n]);}
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[0], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Alice name 1 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст AN1
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[1], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Alice name 2 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст AN2
            System.out.println("Шифротекст был отправлен"); // Отправляем Alice name подписанный и зашифрованный компонент Трента Бобу

            temp = Signature(Integer.toString((String.valueOf(bobName).hashCode())%P),G,P,A);
            System.out.println("Bob hash name after signature: ");
            for(int n = 0; n < temp.length; n++){System.out.println(temp[n]);}
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[0], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Bob name 1 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст BN1
            //C1 = Gen_C1(b_G, R, b_P);
            C2 = Gen_C2(temp[1], b_H, R, b_P); // Шифрование M={C1,C2}=Ra открытым ключом Трента
            System.out.println("Bob name 2 Шифротекст от Боба для Трента: [" + C1 + ";" + C2 + "]");
            //out.writeInt(C1);
            out.writeInt(C2); // Отправили шифротекст BN2
            System.out.println("Шифротекст был отправлен"); // Отправляем Bob name подписанный и зашифрованный компонент Трента Бобу





            out.flush(); // заставляем поток закончить передачу данных.
            out2.flush(); // заставляем поток закончить передачу данных.

            String choice = "";
            while(true) {
                System.out.println("\nТрент завершил начальную инициализацию, закрыть программу? [Y/N]: ");
                Scanner sc = new Scanner(System.in);
                choice = sc.next();
                if (choice.equals("Y")) { System.exit(9); }
                choice = "";
            }
        } catch(Exception x) { x.printStackTrace(); }
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
    public static int Gen_C1 (int external_G, int R, int external_P){
        int res = power(external_G,R,external_P);
        return res;
    }
    public static int Gen_C2 (int M, int external_H, int R, int external_P){
        int res = (M * power(external_H, R, external_P)) % external_P;
        return res;
    }
    public static int Dec_C (int C1, int C2, int P, int A){
        System.out.println("\nC1=" + C1 + " C2=" + C2);
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
    public static int random(int from, int to){
        return from + (int) (Math.random() * to);
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
}
