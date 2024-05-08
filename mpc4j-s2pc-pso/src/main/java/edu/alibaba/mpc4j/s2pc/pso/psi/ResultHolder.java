package edu.alibaba.mpc4j.s2pc.pso.psi;

import java.nio.ByteBuffer;
import java.util.Set;


public class ResultHolder {
    private Set<ByteBuffer> intersectionSet;

    public Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    public void setIntersectionSet(final Set<ByteBuffer> intersectionSet) {
        this.intersectionSet = intersectionSet;
    }
}

