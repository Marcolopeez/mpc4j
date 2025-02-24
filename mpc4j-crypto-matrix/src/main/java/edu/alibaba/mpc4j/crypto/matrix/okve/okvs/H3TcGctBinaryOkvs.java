package edu.alibaba.mpc4j.crypto.matrix.okve.okvs;

import cc.redberry.rings.linear.LinearSolver.SystemInfo;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.CuckooTableSingletonTcFinder;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H3CuckooTable;
import edu.alibaba.mpc4j.common.tool.utils.*;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.*;

/**
 * 3哈希-两核乱码布谷鸟表。原始构造来自于下述论文的Section 4.1: OKVS based on a 3-Hash Garbled Cuckoo Table：
 * Garimella G, Pinkas B, Rosulek M, et al. Oblivious Key-Value Stores and Amplification for Private Set Intersection.
 * CRYPTO 2021, Springer, Cham, 2021, pp. 395-425.
 *
 * @author Weiran Liu
 * @date 2021/09/06
 */
class H3TcGctBinaryOkvs<T> extends AbstractBinaryOkvs<T> implements SparseOkvs<T> {
    /**
     * 3 sparse hashes
     */
    private static final int SPARSE_HASH_NUM = 3;
    /**
     * 3哈希-两核乱码布谷鸟表需要4个哈希函数：3个布谷鸟哈希的哈希函数，1个右侧哈希函数
     */
    static final int HASH_NUM = SPARSE_HASH_NUM + 1;
    /**
     * 3哈希-两核乱码布谷鸟表左侧编码放大系数
     */
    private static final double LEFT_EPSILON = 1.3;
    /**
     * 3哈希-两核乱码布谷鸟表右侧编码放大系数
     */
    private static final double RIGHT_EPSILON = 0.5;
    /**
     * 左侧编码比特长度，等于1.3 * n，向上取整为Byte.SIZE的整数倍
     */
    private final int lm;
    /**
     * 右侧编码比特长度，等于0.5 * log(n) + λ，向上取整为Byte.SIZE的整数倍
     */
    private final int rm;
    /**
     * 布谷鸟哈希的第1个哈希函数
     */
    private final Prf h1;
    /**
     * 布谷鸟哈希的第2个哈希函数
     */
    private final Prf h2;
    /**
     * 布谷鸟哈希的第3个哈希函数
     */
    private final Prf h3;
    /**
     * 用于计算右侧r(x)的哈希函数
     */
    private final Prf hr;
    /**
     * 数据到h1的映射表
     */
    private TObjectIntMap<T> dataH1Map;
    /**
     * 数据到h2的映射表
     */
    private TObjectIntMap<T> dataH2Map;
    /**
     * 数据到h3的映射表
     */
    private TObjectIntMap<T> dataH3Map;
    /**
     * 数据到hr的映射表
     */
    private Map<T, boolean[]> dataHrMap;

    H3TcGctBinaryOkvs(EnvType envType, int n, int l, byte[][] keys) {
        super(n, getLm(n) + getRm(n), l);
        assert keys.length == HASH_NUM;
        lm = getLm(n);
        rm = getRm(n);
        h1 = PrfFactory.createInstance(envType, Integer.BYTES);
        h1.setKey(keys[0]);
        h2 = PrfFactory.createInstance(envType, Integer.BYTES);
        h2.setKey(keys[1]);
        h3 = PrfFactory.createInstance(envType, Integer.BYTES);
        h3.setKey(keys[2]);
        hr = PrfFactory.createInstance(envType, rm / Byte.SIZE);
        hr.setKey(keys[3]);
    }

    @Override
    public int[] sparsePosition(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        int[] sparsePositions = new int[3];
        sparsePositions[0] = h1.getInteger(0, keyBytes, lm);
        // h1 and h2 are distinct
        int h2Index = 0;
        do {
            sparsePositions[1] = h2.getInteger(h2Index, keyBytes, lm);
            h2Index++;
        } while (sparsePositions[1] == sparsePositions[0]);
        // h3 and h1 / h2 are distinct
        int h3Index = 0;
        do {
            sparsePositions[2] = h3.getInteger(h3Index, keyBytes, lm);
            h3Index++;
        } while (sparsePositions[2] == sparsePositions[0] || sparsePositions[2] == sparsePositions[1]);

        return sparsePositions;
    }

    @Override
    public int sparsePositionNum() {
        return SPARSE_HASH_NUM;
    }

    @Override
    public boolean[] densePositions(T key) {
        byte[] keyBytes = ObjectUtils.objectToByteArray(key);
        return BinaryUtils.byteArrayToBinary(hr.getBytes(keyBytes));
    }

    @Override
    public int maxDensePositionNum() {
        return rm;
    }

