package edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.bcg19;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.bsp.BspCotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotConfig;
import edu.alibaba.mpc4j.s2pc.pcg.ot.cot.msp.MspCotFactory;

/**
 * BCG19-REG-MSP-COT协议配置项。
 *
 * @author Weiran Liu
 * @date 2022/01/25
 */
public class Bcg19RegMspCotConfig extends AbstractMultiPartyPtoConfig implements MspCotConfig {
    /**
     * BSP-COT协议配置项
     */
    private final BspCotConfig bspCotConfig;

    private Bcg19RegMspCotConfig(Builder builder) {
        super(SecurityModel.MALICIOUS, builder.bspcotConfig);
        bspCotConfig = builder.bspcotConfig;
    }

    public BspCotConfig getBspCotConfig() {
        return bspCotConfig;
    }

    @Override
    public MspCotFactory.MspCotType getPtoType() {
        return MspCotFactory.MspCotType.BCG19_REG;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<Bcg19RegMspCotConfig> {
        /**
         * BSPCOT协议配置项
         */
        private BspCotConfig bspcotConfig;

        public Builder(SecurityModel securityModel) {
            bspcotConfig = BspCotFactory.createDefaultConfig(securityModel);
        }

        public Builder setBspcotConfig(BspCotConfig bspcotConfig) {
            this.bspcotConfig = bspcotConfig;
            return this;
        }

        @Override
        public Bcg19RegMspCotConfig build() {
            return new Bcg19RegMspCotConfig(this);
        }
    }
}
