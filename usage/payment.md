# Outgoing payments

Imagine that your need to send money to people around the globe. It's clear that you need to
use some payment provider that knows how to work with banks in different countries and can handle sensitive users' data.
If you try to do the payment with one REST call - this is wrong: if connection breaks by any reason 
you just do not know the status of the payment. With machamp outgoing payments can be implemented in scalable
and reliable way.

First, you need to create internal record with payment information and async task for its processing:

```kotlin
transactionTemplate.execute {
    val paymentId = paymentDao.createPayment(userId, amount, description)
    asyncTaskDao.createTask(InitPaymentAsyncTaskHandler.TASK_TYPE, "{\"paymentId\": $paymentId}")
}
```

Please note, that this code only inserts records to the database, it can initialize dozens of payments per second 
on the smallest available cloud instances. And we don't care if payment provider is down right now. More than that, 
if we change payment provider, this code will remain the same.

Now we need to transfer information about the payment to the payment provider, get payment provider ID for the payment 
and create async task for further processing. Here is the class that implements it:

```kotlin
import io.github.yakovsirotkin.machamp.AsyncTask
import io.github.yakovsirotkin.machamp.AsyncTaskDao
import io.github.yakovsirotkin.machamp.AsyncTaskHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate

@Component
class InitPaymentAsyncTaskHandler @Autowired constructor(
    val asyncTaskDao: AsyncTaskDao,
    val paymentDao: PaymentDao,
    val transactionTemplate: TransactionTemplate,
    val paymentProviderClient: PaymentProviderClient
) : AsyncTaskHandler {

    companion object {
        const val TASK_TYPE = "payment.init"
    }

    override fun getType(): String {
        return TASK_TYPE
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        val paymentId = asyncTask.description.get("paymentId").asLong()
        val paymentInfo = paymentDao.loadPaymentInfo(paymentId)
        val paymentProviderId = paymentProviderClient.initPayment(
            paymentInfo.userId,
            paymentInfo.amount,
            paymentInfo.description
        )
        transactionTemplate.execute {
            /*
            We need to guarantee that only one paymentProviderId can be saved to the database for each payment.
            By design machamp process the async task again not sooner than 1 minute, so this is paranoid approach 
            to avoid duplicate payments.    
            
            If the tasks was processed more than once (maybe because of GC pause or debug mode), several 
            payment check task can be created, but this is OK.  
             */
            paymentDao.savePaymentProviderIdIfCurrentValueIsNull(paymentId, paymentProviderId)
            asyncTaskDao.deleteTask(asyncTask.taskId)
            asyncTaskDao.createTask(
                CheckPaymentAsyncTaskHandler.TASK_TYPE,
                "{\"paymentId\": $paymentId}"
            )
        }
        return false //The task should be deleted in the transaction above this line
    }
}
```

Let's assume that payment provider has 4 states for the payment: INIT, PROCESSING, CANCELLED and DONE. 
Now we have code that moves payments to the INIT state. In case of errors several orphaned payment 
can be created at payment provider system. The class below implements further payment processing.   

```kotlin
import io.github.yakovsirotkin.machamp.AsyncTask
import io.github.yakovsirotkin.machamp.AsyncTaskHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class CheckPaymentAsyncTaskHandler @Autowired constructor(
    val paymentDao: PaymentDao,
    val paymentProviderClient: PaymentProviderClient
) : AsyncTaskHandler {

    companion object {
        const val TASK_TYPE = "payment.check"
    }

    override fun getType(): String {
        return TASK_TYPE
    }

    override fun process(asyncTask: AsyncTask): Boolean {
        val paymentId = asyncTask.description.get("paymentId").asLong()
        val paymentProviderId = paymentDao.loadPaymentInfo(paymentId).paymentProviderId!!
        val status = paymentProviderClient.getPaymentStatus(paymentProviderId)
        paymentDao.updatePaymentStatus(paymentId, status)
        when (status) {
            "INIT" -> 
                paymentProviderClient.doPayment(paymentProviderId)
            "CANCELLED", "DONE" -> 
                return true //We reached the final state, no need to check anymore
        }
        return false
    }
}
```

The 1st call of `payment.check` task expected to discover INIT state and initiate the real payment. 
The 2nd call expected to be done in 1 minute, 3rd - in 3 minutes, 4th - in 7 minutes, and so on - 
each interval is 2 time longer than the previous one, until the payment reaches the final status. 
It can be different statuses between INIT and the final status, but we just record it to the database,
no action required.

This implementation is scalable and reliable, but the information about the payment is delayed. 
Usually payment provider allows to configure callback for payment updates, here is the sample implementation: 

```kotlin
import io.github.yakovsirotkin.machamp.AsyncTaskDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CallbackController @Autowired constructor(
    val asyncTaskDao: AsyncTaskDao,
    val paymentDao: PaymentDao
){
    @PostMapping("/paymentProviderCallback")
    fun processCallback(@RequestBody paymentDto: PaymentDto) {
        val paymentProviderId = paymentDto.paymentProviderId
        val paymentId = paymentDao.findPaymentId(paymentProviderId)
        if (paymentId != null) {
            val taskId = asyncTaskDao.createTask(CheckPaymentAsyncTaskHandler.TASK_TYPE,
                "{\"paymentId\": $paymentId}")
            asyncTaskDao.deleteDuplicateTask(
                taskId, CheckPaymentAsyncTaskHandler.TASK_TYPE,"paymentId", paymentId)
        }
    }
}
```

That's all! Please send you feedback to yakov.sirotkin@gmail.com.    

