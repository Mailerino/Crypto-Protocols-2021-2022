package com.company;

import java.net.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Random;

public class Main {
    private static ServerSocket ss = null;
    private static Socket socket = null;
    private static int Port = (int)(Math.random()*((65535-1025)+1))+1025; // Порт Стола (1025..65535)
    public static String internal_username = "unknown";
    public static String external_username = "unknown";
    public static Boolean hasConnection = false;
    public static int cards[] = new int[52];
    public static int en_cards[] = new int[52];
    public static int my_cards[] = new int[5];
    public static int my_enc_cards[] = new int[5];
    public static int ex_cards[] = new int[5];
    public static int test_cards[] = new int[5];
    public static int ex_double_enc_cards[] = new int[5];
    public static int score[][] = new int[5][2]; // Счет стола
    public static int count = 0; // Кол-во честных карт оппонента

    public static int P = 0, Q = 0, N = 0, F = 0;
    public static int E = 0, D = 0; // Открытый и Открытый ключ
    public static int exp[] = new int[100]; // Будет массив из открытых экспонент
    public static int rnd = 3; // Место начала поисков открытых экспонент
    public static int rand = 0; // Позиция выбранной экспоненты в массиве открытых экспонент
    public static int ex_E = 0, ex_D = 0; // Переменные для проверки честности опонента в конце игры

    public final static int EMPTY_CHAR = 1030;
    public final static int SPACE_NUMBER  = 99;

    // \u001b[0m = Default
    // \u001b[30m = Black
    // \u001b[31m = Red
    // \u001b[32m = Green
    // \u001b[33m = Yellow
    // \u001b[34m = Blue
    // \u001b[35m = Purple
    // \u001b[37m = White

