# Reporting 

Imagine that you are working on corporate system, the system processes contracts: with statuses, 
attachments and many various parameters. Some time ago business asked to implement basic reports. 
First version was implemented with simple SQL requests. After that some subqueries  
and some additional columns were added.

As time goes this approach works slower and slower, SQL code starts to be hard to understand. 
And, after special tables were added for some types of contracts, SQL queries were enriched with UNIONs.
At next stage you create tables just to store report data.

If you use microservice, you can create separate microservice for reports. And you can choose 
to use some queue systems (Kafka, RabbitMQ, etc.) to send updates to reporting.
Sounds great from performance point of view, but what if some updates are lost? 
Or you process them in the wrong order? Or some data in updates payload were corrupted? 
Or you need to change the algorithm for calculating some values? The bottom line 
is that you can't sleep well anymore.

The solution is to use async task processing with machamp. If there are some changes 
in any contract, you just create async task to update the report rows. More than that, 
you can do it in the same database transaction. This is the sample class:

```kotlin
import io.github.yakovsirotkin.machamp.AsyncTaskDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class ContractService @Autowired constructor(
    val transactionTemplate: TransactionTemplate,
    val contractDao: ContractDao,
    val asyncTaskDao: AsyncTaskDao
){
    fun doBusiness(contractId: Long) {
        val contract = contractDao.loadContract(contractId)
        //
        //business logic here
        //
        val reportTaskId = transactionTemplate.execute {
            contractDao.update(contract)
            asyncTaskDao.createTask( //This call can be extracted to special method if it is used in many places.   
                ReportUpdateAsyncTaskHandler.TASK_TYPE,
                "{\"contractId\": $contractId}"
            )
        }!!
        //It's enough to ensure that report is updated after the last change.
        //If contract was updated 10 times in single second, there is no need to repeat this load test on reporting.  
        asyncTaskDao.deleteDuplicateTask(reportTaskId, ReportUpdateAsyncTaskHandler.TASK_TYPE,
            "contractId", contractId)
    }
}
```

The implementation with database transaction guarantees that report will be updated after each data change.

Now let's take a look at report update implementation itself:

```kotlin
import io.github.yakovsirotkin.machamp.AsyncTask
import io.github.yakovsirotkin.machamp.AsyncTaskHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class ReportUpdateAsyncTaskHandler @Autowired constructor(
    val transactionTemplate: TransactionTemplate,
    val reportDao: ReportDao
) : AsyncTaskHandler {

    companion object {
        const val TASK_TYPE = "report"
    }

    override fun getType(): String {
        return TASK_TYPE
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        val contractId = asyncTask.description.get("contractId").asLong()
        var contractReportData = null
        //
        //Here should be the code that prepares all the data for reporting
        //
        transactionTemplate.execute { 
            reportDao.removeData(contractId)
            //If it will be a parallel execution for the same contract, 
            //we should get the Exception about Unique Constraint Violation in database 
            reportDao.addData(contractReportData)
        }            
        return true //report was updated successfully, the task should be deleted    
    }
}
```

The core idea is to update all the contract data at once, without trying partial updates. 
Because of asynchronous approach we do not care if it takes 1 or 5 seconds. 
If we need more throughput, we can increase the number of machamp threads or run 
several machamp instances. 

The common situation is that from time to time you need to update the reports: 
add some columns or modify the existing ones, or some external parameters had been changed recently. 
The solution is to regenerate all the report data from time to time, maybe on weekends, maybe every night.

With machamp initialization of the global update can be done with one SQL statement 
(you can create endpoint for calling it):

```postgresql
--code for PostgreSQL
INSERT INTO async_task (task_type, description, priority)
SELECT 'report', ('{"contractId":' || contract_id || '}')::json, 1000 FROM contract;
```

Default priority value in machamp is 100, so this total update will not affect regular updates.
In case if you realise that you need to start another update before the end of the previous one,
you can just delete all the tasks with priority 1000 and restart the update.

If there are dozens of thousands of rows in report, user experience still can be below the perfect one,
because despite precalculated values report loading and transmitting can take significant time. It can be improved,
if you pre-generates the results: store ready CSV-lines in database or render PDF-files in advance. 

Please send you feedback to yakov.sirotkin@gmail.com.

