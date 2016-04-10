package com.polarnick.hp.tasks.params;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class ValueParam<E> implements Param<E> {

    private final E value;

    public ValueParam(E value) {
        this.value = value;
    }

    @Override
    public E get() {
        return this.value;
    }

    @Override
    public E getWait() throws InterruptedException {
        return this.get();
    }
}
