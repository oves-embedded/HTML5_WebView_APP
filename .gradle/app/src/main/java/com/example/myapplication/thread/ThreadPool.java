package com.example.myapplication.thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {
    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static ExecutorService getExecutor(){
        return executorService;
    }

    public static void shutDown(){
        executorService.shutdownNow();
        executorService=null;
    }

}
