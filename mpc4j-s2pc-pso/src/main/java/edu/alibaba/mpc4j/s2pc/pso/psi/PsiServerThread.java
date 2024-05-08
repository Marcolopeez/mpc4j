package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



class PsiServerThread extends Thread {
    private final PsiServer<ByteBuffer> psiServer;
    private final Set<ByteBuffer> serverElementSet;
    private final int clientElementSize;
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    PsiServerThread(final PsiServer<ByteBuffer> psiServer, final Set<ByteBuffer> serverElementSet, final int clientElementSize) {
        this.psiServer = psiServer;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            psiServer.getRpc().connect();
            psiServer.init(serverElementSet.size(), clientElementSize);
            psiServer.psi(serverElementSet, clientElementSize);
            psiServer.getRpc().disconnect();
        } catch (MpcAbortException e) {
            LOGGER.error("Ocurrió un error: " + e.getMessage(), e);
        }
    }
}