    public static void main(String[] args) throws IOException {
        for(int n = 1; n < 53 ; n++){ // Наполняем массив картами
            cards[n-1] = n;
        }
        //for(int n = 0; n < 52 ; n++){System.out.println(cards[n]);} // Посмотреть массив карт
        InputStream sin = null;
        OutputStream sout = null;
        DataInputStream in = null;
        DataOutputStream out = null;
        System.out.println("\n\u001b[37m" + "Вариант 8. «Покер по телефону (2 игрока)»");
        System.out.println("\u001b[0m" + "Порт текущего стола: " + "\u001b[33m" + Port + "\u001b[0m");
        System.out.println("Введите ваш псевдоним:");
        Scanner sc = new Scanner(System.in);
        internal_username = sc.nextLine();
        while (true) {
            int choice;
            switch (choice = menu()) {
                case 0:
                    System.out.println("Произведено завершение программы");
                    System.exit(10);
                    break;
                case 1:
                    if(hasConnection == false){System.out.println("За столом нет 2 игрока"); System.exit(4);}
                    P = generationLargeNumber();
                    Q = generationLargeNumber();
                    N = P*Q;
                    F = (P-1)*(Q-1);
                    System.out.println("P = " + P + "\nQ = " + Q + "\nN = " + N + "\nφ = " + F);
                    out.writeInt(P);
                    out.writeInt(Q);

                    for(int i = 0; i < 100;) // Находим 100 различных открытых экспонент
                    {
                        if((gcd(rnd, F) == 1) && (rnd < F)) // Проверка на Взаимно простое с φ, открытая экспонента < φ
                        {
                            exp[i] = rnd;
                            i++;
                        }
                        rnd++;
                    }
                    //for(int i = 0; i < 100; i++){System.out.println(exp[i]);} // Вывести список экспонент
                    // Закрытый ключ: modInverse(rnd, FuncEuler) % FuncEuler
                    rand = (int)(Math.random()*(99+1))+0; // Случайным образом выбираем экспоненту из массива открытых экспонент [100]
                    E = exp[rand];
                    D = (int)modInverse(E, F) % F;
                    System.out.println("Открытый ключ [E,N]: " + E + ", " + N + "\nЗакрытый ключ [D,N]: " + D + ", " + N);
                    en_cards = enc_deck(cards,E,N); // Шифруем карты открытым ключом Алисы
                    //for(int n = 0; n < en_cards.length; n++){System.out.println("Шифрованная карта ["+n+"]: " + en_cards[n]);}
                    en_cards = shuffle(en_cards); // Перемешиваем карты
                    for(int n = 0; n < en_cards.length; n++){
                        out.writeInt(en_cards[n]); // Отправляем перемешанные карты Бобу
                    }

                    for(int i = 0; i < my_enc_cards.length; i++){
                        my_enc_cards[i] = in.readInt(); // Приняли 5 шифрограмм от Боба
                    }
                    for(int i = 0; i < 5; i++){System.out.println("Пришло: " + my_enc_cards[i]);}
                    System.out.println("Расшифровываем...\u001b[34m");
                    for(int i = 0; i < 5; i++){
                        my_cards[i] = (int)power2((long)my_enc_cards[i], (long)D, (long)N);
                    }
                    for(int i = 0; i < 5; i++){
                        System.out.println("Alice's card [" + (i+1) + "]: " + my_cards[i] + " (" + suit_of(my_cards[i]) + " " + value_of(my_cards[i]) + ")");
                    }
                    System.out.print("\u001b[0m");
                    // Шаг 4 закончен.

                    ex_double_enc_cards[0] = in.readInt();
                    ex_double_enc_cards[1] = in.readInt();
                    ex_double_enc_cards[2] = in.readInt();
                    ex_double_enc_cards[3] = in.readInt();
                    ex_double_enc_cards[4] = in.readInt(); // Приняли карты Боба в двойном шифровании

                    // Снимаем свой шифр и отправляем назад
                    out.writeInt((int)power2((long)ex_double_enc_cards[0], (long)D,(long)N));
                    out.writeInt((int)power2((long)ex_double_enc_cards[1], (long)D,(long)N));
                    out.writeInt((int)power2((long)ex_double_enc_cards[2], (long)D,(long)N));
                    out.writeInt((int)power2((long)ex_double_enc_cards[3], (long)D,(long)N));
                    out.writeInt((int)power2((long)ex_double_enc_cards[4], (long)D,(long)N));

                    shuffle(my_cards); // Дополнительно перемешиваем свои карты
                    for(int i = 0; i<my_cards.length; i++){ // Играем
                        int temp[] = new int[2];
                        System.out.println("\u001b[33mAlice plays: " + my_cards[i] + " (" + suit_of(my_cards[i]) + " " + value_of(my_cards[i]) + ")");
                        out.writeInt(my_cards[i]);
                        ex_cards[i] = in.readInt();
                        System.out.println("Bob plays: " + ex_cards[i] + " (" + suit_of(ex_cards[i]) + " " + value_of(ex_cards[i]) + ")\n\u001b[0m");
                        temp = game(my_cards[i],ex_cards[i]);
                        score[i][0] = temp[0];
                        score[i][1] = temp[1];
                    }
                    System.out.println("Счёт текущей партии: \nAlice: \u001b[33m" + (score[0][0]+score[1][0]+score[2][0]+score[3][0]+score[4][0]));
                    System.out.println("\u001b[0mBob: \u001b[33m" + (score[0][1]+score[1][1]+score[2][1]+score[3][1]+score[4][1]) + "\u001b[0m");

                    out.writeInt(E); // Обмен открытым и закрытым ключом в конце игры
                    out.writeInt(D);
                    ex_E = in.readInt();
                    ex_D = in.readInt();

                    test_cards[0] = (int)power2(((int)power2((long)ex_double_enc_cards[0], (long)D,(long)N)),ex_D,N);
                    test_cards[1] = (int)power2(((int)power2((long)ex_double_enc_cards[1], (long)D,(long)N)),ex_D,N);
                    test_cards[2] = (int)power2(((int)power2((long)ex_double_enc_cards[2], (long)D,(long)N)),ex_D,N);
                    test_cards[3] = (int)power2(((int)power2((long)ex_double_enc_cards[3], (long)D,(long)N)),ex_D,N);
                    test_cards[4] = (int)power2(((int)power2((long)ex_double_enc_cards[4], (long)D,(long)N)),ex_D,N);

                    count = 0;
                    for(int i =0;i<test_cards.length;i++){
                        for(int j =0; j<ex_cards.length;j++){
                            if(test_cards[i]==ex_cards[j]){count++;}
                        }
                    }
                    System.out.println("Кол-во честных карт " + external_username + ": " + count);
                    System.out.println("Открытый ключ оппонента:  " + ex_E + "\nЗакрытый ключ оппонента: " + ex_D);








                    break;
                case 2:
                    try {
                    ss = new ServerSocket(Port); // создаем сокет сервера и привязываем его к вышеуказанному порту
                    System.out.println("Ожидание 2 игрока...");
                    socket = ss.accept(); // Ждём 2 игрока
                       sin = socket.getInputStream(); // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                       sout = socket.getOutputStream();
                       in = new DataInputStream(sin);
                       out = new DataOutputStream(sout);
                    external_username = in.readUTF();
                    out.writeUTF(internal_username);
                    System.out.println("К столу подключился 2 игрок, его псевдоним: " + "\u001b[33m" + external_username + "\u001b[0m");
                    hasConnection = true; // Поднимаем флаг
                    } catch (Exception x) {
                        System.out.println("Подключение не удалось!");
                    }
                    break;
                case 3:
                    try {
                        while(true){
                            System.out.println("Введите порт стола (1025..65535): ");
                            Scanner sp = new Scanner(System.in);
                            if(sp.hasNextInt()){Port = sp.nextInt();}else{System.out.println("Попробуйте ещё раз!");}
                            break;
                        }
                        if(Port < 1025 || Port > 65535){System.exit(4);}
                        System.out.println("Подключаемся к столу: " + Port);
                        socket = new Socket("127.0.0.1", Port); // Подключаемся к столу
                        sin = socket.getInputStream(); // Конвертируем потоки в другой тип, чтоб легче обрабатывать текстовые сообщения.
                        sout = socket.getOutputStream();
                        in = new DataInputStream(sin);
                        out = new DataOutputStream(sout);
                        System.out.println("Подключение успешно!");
                        out.writeUTF(internal_username);
                        external_username = in.readUTF();
                        System.out.println("Оппонент: " + "\u001b[33m" + external_username + "\u001b[0m");
                        System.out.println("Ожидание начала игры...");
                        P = in.readInt();
                        Q = in.readInt();
                        N = P*Q;
                        F = (P-1)*(Q-1);
                        System.out.println("P = " + P + "\nQ = " + Q + "\nN = " + N + "\nφ = " + F);
                        hasConnection = true;

                        for(int i = 0; i < 100;) // Находим 100 различных открытых экспонент
                        {
                            if((gcd(rnd, F) == 1) && (rnd < F)) // Проверка на Взаимно простое с φ, открытая экспонента < φ
                            {
                                exp[i] = rnd;
                                i++;
                            }
                            rnd++;
                        }
                        //for(int i = 0; i < 100; i++){System.out.println(exp[i]);}
                        // Закрытый ключ: modInverse(rnd, FuncEuler) % FuncEuler
                        rand = (int)(Math.random()*(99+1))+0; // Случайным образом выбираем экспоненту из массива открытых экспонент [100]
                        E = exp[rand];
                        D = (int)modInverse(E,F) % F;
                        System.out.println("Открытый ключ [E,N]: " + E + ", " + N + "\nЗакрытый ключ [D,N]: " + D + ", " + N);

                        for(int n = 0; n < en_cards.length; n++){
                            en_cards[n] = in.readInt(); // Принимаем зашифрованные + перетасованные карты от Алисы
                        }
                        int temp1 = 0;
                        for(int i = 0; i < ex_cards.length;){
                            do {temp1 = (int)(Math.random()*(51+1))+0;
                                //System.out.println("Где-то совпало, temp1: " + temp1);
                            }
                            while(((ex_cards[0] == temp1)||(ex_cards[1] == temp1)||(ex_cards[2] == temp1)||(ex_cards[3] == temp1)||(ex_cards[4] == temp1)));
                            ex_cards[i]=temp1;
                            i++;
                        }

                        for (int i = 0; i < ex_cards.length; i++){System.out.println("EX_CARDS[" + i + "]: " + ex_cards[i]);} // Проверяем что напикали


                        test_cards[0] = en_cards[ex_cards[0]];
                        test_cards[1] = en_cards[ex_cards[1]];
                        test_cards[2] = en_cards[ex_cards[2]];
                        test_cards[3] = en_cards[ex_cards[3]];
                        test_cards[4] = en_cards[ex_cards[4]];
                        out.writeInt(test_cards[0]);
                        out.writeInt(test_cards[1]);
                        out.writeInt(test_cards[2]);
                        out.writeInt(test_cards[3]);
                        out.writeInt(test_cards[4]); // Отправили 5 шифрограмм Алисы -> Алисе

                        for(int i = 0; i < 5; i++){System.out.println("Отправили: " + en_cards[ex_cards[i]]);}

                        // Шаг 5
                        for(int i = 0; i < my_enc_cards.length;){// проверка на то чтобы не было карт которые отправил Алисе + на повторение
                            do {temp1 = (int)(Math.random()*(51+1))+0;
                                //System.out.println("Где-то совпало, temp: " + temp);
                            }
                            while(((ex_cards[0] == temp1)||(ex_cards[1] == temp1)||(ex_cards[2] == temp1)||(ex_cards[3] == temp1)||(ex_cards[4] == temp1)||(my_enc_cards[0] == temp1)||(my_enc_cards[1] == temp1)||(my_enc_cards[2] == temp1)||(my_enc_cards[3] == temp1)||(my_enc_cards[4] == temp1)));
                            my_enc_cards[i]=temp1;
                            i++;
                        }

                        for (int i = 0; i < my_enc_cards.length; i++){System.out.println("My_Enc_CARDS[" + i + "]: " + my_enc_cards[i]);} // Проверяем что напикали

                        out.writeInt((int)power2((long)(en_cards[my_enc_cards[0]]),(long)E,(long)N));
                        out.writeInt((int)power2((long)(en_cards[my_enc_cards[1]]),(long)E,(long)N));
                        out.writeInt((int)power2((long)(en_cards[my_enc_cards[2]]),(long)E,(long)N));
                        out.writeInt((int)power2((long)(en_cards[my_enc_cards[3]]),(long)E,(long)N));
                        out.writeInt((int)power2((long)(en_cards[my_enc_cards[4]]),(long)E,(long)N)); // Отправили 5 шифрограмм Боба -> Алисе

                        // Шаг 5 закончен.
                        // Шаг 7
                        for(int i = 0; i < my_enc_cards.length; i++){
                            my_enc_cards[i] = in.readInt(); // Приняли 5 шифрограмм от Алисы
                        }
                        for(int i = 0; i < 5; i++){System.out.println("Пришло: " + my_enc_cards[i]);}
                        System.out.println("Расшифровываем...");
                        for(int i = 0; i < 5; i++){
                            my_cards[i] = (int)power2((long)my_enc_cards[i], (long)D, (long)N);
                        }
                        System.out.print("\u001b[34m");
                        for(int i = 0; i < 5; i++){
                            System.out.println("Bob's card [" + (i+1) + "]: " + my_cards[i] + " (" + suit_of(my_cards[i]) + " " + value_of(my_cards[i]) + ")");
                        }
                        System.out.print("\u001b[0m");
                        shuffle(my_cards); // Дополнительно перемешиваем свои карты

                        for(int i = 0; i<my_cards.length; i++){ // Играем
                            int temp[] = new int[2];
                            ex_cards[i] = in.readInt();
                            System.out.println("\u001b[33mAlice plays: " + ex_cards[i] + " (" + suit_of(ex_cards[i]) + " " + value_of(ex_cards[i]) + ")");
                            out.writeInt(my_cards[i]);
                            System.out.println("Bob plays: " + my_cards[i] + " (" + suit_of(my_cards[i]) + " " + value_of(my_cards[i]) + ")\n\u001b[0m");
                            temp = game(my_cards[i],ex_cards[i]);
                            score[i][0] = temp[0];
                            score[i][1] = temp[1];
                        }
                        System.out.println("Счёт текущей партии: \nAlice: \u001b[33m" + (score[0][1]+score[1][1]+score[2][1]+score[3][1]+score[4][1]));
                        System.out.println("\u001b[0mBob: \u001b[33m" + (score[0][0]+score[1][0]+score[2][0]+score[3][0]+score[4][0]) + "\u001b[0m");

                        out.writeInt(E); // Обмен открытым и закрытым ключом в конце игры
                        out.writeInt(D);
                        ex_E = in.readInt();
                        ex_D = in.readInt();


                        count = 0;

                        test_cards[0] = (int)power2(test_cards[0],ex_D,N);
                        test_cards[1] = (int)power2(test_cards[1],ex_D,N);
                        test_cards[2] = (int)power2(test_cards[2],ex_D,N);
                        test_cards[3] = (int)power2(test_cards[3],ex_D,N);
                        test_cards[4] = (int)power2(test_cards[4],ex_D,N);



                        for(int i =0;i<test_cards.length;i++){
                            for(int j =0; j<ex_cards.length;j++){
                                if(test_cards[i]==ex_cards[j]){count++;}
                            }
                        }
                        System.out.println("Кол-во честных карт " + external_username + ": " + count);
                        System.out.println("Открытый ключ оппонента:  " + ex_E + "\nЗакрытый ключ оппонента: " + ex_D);











                    } catch (Exception x) {
                        System.out.println("Подключение не удалось!");
                    }
                    break;
                default:
                    System.out.println("#Err");
                    break;
            }
        }
    }

