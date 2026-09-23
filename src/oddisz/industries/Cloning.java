package oddisz.industries;

import coalescedlittlemods.ClmModule;

public class Cloning extends BaseCloning {

    @Override
    protected String getBaseGrowthFieldId() {
        return "cloning_base_growth_tier2";
    }

    @Override
    protected int getDefaultBaseGrowth() {
        return 125;
    }

    @Override
    protected String getIndustryName() {
        return "Cloning";
    }

    @Override
    public boolean isAvailableToBuild() {
        return ClmModule.CLONING.isEnabled();
    }
}
