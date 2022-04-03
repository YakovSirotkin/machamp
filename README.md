# machamp
Async task processing engine

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
