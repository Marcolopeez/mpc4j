package edu.alibaba.mpc4j.crypto.matrix.okve.tool;

import cc.redberry.rings.linear.LinearSolver;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.Zp;
import edu.alibaba.mpc4j.common.tool.galoisfield.zp.ZpFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Zp linear solver test with constant inputs.
 *
 * @author Weiran Liu
 * @date 2023/7/2
 */
public class ZpConstantLinearSolverTest {
    /**
     * random state
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    /**
     * Zp instance
     */
    private final Zp zp;
    /**
     * Zp linear solver
     */
    private final ZpLinearSolver linearSolver;

    public ZpConstantLinearSolverTest() {
        zp = ZpFactory.createInstance(EnvType.STANDARD, 40);
        linearSolver = new ZpLinearSolver(zp);
    }

    @Test
    public void test0xl() {
        int m = zp.getL();
        BigInteger[][] matrixA = new BigInteger[0][m];
        BigInteger[] b = new BigInteger[0];
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < m; iColumn++) {
            Assert.assertTrue(zp.isZero(x[iColumn]));
        }
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        for (int iColumn = 0; iColumn < m; iColumn++) {
            Assert.assertFalse(zp.isZero(x[iColumn]));
        }
    }

    @Test
    public void test1x1() {
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[1];
        LinearSolver.SystemInfo systemInfo;

        // A = | 1 |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));

        // A = | 1 |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));

        // A = | 0 |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));

        // A = | 0 |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x1() {
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[1];
        LinearSolver.SystemInfo systemInfo;

        // A = | r[0] |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));

        // A = | r[0] |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM),},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
    }

    @Test
    public void test1x2() {
        int m = 2;
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 1 1 |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne()},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(zp.add(x[0], x[1])));

        // A = | 1 1 |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isEqual(b[0], zp.add(x[0], x[1])));

        // A = | 0 1 |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne()},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));

        // A = | 0 1 |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createOne()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isEqual(b[0], x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertTrue(zp.isEqual(b[0], x[1]));

        // A = | 1 0 |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 1 0 |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 0 0 |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[0]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[0]));

        // A = | 0 0 |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testRandom1x2() {
        int m = 2;
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = 0, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
        };
        b = new BigInteger[]{
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | r[0] r[1] |, b = r, solve Ax = b.
        matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
    }

    @Test
    public void testAllOne2x2() {
        int m = 2;
        BigInteger[][] matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createOne(), zp.createOne()},
            new BigInteger[]{zp.createOne(), zp.createOne()},
        };
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isEqual(b[0], zp.add(x[0], x[1])));

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isEqual(b[0], zp.add(x[0], x[1])));
    }

    @Test
    public void testAllZero2x2() {
        int m = 2;
        BigInteger[][] matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createZero(), zp.createZero()},
            new BigInteger[]{zp.createZero(), zp.createZero()},
        };
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x2() {
        int m = 2;
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 1 0 |, b = | r0 |, solve Ax = b.
        //     | 0 1 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createOne(), zp.createZero()},
            new BigInteger[] {zp.createZero(), zp.createOne()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
            Assert.assertTrue(zp.isEqual(b[0], x[0]));
            Assert.assertTrue(zp.isEqual(b[1], x[1]));
            systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
            Assert.assertTrue(zp.isEqual(b[0], x[0]));
            Assert.assertTrue(zp.isEqual(b[1], x[1]));
        }
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertTrue(zp.isEqual(b[1], x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertTrue(zp.isEqual(b[1], x[1]));

        // A = | 0 0 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createZero()},
            new BigInteger[] {zp.createOne(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 |      | r1  |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createOne()},
            new BigInteger[] {zp.createOne(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[1]));
        Assert.assertTrue(zp.isEqual(b[1], x[0]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[1]));
        Assert.assertTrue(zp.isEqual(b[1], x[0]));
    }

    @Test
    public void testRandom2x2() {
        int m = 2;
        BigInteger[][] matrixA = new BigInteger[][]{
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
            new BigInteger[]{zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
        };
        matrixA[1] = BigIntegerUtils.clone(matrixA[0]);
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | r[0] r[1] |, b = | 0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | 0  |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | r[0] r[1] |, b = | r0 |, solve Ax = b.
        //     | r[0] r[1] |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        }
        // r0 == r1
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));
    }

    @Test
    public void testAllOne2x3() {
        int m = 3;
        BigInteger[][] matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createOne(), zp.createOne(), zp.createOne()},
            new BigInteger[] {zp.createOne(), zp.createOne(), zp.createOne()},
        };
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 1 1 1 |, b = | 0 |, solve Ax = b.
        //     | 1 1 1 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[2]));
        Assert.assertTrue(zp.isEqual(b[0], zp.add(zp.add(x[0], x[1]), x[2])));

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | 0  |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 1 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        // r0 != r1
        if (!zp.isEqual(b[0], b[1])) {
            systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
            systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
            Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        }
        // r0 = r1
        b[1] = b[0];
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[0], x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[2]));
        Assert.assertTrue(zp.isEqual(b[0], zp.add(zp.add(x[0], x[1]), x[2])));
    }

    @Test
    public void testAllZero3x2() {
        int m = 3;
        BigInteger[][] matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createZero(), zp.createZero()},
            new BigInteger[] {zp.createZero(), zp.createZero(), zp.createZero()},
        };
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 0 0 0 |, b = | 0 |, solve Ax = b.
        //     | 0 0 0 |      | 0 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertFalse(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertFalse(zp.isZero(x[2]));

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | 0  |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | 0  |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createZero(),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);

        // A = | 0 0 0 |, b = | r0 |, solve Ax = b.
        //     | 0 0 0 |      | r1 |
        b = new BigInteger[]{
            zp.createNonZeroRandom(SECURE_RANDOM),
            zp.createNonZeroRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Inconsistent, systemInfo);
    }

    @Test
    public void testSpecial2x3() {
        int m = 3;
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 0 0 1 |, b = | 0 |, solve Ax = b.
        //     | 1 0 0 |      | 0 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createZero(), zp.createOne()},
            new BigInteger[] {zp.createOne(), zp.createZero(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));

        // A = | 0 0 1 |, b = | r0 |, solve Ax = b.
        //     | 1 0 0 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createZero(), zp.createOne()},
            new BigInteger[] {zp.createOne(), zp.createZero(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[1], x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isEqual(b[0], x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(b[1], x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isEqual(b[0], x[2]));

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 0 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createOne(), zp.createOne()},
            new BigInteger[] {zp.createOne(), zp.createOne(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(zp.add(x[1], x[2]), b[0]));
        Assert.assertTrue(zp.isEqual(zp.add(x[0], x[1]), b[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(zp.add(x[1], x[2]), b[0]));
        Assert.assertTrue(zp.isEqual(zp.add(x[0], x[1]), b[1]));
        Assert.assertFalse(zp.isZero(x[2]));

        // A = | 0 1 1 |, b = | r0 |, solve Ax = b.
        //     | 1 1 1 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createOne(), zp.createOne()},
            new BigInteger[] {zp.createOne(), zp.createOne(), zp.createOne()},
        };
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(zp.add(x[1], x[2]), b[0]));
        Assert.assertTrue(zp.isEqual(zp.add(zp.add(x[0], x[1]), x[2]), b[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isEqual(zp.add(x[1], x[2]), b[0]));
        Assert.assertTrue(zp.isEqual(zp.add(zp.add(x[0], x[1]), x[2]), b[1]));
        Assert.assertFalse(zp.isZero(x[2]));
    }

    @Test
    public void testRandomSpecial2x3() {
        int m = 3;
        BigInteger[][] matrixA;
        BigInteger[] b;
        BigInteger[] x = new BigInteger[m];
        LinearSolver.SystemInfo systemInfo;

        // A = | 0 0 r |, b = | 0 |, solve Ax = b.
        //     | r 0 0 |      | 0 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createZero(), zp.createNonZeroRandom(SECURE_RANDOM)},
            new BigInteger[] {zp.createNonZeroRandom(SECURE_RANDOM), zp.createZero(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createZero(),
            zp.createZero(),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertTrue(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        Assert.assertTrue(zp.isZero(x[0]));
        Assert.assertFalse(zp.isZero(x[1]));
        Assert.assertTrue(zp.isZero(x[2]));

        // A = | 0 0 r |, b = | r0 |, solve Ax = b.
        //     | r 0 0 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createZero(), zp.createNonZeroRandom(SECURE_RANDOM)},
            new BigInteger[] {zp.createNonZeroRandom(SECURE_RANDOM), zp.createZero(), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(zp.isZero(x[1]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[1]));

        // A = | 0 r r |, b = | r0 |, solve Ax = b.
        //     | r r 0 |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
            new BigInteger[] {zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM), zp.createZero()},
        };
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[2]));

        // A = | 0 r r |, b = | r0 |, solve Ax = b.
        //     | r r r |      | r1 |
        matrixA = new BigInteger[][]{
            new BigInteger[] {zp.createZero(), zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
            new BigInteger[] {zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM), zp.createNonZeroRandom(SECURE_RANDOM)},
        };
        b = new BigInteger[]{
            zp.createRandom(SECURE_RANDOM),
            zp.createRandom(SECURE_RANDOM),
        };
        systemInfo = linearSolver.freeSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertTrue(zp.isZero(x[2]));
        systemInfo = linearSolver.fullSolve(BigIntegerUtils.clone(matrixA), BigIntegerUtils.clone(b), x);
        Assert.assertEquals(LinearSolver.SystemInfo.Consistent, systemInfo);
        assertCorrect(matrixA, b, x);
        Assert.assertFalse(zp.isZero(x[2]));
    }

    private void assertCorrect(BigInteger[][] matrixA, BigInteger[] b, BigInteger[] x) {
        int nRows = b.length;
        int nColumns = x.length;
        for (int iRow = 0; iRow < nRows; iRow++) {
            BigInteger result = zp.createZero();
            for (int iColumn = 0; iColumn < nColumns; iColumn++) {
                result = zp.add(result, zp.mul(matrixA[iRow][iColumn], x[iColumn]));
            }
            Assert.assertEquals(b[iRow], result);
        }
    }
}
