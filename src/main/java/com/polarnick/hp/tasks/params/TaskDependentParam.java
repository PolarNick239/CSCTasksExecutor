package com.polarnick.hp.tasks.params;

import com.polarnick.hp.utils.Future;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class TaskDependentParam<E> implements Param<E> {

    private final int taskId;
    private Future<E> future;

    public TaskDependentParam(int taskId) {
        this.taskId = taskId;
    }

    public void setFuture(Future<E> future) {
        this.future = future;
    }

    public int getTaskId() {
        return taskId;
    }

    @Override
    public E get() {
        return this.future.getResult();
    }

    @Override
    public E getWait() throws InterruptedException {
        return this.future.waitResult();
    }
}