    public static int[] shuffle(int[] array) {
        Random rand = new Random();
        for (int i = 0; i < array.length; i++) {
            int randomIndexToSwap = rand.nextInt(array.length);
            int temp = array[randomIndexToSwap];
            array[randomIndexToSwap] = array[i];
            array[i] = temp;
        }
        return array;
    }
    public static String value_of(int x) { // Узнаем название карты
        String v = "";
        if (x % 13 == 12) {
            v = "Ace";
            return v;
        }
        if (x % 13 == 0) {
            v = "2";
            return v;
        } else if (x % 13 == 1) {
            v = "3";
            return v;
        } else if (x % 13 == 2) {
            v = "4";
            return v;
        } else if (x % 13 == 3) {
            v = "5";
            return v;
        } else if (x % 13 == 4) {
            v = "6";
            return v;
        } else if (x % 13 == 5) {
            v = "7";
            return v;
        } else if (x % 13 == 6) {
            v = "8";
            return v;
        } else if (x % 13 == 7) {
            v = "9";
            return v;
        } else if (x % 13 == 8) {
            v = "10";
            return v;
        } else if (x % 13 == 9) {
            v = "Jack";
            return v;
        } else if (x % 13 == 10) {
            v = "Queen";
            return v;
        } else if (x % 13 == 11) {
            v = "King";
            return v;
        } else {
            v = "Ace";
            return v;
        }
    }
    public static String suit_of(int card){ // Узнаём масть карты
        String s="";
        if(card/13==0)
        {
            s="Spade";
            return s;
        }
        else if(card/13==1)
        {
            s="Heart";
            return s;
        }
        else if(card/13==2)
        {
            s="Club";
            return s;
        }
        else if(card/13==3)
        {
            s="Diamond";
            return s;
        }
        else{
            s="Spade";
            return s;
        }
    }

