package ru.vampa.primitiveBank.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Служба работы со счетами в памяти через HashMap
 *
 * @author vbelyashov
 */

public class HashMapAccountService implements IAccountService {
    private final Map<Long, AtomicLong> accounts = new HashMap<>();

    @Override
    public long getTotalBalance() {
        return accounts.values().stream().mapToLong(AtomicLong::get).sum();
    }

    @Override
    public long getBalance(long accountId) throws NoAccountRegisteredException {
        final AtomicLong account = getAccount(accountId);
        return account.get();
    }

    @Override
    public void transfer(long accountId, long destinationId, long amount)
            throws NoAccountRegisteredException, InsufficientFundsException {
        if (accountId < destinationId)
            synchronized ((Long) accountId) {
                synchronized ((Long) destinationId) {
                    doTransfer(accountId, destinationId, amount);
                }
            }
        else if (destinationId < accountId)
            synchronized ((Long) destinationId) {
                synchronized ((Long) accountId) {
                    doTransfer(accountId, destinationId, amount);
                }
            }
    }

    private void doTransfer(long accountId, long destinationId, long amount) throws NoAccountRegisteredException, InsufficientFundsException {
        if (amount < 0) transfer(destinationId, accountId, -amount);

        final AtomicLong to = getAccount(destinationId);
        trySubtract(accountId, amount);
        to.addAndGet(amount);
    }

    @Override
    public void deposit(long accountId, long amount) throws NoAccountRegisteredException {
        if (amount < 0)
            throw new IllegalArgumentException();
        getAccount(accountId).addAndGet(amount);
    }

    @Override
    public void withdraw(long accountId, long amount) throws NoAccountRegisteredException, InsufficientFundsException {
        trySubtract(accountId, amount);
    }

    /**
     * Получаем AtomicLong счета, если он существует
     * @param accountId id счета
     * @throws NoAccountRegisteredException если счет не существует
     */
    private AtomicLong getAccount(Long accountId) throws NoAccountRegisteredException {
        final AtomicLong account = accounts.get(accountId);
        if (account == null)
            throw new NoAccountRegisteredException(accountId);
        return account;
    }

    /**
     * Пытаемся снять деньги со счета, пока не поймем, что либо сняли, либо на счете столько нет
     * @param accountId id счета
     * @param amount сумма снятия
     * @throws NoAccountRegisteredException если счет не существует
     * @throws InsufficientFundsException если недостаточно средств на счете
     */
    private void trySubtract(long accountId, long amount) throws NoAccountRegisteredException, InsufficientFundsException {
        if (amount < 0)
            throw new IllegalArgumentException();

        final AtomicLong account = getAccount(accountId);
        long prev, next;
        do {
            prev = account.get();
            if (prev < amount)
                throw new InsufficientFundsException(accountId);
            next = prev - amount;
        } while (!account.compareAndSet(prev, next));
    }
}
