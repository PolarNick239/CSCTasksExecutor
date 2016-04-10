package com.polarnick.hp.tasks;

import com.polarnick.hp.tasks.params.TaskDependentParam;
import com.polarnick.hp.utils.Future;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public abstract class Task<E> implements Runnable {

    private final int taskId;
    private final String clientId;
    private final Future<E> result;
    private final List<TaskDoneCallback> doneCallbacks;

    public Task(int taskId, String clientId) {
        this.taskId = taskId;
        this.clientId = clientId;
        this.result = new Future<>();
        this.doneCallbacks = new ArrayList<>();
    }

    public List<TaskDependentParam> getDependencies() {
        return Collections.emptyList();
    }

    public abstract E execute();

    public void run() {
        E result = this.execute();
        this.result.setResult(result);
        for (TaskDoneCallback callback: doneCallbacks) {
            callback.onTaskDone(this);
        }
        doneCallbacks.clear();
    }

    public String getClientId() {
        return clientId;
    }

    public int getTaskId() {
        return taskId;
    }

    public boolean isFinished() {
        return this.result.isFinished();
    }

    public Future<E> getFuture() {
        return this.result;
    }

    public E waitResult() throws InterruptedException {
        return result.waitResult();
    }

    public E getResult() {
        return result.getResult();
    }

    public void addDoneCallback(TaskDoneCallback callback) {
        assert !this.isFinished();
        doneCallbacks.add(callback);
    }

}