    public static int[] game(int a, int b){
        String suit_a = suit_of(a);
        String suit_b = suit_of(b);// Если карты по мощности равны определяем победителя по старшинству мастей в покере: Spade, Heart, Diamond, Club
        int pow_a = 0;
        int pow_b = 0;
        if(suit_a.equals("Spade")){pow_a = 4;} // Если игрок А прислал самую старшую масть = выиграл А
        if(suit_a.equals("Heart")){pow_a = 3;}
        if(suit_a.equals("Diamond")){pow_a = 2;}
        if(suit_a.equals("Club")){pow_a = 1;} // Если игрок А прислал самую младшую масть = выиграл Б

        if(suit_b.equals("Spade")){pow_b = 4;}
        if(suit_b.equals("Heart")){pow_b = 3;}
        if(suit_b.equals("Diamond")){pow_b = 2;}
        if(suit_b.equals("Club")){pow_b = 1;}

        int temp[] = new int[2];

        if(a>b){temp[0]=1; return temp;}
        if(a<b){temp[1]=1; return temp;}
        else{
            if(pow_a > pow_b){temp[0]=1; return temp;}
            if(pow_a < pow_b){temp[1]=1; return temp;} // Определяем победителя по мастям в случае если мощность карт одинакова
        }
        return temp; // Если возвращает это 0 0, то ошибка в подборе карт
    }

