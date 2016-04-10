package com.polarnick.hp.utils;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Date: 10.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class FixedThreadPoolExecutor {

    private final Queue<Runnable> tasks;
    private final Object lock = new Object();

    public FixedThreadPoolExecutor(int threads) {
        tasks = new ArrayDeque<>();
        for (int i = 0; i < threads; i++) {
            new Thread(new Worker(), "Worker#" + i).start();
        }
    }

    public void execute(Runnable runnable) {
        synchronized (lock) {
            tasks.add(runnable);
            lock.notify();
        }
    }

    private class Worker implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    Runnable task;
                    synchronized (lock) {
                        while (tasks.size() == 0) {
                            lock.wait();
                        }
                        task = tasks.poll();
                    }
                    task.run();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
