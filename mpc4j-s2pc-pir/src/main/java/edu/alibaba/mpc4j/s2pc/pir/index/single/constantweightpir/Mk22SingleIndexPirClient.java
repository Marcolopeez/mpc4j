package edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirClient;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.pir.index.single.constantweightpir.Mk22SingleIndexPirPtoDesc.*;

/**
 * Constant-weight PIR client
 *
 * @author Qixian Zhou
 * @date 2023/6/18
 */
public class Mk22SingleIndexPirClient extends AbstractSingleIndexPirClient {


    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * constant weight PIR params
     */
    private Mk22SingleIndexPirParams params;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * public key
     */
    private byte[] publicKey;
    /**
     * private key
     */
    private byte[] secretKey;

    public Mk22SingleIndexPirClient(Rpc clientRpc, Party serverParty, Mk22SingleIndexPirConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, int serverElementSize, int elementBitLength) {
        assert (indexPirParams instanceof Mk22SingleIndexPirParams);
        params = (Mk22SingleIndexPirParams) indexPirParams;
        params.setQueryParams(serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload = clientSetup(serverElementSize, elementBitLength);
        // client sends Galois keys and relinKeys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(int serverElementSize, int elementByteLength) {
        params = Mk22SingleIndexPirParams.DEFAULT_PARAMS;
        params.setQueryParams(serverElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> publicKeysPayload = clientSetup(serverElementSize, elementByteLength);
        // client sends Galois keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientPublicKeysHeader, publicKeysPayload));
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[] pir(int index) throws MpcAbortException {
        setPtoInput(index);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // client generates query
        List<byte[]> clientQueryPayload = generateQuery(index);
        DataPacketHeader clientQueryHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientQueryHeader, clientQueryPayload));
        stopWatch.stop();
        long genQueryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, genQueryTime, "Client generates query");

        // receive response
        DataPacketHeader serverResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> serverResponsePayload = rpc.receive(serverResponseHeader).getPayload();
        stopWatch.start();
        byte[] element = decodeResponse(serverResponsePayload, index);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles reply");

        logPhaseInfo(PtoState.PTO_END);
        return element;
    }

    @Override
    public List<byte[]> clientSetup(int serverElementSize, int elementBitLength) {
        if (params == null) {
            params = Mk22SingleIndexPirParams.DEFAULT_PARAMS;
        }
        int maxPartitionBitLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength();
        setInitInput(serverElementSize, elementBitLength, maxPartitionBitLength);
        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
            partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        return generateKeyPair();
    }

    @Override
    public List<byte[]> generateQuery(int index) {
        int indexOfPlaintext = index / elementSizeOfPlaintext;
        // encode PT indices
        int[] encodedIndex;
        switch (params.getEqualityType()) {
            case FOLKLORE:
                encodedIndex = Mk22SingleIndexPirUtils.getFolkloreCodeword(
                    indexOfPlaintext, params.getCodewordsBitLength()
                );
                break;
            case CONSTANT_WEIGHT:
                encodedIndex = Mk22SingleIndexPirUtils.getPerfectConstantWeightCodeword(
                    indexOfPlaintext, params.getCodewordsBitLength(), params.getHammingWeight()
                );
                break;
            default:
                throw new IllegalStateException("Invalid Equality Operator Type");
        }
        return Mk22SingleIndexPirNativeUtils.generateQuery(
            params.getEncryptionParams(),
            publicKey,
            secretKey,
            encodedIndex,
            params.getUsedSlotsPerPlain(),
            params.getNumInputCiphers()
        );
    }

    @Override
    public byte[] decodeResponse(List<byte[]> response, int index) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(response.size() == partitionSize);
        ZlDatabase[] databases = new ZlDatabase[partitionSize];
        IntStream intStream = IntStream.range(0, partitionSize);
        intStream = parallel ? intStream.parallel() : intStream;
        intStream.forEach(partitionIndex -> {
            long[] coeffs = Mk22SingleIndexPirNativeUtils.decryptReply(
                params.getEncryptionParams(), secretKey, response.get(partitionIndex)
            );
            byte[] bytes = PirUtils.convertCoeffsToBytes(coeffs, params.getPlainModulusBitLength());
            int offset = index % elementSizeOfPlaintext;
            byte[] partitionBytes = new byte[partitionByteLength];
            System.arraycopy(bytes, offset * partitionByteLength, partitionBytes, 0, partitionByteLength);
            databases[partitionIndex] = ZlDatabase.create(partitionByteLength * Byte.SIZE, new byte[][]{partitionBytes});
        });
        return NaiveDatabase.createFromZl(elementBitLength, databases).getBytesData(0);
    }

    /**
     * client generates key pair.
     *
     * @return public keys.
     */
    private List<byte[]> generateKeyPair() {
        List<byte[]> keyPair = Mk22SingleIndexPirNativeUtils.keyGen(params.getEncryptionParams());
        assert (keyPair.size() == 4);
        this.publicKey = keyPair.remove(0);
        this.secretKey = keyPair.remove(0);
        // add Galois keys
        List<byte[]> publicKeys = new ArrayList<>();
        publicKeys.add(keyPair.remove(0));
        // add Relin keys
        publicKeys.add(keyPair.remove(0));
        return publicKeys;
    }
}