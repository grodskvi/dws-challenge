# dws-challenge

## Further improvements
* Use persistent storage
* Use Spring's Transaction Management
* Add metrics: request counts, error counts, requests timing, time waiting for locks
* Store transfer results(both succeeded and failed)
* Separate accepting and processing transfer requests, so they could be handled asynchronously
* Separate domain entities(Account, TransferRequest) and their representation in REST
* Improve controller tests by using json assert libraries
