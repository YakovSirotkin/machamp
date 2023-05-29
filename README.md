# machamp


<p>
  <img alt="Version" src="https://img.shields.io/badge/version-0.0.18-blue.svg?cacheSeconds=2592000" />
  <a href="https://github.com/yakovsirotkin/machamp/blob/master/LICENSE">
    <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg"/>
  </a>
</p>
Async task processing engine for Spring Boot and PostgreSQL

# Design
Let's assume that an application needs to email the user, but the SMTP host is unreachable. Throwing an exception and showing it to the user after connection timeout seems bad. Resending immediately will not help. Resending with a delay is better, but if the number of attempts is not limited, we can easily overload the server if the outage continues. Looks like we need to store information
about the outgoing email somewhere and process it later. However, this will require some additional code that saves-loads emails, and it is not likely to be used every year, so there is a high chance that it will be broken when it is needed.
Alternatively, we can simply save an outgoing email to the database all the time and automatically process it with a standard workflow to solve this issue. 

Machamp provides implementation for the standard workflow mentioned above. It has several threads (implemented as coroutines). Each thread loads tasks one by one, if there are no tasks, it pauses for 1 second. If the system is lazy and has 10 threads, the expected delay for processing a new task will be about 0.1 second. Also, we have limited potential load on the external server, proportional to the number of threads.


Another important aspect is that if the task fails, its processing is delayed by 1 minute. After the 2nd failed attempt delay will be increased to 2 minutes, the 3rd - 4 minutes and so on using the powers of 2. Hence, if we receive a huge set of broken tasks, it will affect the overall performance, but the impact will be limited and the system will be back to normal automatically.
More than that, if we deploy a fix in 2 days, all the tasks will be processed in another 2 days automatically.

This solution is relevant to many situations when we need to call an external system, including almost all payment systems.  


<a href="http://telamon.ru/articles/async.html">Short article in Russian</a>

# Usage

## Download

### Gradle

```gradle
//gradle kotlin DSL
implementation("io.github.yakovsirotkin:machamp-spring-boot-starter:0.0.18")

//gradle groovy DSL
implementation 'io.github.yakovsirotkin:machamp-spring-boot-starter:0.0.18'
```

### Maven

```maven
<dependency>
  <groupId>io.github.yakovsirotkin</groupId>
  <artifactId>machamp-spring-boot-starter</artifactId>
  <version>0.0.18</version>
</dependency>
```

