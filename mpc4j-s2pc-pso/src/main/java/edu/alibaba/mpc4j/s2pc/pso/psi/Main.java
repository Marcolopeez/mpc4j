package edu.alibaba.mpc4j.s2pc.pso.psi;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory.CuckooHashBinType;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiConfig;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;

import com.google.common.base.Splitter;


public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws Exception {LOGGER.info("read log config");
        Properties log4jProperties = new Properties();
        log4jProperties.load(Main.class.getResourceAsStream("/log4j.properties"));
        PropertyConfigurator.configure(log4jProperties);

        // Set VM options to link to the native libraries
        setVMOptions();

        byte[] bytesClientSet = hexStringToByteArray("d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab354b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8aef2d127de37b942baad06145e54b0c619a1f22327b2ebbcfbec78f5564afe39d7902699be42c8a8e46fbbb4501726517e86b22c56a189f7625a6da49081b24512c624232cdd221771294dfbb310aca000a0df6ac8b66b696d90ef06fdefb64a3");
        byte[] bytesServerSet = hexStringToByteArray("d4735e3a265e16eee03f59718b9b5d03019c07d8b6c51f90da3a666eec13ab35e7f6c011776e8db7cd330b54174fd76f7d0216b612387a5ffcfb81e6f09196834FC82B26AECB47D2868C4EFBE3581732A3E7CBCC6C2EFB32062C08170A05EEB87902699be42c8a8e46fbbb4501726517e86b22c56a189f7625a6da49081b245119581e27de7ced00ff1ce50b2047e7a567c76b1cbaebabe5ef03f7c3017bb5b7");
        Set<ByteBuffer> clientSet = convertByteArrayToByteBufferSet(bytesClientSet);
        Set<ByteBuffer> serverSet = convertByteArrayToByteBufferSet(bytesServerSet);

        PsiConfig config = buildPsiType(args[0]);

        RpcManager rpcManager = new MemoryRpcManager(2);
        Rpc serverRpc = rpcManager.getRpc(0);
        Rpc clientRpc = rpcManager.getRpc(1);
        PsiServer<ByteBuffer> server = PsiFactory.createServer(serverRpc, clientRpc.ownParty(), config);
        PsiClient<ByteBuffer> client = PsiFactory.createClient(clientRpc, serverRpc.ownParty(), config);

        server.setTaskId(5);
        client.setTaskId(5);

        try {
            //--------------------------------------------------------------------------------------------------------------------------
            BigInteger beta = client.psi_1(clientSet.size(), serverSet.size(), clientSet, serverSet.size());
            List<byte[]> hyBetaPayload = client.psi_2(serverSet.size(), clientSet.size());

            String betaString = beta.toString();
            String hyBetaHexString = bytesListToHexString(hyBetaPayload);
            LOGGER.info("beta received: " + betaString);
            LOGGER.info("hyBeta received: " + hyBetaHexString);
            //--------------------------------------------------------------------------------------------------------------------------
            List<byte[]> reconstructedBeta = hexStringToBytesList(hyBetaHexString);

            List<byte[]>[] result = server.psi_1(serverSet.size(), clientSet.size(), serverSet, clientSet.size(), reconstructedBeta);

            String hxAlphaHexString = bytesListToHexString(result[0]);
            String peqtHexString = bytesListToHexString(result[1]);
            LOGGER.info("hxAlpha received: " + hxAlphaHexString);
            LOGGER.info("peqt received: " + peqtHexString);
            //--------------------------------------------------------------------------------------------------------------------------
            List<List<byte[]>> reconstructedResult = new ArrayList<>();
            reconstructedResult.add(hexStringToBytesList(hxAlphaHexString));
            reconstructedResult.add(hexStringToBytesList(peqtHexString));

            Set<ByteBuffer> intersectionSet = client.psi_3(clientSet.size(), serverSet.size(), clientSet, serverSet.size(), new BigInteger(betaString), reconstructedResult);

            LOGGER.info("Intersection: {}", setToString(intersectionSet));
        } catch (Exception e) {
            LOGGER.error("Ocurrió un error: " + e.getMessage(), e);
        }
        System.exit(0);
    }

    private static void setVMOptions() {
        String libPathTool = "/home/marco/mpc4j-1.0.4/mpc4j/mpc4j-native-tool/cmake-build-release";
        String libPathFhe = "/home/marco/mpc4j-1.0.4/mpc4j/mpc4j-native-fhe/cmake-build-release";

        try {
            // Loading the native library for mpc4j-native-tool and mpc4j-native-fhe
            System.load(libPathTool + "/libmpc4j-native-tool.so");
            System.load(libPathFhe + "/libmpc4j-native-fhe.so");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error al cargar la biblioteca nativa: " + e.getMessage());
        }
    }

    public static PsiConfig buildPsiType(final String psiType){
        switch (psiType) {
            case "HFH99_ECC_COMPRESS":
                return new Hfh99EccPsiConfig.Builder().setCompressEncode(true).build();
            case "HFH99_ECC_UNCOMPRESS":
                return new Hfh99EccPsiConfig.Builder().setCompressEncode(false).build();
            case "KKRT16":
                return new Kkrt16PsiConfig.Builder().build();
            case "KKRT16_NO_STASH_NAIVE":
                return new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NO_STASH_NAIVE).build();
            case "KKRT16_NAIVE_4_HASH":
                return new Kkrt16PsiConfig.Builder().setCuckooHashBinType(CuckooHashBinType.NAIVE_4_HASH).build();
            default:
                throw new IllegalArgumentException("Invalid argument. PSI_TYPES valid: HFH99_ECC_COMPRESS, HFH99_ECC_UNCOMPRESS, KKRT16, KKRT16_NO_STASH_NAIVE, KKRT16_NAIVE_4_HASH");
        }
    }




    private static Set<ByteBuffer> convertByteArrayToByteBufferSet(final byte[] byteArray) {
        Set<ByteBuffer> byteBufferSet = new HashSet<>();
        int blockSize = 32;

        for (int i = 0; i < byteArray.length; i += blockSize) {
            int endIndex = Math.min(i + blockSize, byteArray.length);
            byte[] blockBytes = new byte[endIndex - i];
            System.arraycopy(byteArray, i, blockBytes, 0, blockBytes.length);
            ByteBuffer byteBuffer = ByteBuffer.wrap(blockBytes);
            byteBufferSet.add(byteBuffer);
        }

        return byteBufferSet;
    }

    private static byte[] hexStringToByteArray(final String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }



    private static String setToString(final Set<ByteBuffer> set) {
        StringBuilder hexString = new StringBuilder("0x");
        for (ByteBuffer byteBuffer : set) {
            byte[] bytes = new byte[byteBuffer.limit()];
            byteBuffer.rewind();
            byteBuffer.get(bytes);
            for (byte b : bytes) {
                hexString.append(String.format("%02X", b));
            }
        }
        return hexString.toString().trim();
    }


    public static String bytesListToHexString(List<byte[]> byteArrayList) {
        String elementDelimiter = ":";
        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteArrayList.size(); i++) {
            byte[] byteArray = byteArrayList.get(i);
            for (byte b : byteArray) {
                hexString.append(String.format("%02X", b));
            }
            if (i < byteArrayList.size() - 1) {
                hexString.append(elementDelimiter); // Agregar delimitador entre elementos
            }
        }
        return hexString.toString();
    }

    public static List<byte[]> hexStringToBytesList(String hexString) {
        String elementDelimiter = ":";
        List<byte[]> byteArrayList = new ArrayList<>();

        Iterable<String> elements = Splitter.onPattern(elementDelimiter).split(hexString);
        for (String element : elements) {
            byte[] byteArray = new byte[element.length() / 2];
            for (int i = 0; i < element.length(); i += 2) {
                String hexByte = element.substring(i, i + 2);
                byteArray[i / 2] = (byte) Integer.parseInt(hexByte, 16);
            }
            byteArrayList.add(byteArray);
        }

        return byteArrayList;
    }

}
