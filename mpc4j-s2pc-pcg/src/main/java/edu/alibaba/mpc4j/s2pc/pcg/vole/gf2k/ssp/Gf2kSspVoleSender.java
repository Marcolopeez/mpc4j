package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleSenderOutput;

/**
 * Single single-point GF2K VOLE sender.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
public interface Gf2kSspVoleSender extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxNum num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha α.
     * @param num   num.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kSspVoleSenderOutput send(int alpha, int num) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param alpha           α.
     * @param num             num.
     * @param preSenderOutput pre-computed sender output.
     * @return the sender output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    Gf2kSspVoleSenderOutput send(int alpha, int num, Gf2kVoleSenderOutput preSenderOutput) throws MpcAbortException;
}
