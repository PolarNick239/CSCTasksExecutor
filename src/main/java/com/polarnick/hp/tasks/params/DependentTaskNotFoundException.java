package com.polarnick.hp.tasks.params;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class DependentTaskNotFoundException extends Exception {

    public DependentTaskNotFoundException() {
        super("Dependent task was not found!");
    }

    public DependentTaskNotFoundException(int taskId) {
        super("Dependent task with taskId=" + taskId + " was not found!");
    }

}
