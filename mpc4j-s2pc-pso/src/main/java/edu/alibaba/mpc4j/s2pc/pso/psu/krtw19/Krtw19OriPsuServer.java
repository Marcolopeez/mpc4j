package edu.alibaba.mpc4j.s2pc.pso.psu.krtw19;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.hashbin.object.EmptyPadHashBin;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.Okvs;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory.OkvsType;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.s2pc.opf.oprf.*;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.CoreCotSender;
import edu.alibaba.mpc4j.s2pc.pso.psu.AbstractPsuServer;
import edu.alibaba.mpc4j.s2pc.pso.psu.krtw19.Krtw19OriPsuPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * KRTW19-ORI-PSU协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/15
 */
public class Krtw19OriPsuServer extends AbstractPsuServer {
    /**
     * RMPT协议的OPRF接收方
     */
    private final OprfReceiver rpmtOprfReceiver;
    /**
     * PEQT协议的OPRF发送方
     */
    private final OprfSender peqtOprfSender;
    /**
     * 核COT协议发送方
     */
    private final CoreCotSender coreCotSender;
    /**
     * OKVS类型
     */
    private final OkvsType okvsType;
    /**
     * 流水线执行数量
     */
    private final int pipeSize;
    /**
     * 桶哈希函数密钥
     */
    private byte[][] hashBinKeys;
    /**
     * OKVS哈希函数
     */
    private byte[][] okvsHashKeys;
    /**
     * 桶数量（β）
     */
    private int binNum;
    /**
     * 最大桶大小（m）
     */
    private int maxBinSize;
    /**
     * 服务端元素哈希桶
     */
    private EmptyPadHashBin<ByteBuffer> hashBin;
    /**
     * OKVS大小
     */
    private int okvsM;
    /**
     * 有限域比特长度
     */
    private int fieldBitLength;
    /**
     * 有限域哈希函数
     */
    private Hash finiteFieldHash;
    /**
     * PEQT输出哈希函数
     */
    private Hash peqtHash;
    /**
     * 加密伪随机数生成器
     */
    private Prg encPrg;

