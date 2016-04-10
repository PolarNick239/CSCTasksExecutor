package com.polarnick.hp.tasks;

import com.polarnick.hp.tasks.params.Param;
import com.polarnick.hp.tasks.params.TaskDependentParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Date: 09.04.16.
 *
 * @author Nickolay Polyarniy
 */
public class ComputationTask extends Task<Long> {

    private final Param<Long> a;
    private final Param<Long> b;
    private final Param<Long> p;
    private final Param<Long> m;
    private final Param<Long> n;

    public ComputationTask(int taskId, String clientId,
                           Param<Long> a, Param<Long> b, Param<Long> p, Param<Long> m, Param<Long> n) {
        super(taskId, clientId);
        this.a = a;
        this.b = b;
        this.p = p;
        this.m = m;
        this.n = n;
    }

    @Override
    public List<TaskDependentParam> getDependencies() {
        List<TaskDependentParam> params = new ArrayList<>();
        for (Param param: new Param[]{a, b, p, m, n}){
            if (param instanceof TaskDependentParam) {
                params.add((TaskDependentParam) param);
            }
        }
        return params;
    }

    @Override
    public Long execute() {
        long a = this.a.get();
        long b = this.b.get();
        long p = this.p.get();
        long m = this.m.get();
        long n = this.n.get();

        while (n-- > 0) {
            b = (a * p + b) % m;
            a = b;
        }
        return a;
    }

    public Param<Long> getA() {
        return a;
    }

    public Param<Long> getB() {
        return b;
    }

    public Param<Long> getP() {
        return p;
    }

    public Param<Long> getM() {
        return m;
    }

    public Param<Long> getN() {
        return n;
    }
}
