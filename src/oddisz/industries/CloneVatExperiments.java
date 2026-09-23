package oddisz.industries;

import coalescedlittlemods.ClmModule;

public class CloneVatExperiments extends BaseCloning {

    @Override
    protected String getBaseGrowthFieldId() {
        return "cloning_base_growth_tier1";
    }

    @Override
    protected int getDefaultBaseGrowth() {
        return 50;
    }

    @Override
    protected String getIndustryName() {
        return "Clone Vat Experiments";
    }

    @Override
    public boolean isAvailableToBuild() {
        return ClmModule.CLONING.isEnabled();
    }
}
