package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import java.util.stream.IntStream;

/**
 * sparse GF(2^e) DOKVS. The positions can be split into the sparse part and the dense part.
 *
 * @author Weiran Liu
 * @date 2023/7/4
 */
public interface SparseGf2eDokvs<T> extends Gf2eDokvs<T> {
    /**
     * Gets the sparse range. All sparse positions are in range [0, sparseRange).
     *
     * @return the sparse range.
     */
    int sparsePositionRange();

    /**
     * Gets the sparse positions.
     *
     * @param key key.
     * @return the sparse positions.
     */
    int[] sparsePositions(T key);

    /**
     * Gets the maximal sparse num.
     *
     * @return the maximal sparse num.
     */
    int maxSparsePositionNum();

    /**
     * Gets the dense positions.
     *
     * @param key key.
     * @return the dense positions.
     */
    boolean[] binaryDensePositions(T key);

    /**
     * Gets the maximal dense num.
     *
     * @return the maximal dense num.
     */
    int maxDensePositionNum();

    /**
     * Gets the maximal num.
     *
     * @return the maximal num.
     */
    default int maxPositionNum() {
        return maxSparsePositionNum() + maxDensePositionNum();
    }

    /**
     * Gets the positions.
     *
     * @param key the key.
     * @return the positions.
     */
    default int[] positions(T key) {
        int sparseRange = sparsePositionRange();
        int denseNum = maxDensePositionNum();
        int[] sparsePositions = sparsePositions(key);
        boolean[] binaryDensePositions = binaryDensePositions(key);
        int[] densePositions = IntStream.range(0, denseNum)
            .filter(denseIndex -> binaryDensePositions[denseIndex])
            .map(densePosition -> densePosition + sparseRange)
            .toArray();
        int[] positions = new int[sparsePositions.length + densePositions.length];
        System.arraycopy(sparsePositions, 0, positions, 0, sparsePositions.length);
        System.arraycopy(densePositions, 0, positions, sparsePositions.length, densePositions.length);
        return positions;
    }
}
