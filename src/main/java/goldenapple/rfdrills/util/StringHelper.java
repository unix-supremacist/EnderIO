package goldenapple.rfdrills.util;

import cofh.core.key.IKeyBinding;
import goldenapple.rfdrills.item.soulupgrade.AbstractSoulUpgrade;
import goldenapple.rfdrills.item.soulupgrade.SoulUpgradeHelper;
import java.util.Locale;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

public class StringHelper {

    public static String formatEnergy(int energy) {
        if (energy >= 10000000) {
            return String.format(Locale.US, "%.1fM", (float) energy / 1000000);
        } else if (energy >= 10000) {
            return String.format(Locale.US, "%.1fk", (float) energy / 1000);
        } else return Integer.toString(energy);
    }

    public static String writeUpgradeInfo(ItemStack itemStack, AbstractSoulUpgrade upgrade) {
        return writeUpgradeInfo(SoulUpgradeHelper.getUpgradeLevel(itemStack, upgrade), upgrade);
    }

    public static String writeUpgradeInfo(int level, AbstractSoulUpgrade upgrade) {
        String upgradeName = StatCollector.translateToLocal("enderio.upgrade." + upgrade.getUnlocalizedName());
        if (level == 1) {
            return upgradeName;
        } else {
            String levelName = StatCollector.translateToLocal("enderio.level." + level);
            return upgradeName + " " + levelName;
        }
    }

    public static String writeEnergyPerBlockInfo(int energy, boolean green) {
        String energyFormatted = (green ? EnumChatFormatting.GREEN : EnumChatFormatting.RED)
                + formatEnergy(energy)
                + EnumChatFormatting.RESET;
        return StatCollector.translateToLocalFormatted("enderio.energy_per_block.tooltip", energyFormatted);
    }

    public static String writeEnergyPerBlockInfo(int energy) {
        String energyFormatted = formatEnergy(energy);
        return StatCollector.translateToLocalFormatted("enderio.energy_per_block.tooltip", energyFormatted);
    }

    public static String writeEnergyInfo(int energyLevel, int maxEnergy) {
        String energy1 = formatEnergy(energyLevel);
        String energy2 = formatEnergy(maxEnergy);
        return StatCollector.translateToLocal("info.cofh.charge") + String.format(": %s / %s RF", energy1, energy2);
    }

    private static String writeModeSwitchInfo(String unlocalizedName, int key) {
        return StatCollector.translateToLocalFormatted(
                unlocalizedName, cofh.lib.util.helpers.StringHelper.getKeyName(key));
    }

    public static String writeModeSwitchInfo(String unlocalizedName, KeyBinding keyBinding) { // vanilla bindings
        return writeModeSwitchInfo(unlocalizedName, keyBinding.getKeyCode());
    }

    public static String writeModeSwitchInfo(String unlocalizedName, IKeyBinding keyBinding) { // CoFH bindings
        return writeModeSwitchInfo(unlocalizedName, keyBinding.getKey());
    }
}
