package ru.vampa.primitiveBank.service;

/**
 * Исключение, вызванное недостатком средств. Содержит id счета
 *
 * @author vbelyashov
 */
public class InsufficientFundsException extends Exception {
    InsufficientFundsException(long accountId) {
        super(String.valueOf(accountId), null, true, false);
    }
}
