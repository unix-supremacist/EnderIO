package goldenapple.rfdrills.item;

import cofh.api.item.IEmpowerableItem;
import cofh.core.item.IEqualityOverrideItem;
import cofh.core.util.KeyBindingEmpower;
import crazypants.enderio.EnderIO;
import crazypants.enderio.EnderIOTab;
import goldenapple.rfdrills.config.ConfigHandler;
import goldenapple.rfdrills.util.MiscUtil;
import goldenapple.rfdrills.util.StringHelper;
import goldenapple.rfdrills.util.ToolHelper;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;

public class ItemChainsaw extends ItemAxe implements IEnergyTool, IEqualityOverrideItem, IEmpowerableItem {
    private final String name;
    private final ToolTier tier;

    public ItemChainsaw(String name, ToolTier tier) {
        super(tier.material);
        this.name = name;
        this.tier = tier;
        this.setCreativeTab(EnderIOTab.tabEnderIO);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void getSubItems(Item item, CreativeTabs creativeTab, List list) {
        list.add(setEnergy(new ItemStack(item, 1, 0), 0));
        list.add(setEnergy(new ItemStack(item, 1, 0), tier.maxEnergy));
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return tier.rarity;
    }

    @Override
    public boolean canHarvestBlock(Block block, ItemStack stack) {
        if (getEnergyStored(stack) > 0 && isEmpowered(stack))
            return block instanceof IShearable
                    || block == Blocks.web
                    || block == Blocks.redstone_wire
                    || block == Blocks.tripwire;
        else return false;
    }

    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        if (getToolClasses(stack).contains(toolClass) && getEnergyStored(stack) >= tier.energyPerBlock)
            return super.getHarvestLevel(stack, toolClass);
        else return -1;
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        if (getEnergyStored(stack) >= getEnergyPerUse(stack, block, meta)) {
            if (block instanceof IShearable && isEmpowered(stack)) return 100.0F;
            else if (block.getMaterial() == Material.cloth) return super.getDigSpeed(stack, block, meta) * 3;
            else return super.getDigSpeed(stack, block, meta);
        } else return 1.0F;
    }