[kscript](https://github.com/holgerbrandl/kscript)

```kotlin
@file:DependsOn("io.github.yakovsirotkin:machamp-spring-boot-starter:0.0.18")
```

## Database table creation
[machamp-core/src/main/resources/sql/001-init.sql](https://github.com/YakovSirotkin/machamp/blob/main/machamp-core/src/main/resources/sql/001-init.sql)
```
CREATE TABLE async_task
(
    task_id      BIGSERIAL PRIMARY KEY,
    task_type    TEXT,
    description  JSON,
    attempt      SMALLINT    NOT NULL DEFAULT 0,
    priority     INTEGER     NOT NULL DEFAULT 100,
    process_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    taken        TIMESTAMPTZ          DEFAULT NULL
);
```
## Task Definition

### Kotlin
```
package io.github.yakovsirotkin.machamp

import org.springframework.stereotype.Component

public const val TASK_TYPE = "MY_TASK"

@Component
class MyAsyncTaskHandler : AsyncTaskHandler {
    override fun getType(): String {
        return TASK_TYPE
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        val description = asyncTask.description //Read task parameters
        //
        //process asyncTask here
        //
        return true//Task processed successfully and can be deleted from database
    }
}
```

### Java
```
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@Component
public class MyAsyncTaskHandler implements AsyncTaskHandler {

    public static final String TASK_TYPE = "MY_TASK";

    @Override
    public String getType() {
        return TASK_TYPE;
    }


    @Override
    public boolean process(AsyncTask asyncTask) {
        JsonNode description = asyncTask.getDescription();//Read task parameters
        //
        //process asyncTask here
        //
        return true;//Task processed successfully and can be deleted from database        
    }
}
```
## Adding async task

### Kotlin
```
import io.github.yakovsirotkin.machamp.AsyncTaskDao

private val asyncTaskDao: AsyncTaskDao, //in bean constructor

        //In code
        asyncTaskDao.createTask(
            taskType = MyAsyncTaskHandlerJava.TASK_TYPE,
            description = "{\"value\": 1}",
            priority = 100, //optional parameter
            delayInSeconds = 0 //optional parameter
        )
```

### Java

```
import io.github.yakovsirotkin.machamp.AsyncTaskDao;

    private AsyncTaskDao asyncTaskDao; //bean property
   
        //In code
        asyncTaskDao.createTask(
                MyAsyncTaskHandler.TASK_TYPE, //task type
                "{\"value\": 1}", //description
                100, //priority
                0 //delay in seconds
        );  
```
## Lazy initialization

In case of `spring.main.lazy-initialization=true` you should initialize bean that depends on
`io.github.yakovsirotkin.machamp.AsyncTaskProcessor` or it will not process anything.

## Configuration parameters

| Option                        | default value | description                                                                        |
|-------------------------------|---------------|------------------------------------------------------------------------------------|
| machamp.processor.threads     | `10`          | Number of coroutines that process async tasks in parallel.                         |
| machamp.priority.enabled      | `true`        | Load tasks with less priority values first if `true` or ignore priority otherwise. |
| machamp.priority.defaultValue | `100`         | Default priority value for async tasks.                                            |
| machamp.adminEnabled          | `false`       | Allows to enable admin interface at /machamp/admin/                                |
| machamp.taskTable             | `async_task`  | Allows to set the name for the database table with machamp tasks                   |

#Other databases support

##SQL Server

To use machamp with SQL Server you need to use `machamp-sqlserver-spring-boot-starter` package instead of
`machamp-spring-boot-starter`.

To create database table you need to apply script

[machamp-sqlserver/src/main/resources/sql/001-init.sql](https://github.com/YakovSirotkin/machamp/blob/main/machamp-sqlserver/src/main/resources/sql/001-init.sql)

```
CREATE TABLE async_task
(
    task_id      BIGINT IDENTITY(1,1) PRIMARY KEY,
    task_type    VARCHAR(255),
    description  NVARCHAR(max),
    attempt      SMALLINT    NOT NULL DEFAULT 0,
    priority     INTEGER     NOT NULL DEFAULT 100,
    process_time DATETIMEOFFSET NOT NULL DEFAULT GETUTCDATE(),
    created      DATETIMEOFFSET NOT NULL DEFAULT GETUTCDATE(),
    taken        DATETIMEOFFSET          DEFAULT NULL
);
```

##Oracle

To use machamp with Oracle you need to use `machamp-oracle-spring-boot-starter` package instead of
`machamp-spring-boot-starter`. Machamp supports Oracle starting version 12c.

To create database table you need to apply script

[machamp-oracle/src/main/resources/sql/001-init.sql](https://github.com/YakovSirotkin/machamp/blob/main/machamp-oracle/src/main/resources/sql/001-init.sql)

```
CREATE TABLE async_task
(
    task_id      NUMBER(38) PRIMARY KEY,
    task_type    VARCHAR2(255),
    description  CLOB,
    attempt      NUMBER(38) DEFAULT 0   NOT NULL,
    priority     NUMBER(38) DEFAULT 100 NOT NULL,
    process_time TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    created      TIMESTAMP WITH TIME ZONE DEFAULT SYSTIMESTAMP NOT NULL,
    taken        TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE SEQUENCE async_task_seq START WITH 1;
```
### Oracle additional configuration parameters

| Option                        | default value    | description                                                                        |
|-------------------------------|------------------|------------------------------------------------------------------------------------|
| machamp.taskSequence          | `async_task_seq` | Allows to set the name for the sequence for the machamp tasks ids                  |

