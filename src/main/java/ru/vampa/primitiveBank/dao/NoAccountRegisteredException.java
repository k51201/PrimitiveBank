package ru.vampa.primitiveBank.dao;

/**
 * Исключение, говорящее нам, что такого счета в системе нет.
 * Id прилагается
 *
 * @author vbelyashov
 */
public class NoAccountRegisteredException extends RuntimeException {
    NoAccountRegisteredException(long accountId) {
        super("account with id " + accountId + " not found", null, true, false);
    }
}
