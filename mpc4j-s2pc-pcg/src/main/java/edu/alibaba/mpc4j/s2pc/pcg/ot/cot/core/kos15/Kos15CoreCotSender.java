package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrix;
import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2k;
import edu.alibaba.mpc4j.common.tool.galoisfield.gf2k.Gf2kFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.ot.KdfOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.base.BaseOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.AbstractCoreCotSender;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.CotSenderOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.core.kos15.Kos15CoreCotPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * KOS15-核COT协议发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/30
 */
public class Kos15CoreCotSender extends AbstractCoreCotSender {
    /**
     * 基础OT协议接收方
     */
    private final BaseOtReceiver baseOtReceiver;
    /**
     * GF(2^128)运算接口
     */
    private final Gf2k gf2k;
    /**
     * KDF-OT协议输出
     */
    private KdfOtReceiverOutput kdfOtReceiverOutput;
    /**
     * 随机预言机
     */
    private Prf randomOracle;
    /**
     * 扩展数量
     */
    private int extendNum;
    /**
     * 扩展字节数量
     */
    private int extendByteNum;
    /**
     * Q的转置矩阵
     */
    private TransBitMatrix qTransposeMatrix;

    public Kos15CoreCotSender(Rpc senderRpc, Party receiverParty, Kos15CoreCotConfig config) {
        super(Kos15CoreCotPtoDesc.getInstance(), senderRpc, receiverParty, config);
        baseOtReceiver = BaseOtFactory.createReceiver(senderRpc, receiverParty, config.getBaseOtConfig());
        addSubPtos(baseOtReceiver);
        gf2k = Gf2kFactory.createInstance(envType);
    }

    @Override
    public void init(byte[] delta, int maxNum) throws MpcAbortException {
        setInitInput(delta, maxNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        baseOtReceiver.init();
        kdfOtReceiverOutput = new KdfOtReceiverOutput(envType, baseOtReceiver.receive(deltaBinary));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime);

        stopWatch.start();
        List<byte[]> randomOracleKeyPayload = new LinkedList<>();
        byte[] randomOracleKey = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(randomOracleKey);
        randomOracleKeyPayload.add(randomOracleKey);
        DataPacketHeader randomOracleKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_RANDOM_ORACLE_KEY.ordinal(), extraInfo,
            ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomOracleKeyHeader, randomOracleKeyPayload));
        // 设置随机预言机
        randomOracle = PrfFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        randomOracle.setKey(randomOracleKey);
        stopWatch.stop();
        long randomOracleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, randomOracleTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public CotSenderOutput send(int num) throws MpcAbortException {
        setPtoInput(num);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // l' = l + (κ + s)
        extendNum = num + CommonConstants.BLOCK_BIT_LENGTH + CommonConstants.STATS_BIT_LENGTH;
        extendByteNum = CommonUtils.getByteLength(extendNum);
        DataPacketHeader matrixHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_MATRIX.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> matrixPayload = rpc.receive(matrixHeader).getPayload();
        handleMatrixPayload(matrixPayload);
        stopWatch.stop();
        long matrixTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, matrixTime);

        stopWatch.start();
        DataPacketHeader correlateCheckHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_CHECK.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> correlateCheckPayload = rpc.receive(correlateCheckHeader).getPayload();
        CotSenderOutput senderOutput = handleCorrelateCheckPayload(correlateCheckPayload);
        stopWatch.stop();
        long checkTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, checkTime);

        logPhaseInfo(PtoState.PTO_END);
        return senderOutput;
    }

    private void handleMatrixPayload(List<byte[]> matrixPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(matrixPayload.size() == CommonConstants.BLOCK_BIT_LENGTH);
        Prg prg = PrgFactory.createInstance(envType, extendByteNum);
        // 定义并设置矩阵Q
        TransBitMatrix qMatrix = TransBitMatrixFactory.createInstance(
            envType, extendNum, CommonConstants.BLOCK_BIT_LENGTH, parallel
        );
        // 设置矩阵Q的每一列
        byte[][] uArray = matrixPayload.toArray(new byte[0][]);
        IntStream qMatrixIntStream = IntStream.range(0, CommonConstants.BLOCK_BIT_LENGTH);
        qMatrixIntStream = parallel ? qMatrixIntStream.parallel() : qMatrixIntStream;
        qMatrixIntStream.forEach(columnIndex -> {
            byte[] columnBytes = prg.extendToBytes(kdfOtReceiverOutput.getKb(columnIndex, extraInfo));
            BytesUtils.reduceByteArray(columnBytes, extendNum);
            if (deltaBinary[columnIndex]) {
                BytesUtils.xori(columnBytes, uArray[columnIndex]);
            }
            qMatrix.setColumn(columnIndex, columnBytes);
        });
        // 矩阵转置，方便按行获取Q
        qTransposeMatrix = qMatrix.transpose();
    }

    private CotSenderOutput handleCorrelateCheckPayload(List<byte[]> correlateCheckPayload) throws MpcAbortException {
        // 包含x和t，数据包大小为2
        MpcAbortPreconditions.checkArgument(correlateCheckPayload.size() == 2);
        // 解包x和t
        byte[] xPolynomial = correlateCheckPayload.remove(0);
        byte[] tPolynomial = correlateCheckPayload.remove(0);
        // Sample (χ_1, ..., χ_{l'}) ← F_{Rand}(F_{2^κ}^{l'}).
        IntStream extendIndexIntStream = IntStream.range(0, extendNum);
        extendIndexIntStream = parallel ? extendIndexIntStream.parallel() : extendIndexIntStream;
        // q_j · χ_j
        byte[][] indexPolynomials = extendIndexIntStream
            .mapToObj(extendIndex -> {
                // 调用随机预言的输入是ExtraInfo || extendIndex
                byte[] indexMessage = ByteBuffer.allocate(Long.BYTES + Integer.BYTES)
                    .putLong(extraInfo).putInt(extendIndex).array();
                byte[] chiPolynomial = randomOracle.getBytes(indexMessage);
                byte[] qi = qTransposeMatrix.getColumn(extendIndex);
                gf2k.muli(chiPolynomial, qi);
                return chiPolynomial;
            })
            .toArray(byte[][]::new);
        // q = Σ_{j = 1}^{l'} (q_j · χ_j)
        byte[] qPolynomial = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        for (int extendIndex = 0; extendIndex < extendNum; extendIndex++) {
            gf2k.addi(qPolynomial, indexPolynomials[extendIndex]);
        }
        // check that t = q + x · Δ. If the check fails, output Abort.
        gf2k.muli(xPolynomial, delta);
        gf2k.addi(xPolynomial, qPolynomial);
        MpcAbortPreconditions.checkArgument(Arrays.equals(tPolynomial, xPolynomial));
        // 生成r0
        byte[][] r0Array = IntStream.range(0, num)
            .mapToObj(qTransposeMatrix::getColumn)
            .toArray(byte[][]::new);

        return CotSenderOutput.create(delta, r0Array);
    }
}