    @Override
    public byte[] decode(byte[][] storage, T key) {
        // 这里不能验证storage每一行的长度，否则整体算法复杂度会变为O(n^2)
        assert storage.length == getM();
        int[] sparsePositions = sparsePosition(key);
        boolean[] densePositions = densePositions(key);
        byte[] value = new byte[byteL];
        // 3个哈希值一定各不相同，3次异或
        BytesUtils.xori(value, storage[sparsePositions[0]]);
        BytesUtils.xori(value, storage[sparsePositions[1]]);
        BytesUtils.xori(value, storage[sparsePositions[2]]);
        for (int rmIndex = 0; rmIndex < rm; rmIndex++) {
            if (densePositions[rmIndex]) {
                BytesUtils.xori(value, storage[lm + rmIndex]);
            }
        }
        assert BytesUtils.isFixedReduceByteArray(value, byteL, l);
        return value;
    }

    @Override
    public OkvsFactory.OkvsType getOkvsType() {
        return OkvsFactory.OkvsType.H3_SINGLETON_GCT;
    }

    @Override
    public byte[][] encode(Map<T, byte[]> keyValueMap) throws ArithmeticException {
        assert keyValueMap.size() <= n;
        keyValueMap.values().forEach(x -> {
            assert BytesUtils.isFixedReduceByteArray(x, byteL, l);
        });
        // 构造数据到哈希值的查找表
        Set<T> keySet = keyValueMap.keySet();
        dataH1Map = new TObjectIntHashMap<>(keySet.size());
        dataH2Map = new TObjectIntHashMap<>(keySet.size());
        dataH3Map = new TObjectIntHashMap<>(keySet.size());
        dataHrMap = new HashMap<>(keySet.size());
        for (T key : keySet) {
            int[] sparsePositions = sparsePosition(key);
            boolean[] densePositions = densePositions(key);
            dataH1Map.put(key, sparsePositions[0]);
            dataH2Map.put(key, sparsePositions[1]);
            dataH3Map.put(key, sparsePositions[2]);
            dataHrMap.put(key, densePositions);
        }
        // 生成3哈希-布谷鸟图
        H3CuckooTable<T> h3CuckooTable = generateCuckooTable(keyValueMap);
        // 找到2-core图
        CuckooTableSingletonTcFinder<T> singletonFinder = new CuckooTableSingletonTcFinder<>();
        singletonFinder.findTwoCore(h3CuckooTable);
        // 根据2-core图的所有数据和所有边构造矩阵
        Set<T> coreDataSet = singletonFinder.getRemainedDataSet();
        TIntSet coreVertexSet = new TIntHashSet(keySet.size());
        coreDataSet.stream().map(h3CuckooTable::getVertices).forEach(coreVertexSet::addAll);
        // 生成矩阵，矩阵中包含右侧的全部解，以及2-core中的全部解
        byte[][] storage = generateStorage(keyValueMap, coreVertexSet, coreDataSet);
        // 将矩阵拆分为L || R
        byte[][] leftStorage = new byte[lm][];
        byte[][] rightStorage = new byte[rm][];
        System.arraycopy(storage, 0, leftStorage, 0, lm);
        System.arraycopy(storage, lm, rightStorage, 0, rm);
        // 从栈中依次弹出数据，为相应节点赋值
        Stack<T> removedDataStack = singletonFinder.getRemovedDataStack();
        Stack<int[]> removedDataVerticesStack = singletonFinder.getRemovedDataVertices();
        while (!removedDataStack.empty()) {
            T removedData = removedDataStack.pop();
            int[] removedDataVertices = removedDataVerticesStack.pop();
            int vertex0 = removedDataVertices[0];
            int vertex1 = removedDataVertices[1];
            int vertex2 = removedDataVertices[2];
            boolean[] rx = dataHrMap.get(removedData);
            byte[] innerProduct = BytesUtils.innerProduct(rightStorage, byteL, rx);
            byte[] valueBytes = keyValueMap.get(removedData);
            BytesUtils.xori(innerProduct, valueBytes);
            // 三个顶点必然各不相同，三次异或
            fullDistinctVertices(leftStorage, innerProduct, vertex0, vertex1, vertex2, removedData);
        }
        // 左侧矩阵补充随机数
        for (int vertex = 0; vertex < lm; vertex++) {
            if (leftStorage[vertex] == null) {
                leftStorage[vertex] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            }
        }
        // 更新矩阵
        System.arraycopy(leftStorage, 0, storage, 0, leftStorage.length);
        // 不应该再有没有更新的矩阵行了
        for (byte[] bytes : storage) {
            assert bytes != null;
        }

        return storage;
    }

