Примитивная банковская система. Написана в качестве тестового задания на Netty.
Принимаются запросы:
- "GET /accounts/111" для запроса баланса, где "111" - числовой идентификатор счета;
- "POST /accounts/111/deposit" - для внесения на счет, "111" - числовой идентификатор счета;
- "POST /accounts/111/withdraw" - для снятия, "111" - числовой идентификатор счета;
- "POST /accounts/111/transfer/222" - для перевода со счета "111" на счет "222".
В последних трёх методах сумма операции передается в теле запроса и не может быть меньше либо равна 0.
