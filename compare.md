# Comparison with Apache Kafka and RabbitMQ

## Performance

It's clear that any in-memory queue outperform anything based on database. 
If you aggregate logs from the swarm of servers - do not try to save each line into database for further processing.

## Maintenance and observability

If there is a traditional database in the system, you probably have all the required infrastructure to support it and 
many engineers know how to check database status and fix potential issues. With separate queue solution there is a high
risk that it will start working in black box mode and only few people knows how to manage it. Of course, if the database
is not needed in your system, you probably shouldn't maintain one just to implement queue over it.

## API stability

SQL is probably one of the oldest popular technology nowadays. Machamp can be easily replaced with custom solution 
in any technology stack.

Apache Kafka and RabbitMQ are under active development nowadays, and it's quite possible that next version will require
to switch to another client library.

## Error handling

Machamp strength is built-in error handling with automatic increase of retry interval. This is critical for 
handling peak loads or outages of the external providers, for example, in payment systems. For processing events 
in real-time online game retrying failed operation in 1 minute doesn't make any sense.


## Database transaction support 

Machamp uses `JdbcTemplate` for database operations, so it's perfectly compatible with Spring Boot `TransactionTemplate`.
Let's consider internal money transfers between 2 users. And let's assume that we have 2 parts here: deduction from sender 
and balance increment for recipient. Changing sender balance, deleting task for deduction 
and adding task for balance increase should be done as one database transaction.

It's not recommended to do work with queues and other external systems inside database transaction.

## Batch support

Let's consider example from [reporting use case](usage/reporting.md):
```postgresql
--code for PostgreSQL
INSERT INTO async_task (task_type, description, priority)
SELECT 'report', ('{"contractId":' || contract_id || '}')::json, 1000 FROM contract;
```

Here we create tasks for each row in some table, everything is done inside the database, 
and it works almost instantly.

Creating batches for traditional queues is much harder and not that efficient. 

## Development and testing

For Kafka/RabbitMQ you probably need to run separate docker image for local development or add it to 
Testcontainers configuration. I consider it minor nowadays.