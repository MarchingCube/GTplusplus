package gtPlusPlus.xmod.gregtech.common.tileentities.machines.multi.production.turbines;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.lazy;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static gregtech.api.enums.GT_HatchElement.*;
import static gregtech.api.util.GT_StructureUtility.buildHatchAdder;
import static gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase.GTPPHatchElement.TTDynamo;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;

import gregtech.api.enums.Materials;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.GT_MetaGenerated_Tool;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Dynamo;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_InputBus;
import gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Muffler;
import gregtech.api.util.GT_Multiblock_Tooltip_Builder;
import gregtech.api.util.GT_Recipe;
import gregtech.api.util.GT_Utility;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.api.objects.data.AutoMap;
import gtPlusPlus.api.objects.minecraft.BlockPos;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.util.Utils;
import gtPlusPlus.core.util.math.MathUtils;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import gtPlusPlus.core.util.minecraft.gregtech.PollutionUtils;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.GT_MetaTileEntity_Hatch_Turbine;
import gtPlusPlus.xmod.gregtech.api.metatileentity.implementations.base.GregtechMeta_MultiBlockBase;

public abstract class GregtechMetaTileEntity_LargerTurbineBase extends
        GregtechMeta_MultiBlockBase<GregtechMetaTileEntity_LargerTurbineBase> implements ISurvivalConstructable {

    protected int baseEff = 0;
    protected long optFlow = 0;
    protected double realOptFlow = 0;
    protected int storedFluid = 0;
    protected int counter = 0;
    protected boolean mFastMode = false;
    protected double mufflerReduction = 1;
    protected float[] flowMultipliers = new float[] { 1, 1, 1 };

    public ITexture frontFace;
    public ITexture frontFaceActive;

    public ArrayList<GT_MetaTileEntity_Hatch_Turbine> mTurbineRotorHatches = new ArrayList<GT_MetaTileEntity_Hatch_Turbine>();

    public GregtechMetaTileEntity_LargerTurbineBase(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
        frontFace = getTextureFrontFace();
        frontFaceActive = getTextureFrontFaceActive();
    }

    public GregtechMetaTileEntity_LargerTurbineBase(String aName) {
        super(aName);
        frontFace = getTextureFrontFace();
        frontFaceActive = getTextureFrontFaceActive();
    }

    protected abstract ITexture getTextureFrontFace();

    protected abstract ITexture getTextureFrontFaceActive();

    protected abstract String getTurbineType();

    protected abstract String getCasingName();

    protected abstract boolean requiresOutputHatch();

    @Override
    protected final GT_Multiblock_Tooltip_Builder createTooltip() {
        GT_Multiblock_Tooltip_Builder tt = new GT_Multiblock_Tooltip_Builder();
        tt.addMachineType(getMachineType()).addInfo("Controller Block for the XL " + getTurbineType() + " Turbine")
                .addInfo("Runs as fast as 16 Large Turbines of the same type, takes the space of 12")
                .addInfo("Right-click with screwdriver to enable Fast Mode, to run it even faster")
                .addInfo("Optimal flow will increase or decrease accordingly on mode switch")
                .addInfo("Fast Mode increases speed to 48x instead of 16x, with some penalties")
                .addInfo("Maintenance problems and turbine damage happen 12x as often in Fast Mode")
                .addInfo("XL Steam Turbines can use Loose Mode with either Slow or Fast Mode")
                .addPollutionAmount(getPollutionPerSecond(null)).addInfo("Pollution is 3x higher in Fast Mode")
                .addSeparator().beginStructureBlock(7, 9, 7, false).addController("Top Middle")
                .addCasingInfo(getCasingName(), 360).addCasingInfo("Rotor Shaft", 30)
                .addOtherStructurePart("Rotor Assembly", "Any 1 dot hint", 1).addInputBus("Any 4 dot hint (min 1)", 4)
                .addInputHatch("Any 4 dot hint(min 1)", 4);
        if (requiresOutputHatch()) {
            tt.addOutputHatch("Any 4 dot hint(min 1)", 4);
        }
        tt.addDynamoHatch("Any 4 dot hint(min 1)", 4).addMaintenanceHatch("Any 4 dot hint(min 1)", 4);
        if (requiresMufflers()) {
            tt.addMufflerHatch("Any 7 dot hint (x4)", 7);
        }
        tt.toolTipFinisher(CORE.GT_Tooltip_Builder.get());
        return tt;
    }

    private static final String STRUCTURE_PIECE_MAIN = "main";
    private static final ClassValue<IStructureDefinition<GregtechMetaTileEntity_LargerTurbineBase>> STRUCTURE_DEFINITION = new ClassValue<IStructureDefinition<GregtechMetaTileEntity_LargerTurbineBase>>() {

        @Override
        protected IStructureDefinition<GregtechMetaTileEntity_LargerTurbineBase> computeValue(Class<?> type) {
            return StructureDefinition.<GregtechMetaTileEntity_LargerTurbineBase>builder()
                    // c = turbine casing
                    // s = turbine shaft
                    // t = turbine housing
                    // h = dynamo/maint
                    // m = muffler
                    .addShape(
                            STRUCTURE_PIECE_MAIN,
                            (new String[][] {
                                    { "ccchccc", "ccccccc", "ccmmmcc", "ccm~mcc", "ccmmmcc", "ccccccc", "ccchccc" },
                                    { "ctchctc", "cscccsc", "cscccsc", "cscccsc", "cscccsc", "cscccsc", "ctchctc" },
                                    { "ccchccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccchccc" },
                                    { "ccchccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccchccc" },
                                    { "ctchctc", "cscccsc", "cscccsc", "cscccsc", "cscccsc", "cscccsc", "ctchctc" },
                                    { "ccchccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccchccc" },
                                    { "ccchccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccchccc" },
                                    { "ctchctc", "cscccsc", "cscccsc", "cscccsc", "cscccsc", "cscccsc", "ctchctc" },
                                    { "ccchccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccccccc", "ccchccc" }, }))
                    .addElement('c', lazy(t -> ofBlock(t.getCasingBlock(), t.getCasingMeta())))
                    .addElement('s', lazy(t -> ofBlock(t.getShaftBlock(), t.getTurbineShaftMeta())))
                    .addElement(
                            't',
                            lazy(
                                    t -> buildHatchAdder(GregtechMetaTileEntity_LargerTurbineBase.class)
                                            .adder(GregtechMetaTileEntity_LargerTurbineBase::addTurbineHatch)
                                            .hatchClass(GT_MetaTileEntity_Hatch_Turbine.class)
                                            .casingIndex(t.getCasingTextureIndex()).dot(1).build()))
                    .addElement(
                            'h',
                            lazy(
                                    t -> buildHatchAdder(GregtechMetaTileEntity_LargerTurbineBase.class)
                                            .atLeast(
                                                    InputBus,
                                                    InputHatch,
                                                    OutputHatch,
                                                    Dynamo.or(TTDynamo),
                                                    Maintenance)
                                            .casingIndex(t.getCasingTextureIndex()).dot(4)
                                            .buildAndChain(t.getCasingBlock(), t.getCasingMeta())))
                    .addElement(
                            'm',
                            lazy(
                                    t -> buildHatchAdder(GregtechMetaTileEntity_LargerTurbineBase.class)
                                            .atLeast(Muffler).casingIndex(t.getCasingTextureIndex()).dot(7)
                                            .buildAndChain(t.getCasingBlock(), t.getCasingMeta())))
                    .build();
        }
    };

    @Override
    public IStructureDefinition<GregtechMetaTileEntity_LargerTurbineBase> getStructureDefinition() {
        return STRUCTURE_DEFINITION.get(getClass());
    }

    private boolean requiresMufflers() {
        if (!PollutionUtils.isPollutionEnabled()) {
            return false;
        }
        return getPollutionPerSecond(null) > 0;
    }

    public final double getMufflerReduction() {
        double totalReduction = 0;
        for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                totalReduction += ((double) tHatch.calculatePollutionReduction(100)) / 100;
            }
        }
        return totalReduction / 4;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        this.mDynamoHatches.clear();
        this.mTecTechDynamoHatches.clear();
        this.mTurbineRotorHatches.clear();
        this.mMaintenanceHatches.clear();
        if (requiresMufflers()) {
            this.mMufflerHatches.clear();
        }
        this.mInputBusses.clear();
        this.mInputHatches.clear();
        if (requiresOutputHatch()) {
            this.mOutputHatches.clear();
        }
        // we do not check for casing count here. the bare minimal is 372 but we only require 360
        boolean aStructure = checkPiece(STRUCTURE_PIECE_MAIN, 3, 3, 0);
        log("Structure Check: " + aStructure);
        if (mTurbineRotorHatches.size() != 12 || mMaintenanceHatches.size() != 1
                || (mDynamoHatches.size() < 1 && mTecTechDynamoHatches.size() < 1)
                || (requiresMufflers() && mMufflerHatches.size() != 4)
                || mInputBusses.size() < 1
                || mInputHatches.size() < 1
                || (requiresOutputHatch() && mOutputHatches.size() < 1)) {
            log(
                    "Bad Hatches - Turbine Housings: " + mTurbineRotorHatches.size()
                            + ", Maint: "
                            + mMaintenanceHatches.size()
                            + ", Dynamo: "
                            + mDynamoHatches.size()
                            + ", Muffler: "
                            + mMufflerHatches.size()
                            + ", Input Buses: "
                            + mInputBusses.size()
                            + ", Input Hatches: "
                            + mInputHatches.size()
                            + ", Output Hatches: "
                            + mOutputHatches.size());
            return false;
        }
        mufflerReduction = getMufflerReduction();
        return aStructure;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 3, 3, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        int realBudget = elementBudget >= 200 ? elementBudget : Math.min(200, elementBudget * 2);
        return survivialBuildPiece(STRUCTURE_PIECE_MAIN, stackSize, 3, 3, 0, realBudget, env, false, true);
    }

    public boolean addTurbineHatch(final IGregTechTileEntity aTileEntity, final int aBaseCasingIndex) {
        if (aTileEntity == null) {
            return false;
        }
        final IMetaTileEntity aMetaTileEntity = aTileEntity.getMetaTileEntity();
        if (aMetaTileEntity == null) {
            return false;
        }
        if (aMetaTileEntity instanceof GT_MetaTileEntity_Hatch_Turbine) {
            log("Found GT_MetaTileEntity_Hatch_Turbine");
            updateTexture(aTileEntity, aBaseCasingIndex);
            GT_MetaTileEntity_Hatch_Turbine aTurbineHatch = (GT_MetaTileEntity_Hatch_Turbine) aMetaTileEntity;
            IGregTechTileEntity g = this.getBaseMetaTileEntity();
            if (aTurbineHatch.setController(new BlockPos(g.getXCoord(), g.getYCoord(), g.getZCoord(), g.getWorld()))) {
                boolean aDidAdd = this.mTurbineRotorHatches.add(aTurbineHatch);
                Logger.INFO("Injected Controller into Turbine Assembly. Found: " + this.mTurbineRotorHatches.size());
                return aDidAdd;
            } else {
                Logger.INFO("Failed to inject controller into Turbine Assembly Hatch.");
            }
        }
        log("Bad Turbine Housing");
        return false;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return getMaxEfficiency(aStack) > 0;
    }

    public Block getCasingBlock() {
        return ModBlocks.blockSpecialMultiCasings;
    }

    public final Block getShaftBlock() {
        return ModBlocks.blockSpecialMultiCasings;
    }

    public abstract int getCasingMeta();

    public byte getTurbineShaftMeta() {
        return 0;
    }

    public abstract int getCasingTextureIndex();

    public abstract int getFuelValue(FluidStack aLiquid);

    public static boolean isValidTurbine(ItemStack aTurbine) {
        return (aTurbine != null && aTurbine.getItem() instanceof GT_MetaGenerated_Tool
                && aTurbine.getItemDamage() >= 170
                && aTurbine.getItemDamage() <= 176);
    }

    protected ArrayList<ItemStack> getAllBufferedTurbines() {
        startRecipeProcessing();
        ArrayList<ItemStack> aTurbinesInStorage = new ArrayList<>();
        for (ItemStack aStack : getStoredInputs()) {
            if (isValidTurbine(aStack)) {
                int stackSize = aStack.stackSize;
                while (stackSize > 0) {
                    int tmpStackSize = Math.min(stackSize, aStack.getMaxStackSize());
                    ItemStack copy = aStack.copy();
                    copy.stackSize = tmpStackSize;
                    aTurbinesInStorage.add(copy);
                    stackSize -= tmpStackSize;
                }
            }
        }
        endRecipeProcessing();
        return aTurbinesInStorage;
    }

    public boolean areAllTurbinesTheSame() {
        ArrayList<GT_MetaTileEntity_Hatch_Turbine> aTurbineAssemblies = getFullTurbineAssemblies();
        if (aTurbineAssemblies.size() < 12) {
            log("Found " + aTurbineAssemblies.size() + ", expected 12.");
            return false;
        }
        AutoMap<Materials> aTurbineMats = new AutoMap<>();
        AutoMap<Integer> aTurbineSizes = new AutoMap<>();
        for (GT_MetaTileEntity_Hatch_Turbine aHatch : aTurbineAssemblies) {
            aTurbineMats.add(GT_MetaGenerated_Tool.getPrimaryMaterial(aHatch.getTurbine()));
            aTurbineSizes.add(getTurbineSize(aHatch.getTurbine()));
        }
        Materials aBaseMat = aTurbineMats.get(0);
        int aBaseSize = aTurbineSizes.get(0);
        for (int aSize : aTurbineSizes) {
            if (aBaseSize != aSize) {
                return false;
            }
        }
        for (Materials aMat : aTurbineMats) {
            if (aBaseMat != aMat) {
                return false;
            }
        }
        return true;
    }

    public static int getTurbineSize(ItemStack aTurbine) {
        if (isValidTurbine(aTurbine)) {
            if (aTurbine.getItemDamage() >= 170 && aTurbine.getItemDamage() < 172) {
                return 1;
            } else if (aTurbine.getItemDamage() >= 172 && aTurbine.getItemDamage() < 174) {
                return 2;
            } else if (aTurbine.getItemDamage() >= 174 && aTurbine.getItemDamage() < 176) {
                return 3;
            } else if (aTurbine.getItemDamage() >= 176 && aTurbine.getItemDamage() < 178) {
                return 4;
            }
        }
        return 0;
    }

    public static String getTurbineSizeString(int aSize) {
        switch (aSize) {
            case 1:
                return "Small Turbine";
            case 2:
                return "Turbine";
            case 3:
                return "Large Turbine";
            case 4:
                return "Huge Turbine";
            default:
                return "";
        }
    }

    protected ArrayList<GT_MetaTileEntity_Hatch_Turbine> getEmptyTurbineAssemblies() {
        ArrayList<GT_MetaTileEntity_Hatch_Turbine> aEmptyTurbineRotorHatches = new ArrayList<GT_MetaTileEntity_Hatch_Turbine>();
        // log("Checking "+mTurbineRotorHatches.size()+" Assemblies for empties.");
        for (GT_MetaTileEntity_Hatch_Turbine aTurbineHatch : this.mTurbineRotorHatches) {
            if (!aTurbineHatch.hasTurbine()) {
                // log("Found Assembly without Turbine.");
                aEmptyTurbineRotorHatches.add(aTurbineHatch);
            }
        }
        return aEmptyTurbineRotorHatches;
    }

    protected ArrayList<GT_MetaTileEntity_Hatch_Turbine> getFullTurbineAssemblies() {
        ArrayList<GT_MetaTileEntity_Hatch_Turbine> aTurbineRotorHatches = new ArrayList<GT_MetaTileEntity_Hatch_Turbine>();
        // log("Checking "+mTurbineRotorHatches.size()+" Assemblies for Turbines.");
        for (GT_MetaTileEntity_Hatch_Turbine aTurbineHatch : this.mTurbineRotorHatches) {
            if (aTurbineHatch.hasTurbine()) {
                // log("Found Assembly with Turbine.");
                aTurbineRotorHatches.add(aTurbineHatch);
            }
        }
        return aTurbineRotorHatches;
    }

    protected boolean depleteTurbineFromStock(ItemStack aTurbine) {
        if (aTurbine == null) {
            return false;
        }
        startRecipeProcessing();
        for (GT_MetaTileEntity_Hatch_InputBus aInputBus : this.mInputBusses) {
            for (int slot = aInputBus.getSizeInventory() - 1; slot >= 0; slot--) {
                ItemStack aStack = aInputBus.getStackInSlot(slot);
                if (aStack != null && GT_Utility.areStacksEqual(aStack, aTurbine)) {
                    aStack.stackSize -= aTurbine.stackSize;
                    updateSlots();
                    endRecipeProcessing();
                    return true;
                }
            }
        }
        endRecipeProcessing();
        return false;
    }

    @Override
    public boolean checkRecipe(final ItemStack aStack) {
        return checkRecipeGeneric(
                new ItemStack[] {},
                getStoredFluids().toArray(new FluidStack[] {}),
                1,
                100,
                100,
                10000);
    }

    @Override
    public boolean checkRecipeGeneric(ItemStack[] aItemInputs, FluidStack[] aFluidInputs, int aMaxParallelRecipes,
            long aEUPercent, int aSpeedBonusPercent, int aOutputChanceRoll, GT_Recipe aRecipe) {

        try {
            ArrayList<GT_MetaTileEntity_Hatch_Turbine> aEmptyTurbineRotorHatches = getEmptyTurbineAssemblies();
            if (aEmptyTurbineRotorHatches.size() > 0) {
                log("Found " + aEmptyTurbineRotorHatches.size() + " Assemblies without Turbine.");
                hatch: for (GT_MetaTileEntity_Hatch_Turbine aHatch : aEmptyTurbineRotorHatches) {
                    ArrayList<ItemStack> aTurbines = getAllBufferedTurbines();
                    for (ItemStack aTurbineItem : aTurbines) {
                        if (aTurbineItem == null) {
                            continue;
                        }
                        if (aHatch.insertTurbine(aTurbineItem.copy())) {
                            boolean aDidDeplete = depleteTurbineFromStock(aTurbineItem);
                            log("Put Turbine into Assembly - " + aDidDeplete);
                            continue hatch;
                        }
                    }
                }
            }

            if (getEmptyTurbineAssemblies().size() > 0 || !areAllTurbinesTheSame()) {
                log("BAD RETURN - 1");
                stopMachine();
                return false;
            }

            ArrayList<FluidStack> tFluids = getStoredFluids();

            if (tFluids.size() > 0) {
                if (baseEff == 0 || optFlow == 0
                        || counter >= 512
                        || this.getBaseMetaTileEntity().hasWorkJustBeenEnabled()
                        || this.getBaseMetaTileEntity().hasInventoryBeenModified()) {
                    counter = 0;

                    // log("Running checkRecipeGeneric(1)");
                    float aTotalBaseEff = 0;
                    float aTotalOptimalFlow = 0;

                    ItemStack aStack = getFullTurbineAssemblies().get(0).getTurbine();
                    for (int i = 0; i < getSpeedMultiplier(); i++) {
                        if (i == 0) {
                            aTotalBaseEff += GT_Utility.safeInt(
                                    (long) ((5F
                                            + ((GT_MetaGenerated_Tool) aStack.getItem()).getToolCombatDamage(aStack))
                                            * 1000F));
                            // log("Bumped base eff to "+aTotalBaseEff);
                        }
                        aTotalOptimalFlow += GT_Utility.safeInt(
                                (long) Math.max(
                                        Float.MIN_NORMAL,
                                        ((GT_MetaGenerated_Tool) aStack.getItem()).getToolStats(aStack)
                                                .getSpeedMultiplier()
                                                * GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mToolSpeed
                                                * 50));
                        if (aTotalOptimalFlow < 0) {
                            log("Int overflow, report to issue tracker");
                            aTotalOptimalFlow = 100;
                        }
                        // log("Bumped base optimal flow to "+aTotalOptimalFlow);
                    }

                    flowMultipliers[0] = GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mSteamMultiplier;
                    flowMultipliers[1] = GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mGasMultiplier;
                    flowMultipliers[2] = GT_MetaGenerated_Tool.getPrimaryMaterial(aStack).mPlasmaMultiplier;
                    // log("Running checkRecipeGeneric(2)");
                    // log("Total base eff: "+aTotalBaseEff);
                    // log("Total base optimal flow: "+aTotalOptimalFlow);
                    baseEff = MathUtils.roundToClosestInt(aTotalBaseEff);
                    optFlow = MathUtils.roundToClosestInt(aTotalOptimalFlow);
                    // log("Total eff: "+baseEff);
                    // log("Total optimal flow: "+optFlow);
                    if (optFlow <= 0 || baseEff <= 0) {
                        log("Running checkRecipeGeneric(bad-1)");
                        stopMachine(); // in case the turbine got removed
                        return false;
                    }
                } else {
                    counter++;
                }
            }

            // log("Total eff: "+baseEff);
            // log("Total optimal flow: "+optFlow);

            // How much the turbine should be producing with this flow
            int newPower = fluidIntoPower(tFluids, optFlow, baseEff, flowMultipliers);
            // log("Bumped newPower to "+newPower);
            // log("New Power: "+newPower);
            long difference = newPower - this.lEUt; // difference between current output and new output
            // log("diff: "+difference);

            // Magic numbers: can always change by at least 10 eu/t, but otherwise by at most 1 percent of the
            // difference in power level (per tick)
            // This is how much the turbine can actually change during this tick
            int maxChangeAllowed = Math.max(10, GT_Utility.safeInt((long) Math.abs(difference) / 100));
            // log("Max Change Allowed: "+maxChangeAllowed);

            if (Math.abs(difference) > maxChangeAllowed) { // If this difference is too big, use the maximum allowed
                                                           // change
                int change = maxChangeAllowed * (difference > 0 ? 1 : -1); // Make the change positive or negative.
                this.lEUt += change; // Apply the change
                // log("Applied power change.");
            } else {
                this.lEUt = newPower;
                // log("Using same value.");
            }
            if (this.lEUt <= 0) {
                this.lEUt = 0;
                this.mEfficiency = 0;
                log("Running checkRecipeGeneric(bad-2)");
                // stopMachine();
                return false;
            } else {
                this.mMaxProgresstime = 1;
                this.mEfficiencyIncrease = 10;
                // Overvoltage is handled inside the MultiBlockBase when pushing out to dynamos. no need to do it here.
                // Play sounds (GT++ addition - GT multiblocks play no sounds)
                startProcess();
                // log("GOOD RETURN - Making: "+this.mEUt+" EU/t");
                return true;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    public boolean doRandomMaintenanceDamage() {
        if (getMaxParallelRecipes() == 0 || getRepairStatus() == 0) {
            stopMachine();
            return false;
        }
        if (mRuntime++ > 1000) {
            mRuntime = 0;

            if (getBaseMetaTileEntity().getRandomNumber(6000) < getMaintenanceThreshold()) {
                switch (getBaseMetaTileEntity().getRandomNumber(6)) {
                    case 0:
                        mWrench = false;
                        break;
                    case 1:
                        mScrewdriver = false;
                        break;
                    case 2:
                        mSoftHammer = false;
                        break;
                    case 3:
                        mHardHammer = false;
                        break;
                    case 4:
                        mSolderingTool = false;
                        break;
                    case 5:
                        mCrowbar = false;
                        break;
                }
            }
            for (GT_MetaTileEntity_Hatch_Turbine aHatch : getFullTurbineAssemblies()) {
                // This cycle depletes durability from the turbine rotors.
                // The amount of times it is run depends on turbineDamageMultiplier
                // In XL turbines, durability loss is around 5.2-5.3x faster than in singles
                // To compensate for that, the mEU/t scaling is divided by 5 to make it only slightly faster
                for (int i = 0; i < getTurbineDamageMultiplier(); i++) {
                    aHatch.damageTurbine(lEUt / 5, damageFactorLow, damageFactorHigh);
                }
            }
        }
        return true;
    }

    @Override
    public int getMaxParallelRecipes() {
        return (getFullTurbineAssemblies().size());
    }

    abstract int fluidIntoPower(ArrayList<FluidStack> aFluids, long aOptFlow, int aBaseEff, float[] flowMultipliers);

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 1;
    }

    public int getMaxEfficiency(ItemStack aStack) {
        return this.getMaxParallelRecipes() == 12 ? 10000 : 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    public boolean isLooseMode() {
        return false;
    }

    @Override
    public String[] getExtraInfoData() {
        int mPollutionReduction = (int) (100 * mufflerReduction);

        String tRunning = mMaxProgresstime > 0
                ? EnumChatFormatting.GREEN + StatCollector.translateToLocal("GT5U.turbine.running.true")
                        + EnumChatFormatting.RESET
                : EnumChatFormatting.RED + StatCollector.translateToLocal("GT5U.turbine.running.false")
                        + EnumChatFormatting.RESET;

        String tMaintainance = getIdealStatus() == getRepairStatus()
                ? EnumChatFormatting.GREEN + StatCollector.translateToLocal("GT5U.turbine.maintenance.false")
                        + EnumChatFormatting.RESET
                : EnumChatFormatting.RED + StatCollector.translateToLocal("GT5U.turbine.maintenance.true")
                        + EnumChatFormatting.RESET;
        int tDura = 0;

        StringBuilder aTurbineDamage = new StringBuilder();
        for (GT_MetaTileEntity_Hatch_Turbine aHatch : this.getFullTurbineAssemblies()) {
            ItemStack aTurbine = aHatch.getTurbine();
            tDura = MathUtils.safeInt(
                    (long) (100.0f / GT_MetaGenerated_Tool.getToolMaxDamage(aTurbine)
                            * (GT_MetaGenerated_Tool.getToolDamage(aTurbine)) + 1));
            aTurbineDamage.append(EnumChatFormatting.RED).append(tDura).append(EnumChatFormatting.RESET).append("% | ");
        }

        long storedEnergy = 0;
        long maxEnergy = 0;
        for (GT_MetaTileEntity_Hatch_Dynamo tHatch : mDynamoHatches) {
            if (isValidMetaTileEntity(tHatch)) {
                storedEnergy += tHatch.getBaseMetaTileEntity().getStoredEU();
                maxEnergy += tHatch.getBaseMetaTileEntity().getEUCapacity();
            }
        }

        boolean aIsSteam = this.getClass().getName().toLowerCase().contains("steam");

        String[] ret = new String[] {
                // 8 Lines available for information panels
                tRunning + ": "
                        + EnumChatFormatting.RED
                        + ((lEUt * mEfficiency) / 10000)
                        + EnumChatFormatting.RESET
                        + " EU/t",
                tMaintainance,
                StatCollector.translateToLocal("GT5U.turbine.efficiency") + ": "
                        + EnumChatFormatting.YELLOW
                        + (mEfficiency / 100F)
                        + EnumChatFormatting.RESET
                        + "%",
                StatCollector.translateToLocal("GT5U.multiblock.energy") + ": "
                        + EnumChatFormatting.GREEN
                        + storedEnergy
                        + EnumChatFormatting.RESET
                        + " EU / "
                        + EnumChatFormatting.YELLOW
                        + maxEnergy
                        + EnumChatFormatting.RESET
                        + " EU",
                StatCollector.translateToLocal("GT5U.turbine.flow") + ": "
                        + EnumChatFormatting.YELLOW
                        + MathUtils.safeInt((long) realOptFlow)
                        + EnumChatFormatting.RESET
                        + " L/s"
                        + EnumChatFormatting.YELLOW
                        + " ("
                        + (isLooseMode() ? StatCollector.translateToLocal("GT5U.turbine.loose")
                                : StatCollector.translateToLocal("GT5U.turbine.tight"))
                        + ")",
                StatCollector.translateToLocal("GT5U.turbine.fuel") + ": "
                        + EnumChatFormatting.GOLD
                        + storedFluid
                        + EnumChatFormatting.RESET
                        + "L",
                StatCollector.translateToLocal("GT5U.turbine.dmg") + ": " + aTurbineDamage,
                StatCollector.translateToLocal("GT5U.multiblock.pollution") + ": "
                        + EnumChatFormatting.GREEN
                        + mPollutionReduction
                        + EnumChatFormatting.RESET
                        + " %" };
        if (!aIsSteam) ret[4] = StatCollector.translateToLocal("GT5U.turbine.flow") + ": "
                + EnumChatFormatting.YELLOW
                + MathUtils.safeInt((long) realOptFlow)
                + EnumChatFormatting.RESET
                + " L/t";
        return ret;
    }

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    public boolean polluteEnvironment(int aPollutionLevel) {
        if (this.requiresMufflers()) {
            mPollution += aPollutionLevel * getPollutionMultiplier() * mufflerReduction;
            for (GT_MetaTileEntity_Hatch_Muffler tHatch : mMufflerHatches) {
                if (isValidMetaTileEntity(tHatch)) {
                    if (mPollution >= 10000) {
                        if (PollutionUtils.addPollution(this.getBaseMetaTileEntity(), 10000)) {
                            mPollution -= 10000;
                        }
                    } else {
                        break;
                    }
                }
            }
            return mPollution < 10000;
        }
        return true;
    }

    @Override
    public long maxAmperesOut() {
        // This should not be a hard limit, due to TecTech dynamos
        if (mFastMode) {
            return 64;
        } else {
            return 16;
        }
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setBoolean("mFastMode", mFastMode);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        mFastMode = aNBT.getBoolean("mFastMode");
        super.loadNBTData(aNBT);
    }

    @Override
    public void onModeChangeByScrewdriver(byte aSide, EntityPlayer aPlayer, float aX, float aY, float aZ) {
        mFastMode = Utils.invertBoolean(mFastMode);
        if (mFastMode) {
            PlayerUtils.messagePlayer(aPlayer, "Running in Fast (48x) Mode.");
        } else {
            PlayerUtils.messagePlayer(aPlayer, "Running in Slow (16x) Mode.");
        }
    }

    @Override
    public final ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, byte aSide, byte aFacing,
            byte aColorIndex, boolean aActive, boolean aRedstone) {
        return new ITexture[] { Textures.BlockIcons.MACHINE_CASINGS[1][aColorIndex + 1],
                aFacing == aSide ? getFrontFacingTurbineTexture(aActive)
                        : Textures.BlockIcons.getCasingTextureForId(getCasingTextureIndex()) };
    }

    protected ITexture getFrontFacingTurbineTexture(boolean isActive) {
        if (isActive) {
            return frontFaceActive;
        }
        return frontFace;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        if (aBaseMetaTileEntity.isServerSide()) {
            if (mUpdate == 1 || mStartUpCheck == 1) {
                log("Cleared Rotor Assemblies.");
                this.mTurbineRotorHatches.clear();
            }
        }
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (this.maxProgresstime() > 0 || this.getBaseMetaTileEntity().hasWorkJustBeenEnabled()) {
            enableAllTurbineHatches();
        }
        if (this.maxProgresstime() <= 0) {
            stopMachine();
        }
    }

    @Override
    public void startProcess() {
        super.startProcess();
        enableAllTurbineHatches();
    }

    @Override
    public void stopMachine() {
        baseEff = 0;
        optFlow = 0;
        disableAllTurbineHatches();
        super.stopMachine();
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        for (GT_MetaTileEntity_Hatch_Turbine h : this.mTurbineRotorHatches) {
            h.clearController();
        }
        disableAllTurbineHatches();
        this.mTurbineRotorHatches.clear();
    }

    public void enableAllTurbineHatches() {
        updateTurbineHatches(this.isMachineRunning());
    }

    public void disableAllTurbineHatches() {
        updateTurbineHatches(false);
    }

    private Long mLastHatchUpdate;

    public int updateTurbineHatches(boolean aState) {
        int aUpdated = 0;
        if (mLastHatchUpdate == null) {
            mLastHatchUpdate = System.currentTimeMillis() / 1000;
        }
        if (this.mTurbineRotorHatches.isEmpty() || ((System.currentTimeMillis() / 1000) - mLastHatchUpdate) <= 2) {
            return 0;
        }
        for (GT_MetaTileEntity_Hatch_Turbine h : this.mTurbineRotorHatches) {
            h.setActive(aState);
            aUpdated++;
        }

        mLastHatchUpdate = System.currentTimeMillis() / 1000;
        return aUpdated;
    }

    @Override
    public int getEuDiscountForParallelism() {
        return 0;
    }

    @Override
    public void onPreTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPreTick(aBaseMetaTileEntity, aTick);
        // Fix GT bug
        if (this.getBaseMetaTileEntity().getFrontFacing() != 1) {
            log("Fixing Bad Facing. (GT Bug)");
            this.getBaseMetaTileEntity().setFrontFacing((byte) 1);
        }
    }

    /**
     * Called every tick the Machine runs
     */
    public boolean onRunningTick(ItemStack aStack) {
        if (lEUt > 0) {
            addEnergyOutput((lEUt * mEfficiency) / 10000);
            return true;
        }
        return false;
    }

    @Override
    public boolean addEnergyOutput(long aEU) {
        if (aEU <= 0) {
            return true;
        }
        if (this.mAllDynamoHatches.size() > 0) {
            return addEnergyOutputMultipleDynamos(aEU, true);
        }
        return false;
    }

    public boolean addEnergyOutputMultipleDynamos(long aEU, boolean aAllowMixedVoltageDynamos) {
        int injected = 0;
        long totalOutput = 0;
        long aFirstVoltageFound = -1;
        boolean aFoundMixedDynamos = false;
        for (GT_MetaTileEntity_Hatch aDynamo : mAllDynamoHatches) {
            if (aDynamo == null) {
                return false;
            }
            if (isValidMetaTileEntity(aDynamo)) {
                long aVoltage = aDynamo.maxEUOutput();
                long aTotal = aDynamo.maxAmperesOut() * aVoltage;
                // Check against voltage to check when hatch mixing
                if (aFirstVoltageFound == -1) {
                    aFirstVoltageFound = aVoltage;
                } else {
                    if (aFirstVoltageFound != aVoltage) {
                        aFoundMixedDynamos = true;
                    }
                }
                totalOutput += aTotal;
            }
        }

        long leftToInject;
        long aVoltage;
        int aAmpsToInject;
        int aRemainder;
        int ampsOnCurrentHatch;
        for (GT_MetaTileEntity_Hatch aDynamo : mAllDynamoHatches) {
            if (isValidMetaTileEntity(aDynamo)) {
                leftToInject = aEU - injected;
                aVoltage = aDynamo.maxEUOutput();
                aAmpsToInject = (int) (leftToInject / aVoltage);
                aRemainder = (int) (leftToInject - (aAmpsToInject * aVoltage));
                ampsOnCurrentHatch = (int) Math.min(aDynamo.maxAmperesOut(), aAmpsToInject);
                for (int i = 0; i < ampsOnCurrentHatch; i++) {
                    aDynamo.getBaseMetaTileEntity().increaseStoredEnergyUnits(aVoltage, false);
                }
                injected += aVoltage * ampsOnCurrentHatch;
                if (aRemainder > 0 && ampsOnCurrentHatch < aDynamo.maxAmperesOut()) {
                    aDynamo.getBaseMetaTileEntity().increaseStoredEnergyUnits(aRemainder, false);
                    injected += aRemainder;
                }
            }
        }
        return injected > 0;
    }

    public int getSpeedMultiplier() {
        return mFastMode ? 48 : 16;
    }

    public int getMaintenanceThreshold() {
        return mFastMode ? 12 : 1;
    }

    public int getPollutionMultiplier() {
        return mFastMode ? 3 : 1;
    }

    public int getTurbineDamageMultiplier() {
        return mFastMode ? 3 : 1;
    }
}
