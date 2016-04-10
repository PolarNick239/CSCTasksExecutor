package com.polarnick.hp.client;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.polarnick.hp.tasks.ComputationTask;
import com.polarnick.hp.tasks.Task;
import com.polarnick.hp.tasks.params.DependentTaskNotFoundException;
import com.polarnick.hp.tasks.params.Param;
import com.polarnick.hp.tasks.params.TaskDependentParam;
import com.polarnick.hp.tasks.params.ValueParam;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class ConsoleClient {

    private static final String USAGES = "Commands:\n" +
            "   submit <a> <b> <p> <m> <n>  - submit task to server\n" +
            "   exec <a> <b> <p> <m> <n>    - submit and get task to server\n" +
            "   get <taskId>                - wait task result with given taskId\n" +
            "   list                        - list all tasks\n" +
            "   help                        - prints this help\n" +
            "   exit                        - exit\n" +
            "Note that any task parameter can be integer value (10, 100, -239) or be taskId in format tTaskId (t4, t521).\n";

    private final Client client;

    public ConsoleClient(String clientName, String host, int port) {
        client = new Client(clientName, host, port);
    }

    private void submitTask(Param<Long> a, Param<Long> b, Param<Long> p, Param<Long> m, Param<Long> n)
            throws IOException {
        Task task = new ComputationTask(-1, null, a, b, p, m, n);
        try {
            int taskId = client.sendTask(task);
            System.out.println(" taskId=" + taskId);
        } catch (DependentTaskNotFoundException e) {
            System.out.println("Dependent task was not found!");
        }
    }

    private Param<Long> parseParam(String param) {
        if (param.startsWith("t")) {
            try {
                int taskId = Integer.parseInt(param.substring(1));
                return new TaskDependentParam<>(taskId);
            } catch (NumberFormatException e) {
                System.out.println("Bad taskId "+param.substring(1)+" in parameter " + param + "!");
                return null;
            }
        } else {
            try {
                long value = Long.parseLong(param);
                return new ValueParam<>(value);
            } catch (NumberFormatException e) {
                System.out.println("Bad param value: " + param + "!");
                return null;
            }
        }
    }

    private void execTask(Param<Long> a, Param<Long> b, Param<Long> p, Param<Long> m, Param<Long> n)
            throws IOException {
        Task task = new ComputationTask(-1, null, a, b, p, m, n);
        try {
            long result = client.executeTask(task);
            System.out.println(" result=" + result);
        } catch (DependentTaskNotFoundException e) {
            System.out.println("Dependent task was not found!");
        }
    }

    private void getTask(int taskId) throws IOException {
        Long result = client.subscribeOnTask(taskId);
        if (result == null) {
            System.out.println("Task not found: " + taskId + "!");
        } else {
            System.out.println(" result=" + result);
        }
    }

    private void listTasks() throws IOException {
        List<Task> tasks = client.listTasks();
        System.out.println(tasks.size() + " Tasks");
        for (Task task : tasks) {
            String result = task.isFinished() ? task.getResult().toString() : "...";
            System.out.println(" taskId=" + task.getTaskId()
                    + "\tclientId=" + task.getClientId()
                    + "\tresult=" + result);
        }
    }

    private String readLine(String message) throws IOException {
        System.out.print(message);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        return reader.readLine();
    }

    public void interact() throws IOException {
        System.out.println(USAGES);
        while (true) {
            String command = readLine("Command: ");
            if (command.startsWith("submit") || command.startsWith("exec")) {
                String[] params = command.split(" ");
                if (params.length != 6) {
                    System.out.println("Exactly 5 params was expected: <a> <b> <p> <m> <n>!");
                } else {
                    Param<Long> a = parseParam(params[1]);
                    Param<Long> b = parseParam(params[2]);
                    Param<Long> p = parseParam(params[3]);
                    Param<Long> m = parseParam(params[4]);
                    Param<Long> n = parseParam(params[5]);
                    if (a != null && b != null && p != null && m != null && n != null) {
                        if (command.startsWith("submit")) {
                            submitTask(a, b, p, m, n);
                        } else {
                            assert (command.startsWith("exec"));
                            execTask(a, b, p, m, n);
                        }
                    }
                }
            } else if (command.startsWith("get")) {
                if (command.length() <= "get ".length()) {
                    System.out.println("TaskId integer value was expected!");
                } else {
                    String param = command.substring("get ".length());
                    try {
                        getTask(Integer.parseInt(param));
                    } catch (NumberFormatException e) {
                        System.out.println("Bad taskId: " + param + "!");
                    }
                }
            } else if (command.startsWith("list")) {
                listTasks();
            } else if (command.startsWith("help")) {
                System.out.println(USAGES);
            } else if (command.startsWith("exit")) {
                break;
            } else {
                System.out.println("Wrong command! Use `help` command.");
            }
        }
    }

    private static class ConsoleClientParams {
        @Parameter(names = "-host", description = "Server host address")
        private String host = "localhost";
        @Parameter(names = "-clientName", description = "Client name")
        private String clientName = "Client";
        @Parameter(names = "-port", description = "Port for TCP clients listening")
        private int port = 2391;
    }

    public static void main(String... args) throws IOException {
        ConsoleClientParams params = new ConsoleClientParams();
        new JCommander(params).parse(args);

        new ConsoleClient(params.clientName, params.host, params.port).interact();
    }

}
