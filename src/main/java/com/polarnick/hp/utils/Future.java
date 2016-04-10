package com.polarnick.hp.utils;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class Future<E> {

    private E result;
    private boolean ready;
    private final Object waitingPoint = new Object();

    public Future() {
        this.result = null;
        this.ready = false;
    }

    public Future(E result) {
        this.result = result;
        this.ready = true;
    }

    public void setResult(E result) {
        assert !this.ready;
        this.result = result;
        this.ready = true;
        synchronized (this.waitingPoint) {
            this.waitingPoint.notifyAll();
        }
    }

    public E waitResult() throws InterruptedException {
        if (this.ready) {
            return this.getResult();
        }

        synchronized (this.waitingPoint) {
            while (!this.ready) {
                this.waitingPoint.wait();
            }
        }
        return this.result;
    }

    public E getResult() {
        assert this.ready;
        return this.result;
    }

    public boolean isFinished() {
        return this.ready;
    }
}
