package edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.Okvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.AbstractMqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtPtoDesc.PtoStep;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * GMR21-mqRPMT协议客户端。
 *
 * @author Weiran Liu
 * @date 2022/09/10
 */
public class Gmr21MqRpmtClient extends AbstractMqRpmtClient {
    /**
     * 布谷鸟哈希所用OPRF发送方
     */
    private final OprfSender cuckooHashOprfSender;
    /**
     * OSN发送方
     */
    private final OsnSender osnSender;
    /**
     * PEQT协议所用OPRF接收方
     */
    private final OprfReceiver peqtOprfReceiver;
    /**
     * OKVS类型
     */
    private final OkvsType okvsType;
    /**
     * 布谷鸟哈希类型
     */
    private final CuckooHashBinType cuckooHashBinType;
    /**
     * 布谷鸟哈希函数数量
     */
    private final int cuckooHashNum;
    /**
     * 多项式有限域哈希
     */
    private Hash finiteFieldHash;
    /**
     * OKVS密钥
     */
    private byte[][] okvsHashKeys;
    /**
     * 布谷鸟哈希
     */
    private Prf[] binHashes;
    /**
     * 桶数量
     */
    private int binNum;
    /**
     * 向量(s_1, ..., s_m)
     */
    private Vector<byte[]> sVector;

    public Gmr21MqRpmtClient(Rpc clientRpc, Party serverParty, Gmr21MqRpmtConfig config) {
        super(Gmr21MqRpmtPtoDesc.getInstance(), clientRpc, serverParty, config);
        cuckooHashOprfSender = OprfFactory.createOprfSender(clientRpc, serverParty, config.getCuckooHashOprfConfig());
        addSubPtos(cuckooHashOprfSender);
        osnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        addSubPtos(osnSender);
        peqtOprfReceiver = OprfFactory.createOprfReceiver(clientRpc, serverParty, config.getPeqtOprfConfig());
        addSubPtos(peqtOprfReceiver);
        okvsType = config.getOkvsType();
        cuckooHashBinType = config.getCuckooHashBinType();
        cuckooHashNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxBinNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, maxServerElementSize);
        int maxPrfNum = CuckooHashBinFactory.getHashNum(cuckooHashBinType) * maxClientElementSize;
        // 初始化各个子协议
        cuckooHashOprfSender.init(maxBinNum, maxPrfNum);
        osnSender.init(maxBinNum);
        peqtOprfReceiver.init(maxBinNum);
        // 初始化多项式有限域哈希，根据论文实现，固定为64比特
        finiteFieldHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> keysPayload = rpc.receive(keysHeader).getPayload();
        int okvsHashKeyNum = OkvsFactory.getHashNum(okvsType);
        MpcAbortPreconditions.checkArgument(keysPayload.size() == okvsHashKeyNum);
        // 初始化OKVS密钥
        okvsHashKeys = keysPayload.toArray(new byte[0][]);
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public boolean[] mqRpmt(Set<ByteBuffer> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 设置最大桶数量
        binNum = CuckooHashBinFactory.getBinNum(cuckooHashBinType, serverElementSize);
        // 初始化PEQT哈希
        Hash peqtHash = HashFactory.createInstance(envType, Gmr21MqRpmtPtoDesc.getPeqtByteLength(binNum));
        DataPacketHeader cuckooHashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_CUCKOO_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> cuckooHashKeyPayload = rpc.receive(cuckooHashKeyHeader).getPayload();

        stopWatch.start();
        handleCuckooHashKeyPayload(cuckooHashKeyPayload);
        stopWatch.stop();
        long cuckooHashTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 5, cuckooHashTime, "Client handles cuckoo hash keys");

