package com.polarnick.hp.server;

import com.polarnick.hp.tasks.Serialization;
import com.polarnick.hp.tasks.communication.Protocol;
import com.polarnick.hp.tasks.ComputationTask;
import com.polarnick.hp.tasks.Task;
import com.polarnick.hp.tasks.params.DependentTaskNotFoundException;
import com.polarnick.hp.tasks.params.ValueParam;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class ClientHandler implements Runnable {

    private final TasksManager tasksManager;

    private final Socket connectionSocket;

    public ClientHandler(TasksManager tasksManager, Socket connectionSocket) {
        this.tasksManager = tasksManager;
        this.connectionSocket = connectionSocket;
    }

    @Override
    public void run() {
        try {
            Protocol.ServerRequest request = Protocol.ServerRequest.parseDelimitedFrom(this.connectionSocket.getInputStream());
            Protocol.ServerResponse.Builder responseBuilder = Protocol.ServerResponse.newBuilder()
                    .setRequestId(request.getRequestId());

            if (request.hasSubmit()) {
                Protocol.Task requestTask = request.getSubmit().getTask();
                int taskId = tasksManager.generateTaskId();
                ComputationTask task = new ComputationTask(taskId, request.getClientId(),
                        Serialization.deserializeParam(requestTask.getA()),
                        Serialization.deserializeParam(requestTask.getB()),
                        Serialization.deserializeParam(requestTask.getP()),
                        Serialization.deserializeParam(requestTask.getM()), new ValueParam<>(requestTask.getN()));

                try {
                    tasksManager.addTask(task);
                    responseBuilder.setSubmitResponse(Protocol.SubmitTaskResponse.newBuilder()
                            .setSubmittedTaskId(taskId)
                            .setStatus(Protocol.Status.OK));
                } catch (DependentTaskNotFoundException e) {
                    e.printStackTrace();
                    responseBuilder.setSubmitResponse(Protocol.SubmitTaskResponse.newBuilder()
                            .setStatus(Protocol.Status.ERROR));
                }
            }

            if (request.hasList()) {
                Protocol.ListTasksResponse.Builder listBuilder = Protocol.ListTasksResponse.newBuilder();
                tasksManager.viewTasks(tasks -> {
                    //noinspection unchecked
                    for (Task<Long> task: tasks.values()) {
                        listBuilder.addTasks(Serialization.serializeTaskDescription(task));
                    }
                });
                responseBuilder.setListResponse(listBuilder.setStatus(Protocol.Status.OK));
            }

            if (request.hasSubscribe()) {
                int taskId = request.getSubscribe().getTaskId();
                Task<Long> task = tasksManager.getTask(taskId);
                if (task == null) {
                    responseBuilder.setSubscribeResponse(Protocol.SubscribeResponse.newBuilder()
                            .setStatus(Protocol.Status.ERROR));
                } else {
                    long result = task.waitResult();
                    responseBuilder.setSubscribeResponse(Protocol.SubscribeResponse.newBuilder()
                            .setStatus(Protocol.Status.OK).setValue(result));
                }
            }
            responseBuilder.build().writeDelimitedTo(this.connectionSocket.getOutputStream());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                this.connectionSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
