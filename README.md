# cash-desk-module
Develop a cash operations module to support deposits, withdrawals, and balance checks for multiple cashiers in BGN and EUR currencies. Each cashier has a starting balance in both currencies. The module should allow checking balances for specific date ranges, as well as filtering by cashier name.

# Run - Perform mvn clean install. If necessary delete target folder.  
# H2 Inmemory database localhost:8080/h2-console  username/pass = sa/sa
# Postman collection can be found here :
 [: FIBank.postman_collection.json
](https://github.com/rado88x/cash-desk-module/blob/main/cashdesk/FIBank.postman_collection.json)

Brief project structure:
## 1. Root: CashdeskApplication.java
The Spring Boot entry point (@SpringBootApplication).

## 2. Config: DataLoader.java - Ordered initialization and injection of Beans.
Creating H2 records with the initial cashiers, transactions and denominations at startup using CommandLineRunner.

## 3. Controller: 
CashController.java - Exposes /api/v1/cash-operation and /api/v1/cash-balance.
CashierController.java - Exposes /api/v1/cashier/{id} 

## 4. DTO - transfer objects to provide additional abstraction layer providing decoupling db entities and exposed information to client.

## 5. Enums: 
Currency.java
BGN / EUR.
TransactionType.java
DEPOSIT / WITHDRAW.

## 6. Exception handling:
GlobalExceptionHandler.java
Catches EntityNotFoundException, IllegalArgumentException, and returns appropriate HTTP statuses.

## 7. Model/Entity:
Cashier.java
Transaction.java
Denomination.java  Can be reworked to use Java 17 records.

## 8. Security:
ApiKeyAuthFilter.java A servlet filter that checks the FIB-X-AUTH header against the configured API key.
