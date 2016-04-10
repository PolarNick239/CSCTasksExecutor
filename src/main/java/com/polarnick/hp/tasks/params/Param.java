package com.polarnick.hp.tasks.params;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public abstract interface Param<E> {

    E get();

    E getWait() throws InterruptedException;

}