    private void fullDistinctVertices(byte[][] leftMatrix, byte[] innerProduct,
                                      int vertex0, int vertex1, int vertex2, Object data) {
        if (leftMatrix[vertex0] == null && leftMatrix[vertex1] == null && leftMatrix[vertex2] == null) {
            // 0、1、2都为空
            leftMatrix[vertex0] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            leftMatrix[vertex1] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else if (leftMatrix[vertex0] == null && leftMatrix[vertex1] == null) {
            // 0、1为空
            leftMatrix[vertex0] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex1] = innerProduct;
        } else if (leftMatrix[vertex0] == null && leftMatrix[vertex2] == null) {
            // 0、2为空
            leftMatrix[vertex0] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else if (leftMatrix[vertex1] == null && leftMatrix[vertex2] == null) {
            // 1、2为空
            leftMatrix[vertex1] = BytesUtils.randomByteArray(byteL, l, secureRandom);
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else if (leftMatrix[vertex0] == null) {
            // 0为空
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex0] = innerProduct;
        } else if (leftMatrix[vertex1] == null) {
            // 1为空
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex2]);
            leftMatrix[vertex1] = innerProduct;
        } else if (leftMatrix[vertex2] == null) {
            // 2为空
            BytesUtils.xori(innerProduct, leftMatrix[vertex0]);
            BytesUtils.xori(innerProduct, leftMatrix[vertex1]);
            leftMatrix[vertex2] = innerProduct;
        } else {
            // 三个都不为空，不可能出现这种情况
            throw new IllegalStateException(data + "的顶点(" + vertex0 + ", " + vertex1 + ", " + vertex2 + ")均不为空");
        }
    }

    private byte[][] generateStorage(Map<T, byte[]> keyValueMap, TIntSet coreVertexSet, Set<T> coreDataSet) {
        // initialize variables L = {l_1, ..., l_m}, R = (r_1, ..., r_{χ + λ})
        byte[][] vectorX = new byte[m][];
        if (coreDataSet.size() > m) {
            throw new ArithmeticException("Back Edge数量 = " + coreDataSet.size() + "，超过了给定的最大值，无解");
        }
        // 2-core图不为空，求解线性方程组
        if (coreDataSet.size() != 0) {
            int size = coreDataSet.size();
            // 矩阵M有2-core边数量的行
            byte[][] matrixM = new byte[size][byteM];
            byte[][] vectorY = new byte[size][];
            int rowIndex = 0;
            for (T coreData : coreDataSet) {
                int h1Value = dataH1Map.get(coreData);
                int h2Value = dataH2Map.get(coreData);
                int h3Value = dataH3Map.get(coreData);
                boolean[] rx = dataHrMap.get(coreData);
                BinaryUtils.setBoolean(matrixM[rowIndex], offsetM + h1Value, true);
                BinaryUtils.setBoolean(matrixM[rowIndex], offsetM + h2Value, true);
                BinaryUtils.setBoolean(matrixM[rowIndex], offsetM + h3Value, true);
                for (int columnIndex = 0; columnIndex < rm; columnIndex++) {
                    BinaryUtils.setBoolean(matrixM[rowIndex], offsetM + lm + columnIndex, rx[columnIndex]);
                }
                vectorY[rowIndex] = BytesUtils.clone(keyValueMap.get(coreData));
                rowIndex++;
            }
            SystemInfo systemInfo = linearSolver.freeSolve(matrixM, m, vectorY, vectorX);
            if (systemInfo.compareTo(SystemInfo.Inconsistent) == 0) {
                throw new ArithmeticException("无法完成编码过程，线性系统无解");
            }
        } else {
            // 不存在环路，所有vectorX均设置为0，注意这里不能设置为空
            Arrays.fill(vectorX, new byte[byteL]);
        }
        byte[][] matrix = new byte[m][];
        for (int vertex : coreVertexSet.toArray()) {
            // 把2-core图的边所对应的矩阵设置好
            matrix[vertex] = BytesUtils.clone(vectorX[vertex]);
        }
        // 把R的矩阵设置好
        System.arraycopy(vectorX, lm, matrix, lm, rm);
        return matrix;
    }

    /**
     * 给定待编码的键值对个数，计算左侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 左侧哈希比特长度，向上取整为Byte.SIZE的整数倍。
     */
    static int getLm(int n) {
        // 根据论文第5.4节，lm = 1.3 * n，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength((int) Math.ceil(LEFT_EPSILON * n)) * Byte.SIZE;
    }

    /**
     * 给定待编码的键值对个数，计算右侧映射比特长度。
     *
     * @param n 待编码的键值对个数。
     * @return 右侧映射比特长度。向上取整为Byte.SIZE的整数倍。
     */
    static int getRm(int n) {
        // 根据论文第5.4节，r = 0.5 * log(n) + λ，向上取整到Byte.SIZE的整数倍
        return CommonUtils.getByteLength(
            (int) Math.ceil(RIGHT_EPSILON * DoubleUtils.log2(n)) + CommonConstants.STATS_BIT_LENGTH
        ) * Byte.SIZE;
    }

    /**
     * 生成3哈希-布谷鸟图。
     *
     * @param keyValueMap 键值对映射。
     * @return 3哈希-布谷鸟图。
     */
    H3CuckooTable<T> generateCuckooTable(Map<T, byte[]> keyValueMap) {
        Set<T> keySet = keyValueMap.keySet();
        // 构造3哈希-布谷鸟图
        H3CuckooTable<T> h3CuckooTable = new H3CuckooTable<>(lm);
        for (T key : keySet) {
            int h1Value = dataH1Map.get(key);
            int h2Value = dataH2Map.get(key);
            int h3Value = dataH3Map.get(key);
            h3CuckooTable.addData(new int[]{h1Value, h2Value, h3Value}, key);
        }
        return h3CuckooTable;
    }

    @Override
    public int getNegLogFailureProbability() {
        // 根据论文第5.4节，r = 0.5 * log(n) + λ，失败概率为2^(-29.355)
        return 29;
    }
}
