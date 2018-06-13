package ru.vampa.primitiveBank.service;

/**
 * Исключение, говорящее нам, что такого счета в системе нет.
 * Id прилагается
 *
 * @author vbelyashov
 */
public class NoAccountRegisteredException extends Exception {
    NoAccountRegisteredException(long accountId) {
        super(String.valueOf(accountId), null, true, false);
    }
}
