package com.polarnick.hp.server;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Date: 30.03.16.
 *
 * @author Nickolay Polyarniy
 */
public class Server implements Runnable {

    private final int port;
    private final TasksManager tasksManager;

    public Server(int port, int executionThreads) {
        this.port = port;
        this.tasksManager = new TasksManager(executionThreads);
    }

    public void run() {
        try {
            new Thread(this.tasksManager, "TasksManager").start();
            try (ServerSocket serverSocket = new ServerSocket(this.port)) {
                //noinspection InfiniteLoopStatement
                while (true) {
                    Socket connectionSocket = serverSocket.accept();
                    new Thread(new ClientHandler(this.tasksManager, connectionSocket), "ClientHandler").start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static class MainParams {
        @Parameter(names = "-port", description = "Port for TCP clients listening")
        private int port = 2391;
        @Parameter(names = "-threads", description = "Number of threads for tasks execution")
        private int threadsNumber = -1;
    }

    public static void main(String... args) throws IOException {
        MainParams params = new MainParams();
        new JCommander(params, args);

        int threadsNumber = params.threadsNumber;
        if (threadsNumber == -1) {
            threadsNumber = Runtime.getRuntime().availableProcessors();
        }
        Server server = new Server(params.port, threadsNumber);
        server.run();
    }
}
