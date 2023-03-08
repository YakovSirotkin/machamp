# machamp

<p>
  <img alt="Version" src="https://img.shields.io/badge/version-0.0.17-blue.svg?cacheSeconds=2592000" />
  <a href="https://github.com/yakovsirotkin/machamp/blob/master/LICENSE">
    <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg"/>
  </a>
</p>
Async task processing engine for Spring Boot and PostgreSQL

# Design 
Let's assume that an application needs to email to the user, but SMTP host is unreachable. Throwing exception to user 
after connection timeout seems bad. Resending immediately will not help. Resending with a delay is better, but if the 
number of attempts is not limited, we can easily overload the server if the outage continue. Looks like we need to store information
about the outgoing email somewhere and process it lately. But it will require some additional code that saves-loads emails, and 
it will be used not every year, so there is a high chance that it will be broken when it is needed. 
We can just always save an outgoing email to database and automatically process it with a standard workflow to solve this
issue.   

Machamp provides implementation for the standard workflow mentioned above. It has several threads (implemented as coroutines), 
each thread loads tasks one by one, if there is no tasks, it pauses for 1 second. If the system is lazy and has 10 threads 
expected delay for processing a new task will be about 0.1 second. Also, we have limited potential load to the external server, 
proportional to the number of threads. 

Another important aspects is that if the task fails, its processing delayed by 1 minute. After the 2nd failed attempt delay 
will be 2 minutes, the 3rd - 4 minutes and so on by powers of 2. So, if we receive the huge set of broken tasks, 
it will affect the overall performance, but the impact will be limited and the system will be back to normal automatically. 
More than that, if we deploy a fix in 2 days, all the tasks will be processed in another 2 days automatically.

This solution is relevant to many situation when we need to call an external system and includes almost all payment 
systems.   

<a href="http://telamon.ru/articles/async.html">Short article in Russian</a>

# Usage

## Download

### Gradle

```gradle
//gradle kotlin DSL
implementation("io.github.yakovsirotkin:machamp-spring-boot-starter:0.0.17") 

//gradle groovy DSL
implementation 'io.github.yakovsirotkin:machamp-spring-boot-starter:0.0.17' 
```

### Maven

```maven
<dependency>
  <groupId>io.github.yakovsirotkin</groupId>
  <artifactId>machamp-spring-boot-starter</artifactId>
  <version>0.0.17</version>
</dependency>
```

[kscript](https://github.com/holgerbrandl/kscript)

```kotlin
@file:DependsOn("io.github.yakovsirotkin:machamp-spring-boot-starter:0.0.17")
```

## Database table creation
[src/main/resources/sql/001-init.sql](https://github.com/YakovSirotkin/machamp/blob/main/machamp-core/src/main/resources/sql/001-init.sql)
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
 
