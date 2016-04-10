package com.polarnick.hp.server;

import com.polarnick.hp.tasks.ComputationTask;
import com.polarnick.hp.tasks.Task;
import com.polarnick.hp.tasks.params.DependentTaskNotFoundException;
import com.polarnick.hp.tasks.params.Param;
import com.polarnick.hp.tasks.params.TaskDependentParam;
import com.polarnick.hp.tasks.params.ValueParam;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.*;

/**
 * Date: 10.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class TasksManagerTest {

    final int N = 10000;
    Random r;
    TasksManager tasksManager;
    List<ComputationTask> tasks;
    Map<Long, Integer> taskByValue;
    ExecutorService executor;

    @org.testng.annotations.BeforeMethod
    public void setUp() throws Exception {
        r = new Random(239);
        tasksManager = new TasksManager(N / 3);
        tasks = new ArrayList<>(N);
        taskByValue = new HashMap<>();
        for (int i = 0; i < N; i++) {
            ComputationTask task = new ComputationTask(-1, null,
                    new ValueParam<>((long) r.nextInt(100)),
                    new ValueParam<>((long) r.nextInt(100)),
                    new ValueParam<>((long) r.nextInt(100)),
                    new ValueParam<>((long) (N / 6)), new ValueParam<>(100000l));
            task.run();
            tasks.add(task);
            if ((i + 1) % 100 == 0) {
                System.out.println("Prepared " + (i + 1) + " / " + N + " tasks!");
            }
        }
        new Thread(tasksManager).start();
        executor = Executors.newFixedThreadPool(N);
    }

    private Param<Long> getParam(long value) {
        if (taskByValue.containsKey(value)) {
            if (r.nextBoolean()) {
                return new TaskDependentParam<>(taskByValue.get(value));
            } else {
                return new ValueParam<>(value);
            }
        } else {
            return new ValueParam<>(value);
        }
    }

    @Test
    public void testStress() throws Exception {
        List<ComputationTask> tasksExpected = new ArrayList<>(N);
        List<Integer> tasksIds = new ArrayList<>(N);
        List<ComputationTask> tasksToExecute = new ArrayList<>(N);

        for (int i = 0; i < N; i++) {
            int expectedI = r.nextInt(tasks.size());
            ComputationTask taskExpected = tasks.get(expectedI);
            tasksExpected.add(taskExpected);

            int taskId = tasksManager.generateTaskId();
            tasksIds.add(taskId);

            tasksToExecute.add(new ComputationTask(taskId, "testStress",
                    getParam(taskExpected.getA().get()), getParam(taskExpected.getB().get()), getParam(taskExpected.getP().get()),
                    getParam(taskExpected.getM().get()), getParam(taskExpected.getN().get())
            ));
            taskByValue.put(taskExpected.getResult(), taskId);
        }
        for (int j = 0; j < N; j++) {
            final int i = j;
            try {
                tasksManager.addTask(tasksToExecute.get(i));
            } catch (DependentTaskNotFoundException e) {
                fail();
            }
            executor.execute(() -> {
                int taskId = tasksIds.get(i);
                Task task = tasksManager.getTask(taskId);
                if (task.isFinished()) {
                    System.out.println("getTask(" + (i + 1) + " / " + N + ") done!");
                } else {
                    System.out.println("getTask(" + (i + 1) + " / " + N +") ... ");
                    try {
                        task.waitResult();
                    } catch (InterruptedException e) {
                        fail();
                    }
                    System.out.println("getTask(" + (i + 1) + " / " + N + ") ... done!");
                }
                assertEquals(task.getResult(), tasksExpected.get(i).getResult());
            });
        }
    }

}