        stopWatch.start();
        OprfSenderOutput cuckooHashOprfSenderOutput = cuckooHashOprfSender.oprf(binNum);
        stopWatch.stop();
        long cuckooHashOprfTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 5, cuckooHashOprfTime, "Client runs OPRF for cuckoo hash bins");

        stopWatch.start();
        List<byte[]> okvsPayload = generateOkvsPayload(cuckooHashOprfSenderOutput);
        DataPacketHeader okvsHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(okvsHeader, okvsPayload));
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 5, okvsTime, "Client generates OKVS");

        stopWatch.start();
        OsnPartyOutput osnSenderOutput = osnSender.osn(sVector, Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH);
        IntStream bOprfIntStream = IntStream.range(0, binNum);
        bOprfIntStream = parallel ? bOprfIntStream.parallel() : bOprfIntStream;
        byte[][] bArray = bOprfIntStream.mapToObj(osnSenderOutput::getShare).toArray(byte[][]::new);
        stopWatch.stop();
        long osnTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 5, osnTime, "Client runs OSN");

        stopWatch.start();
        // 以b为输入调用OPRF
        OprfReceiverOutput peqtOprfReceiverOutput = peqtOprfReceiver.oprf(bArray);
        IntStream bPrimeOprfIntStream = IntStream.range(0, binNum);
        bPrimeOprfIntStream = parallel ? bPrimeOprfIntStream.parallel() : bPrimeOprfIntStream;
        ByteBuffer[] bPrimeOprfs = bPrimeOprfIntStream
            .mapToObj(peqtOprfReceiverOutput::getPrf)
            .map(peqtHash::digestToBytes)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 接收aPrimeOprf
        DataPacketHeader aPrimeOprfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_A_PRIME_OPRFS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> aPrimeOprfPayload = rpc.receive(aPrimeOprfHeader).getPayload();
        MpcAbortPreconditions.checkArgument(aPrimeOprfPayload.size() == binNum);
        ByteBuffer[] aPrimeOprfs = aPrimeOprfPayload.stream()
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        // 对比并得到结果
        boolean[] containVector = new boolean[binNum];
        IntStream.range(0, binNum).forEach(binIndex ->
            containVector[binIndex] = bPrimeOprfs[binIndex].equals(aPrimeOprfs[binIndex])
        );
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 5, 5, peqtTime, "Client runs OPRF for PEQT");

        logPhaseInfo(PtoState.PTO_END);
        return containVector;
    }

    private void handleCuckooHashKeyPayload(List<byte[]> cuckooHashKeyPayload) {
        // 读取哈希函数种子
        binHashes = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                byte[] key = cuckooHashKeyPayload.remove(0);
                Prf hash = PrfFactory.createInstance(envType, Integer.BYTES);
                hash.setKey(key);
                return hash;
            })
            .toArray(Prf[]::new);
    }

    private List<byte[]> generateOkvsPayload(OprfSenderOutput cuckooHashOprfSenderOutput) {
        // For each j ∈ [m], Bob choose a random s_j.
        sVector = IntStream.range(0, binNum)
            .mapToObj(index -> {
                byte[] si = new byte[Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH];
                secureRandom.nextBytes(si);
                return si;
            })
            .collect(Collectors.toCollection(Vector::new));
        // 计算OKVS键值
        Vector<byte[][]> keyArrayVector = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> clientElementArrayList.stream()
                .map(clientElement -> {
                    byte[] entryBytes = clientElement.array();
                    ByteBuffer extendEntryByteBuffer = ByteBuffer.allocate(entryBytes.length + Integer.BYTES);
                    // y || i
                    extendEntryByteBuffer.put(entryBytes);
                    extendEntryByteBuffer.putInt(hashIndex);
                    return extendEntryByteBuffer.array();
                })
                .toArray(byte[][]::new)
            ).collect(Collectors.toCollection(Vector::new));
        // Bob interpolates a polynomial P of degree < 3n such that for every y ∈ Y and i ∈ {1, 2, 3}, we have
        // P(y || i) = s_{h_i(y)} ⊕ PRF(k_{h_i(y)}, y || i)
        byte[][] valueArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // 计算OPRF有密码学运算，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> {
                        byte[] clientElement = clientElementArrayList.get(clientElementIndex).array();
                        byte[] extendBytes = keyArrayVector.elementAt(hashIndex)[clientElementIndex];
                        int binIndex = binHashes[hashIndex].getInteger(clientElement, binNum);
                        byte[] oprf = cuckooHashOprfSenderOutput.getPrf(binIndex, extendBytes);
                        byte[] value = finiteFieldHash.digestToBytes(oprf);
                        BytesUtils.xori(value, sVector.elementAt(binIndex));
                        return value;
                    })
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .toArray(byte[][]::new);
        ByteBuffer[] keyArray = IntStream.range(0, cuckooHashNum)
            .mapToObj(hashIndex -> {
                // 计算OPRF有密码学运算，并发处理
                IntStream clientElementIntStream = IntStream.range(0, clientElementSize);
                clientElementIntStream = parallel ? clientElementIntStream.parallel() : clientElementIntStream;
                return clientElementIntStream
                    .mapToObj(clientElementIndex -> keyArrayVector.elementAt(hashIndex)[clientElementIndex])
                    .toArray(byte[][]::new);
            })
            .flatMap(Arrays::stream)
            .map(ByteBuffer::wrap)
            .toArray(ByteBuffer[]::new);
        Map<ByteBuffer, byte[]> keyValueMap = IntStream.range(0, cuckooHashNum * clientElementSize)
            .boxed()
            .collect(Collectors.toMap(
                index -> keyArray[index],
                index -> valueArray[index]
            ));
        Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
            envType, okvsType, cuckooHashNum * clientElementSize,
            Gmr21MqRpmtPtoDesc.FINITE_FIELD_BYTE_LENGTH * Byte.SIZE, okvsHashKeys
        );
        // OKVS编码可以并行处理
        okvs.setParallelEncode(parallel);
        return Arrays.stream(okvs.encode(keyValueMap)).collect(Collectors.toList());
    }
}
