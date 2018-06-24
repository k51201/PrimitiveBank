package ru.vampa.primitiveBank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import ru.vampa.primitiveBank.dao.IAccountDao;
import ru.vampa.primitiveBank.dao.InsufficientFundsException;
import ru.vampa.primitiveBank.dao.NoAccountRegisteredException;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    @Autowired
    private IAccountDao accountDao;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Long getBalance(@PathVariable("id") long id) {
        return accountDao.getBalance(id);
    }

    @RequestMapping(value = "/{id}/deposit", method = RequestMethod.POST)
    public void deposit(@PathVariable("id") long id, @RequestBody long amount) {
        accountDao.deposit(id, amount);
    }

    @RequestMapping(value = "/{id}/withdraw", method = RequestMethod.POST)
    public void withdraw(@PathVariable("id") long id, @RequestBody long amount) {
        accountDao.withdraw(id, amount);
    }

    @RequestMapping(value = "/{id}/transfer/{toId}", method = RequestMethod.POST)
    public void deposit(@PathVariable("id") long id, @PathVariable("toId") long toId, @RequestBody long amount) {
        accountDao.transfer(id, toId, amount);
    }

    @ExceptionHandler(NoAccountRegisteredException.class)
    public ResponseEntity<Object> handleNoAccountRegisteredException(Exception ex, WebRequest req) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<Object> handleInsufficientFundsException(Exception ex, WebRequest req) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }
}
