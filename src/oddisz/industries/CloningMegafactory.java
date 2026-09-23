package oddisz.industries;

import coalescedlittlemods.ClmModule;

public class CloningMegafactory extends BaseCloning {

    @Override
    protected String getBaseGrowthFieldId() {
        return "cloning_base_growth_tier3";
    }

    @Override
    protected int getDefaultBaseGrowth() {
        return 250;
    }

    @Override
    protected String getIndustryName() {
        return "Mass Cloning Megafactory";
    }

    @Override
    public boolean isAvailableToBuild() {
        return ClmModule.CLONING.isEnabled();
    }
}
