package com.company;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @org.junit.jupiter.api.Test
    void exponentiationRing() {
        assertEquals(9, Main.power(3,2,10));
        assertEquals(2, Main.power(5,1,3));
    }

    @org.junit.jupiter.api.Test
    void isPrime() {
        assertEquals(true, Main.isPrime(11, 3));
    }

    @org.junit.jupiter.api.Test
    void returnElementRing() {
        assertEquals(7, Main.modInverse(15, 26));
    }

    @org.junit.jupiter.api.Test
    void greatestCommonDivisor() {
        assertEquals(30, Main.gcd(180, 150));
        assertEquals(1, Main.gcd(500000, 23451));
    }

    @org.junit.jupiter.api.Test
    void generationLargeNumber() {
        System.out.println("Сгенерированное число: " + Main.generationLargeNumber(1300,3444, 15));
    }
}