    private int getEnergyPerUse(ItemStack stack) {
        return Math.round(tier.energyPerBlock
                / (EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack)
                        + 1)); // Vanilla formula: a 100% / (unbreaking level + 1) chance to not take damage
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return true;
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return Math.max(1.0 - (double) getEnergyStored(stack) / (double) tier.maxEnergy, 0);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        super.setDamage(stack, 0);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity) {
        if (isEmpowered(stack)) {
            if (getEnergyStored(stack) > 0 && Items.shears.itemInteractionForEntity(stack, player, entity)) {
                ToolHelper.drainEnergy(stack, player, getEnergyPerUse(stack) * 2);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer player) {
        World world = player.worldObj;

        if (isEmpowered(stack) && getEnergyStored(stack) > 0) {
            ToolHelper.drainEnergy(
                    stack, player, getEnergyPerUse(stack, world.getBlock(x, y, z), world.getBlockMetadata(x, y, z)));
            Items.shears.onBlockStartBreak(stack, x, y, z, player);
        }
        return false;
    }

    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase entityAttacked, EntityLivingBase entityAttacker) {
        if (entityAttacker instanceof EntityPlayer)
            ToolHelper.drainEnergy(stack, (EntityPlayer) entityAttacker, getEnergyPerUse(stack) * 2);

        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote && player.isSneaking() && MiscUtil.shouldModeShiftClick(this)) {
            setEmpoweredState(stack, !isEmpowered(stack));
            onStateChange(player, stack);
        }
        return stack;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean what) {
        list.add(StringHelper.writeEnergyInfo(getEnergyStored(stack), tier.maxEnergy));

        if (MiscUtil.isShiftPressed()) {
            list.add(StringHelper.writeEnergyPerBlockInfo(getEnergyPerUse(stack)));
            if (tier.hasModes) list.add(writeModeInfo(stack));
            list.add(StatCollector.translateToLocal("enderio.chainsaw.tooltip"));
            if (tier.canBreak) list.add(StatCollector.translateToLocal("enderio.can_break.tooltip"));
            if (toolMaterial.getEnchantability() > 0)
                list.add(StatCollector.translateToLocal("enderio.enchantable.tooltip"));
            if (tier.hasModes) {
                if (!ConfigHandler.modeShiftClickEIO)
                    list.add(StringHelper.writeModeSwitchInfo(
                            "enderio.chainsaw_has_modes.tooltip", KeyBindingEmpower.instance));
                else list.add(StatCollector.translateToLocal("enderio.chainsaw_has_modes.sneak.tooltip"));
            }
            if (MiscUtil.isItemSilent(stack)) list.add(StatCollector.translateToLocal("enderio.silent.tooltip"));
        } else list.add(cofh.lib.util.helpers.StringHelper.shiftForDetails());
    }

    @Override
    public EnumAction getItemUseAction(ItemStack stack) {
        return EnumAction.bow;
    }

    @Override
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(EnderIO.MODID.toLowerCase() + ":" + name);
    }

    @Override
    public String getUnlocalizedName() {
        return "item." + EnderIO.MODID.toLowerCase() + ":" + name;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return "item." + EnderIO.MODID.toLowerCase() + ":" + name;
    }

    /* IEnergyTool */

    @Override
    public ToolTier getTier(ItemStack stack) {
        return tier;
    }

    @Override
    public ItemStack setEnergy(ItemStack stack, int energy) {
        if (stack.stackTagCompound == null) {
            stack.stackTagCompound = new NBTTagCompound();
        }

        stack.stackTagCompound.setInteger("Energy", Math.min(energy, getMaxEnergyStored(stack)));
        return stack;
    }

    @Override
    public ItemStack drainEnergy(ItemStack stack, int energy) {
        return setEnergy(stack, Math.max(getEnergyStored(stack) - energy, 0));
    }

    @Override
    public int getEnergyPerUse(ItemStack stack, Block block, int meta) {
        if (isEmpowered(stack))
            return block instanceof IShearable ? getEnergyPerUse(stack) / 5 : getEnergyPerUse(stack);
        else return getEnergyPerUse(stack);
    }

    @Override
    public String writeModeInfo(ItemStack stack) {
        if (!tier.hasModes) return "";

        if (isEmpowered(stack)) return StatCollector.translateToLocal("enderio.shears_on.mode");
        else return StatCollector.translateToLocal("enderio.shears_off.mode");
    }

    /* IEnergyContainerItem */

    @Override
    public int receiveEnergy(ItemStack stack, int maxReceive, boolean simulate) { // stolen from ItemEnergyContainer
        int energy = getEnergyStored(stack);
        int energyReceived = Math.min(tier.maxEnergy - energy, Math.min(tier.rechargeRate, maxReceive));

        if (!simulate) setEnergy(stack, energy + energyReceived);
        return energyReceived;
    }

    @Override
    public int extractEnergy(ItemStack stack, int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored(ItemStack stack) {
        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        if (stack.stackTagCompound.hasKey("Energy")) return stack.stackTagCompound.getInteger("Energy");
        else {
            stack.stackTagCompound.setInteger("Energy", 0);
            return 0;
        }
    }

    @Override
    public int getMaxEnergyStored(ItemStack stack) {
        return tier.maxEnergy;
    }

    /* IEmpowerableItem */

    @Override
    public boolean isEmpowered(ItemStack stack) { // is shears mode activated?
        if (!tier.hasModes) return ConfigHandler.shearsDefault;

        if (stack.stackTagCompound == null) return ConfigHandler.shearsDefault;

        if (stack.stackTagCompound.hasKey("Mode")) return stack.stackTagCompound.getByte("Mode") == 1;
        else return ConfigHandler.shearsDefault;
    }

    @Override
    public boolean setEmpoweredState(ItemStack stack, boolean b) {
        if (!tier.hasModes) return false;

        if (stack.stackTagCompound == null) stack.stackTagCompound = new NBTTagCompound();

        stack.stackTagCompound.setByte("Mode", b ? (byte) 1 : 0);
        return true;
    }

    @Override
    public void onStateChange(EntityPlayer player, ItemStack stack) {
        if (!MiscUtil.isItemSilent(stack)) {
            if (!isEmpowered(stack)) player.worldObj.playSoundAtEntity(player, "random.orb", 0.2F, 0.6F);
            else player.worldObj.playSoundAtEntity(player, "ambient.weather.thunder", 0.4F, 1.0F);
        }
        player.addChatComponentMessage(new ChatComponentText(writeModeInfo(stack)));
    }

    /* IEqualityOverrideItem */

    @Override
    public boolean isLastHeldItemEqual(ItemStack current, ItemStack previous) {
        return current.getItem()
                == previous
                        .getItem(); // used to prevent not being able to mine while the drill is recharging. Otherwise,
        // the mining progress gets reset every tick because of NBT changes
    }
}
