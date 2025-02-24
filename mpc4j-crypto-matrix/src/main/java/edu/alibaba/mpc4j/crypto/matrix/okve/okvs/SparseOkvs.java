package edu.alibaba.mpc4j.crypto.matrix.okve.okvs;

import java.util.stream.IntStream;

/**
 * sparse (binary) OKVS. The OKVS can be split into the sparse part and the dense part.
 *
 * @author Weiran Liu
 * @date 2023/4/4
 */
public interface SparseOkvs<T> extends BinaryOkvs<T> {
    /**
     * Gets the sparse position.
     *
     * @param key key.
     * @return the sparse position.
     */
    int[] sparsePosition(T key);

    /**
     * Gets the sparse position num.
     *
     * @return the sparse position num.
     */
    int sparsePositionNum();

    /**
     * Gets the dense position.
     *
     * @param key key.
     * @return the dense position.
     */
    boolean[] densePositions(T key);

    /**
     * Gets the max dense position num.
     *
     * @return the max dense position num.
     */
    int maxDensePositionNum();

    /**
     * Gets the binary positions for the given key. All positions are in range [0, m). The positions is distinct.
     *
     * @param key the key.
     * @return the binary positions.
     */
    @Override
    default int[] positions(T key) {
        int sparsePositionNum = sparsePositionNum();
        int densePositionNum = maxDensePositionNum();
        int[] sparsePositions = sparsePosition(key);
        boolean[] binaryDensePositions = densePositions(key);
        int[] densePositions = IntStream.range(0, densePositionNum)
            .filter(rmIndex -> binaryDensePositions[rmIndex])
            .toArray();
        int[] positions = new int[sparsePositions.length + densePositions.length];
        System.arraycopy(sparsePositions, 0, densePositions, 0, sparsePositions.length);
        System.arraycopy(positions, 0, densePositions, sparsePositionNum, densePositions.length);
        return positions;
    }

    /**
     * Gets the maximal position num.
     *
     * @return the maximal position num.
     */
    @Override
    default int maxPositionNum() {
        return sparsePositionNum() + maxDensePositionNum();
    }
}