    public static int[] enc_deck(int[] array,int deg,int mod){
        int temp[] = new int[array.length];
        for(int i = 0; i<array.length;i++){
            temp[i] = array[i];
        }
        for(int i = 0; i<array.length;i++){
            array[i] = (int)power2((long)temp[i],(long)deg,(long)mod);
        }
        return array;
    }
    public static int generationLargeNumber(){
        // Генерация числа
        int min = 1480, max = 9960, k = 10;
        int i = min + (int)(Math.random() * max);
        for( ; i <= max; i ++) { if(isPrime(i, k)) return i; }
        for( ; i >= min; i --) { if(isPrime(i, k)) return i; }
        return -1;
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
    public static long modInverse(long a, long m){ // Обратный элемент по модулю
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
    public static String Coding(String text, int ModLength) {
        // Исходный текст, Длина модуля в двоичном виде
        // Функция перевода каждого символа в его код
        String result = "";
        for(int i = 0; i < text.length(); i ++)
        {
            result += getCodeChar(text.charAt(i), ModLength); // Обрабатываем символы согласно кодировке
        }
        return result;
    }
    public static String getCodeChar(char symbol, int ModLength) {
        // Если символ представлен от А до Я, то кодируем
        if(symbol >= 'А' && symbol <= 'Я') return toDynamicBinaryString(symbol - EMPTY_CHAR, ModLength);
            // Если символ "пробел", то меняем его код на 99
        else if(symbol == ' ')return toDynamicBinaryString(SPACE_NUMBER, ModLength); //
        return ""; // Если символ не из таблицы кодировки и не "пробел", то просто возвращаем
    }
    public static String toDynamicBinaryString(int symbol, int ModLength) {
        // Добавляем незначащие нули, если не хватает на цельный блок
        return String.format("%"+ ModLength +"s", Integer.toBinaryString(symbol)).replace(' ','0');
    }
    public static int CompareLine(String text, String mod) {
        // Функция сравнения блока с текстом, превышает ли он длину модуля
        for(int i = 0; i < mod.length(); i ++) // Пробегаемся по длине модуля
        {
            if(mod.charAt(i) < text.charAt(i)) return 1;
            else if(mod.charAt(i) > text.charAt(i))return 0;
        }
        return 1;
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
    public static long power2(long x, long y, long p) {
        // Возведение в степень по модулю
        long res = 1; // Initialize result

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
    public static String textToBinary(String text, int ModLength) {
        String resultLine = "";
        for(int i = 0; i < text.length(); i ++)
        {
            resultLine += toDynamicBinaryString(text.charAt(i),ModLength);
        }
        return resultLine;
    }
    public static String binaryToText(String text, int ModLength) {
        String resultLine = "";
        String binary = "";
        for(int i = 0; i < text.length(); i += ModLength)
        {
            binary = "";
            for(int j = 0; j < ModLength && i+j < text.length(); j ++)binary = binary + text.charAt(i + j);
            // формируем двоичную запись
            resultLine += getCharCode(binary); // Переводим в число, а далее в символ
        }

        return resultLine;
    }
    public static char getCharCode(String binary) {
        int character = Integer.parseInt(binary,2);
        if(character == 99) return ' ';
        return (char)(character + EMPTY_CHAR);
    }
    public static int menu() {
        int num = 0;
        while (true) {
            System.out.println("Menu: " + "\n0. Exit" + "\n1. Новая партия" + "\n2. Создать стол" + "\n3. Подключиться к столу" + "\nВвод: ");
            Scanner scan = new Scanner(System.in); // Проверка на int
            if (scan.hasNextInt()) {
                num = scan.nextInt();
                break;
            } else {
                System.out.println("Введите число!");
            }
            scan.nextLine(); // Переводим сканнер на следующую строчку (очищаем буфер)
        }
        return num;
    }
}
