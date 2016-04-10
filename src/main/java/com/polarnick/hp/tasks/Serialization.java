package com.polarnick.hp.tasks;

import com.polarnick.hp.tasks.communication.Protocol;
import com.polarnick.hp.tasks.params.Param;
import com.polarnick.hp.tasks.params.TaskDependentParam;
import com.polarnick.hp.tasks.params.ValueParam;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class Serialization {

    public static Param<Long> deserializeParam(Protocol.Task.Param param) {
        if (param.hasDependentTaskId()) {
            return new TaskDependentParam<>(param.getDependentTaskId());
        } else {
            assert param.hasValue();
            return new ValueParam<>(param.getValue());
        }
    }

    public static Protocol.Task.Param serializeParam(Param param) {
        Protocol.Task.Param.Builder res = Protocol.Task.Param.newBuilder();
        if (param instanceof TaskDependentParam) {
            res.setDependentTaskId(((TaskDependentParam) param).getTaskId());
        } else if (param instanceof ValueParam) {
            res.setValue((Long) param.get());
        } else {
            throw new UnsupportedOperationException();
        }
        return res.build();
    }

    public static Protocol.Task serializeTask(Task task) {
        Protocol.Task.Builder res = Protocol.Task.newBuilder();
        if (task instanceof ComputationTask) {
            res.setA(serializeParam(((ComputationTask) task).getA()));
            res.setB(serializeParam(((ComputationTask) task).getB()));
            res.setP(serializeParam(((ComputationTask) task).getP()));
            res.setM(serializeParam(((ComputationTask) task).getM()));
            res.setN(((ComputationTask) task).getN().get());
        } else {
            throw new UnsupportedOperationException();
        }
        return res.build();
    }

    public static Protocol.ListTasksResponse.TaskDescription serializeTaskDescription(Task task) {
        Protocol.ListTasksResponse.TaskDescription.Builder taskBuilder = Protocol.ListTasksResponse
                .TaskDescription.newBuilder();
        taskBuilder.setTaskId(task.getTaskId()).setClientId(task.getClientId())
                .setTask(Serialization.serializeTask(task));
        if (task.isFinished()) {
            taskBuilder.setResult((Long) task.getResult());
        }
        return taskBuilder.build();
    }

    public static Task deserializeTask(Protocol.ListTasksResponse.TaskDescription description) {
        Task<Long> task = new ComputationTask(description.getTaskId(), description.getClientId(),
                deserializeParam(description.getTask().getA()),
                deserializeParam(description.getTask().getB()),
                deserializeParam(description.getTask().getP()),
                deserializeParam(description.getTask().getM()),
                new ValueParam<>(description.getTask().getN())
        );
        if (description.hasResult()) {
            task.getFuture().setResult(description.getResult());
        }
        return task;
    }

}
