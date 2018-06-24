package ru.vampa.primitiveBank.dao;

/**
 * Интерфейс для dao, работающим со счетами
 *
 * @author vbelyashov
 */
public interface IAccountDao {
    /**
     * Суммарный баланс всех доступных счетов
     */
    long getTotalBalance();

    /**
     * @param accountId id счета
     * @return состояние счета
     * @throws NoAccountRegisteredException если счет с таким id не существует
     */
    long getBalance(long accountId) throws NoAccountRegisteredException;

    /**
     * Перевод с одного счета на другой
     * @param accountId id счета-источника
     * @param destinationId id счета-назначения
     * @param amount сумма перевода
     * @throws InsufficientFundsException если недостаточно средств на счете-источнике
     * @throws NoAccountRegisteredException если счет с таким id не существует
     */
    void transfer(long accountId, long destinationId, long amount)
            throws InsufficientFundsException, NoAccountRegisteredException;

    /**
     * Внесение средств на счет
     * @param accountId id счета
     * @param amount сумма внесения, не может быть отрицательной
     * @throws NoAccountRegisteredException если счет с таким id не существует
     * @throws IllegalArgumentException при отрицательной сумме
     */
    void deposit(long accountId, long amount) throws NoAccountRegisteredException;

    /**
     * Снятие средств со счета
     * @param accountId id счета
     * @param amount сумма снятия, не может быть отрицательной
     * @throws InsufficientFundsException если недостаточно средств на счете
     * @throws NoAccountRegisteredException если счет с таким id не существует
     * @throws IllegalArgumentException при отрицательной сумме
     */
    void withdraw(long accountId, long amount) throws InsufficientFundsException, NoAccountRegisteredException;
}
