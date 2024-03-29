package com.renatusnetwork.momentum.data.infinite;

import com.renatusnetwork.momentum.Momentum;
import com.renatusnetwork.momentum.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;

public class InfiniteRewardsYAML {

    private static FileConfiguration rewardsFile = Momentum.getConfigManager().get("rewards");

    public static void loadRewards() {

        // load before so new config
        Momentum.getConfigManager().load("rewards");

        // get int keys
        for (String key : rewardsFile.getConfigurationSection("infinitepk").getKeys(false))
            // if it is an int and .command and .name is set, add it
            if (Utils.isInteger(key) &&
                rewardsFile.isSet("infinitepk." + key + ".command") &&
                rewardsFile.isSet("infinitepk." + key + ".name")) {

                int scoreNeeded = Integer.parseInt(key);
                String command = rewardsFile.getString("infinitepk." + key + ".command");
                String name = rewardsFile.getString("infinitepk." + key + ".name");

                // make new object and add
                Momentum.getInfinitePKManager().addReward(new InfinitePKReward(scoreNeeded, command, name));
            }
    }
}