    public Krtw19OriPsuServer(Rpc serverRpc, Party clientParty, Krtw19OriPsuConfig config) {
        super(Krtw19OriPsuPtoDesc.getInstance(), serverRpc, clientParty, config);
        rpmtOprfReceiver = OprfFactory.createOprfReceiver(serverRpc, clientParty, config.getRpmtOprfConfig());
        addSubPtos(rpmtOprfReceiver);
        peqtOprfSender = OprfFactory.createOprfSender(serverRpc, clientParty, config.getPeqtOprfConfig());
        addSubPtos(peqtOprfSender);
        coreCotSender = CoreCotFactory.createSender(serverRpc, clientParty, config.getCoreCotConfig());
        addSubPtos(coreCotSender);
        okvsType = config.getOkvsType();
        pipeSize = config.getPipeSize();
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // 初始化各个子协议
        rpmtOprfReceiver.init(Krtw19PsuUtils.MAX_BIN_NUM);
        peqtOprfSender.init(Krtw19PsuUtils.MAX_BIN_NUM);
        byte[] delta = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(delta);
        coreCotSender.init(delta, Krtw19PsuUtils.MAX_BIN_NUM);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        List<byte[]> keysPayload = new LinkedList<>();
        // 初始化哈希桶密钥
        hashBinKeys = new byte[1][CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(hashBinKeys[0]);
        keysPayload.add(hashBinKeys[0]);
        // 初始化OKVS密钥
        int okvsHashKeyNum = OkvsFactory.getHashNum(okvsType);
        okvsHashKeys = IntStream.range(0, okvsHashKeyNum)
            .mapToObj(keyIndex -> {
                byte[] okvsKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(okvsKey);
                keysPayload.add(okvsKey);
                return okvsKey;
            })
            .toArray(byte[][]::new);
        DataPacketHeader keysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_KEYS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(keysHeader, keysPayload));
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, keyTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void psu(Set<ByteBuffer> serverElementSet, int clientElementSize, int elementByteLength)
        throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize, elementByteLength);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        initParams();
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, initTime);

        for (int binColumnIndex = 0; binColumnIndex < maxBinSize; binColumnIndex++) {
            handleBinColumn(binColumnIndex);
        }

        logPhaseInfo(PtoState.PTO_END);
    }

    private void initParams() {
        // 取得服务单和客户端元素数量的最大值
        int n = Math.max(serverElementSize, clientElementSize);
        // 设置桶参数
        binNum = Krtw19PsuUtils.getBinNum(n);
        maxBinSize = Krtw19PsuUtils.getMaxBinSize(n);
        hashBin = new EmptyPadHashBin<>(envType, binNum, maxBinSize, serverElementSize, hashBinKeys);
        // 向桶插入元素
        hashBin.insertItems(serverElementArrayList);
        // 放置特殊的元素\bot，并进行随机置乱
        hashBin.insertPaddingItems(botElementByteBuffer);
        // 设置OKVS大小
        okvsM = OkvsFactory.getM(okvsType, maxBinSize - 1);
        // 设置有限域比特长度σ = λ + log(β * (m + 1)^2)
        fieldBitLength = Krtw19PsuUtils.getFiniteFieldBitLength(binNum, maxBinSize);
        int fieldByteLength = fieldBitLength / Byte.SIZE;
        // 设置有限域哈希
        finiteFieldHash = HashFactory.createInstance(envType, fieldByteLength);
        int peqtByteLength = Krtw19PsuUtils.getPeqtByteLength(binNum, maxBinSize);
        peqtHash = HashFactory.createInstance(getEnvType(), peqtByteLength);
        // 设置加密伪随机数生成器
        encPrg = PrgFactory.createInstance(envType, elementByteLength);
    }

    private void handleBinColumn(int binColumnIndex) throws MpcAbortException {
        stopWatch.start();
        // 从哈希桶中提取出列数据
        byte[][] xs = IntStream.range(0, binNum)
            .mapToObj(binIndex -> hashBin.getBin(binIndex).get(binColumnIndex).getItem())
            .map(ByteBuffer::array)
            .toArray(byte[][]::new);
        // 调用OPRF得到q^*并计算哈希结果
        OprfReceiverOutput rpmtOprfReceiverOprfOutput = rpmtOprfReceiver.oprf(xs);
        IntStream qIntStream = IntStream.range(0, rpmtOprfReceiverOprfOutput.getBatchSize());
        qIntStream = parallel ? qIntStream.parallel() : qIntStream;
        byte[][] qs = qIntStream.mapToObj(i -> {
            byte[] q = rpmtOprfReceiverOprfOutput.getPrf(i);
            return finiteFieldHash.digestToBytes(q);
        }).toArray(byte[][]::new);
        stopWatch.stop();
        long qTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, 2, 1 + (binColumnIndex * 4), maxBinSize * 4, qTime);

        stopWatch.start();
        // 初始化s*的空间
        byte[][] ss = new byte[binNum][];
        // Pipeline过程，先执行整除倍，最后再循环一遍
        int pipeTime = binNum / pipeSize;
        int round;
        for (round = 0; round < pipeTime; round++) {
            DataPacketHeader okvsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();
            handleOkvs(ss, xs, qs, okvsPayload, round * pipeSize, (round + 1) * pipeSize);
            extraInfo++;
        }
        int remain = binNum - round * pipeSize;
        if (remain > 0) {
            DataPacketHeader okvsHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_OKVS.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> okvsPayload = rpc.receive(okvsHeader).getPayload();
            handleOkvs(ss, xs, qs, okvsPayload, round * pipeSize, binNum);
            extraInfo++;
        }
        stopWatch.stop();
        long okvsTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, 2, 2 + (binColumnIndex * 4), maxBinSize * 4, okvsTime);

        stopWatch.start();
        // 调用并发送sStarsOprf
        OprfSenderOutput peqtOprfSenderOutput = peqtOprfSender.oprf(binNum);
        IntStream sOprfIntStream = IntStream.range(0, binNum);
        sOprfIntStream = parallel ? sOprfIntStream.parallel() : sOprfIntStream;
        List<byte[]> sStarOprfPayload = sOprfIntStream
            .mapToObj(sIndex -> peqtOprfSenderOutput.getPrf(sIndex, ss[sIndex]))
            .map(peqtHash::digestToBytes)
            .collect(Collectors.toList());
        DataPacketHeader sStarOprfHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_S_STAR_OPRFS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(sStarOprfHeader, sStarOprfPayload));
        stopWatch.stop();
        long peqtTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, 2, 3 + (binColumnIndex * 4), maxBinSize * 4, peqtTime);

        stopWatch.start();
        CotSenderOutput cotSenderOutput = coreCotSender.send(binNum);
        IntStream encIntStream = IntStream.range(0, binNum);
        encIntStream = parallel ? encIntStream.parallel() : encIntStream;
        List<byte[]> encPayload = encIntStream
            .mapToObj(binIndex -> {
                byte[] element = xs[binIndex];
                // do not need CRHF since we call prg
                byte[] ciphertext = encPrg.extendToBytes(cotSenderOutput.getR0(binIndex));
                BytesUtils.xori(ciphertext, element);
                return ciphertext;
            })
            .collect(Collectors.toList());
        DataPacketHeader encHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ENC_ELEMENTS.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(encHeader, encPayload));
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logSubStepInfo(PtoState.PTO_STEP, 2, 4 + (binColumnIndex * 4), maxBinSize * 4, encTime);
    }

    private void handleOkvs(byte[][] ss, byte[][] xs, byte[][] qs, List<byte[]> okvsPayload, int start, int end)
        throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(okvsPayload.size() == (end - start) * okvsM);
        // 恢复OKVS
        byte[][] flatStorageArray = okvsPayload.toArray(new byte[0][]);
        IntStream xIntStream = IntStream.range(start, end);
        xIntStream = parallel ? xIntStream.parallel() : xIntStream;
        xIntStream.forEach(index -> {
            int okvsStart = index - start;
            int okvsEnd = (index + 1) - start;
            byte[][] okvsStorage = Arrays.copyOfRange(flatStorageArray, okvsStart * okvsM, okvsEnd * okvsM);
            ByteBuffer xStar = ByteBuffer.wrap(xs[index]);
            Okvs<ByteBuffer> okvs = OkvsFactory.createInstance(
                envType, okvsType, maxBinSize - 1, fieldBitLength, okvsHashKeys
            );
            ss[index] = okvs.decode(okvsStorage, xStar);
            BytesUtils.xori(ss[index], qs[index]);
        });
    }
}
