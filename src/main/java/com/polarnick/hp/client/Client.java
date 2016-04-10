package com.polarnick.hp.client;

import com.polarnick.hp.tasks.Serialization;
import com.polarnick.hp.tasks.Task;
import com.polarnick.hp.tasks.communication.Protocol;
import com.polarnick.hp.tasks.params.DependentTaskNotFoundException;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class Client {

    private final String clientId;
    private final String host;
    private final int port;
    private int nextRequestId;

    public Client(String clientId, String host, int port) {
        this.clientId = clientId;
        this.host = host;
        this.port = port;
        this.nextRequestId = 0;
    }

    private Protocol.ServerResponse sendRequest(Protocol.ServerRequest.Builder requestBuilder) throws IOException {
        int requestId = ++nextRequestId;
        Protocol.ServerRequest request = requestBuilder.setRequestId(requestId).setClientId(clientId).build();
        try (Socket serverSocket = new Socket(this.host, this.port)) {
            request.writeDelimitedTo(serverSocket.getOutputStream());

            Protocol.ServerResponse response = Protocol.ServerResponse.parseDelimitedFrom(serverSocket.getInputStream());
            assert response.getRequestId() == requestId;
            return response;
        }
    }

    public int sendTask(Task task) throws IOException, DependentTaskNotFoundException {
        Protocol.Task serializedTask = Serialization.serializeTask(task);
        Protocol.ServerResponse response = sendRequest(Protocol.ServerRequest.newBuilder()
                .setSubmit(Protocol.SubmitTask.newBuilder()
                .setTask(serializedTask)));

        assert response.hasSubmitResponse();
        assert !response.hasSubscribeResponse();
        assert !response.hasListResponse();

        if (response.getSubmitResponse().getStatus() == Protocol.Status.ERROR) {
            throw new DependentTaskNotFoundException();
        } else {
            assert response.getSubmitResponse().getStatus() == Protocol.Status.OK;
            return response.getSubmitResponse().getSubmittedTaskId();
        }
    }

    public Long subscribeOnTask(int taskId) throws IOException {
        Protocol.ServerResponse response = sendRequest(Protocol.ServerRequest.newBuilder()
                .setSubscribe(Protocol.Subscribe.newBuilder()
                .setTaskId(taskId)));

        assert !response.hasSubmitResponse();
        assert response.hasSubscribeResponse();
        assert !response.hasListResponse();

        if (response.getSubscribeResponse().getStatus() == Protocol.Status.OK) {
            return response.getSubscribeResponse().getValue();
        } else {
            return null;
        }
    }

    public long executeTask(Task task) throws IOException, DependentTaskNotFoundException {
        int taskId = sendTask(task);
        return subscribeOnTask(taskId);
    }

    public List<Task> listTasks() throws IOException {
        Protocol.ServerResponse response = sendRequest(Protocol.ServerRequest.newBuilder()
                .setList(Protocol.ListTasks.newBuilder()));

        assert !response.hasSubmitResponse();
        assert !response.hasSubscribeResponse();
        assert response.hasListResponse();

        assert response.getListResponse().getStatus() == Protocol.Status.OK;
        return response.getListResponse().getTasksList().stream().map(Serialization::deserializeTask)
                .collect(Collectors.toList());
    }

}
