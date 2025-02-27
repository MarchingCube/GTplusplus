package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.GT_HatchElement.*;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.TAE;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_OutputBus;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Recipe.GT_Recipe_Map;
import gregtech.api.util.GT_Utility;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase;
import gtPlusPlus.xmod.gregtech.common.helpers.CraftingHelper;
import gtPlusPlus.xmod.gregtech.common.helpers.autocrafter.AC_Helper_Utils;

public class GT4Entity_AutoCrafter extends GregtechMeta_MultiBlockBase<GT4Entity_AutoCrafter>
        implements ISurvivalConstructable {

    private MODE mMachineMode = MODE.ASSEMBLY;
    private byte mTier = 1;
    protected GT_Recipe mLastRecipeToBuffer;
    private int mCasing;
    private static IStructureDefinition<GT4Entity_AutoCrafter> STRUCTURE_DEFINITION = null;

    /** The crafting matrix inventory (3x3). */
    public CraftingHelper mInventoryCrafter;

    public enum MODE {

        CRAFTING("DISASSEMBLY", "ASSEMBLY"),
        ASSEMBLY("CRAFTING", "DISASSEMBLY"),
        DISASSEMBLY("ASSEMBLY", "CRAFTING");

        private final String lastMode;
        private final String nextMode;

        MODE(String previous, String next) {
            this.lastMode = previous;
            this.nextMode = next;
        }

        public MODE nextMode() {
            return MODE.valueOf(this.nextMode);
        }

        public MODE lastMode() {
            return MODE.valueOf(this.lastMode);
        }
    }

    public void onRightclick(EntityPlayer aPlayer) {}

    public GT4Entity_AutoCrafter(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public GT4Entity_AutoCrafter(String mName) {
        super(mName);
    }

    @Override
    public String getMachineType() {
        return "Assembler, Disassembler";
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT4Entity_AutoCrafter(this.mName);
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        return super.onRunningTick(aStack);
        // return true;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getPollutionPerSecond(ItemStack aStack) {
        return CORE.ConfigSwitches.pollutionPerSecondMultiAutoCrafter;
    }

    public int getAmountOfOutputs() {
        return 1;
    }

    @Override
    protected GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType(getMachineType()).addInfo("Highly Advanced Autocrafter")
                .addInfo("Right Click with a Screwdriver to change mode")
                .addInfo("200% faster than using single block machines of the same voltage")
                .addInfo("Processes two items per voltage tier").addPollutionAmount(getPollutionPerSecond(null))
                .addSeparator().beginStructureBlock(3, 3, 3, true).addController("Front Center")
                .addCasingInfo("Bulk Production Frame", 10).addInputBus("Any Casing", 1).addOutputBus("Any Casing", 1)
                .addInputHatch("Any Casing", 1).addEnergyHatch("Any Casing", 1).addMaintenanceHatch("Any Casing", 1)
                .addMufflerHatch("Any Casing", 1).toolTipFinisher(CORE.GT_Tooltip_Builder.get());
        return tt;
    }

    @Override
    protected IIconContainer getActiveOverlay() {
        return Textures.BlockIcons.OVERLAY_FRONT_DISASSEMBLER_ACTIVE;
    }

    @Override
    protected IIconContainer getInactiveOverlay() {
        return Textures.BlockIcons.OVERLAY_FRONT_DISASSEMBLER;
    }

    @Override
    protected int getCasingTextureId() {
        return TAE.getIndexFromPage(0, 10);
    }

    @Override
    public IStructureDefinition<GT4Entity_AutoCrafter> getStructureDefinition() {
        if (STRUCTURE_DEFINITION == null) {
            STRUCTURE_DEFINITION = StructureDefinition.<GT4Entity_AutoCrafter>builder()
                    .addShape(
                            mName,
                            transpose(
                                    new String[][] { { "CCC", "CCC", "CCC" }, { "C~C", "C-C", "CCC" },
                                            { "CCC", "CCC", "CCC" }, }))
                    .addElement(
                            'C',
                            buildHatchAdder(GT4Entity_AutoCrafter.class)
                                    .atLeast(InputBus, OutputBus, InputHatch, Maintenance, Energy, Muffler)
                                    .casingIndex(TAE.getIndexFromPage(0, 10)).dot(1).buildAndChain(
                                            onElementPass(x -> ++x.mCasing, ofBlock(ModBlocks.blockCasings2Misc, 12))))
                    .build();
        }
        return STRUCTURE_DEFINITION;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(mName, stackSize, hintsOnly, 1, 1, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivialBuildPiece(mName, stackSize, 1, 1, 0, elementBudget, env, false, true);
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        mCasing = 0;
        if (checkPiece(mName, 1, 1, 0) && mCasing >= 10 && checkHatch()) {
            setTier();
            return true;
        } else return false;
    }

    private static GT_Recipe_Map fCircuitMap;

    @Override
    public GT_Recipe.GT_Recipe_Map getRecipeMap() {
        if (this.mMachineMode == MODE.ASSEMBLY) {
            return GT_Recipe.GT_Recipe_Map.sAssemblerRecipes;
        } else if (this.mMachineMode == MODE.DISASSEMBLY || this.mMachineMode == MODE.CRAFTING) {
            return null;
        }
        return GT_Recipe.GT_Recipe_Map.sAssemblerRecipes;
    }

    @Override
    public void onModeChangeByScrewdriver(byte aSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        if (mMachineMode.nextMode() == MODE.CRAFTING) {
            mMachineMode = MODE.ASSEMBLY;
        } else {
            mMachineMode = mMachineMode.nextMode();
        }

        if (mMachineMode == MODE.CRAFTING) {
            PlayerUtils.messagePlayer(
                    aPlayer,
                    "You are now running the Auto-Crafter in mode: " + EnumChatFormatting.AQUA + "AutoCrafting");
        } else if (mMachineMode == MODE.ASSEMBLY) {
            PlayerUtils.messagePlayer(
                    aPlayer,
                    "You are now running the Auto-Crafter in mode: " + EnumChatFormatting.GREEN + "Assembly");
        } else {
            PlayerUtils.messagePlayer(
                    aPlayer,
                    "You are now running the Auto-Crafter in mode: " + EnumChatFormatting.RED + "Disassembly");
        }
    }

    // @Override
    // public boolean checkRecipe(final ItemStack aStack) {
    //
    // final long tVoltage = this.getMaxInputVoltage();
    // final byte tTier = this.mTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));
    //
    // if (mMachineMode == MODE.DISASSEMBLY) {
    // return doDisassembly();
    // } else if (mMachineMode == MODE.CRAFTING) {
    // return doCrafting(aStack);
    // } else {
    // return super.checkRecipeGeneric(getMaxParallelRecipes(), 100, 200);
    // }
    // }

    private void setTier() {
        long tVoltage = getMaxInputVoltage();
        this.mTier = (byte) Math.max(1, GT_Utility.getTier(tVoltage));
    }

    @Override
    public boolean checkRecipe(final ItemStack aStack) {
        if (mMachineMode == MODE.DISASSEMBLY) {
            return doDisassembly();
        } else if (mMachineMode == MODE.CRAFTING) {
            return doCrafting(aStack);
        } else {
            ArrayList<FluidStack> tFluids = getStoredFluids();
            // Logger.MACHINE_INFO("1");
            for (GT_MetaTileEntity_Hatch_InputBus tBus : mInputBusses) {
                ArrayList<ItemStack> tBusItems = new ArrayList<ItemStack>();
                // Logger.MACHINE_INFO("2");
                if (isValidMetaTileEntity(tBus)) {
                    // Logger.MACHINE_INFO("3");
                    for (int i = tBus.getBaseMetaTileEntity().getSizeInventory() - 1; i >= 0; i--) {
                        if (tBus.getBaseMetaTileEntity().getStackInSlot(i) != null)
                            tBusItems.add(tBus.getBaseMetaTileEntity().getStackInSlot(i));
                    }
                }

                Object[] tempArray = tFluids.toArray(new FluidStack[] {});
                FluidStack[] properArray;
                properArray = ((tempArray != null && tempArray.length > 0) ? (FluidStack[]) tempArray
                        : new FluidStack[] {});

                // Logger.MACHINE_INFO("4");
                if (checkRecipeGeneric(
                        tBusItems.toArray(new ItemStack[] {}),
                        properArray,
                        getMaxParallelRecipes(),
                        100,
                        200,
                        10000))
                    return true;
            }
            return false;
        }
    }

    @Override
    public int getMaxParallelRecipes() {
        return 2 * (Math.max(1, GT_Utility.getTier(getMaxInputVoltage())));
    }

    @Override
    public int getEuDiscountForParallelism() {
        return 100;
    }

    public boolean doDisassembly() {

        final ArrayList<ItemStack> tInputList = this.getStoredInputs();
        for (int tInputList_sS = tInputList.size(), i = 0; i < tInputList_sS - 1; ++i) {
            for (int j = i + 1; j < tInputList_sS; ++j) {
                if (GT_Utility.areStacksEqual(tInputList.get(i), tInputList.get(j))) {
                    if (tInputList.get(i).stackSize < tInputList.get(j).stackSize) {
                        tInputList.remove(i--);
                        tInputList_sS = tInputList.size();
                        break;
                    }
                    tInputList.remove(j--);
                    tInputList_sS = tInputList.size();
                }
            }
        }
        final ItemStack[] tInputs = tInputList.toArray(new ItemStack[tInputList.size()]);

        ItemStack inputItem = tInputs[0];
        if (tInputs[0].stackSize <= 0) {
            tInputs[0] = null;
            this.updateSlots();
        }
        int outputSlots = this.mOutputBusses.get(0).getSizeInventory();

        if (this.mOutputBusses.size() > 1) {
            outputSlots = 0;
            for (GT_MetaTileEntity_Hatch_OutputBus r : this.mOutputBusses) {
                outputSlots += r.getSizeInventory();
            }
        }

        this.mOutputItems = new ItemStack[outputSlots];
        if (inputItem != null && inputItem.stackSize > 0) {
            NBTTagCompound tNBT = inputItem.getTagCompound();
            if (tNBT != null) {
                tNBT = tNBT.getCompoundTag("GT.CraftingComponents");
                if (tNBT != null) {
                    this.lEUt = 16 * (1L << this.mTier - 1) * (1L << this.mTier - 1);
                    this.mMaxProgresstime = (100 - (8 * this.mTier));
                    for (int i = 0; i < this.mOutputItems.length; ++i) {
                        if (this.getBaseMetaTileEntity().getRandomNumber(100) < 60 + 12 * this.mTier) {
                            this.mOutputItems[i] = GT_Utility.loadItem(tNBT, "Ingredient." + i);
                            if (this.mOutputItems[i] != null) {
                                this.mMaxProgresstime *= (int) 1.5;
                            }
                        }
                    }

                    if (this.mTier > 5) {
                        this.mMaxProgresstime >>= this.mTier - 5;
                    }
                    if (this.lEUt > 0) this.lEUt = (-this.lEUt);
                    this.mEfficiency = (10000 - (getIdealStatus() - getRepairStatus()) * 1000);
                    this.mEfficiencyIncrease = 10000;
                    inputItem.stackSize--;
                    if (inputItem.stackSize <= 0) {
                        tInputs[0] = null;
                    }
                    this.updateSlots();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doesCrafterHave9SlotInput() {
        GT_MetaTileEntity_Hatch_InputBus craftingInput = null;
        if (!this.mInputBusses.isEmpty()) {
            for (GT_MetaTileEntity_Hatch_InputBus x : this.mInputBusses) {
                if (x.mInventory.length == 9) {
                    craftingInput = x;
                }
            }
        }
        // Return if no input hatch set.
        if (craftingInput == null) {
            Logger.WARNING("Cannot do Auto-Crafting without a 9-slot Input Bus [MV].");
            return false;

        } else {
            return true;
        }
    }

    private boolean doCrafting(ItemStack aStack) {
        this.mMaxProgresstime = 0;
        return false; // do nothing
        /*
         * try { // Set Crafting input hatch if (!doesCrafterHave9SlotInput()) { return false; } // Read stored data
         * from encrypted data stick. ItemStack storedData_Output[] = NBTUtils.readItemsFromNBT(aStack, "Output");
         * ItemStack storedData_Input[] = NBTUtils.readItemsFromNBT(aStack); if (storedData_Output != null &&
         * storedData_Input != null) { ItemStack loadedData[] = new ItemStack[9]; if (storedData_Input.length >= 1) {
         * int number = 0; for (ItemStack a : storedData_Input) { if (a.getItem() == ModItems.ZZZ_Empty) { //
         * Utils.LOG_WARNING("Allocating free memory into crafting manager slot // "+number+"."); loadedData[number] =
         * null; } else { // Utils.LOG_WARNING("Downloading "+a.getDisplayName()+" into crafting manager // slot
         * "+number+"."); loadedData[number] = a; } number++; } } // Remove inputs here ArrayList<ItemStack> mInputArray
         * = new ArrayList<ItemStack>(); ItemStack allInputs[]; for (GT_MetaTileEntity_Hatch_InputBus x :
         * this.mInputBusses) { if (x.mInventory.length > 0) { for (ItemStack r : x.mInventory) { if (r != null) {
         * mInputArray.add(r); } } } } if (mInputArray.isEmpty()) { return false; } else { List<ItemStack> list =
         * mInputArray; allInputs = list.toArray(new ItemStack[list.size()]); if (allInputs != null && allInputs.length
         * > 0) { this.mEUt = 8 * (1 << this.mTier - 1) * (1 << this.mTier - 1); this.mMaxProgresstime =
         * MathUtils.roundToClosestInt((50 - (5 MathUtils.randDouble(((this.mTier - 2) <= 0 ? 1 : (this.mTier - 2)),
         * this.mTier)))); Logger.WARNING("MPT: " + mMaxProgresstime + " | " + mEUt);
         * this.getBaseMetaTileEntity().setActive(true); // Setup some vars int counter = 0; ItemStack toUse[] = new
         * ItemStack[9]; outerloop: for (ItemStack inputItem : loadedData) { if (inputItem == null) { toUse[counter] =
         * null; continue outerloop; } for (ItemStack r : allInputs) { if (r != null) { //
         * Utils.LOG_WARNING("Input Bus Inventory Iteration - Found:" // +r.getDisplayName()+" | "+allInputs.length); if
         * (GT_Utility.areStacksEqual(r, inputItem)) { if (this.getBaseMetaTileEntity().isServerSide()) { toUse[counter]
         * = inputItem; counter++; continue outerloop; } } } } counter++; } int mCorrectInputs = 0; for (ItemStack
         * isValid : toUse) { if (isValid == null || this.depleteInput(isValid)) { mCorrectInputs++; } else {
         * Logger.WARNING("Input in Slot " + mCorrectInputs + " was not valid."); } } if (this.mTier > 5) {
         * this.mMaxProgresstime >>= this.mTier - 5; } if (this.mEUt > 0) this.mEUt = (-this.mEUt); this.mEfficiency =
         * (10000 - (getIdealStatus() - getRepairStatus()) * 1000); this.mEfficiencyIncrease = 10000; if (mCorrectInputs
         * == 9) { ItemStack mOutputItem = storedData_Output[0]; NBTUtils.writeItemsToGtCraftingComponents(mOutputItem,
         * loadedData, true); this.addOutput(mOutputItem); this.updateSlots(); return true; } else { return false; } } }
         * } } // End Debug catch (Throwable t) { t.printStackTrace(); this.mMaxProgresstime = 0; }
         * this.mMaxProgresstime = 0; return false;
         */
    }

    @Override
    public String[] getExtraInfoData() {
        final String tRunning = (this.mMaxProgresstime > 0 ? "Auto-Crafter running" : "Auto-Crafter stopped");
        final String tMaintainance = (this.getIdealStatus() == this.getRepairStatus() ? "No Maintainance issues"
                : "Needs Maintainance");
        String tSpecialText = "" + (60 + 12 * this.mTier) + "% chance to recover disassembled parts.";
        String tMode;
        if (mMachineMode == MODE.DISASSEMBLY) {
            tMode = "§cDisassembly";
            tSpecialText = "" + (60 + 12 * this.mTier) + "% chance to recover disassembled parts.";
        } else if (mMachineMode == MODE.ASSEMBLY) {
            tMode = "§aAssembly";
            if (mLastRecipeToBuffer != null && mLastRecipeToBuffer.mOutputs[0].getDisplayName() != null) {
                tSpecialText = "Currently processing: " + mLastRecipeToBuffer.mOutputs[0].getDisplayName();
            } else {
                tSpecialText = "Currently processing: Nothing";
            }
        } else {
            tMode = "§dAuto-Crafting";
            tSpecialText = "Does Auto-Crafter have 9-slot input bus? " + doesCrafterHave9SlotInput();
        }

        return new String[] { "Large Scale Auto-Asesembler v1.01c", tRunning, tMaintainance, "Mode: " + tMode,
                tSpecialText };
    }

    private String getMode() {
        return this.mMachineMode.name();
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        String mMode = getMode();
        aNBT.setString("mMode", mMode);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        String modeString = aNBT.getString("mMode");
        MODE newMode = MODE.valueOf(modeString);
        this.mMachineMode = newMode;
        super.loadNBTData(aNBT);
    }

    @Override
    public void explodeMultiblock() {
        AC_Helper_Utils.removeCrafter(this);
        super.explodeMultiblock();
    }

    @Override
    public void onExplosion() {
        AC_Helper_Utils.removeCrafter(this);
        super.onExplosion();
    }

    @Override
    public void onRemoval() {
        AC_Helper_Utils.removeCrafter(this);
        super.onRemoval();
    }

    @Override
    public void doExplosion(long aExplosionPower) {
        AC_Helper_Utils.removeCrafter(this);
        super.doExplosion(aExplosionPower);
    }
}
