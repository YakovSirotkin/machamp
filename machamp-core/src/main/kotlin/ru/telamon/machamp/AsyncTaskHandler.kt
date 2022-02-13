package ru.telamon.machamp

interface AsyncTaskHandler {
    fun getType() : String
    fun process(asyncTask : AsyncTask) : Boolean
}