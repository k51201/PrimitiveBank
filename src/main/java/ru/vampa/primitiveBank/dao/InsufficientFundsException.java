package ru.vampa.primitiveBank.dao;

/**
 * Исключение, вызванное недостатком средств. Содержит id счета
 *
 * @author vbelyashov
 */
public class InsufficientFundsException extends RuntimeException {
    InsufficientFundsException(long accountId) {
        super("on account with id " + accountId + " not enough funds", null, true, false);
    }
}
