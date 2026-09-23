package com.fs.starfarer.api.impl.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.util.Misc;

import coalescedlittlemods.ClmModule;

/**
 * Rule condition that holds while a Coalesced Little Mods module is switched on, so a module's
 * dialogue options disappear with it. In data/campaign/rules.csv: {@code CLM_ModuleEnabled GAMBLING_DEN}
 *
 * Lives in this package because that is where the rules engine looks command classes up by
 * their plain name.
 */
public class CLM_ModuleEnabled extends BaseCommandPlugin {

    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Misc.Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (params == null || params.isEmpty()) return false;
        String name = params.get(0).string;
        try {
            return ClmModule.valueOf(name).isEnabled();
        } catch (IllegalArgumentException e) {
            Global.getLogger(CLM_ModuleEnabled.class)
                    .error("Coalesced Little Mods: rule " + ruleId + " names an unknown module: " + name);
            return false;
        }
    }
}
