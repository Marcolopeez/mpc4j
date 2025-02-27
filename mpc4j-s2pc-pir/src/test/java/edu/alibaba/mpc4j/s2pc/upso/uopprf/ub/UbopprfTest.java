package edu.alibaba.mpc4j.s2pc.upso.uopprf.ub;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.crypto.matrix.okve.okvs.OkvsFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.UopprfTestUtils;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.UbopprfFactory.UbopprfType;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.okvs.OkvsUbopprfConfig;
import edu.alibaba.mpc4j.s2pc.upso.uopprf.ub.pir.PirUbopprfConfig;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * unbalanced batched OPPRF test.
 *
 * @author Weiran Liu
 * @date 2023/4/17
 */
@RunWith(Parameterized.class)
public class UbopprfTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UbopprfTest.class);
    /**
     * default l
     */
    private static final int DEFAULT_L = 64;
    /**
     * default batch size
     */
    private static final int DEFAULT_BATCH_NUM = 64;
    /**
     * large batch size
     */
    private static final int LARGE_BATCH_NUM = 512;
    /**
     * default point num
     */
    private static final int DEFAULT_POINT_NUM = 1 << 10;
    /**
     * large point num
     */
    private static final int LARGE_POINT_NUM = LARGE_BATCH_NUM * 3;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        configurations.add(new Object[]{
            UbopprfType.PIR.name() + "(H3_SINGLETON_GCT)",
            new PirUbopprfConfig.Builder().setOkvsType(OkvsFactory.OkvsType.H3_SINGLETON_GCT).build(),
        });
        configurations.add(new Object[]{
            UbopprfType.OKVS.name() + "(H3_SINGLETON_GCT)",
            new OkvsUbopprfConfig.Builder().setOkvsType(OkvsFactory.OkvsType.H3_SINGLETON_GCT).build(),
        });
        configurations.add(new Object[]{
            UbopprfType.OKVS.name() + "(H2_SINGLETON_GCT)",
            new OkvsUbopprfConfig.Builder().setOkvsType(OkvsFactory.OkvsType.H2_SINGLETON_GCT).build(),
        });
        configurations.add(new Object[]{
            UbopprfType.OKVS.name() + "(GBF)",
            new OkvsUbopprfConfig.Builder().setOkvsType(OkvsFactory.OkvsType.GBF).build(),
        });
        // MegaBin
        configurations.add(new Object[]{
            UbopprfType.OKVS.name() + "(MegaBin)",
            new OkvsUbopprfConfig.Builder().setOkvsType(OkvsFactory.OkvsType.MEGA_BIN).build(),
        });

        return configurations;
    }

    /**
     * the config
     */
    private final UbopprfConfig config;

    public UbopprfTest(String name, UbopprfConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void test2Batch() {
        testPto(DEFAULT_L, 2, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void test1Point() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, 1, false);
    }

    @Test
    public void test2Point() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, 2, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void testSpecialL() {
        testPto(DEFAULT_L + 1, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_L, DEFAULT_BATCH_NUM, DEFAULT_POINT_NUM, true);
    }

    @Test
    public void testLarge() {
        testPto(DEFAULT_L, LARGE_BATCH_NUM, LARGE_POINT_NUM, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(DEFAULT_L, LARGE_BATCH_NUM, LARGE_POINT_NUM, true);
    }

    private void testPto(int l, int batchNum, int pointNum, boolean parallel) {
        testPto(l, batchNum, pointNum, parallel, true);
        testPto(l, batchNum, pointNum, parallel, false);
    }

    private void testPto(int l, int batchNum, int pointNum, boolean parallel, boolean equalTarget) {
        // create the sender and the receiver
        UbopprfSender sender = UbopprfFactory.createSender(firstRpc, secondRpc.ownParty(), config);
        UbopprfReceiver receiver = UbopprfFactory.createReceiver(secondRpc, firstRpc.ownParty(), config);
        sender.setParallel(parallel);
        receiver.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        sender.setTaskId(randomTaskId);
        receiver.setTaskId(randomTaskId);
        try {
            LOGGER.info(
                "-----test {}, l = {}, batch_num = {}, point_num = {}, parallel = {}, equal_target = {}-----",
                sender.getPtoDesc().getPtoName(), l, batchNum, pointNum, parallel, equalTarget
            );
            // generate the sender input
            byte[][][] senderInputArrays = UopprfTestUtils.generateSenderInputArrays(batchNum, pointNum, SECURE_RANDOM);
            byte[][][] senderTargetArrays = equalTarget
                ? UopprfTestUtils.generateEqualSenderTargetArrays(l, senderInputArrays, SECURE_RANDOM)
                : UopprfTestUtils.generateDistinctSenderTargetArrays(l, senderInputArrays, SECURE_RANDOM);
            // generate the receiver input
            byte[][] receiverInputArray = UopprfTestUtils.generateReceiverInputArray(l, senderInputArrays, SECURE_RANDOM);
            UbopprfSenderThread senderThread = new UbopprfSenderThread(sender, l, senderInputArrays, senderTargetArrays);
            UbopprfReceiverThread receiverThread = new UbopprfReceiverThread(receiver, l, receiverInputArray, pointNum);
            // start
            STOP_WATCH.start();
            senderThread.start();
            receiverThread.start();
            // stop
            senderThread.join();
            receiverThread.join();
            STOP_WATCH.stop();
            long time = STOP_WATCH.getTime(TimeUnit.MILLISECONDS);
            STOP_WATCH.reset();
            byte[][] receiverTargetArray = receiverThread.getTargetArray();
            // verify
            assertOutput(l, senderInputArrays, senderTargetArrays, receiverInputArray, receiverTargetArray);
            printAndResetRpc(time);
            // destroy
            new Thread(sender::destroy).start();
            new Thread(receiver::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void assertOutput(int l, byte[][][] senderInputArrays, byte[][][] senderTargetArrays,
                              byte[][] receiverInputArray, byte[][] receiverTargetArray) {
        int byteL = CommonUtils.getByteLength(l);
        int batchNum = senderInputArrays.length;
        Assert.assertEquals(batchNum, senderTargetArrays.length);
        Assert.assertEquals(batchNum, receiverInputArray.length);
        Assert.assertEquals(batchNum, receiverTargetArray.length);
        IntStream.range(0, batchNum).forEach(batchIndex -> {
            int batchPointNum = senderInputArrays[batchIndex].length;
            Assert.assertEquals(batchPointNum, senderTargetArrays[batchIndex].length);
            byte[][] senderInputArray = senderInputArrays[batchIndex];
            byte[][] senderTargetArray = senderTargetArrays[batchIndex];
            byte[] receiverInput = receiverInputArray[batchIndex];
            // the receiver output must have l-bit length
            byte[] receiverTarget = receiverTargetArray[batchIndex];
            Assert.assertTrue(BytesUtils.isFixedReduceByteArray(receiverTarget, byteL, l));
            for (int index = 0; index < batchPointNum; index++) {
                // the sender target must have l-bit length
                byte[] senderTarget = senderTargetArray[index];
                Assert.assertTrue(BytesUtils.isFixedReduceByteArray(senderTarget, byteL, l));
            }
            // if receiver input belongs to one of the sender input, then check equal target
            boolean contain = false;
            int containIndex = -1;
            for (int index = 0; index < batchPointNum; index++) {
                byte[] senderInput = senderInputArray[index];
                if (Arrays.equals(senderInput, receiverInput)) {
                    contain = true;
                    containIndex = index;
                }
            }
            if (contain) {
                Assert.assertEquals(ByteBuffer.wrap(receiverTarget), ByteBuffer.wrap(senderTargetArray[containIndex]));
            } else {
                for (int index = 0; index < batchPointNum; index++) {
                    Assert.assertNotEquals(ByteBuffer.wrap(receiverTarget), ByteBuffer.wrap(senderTargetArray[index]));
                }
            }
        });
    }
}
