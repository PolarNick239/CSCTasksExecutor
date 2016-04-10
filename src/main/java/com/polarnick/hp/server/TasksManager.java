package com.polarnick.hp.server;

import com.polarnick.hp.tasks.Task;
import com.polarnick.hp.tasks.TaskDoneCallback;
import com.polarnick.hp.tasks.params.DependentTaskNotFoundException;
import com.polarnick.hp.tasks.params.TaskDependentParam;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class TasksManager implements TaskDoneCallback, Runnable {

    private int nextTaskId;

    private final Object lock = new Object();

    private final Queue<Task> tasksForSubscribing;
    private final Queue<Task> tasksForPostProcessing;

    private final Map<Integer, Task> tasks;
    private final Map<Integer, Integer> taskDependenciesNumber;
    private final Map<Integer, List<Integer>> taskSubscriptions;
    private final Set<Integer> tasksPostProcessed;

    private final ExecutorService executor;

    public TasksManager(int executionThreads) {
        this.nextTaskId = 0;

        this.tasksForSubscribing = new ArrayDeque<>();
        this.tasksForPostProcessing = new ArrayDeque<>();

        this.tasks = new HashMap<>();
        this.taskDependenciesNumber = new HashMap<>();
        //noinspection unchecked
        this.taskSubscriptions = new HashMap<>();
        this.tasksPostProcessed = new HashSet<>();

        this.executor = Executors.newFixedThreadPool(executionThreads);  // TODO: implement simple ExecutorService
    }

    public synchronized int generateTaskId() {
        return ++nextTaskId;
    }

    public <E> void addTask(Task<E> task) throws DependentTaskNotFoundException {
        synchronized (lock) {
            for (TaskDependentParam param: task.getDependencies()) {
                if (!tasks.containsKey(param.getTaskId())) {
                    throw new DependentTaskNotFoundException(param.getTaskId());
                }
            }

            tasks.put(task.getTaskId(), task);
            tasksForSubscribing.add(task);
            lock.notify();
        }
    }

    @Override
    public void onTaskDone(Task task) {
        synchronized (lock) {
            tasksForPostProcessing.add(task);
            lock.notify();
        }
    }

    private <E> void processSubscribingTask(Task<E> task) {
        taskSubscriptions.put(task.getTaskId(), new ArrayList<>());

        Set<Integer> nonFinishedTasksIds = new HashSet<>();

        List<TaskDependentParam> dependentParams = task.getDependencies();
        Set<Integer> tasksIds = dependentParams.stream().map(TaskDependentParam::getTaskId).collect(Collectors.toSet());

        int dependenciesNumber = tasksIds.size();
        for (Integer taskId : tasksIds) {
            if (tasksPostProcessed.contains(taskId)) {
                dependenciesNumber--;
            } else {
                nonFinishedTasksIds.add(taskId);
            }
        }

        task.addDoneCallback(this);
        dependentParams.stream().forEach(param -> {
            //noinspection unchecked
            param.setFuture(tasks.get(param.getTaskId()).getFuture());
        });

        if (dependenciesNumber == 0) {
            executor.execute(task);
        } else {
            taskDependenciesNumber.put(task.getTaskId(), dependenciesNumber);
            for (Integer taskId : nonFinishedTasksIds) {
                taskSubscriptions.get(taskId).add(task.getTaskId());
            }
        }
    }

    private <E> void processDoneTask(Task<E> task) {
        for (Integer taskId : taskSubscriptions.get(task.getTaskId())) {
            int newNumber = taskDependenciesNumber.get(taskId) - 1;
            taskDependenciesNumber.put(taskId, newNumber);

            if (newNumber == 0) {
                taskDependenciesNumber.remove(taskId);
                executor.execute(tasks.get(taskId));
            }
        }
        taskSubscriptions.remove(task.getTaskId());
        tasksPostProcessed.add(task.getTaskId());
    }

    public <E> Task<E> getTask(int taskId) {
        //noinspection unchecked
        synchronized (this.lock) {
            //noinspection unchecked
            return tasks.get(taskId);
        }
    }

    public interface TasksViewer {
        void view(Map<Integer, Task> integerTaskMap);
    }

    public void viewTasks(TasksViewer viewer) {
        synchronized (lock) {
            viewer.view(Collections.unmodifiableMap(this.tasks));
        }
    }

    public void run() {
        try {
            synchronized (lock) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    lock.wait();
                    while (tasksForSubscribing.size() > 0) {
                        Task newTask = tasksForSubscribing.poll();
                        processSubscribingTask(newTask);
                    }
                    while (tasksForPostProcessing.size() > 0) {
                        Task doneTask = tasksForPostProcessing.poll();
                        processDoneTask(doneTask);
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
