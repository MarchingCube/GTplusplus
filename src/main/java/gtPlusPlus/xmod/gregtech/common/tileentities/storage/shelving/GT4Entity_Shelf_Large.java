package gtPlusPlus.xmod.gregtech.common.tileentities.storage.shelving;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;

import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.DrawableWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import gregtech.api.gui.modularui.GT_UIInfos;
import gregtech.api.gui.modularui.GT_UITextures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.util.GT_Utility;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.core.util.minecraft.PlayerUtils;
import gtPlusPlus.core.util.sys.KeyboardUtils;

public class GT4Entity_Shelf_Large extends GT4Entity_Shelf {

    private final int mSize;
    public int mItemCount;
    public ItemStack mItemStack;

    public GT4Entity_Shelf_Large(final int aID, final String aName, final String aNameRegional,
            final String aDescription, final int aSize) {
        super(aID, aName, aNameRegional, aDescription);
        this.mSize = aSize;
        this.mItemCount = 0;
        this.mItemStack = null;
    }

    public GT4Entity_Shelf_Large(String mName, String mDescriptionArray, final int aSize, ITexture[][][] mTextures) {
        super(mName, mDescriptionArray, mTextures);
        this.mSize = aSize;
        this.mItemCount = 0;
        this.mItemStack = null;
    }

    @Override
    public MetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new GT4Entity_Shelf_Large(this.mName, this.mDescription, mSize, this.mTextures);
    }

    /*
     * @Override public int getInvSize() { return (mSize > 0 && mSize < 255 ? mSize : 255); }
     */

    @Override
    public boolean isGivingInformation() {
        return true;
    }

    @Override
    public boolean isDigitalChest() {
        return true;
    }

    @Override
    public boolean isValidSlot(final int aIndex) {
        return true;
    }

    @Override
    public boolean onRightclick(final IGregTechTileEntity aBaseMetaTileEntity, final EntityPlayer aPlayer) {
        if (aBaseMetaTileEntity.isClientSide()) {
            return true;
        }
        if (KeyboardUtils.isCtrlKeyDown()) {
            String itemName = (this.mItemStack != null ? this.mItemStack.getDisplayName() : "Nothing.");
            String itemAmount = (this.mItemCount > 0 ? "" + this.mItemCount : "bad");
            String itemMessage = "This container currently holds "
                    + (itemAmount.equalsIgnoreCase("bad") ? "nothing." : itemName + " x" + itemAmount + ".");
            PlayerUtils.messagePlayer(aPlayer, itemMessage);
        } else {
            GT_UIInfos.openGTTileEntityUI(aBaseMetaTileEntity, aPlayer);
        }
        return true;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("mItemCount", this.mItemCount);
        if (this.mItemStack != null) {
            aNBT.setTag("mItemStack", (NBTBase) this.mItemStack.writeToNBT(new NBTTagCompound()));
        }
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        if (aNBT.hasKey("mItemCount")) {
            this.mItemCount = aNBT.getInteger("mItemCount");
        }
        if (aNBT.hasKey("mItemStack")) {
            this.mItemStack = ItemStack.loadItemStackFromNBT((NBTTagCompound) aNBT.getTag("mItemStack"));
        }
        super.loadNBTData(aNBT);
    }

    @Override
    public void onOpenGUI() {
        super.onOpenGUI();
    }

    public void onPostTick(final IGregTechTileEntity aBaseMetaTileEntity, final long aTimer) {
        if (this.getBaseMetaTileEntity().isServerSide() && this.getBaseMetaTileEntity().isAllowedToWork()) {
            try {
                if (this.mInventory[0] != null) {
                    this.mType = (byte) this.mIndex;
                } else {
                    this.mType = 0;
                }

                if (this.getItemCount() <= 0) {
                    this.mItemStack = null;
                    this.mItemCount = 0;
                }
                if (this.mItemStack == null && this.mInventory[0] != null) {
                    this.mItemStack = this.mInventory[0].copy();
                }
                if (this.mInventory[0] != null && this.mItemCount < this.getMaxItemCount()
                        && GT_Utility.areStacksEqual(this.mInventory[0], this.mItemStack)) {
                    this.mItemCount += this.mInventory[0].stackSize;
                    if (this.mItemCount > this.getMaxItemCount()) {
                        this.mInventory[0].stackSize = this.mItemCount - this.getMaxItemCount();
                        this.mItemCount = this.getMaxItemCount();
                    } else {
                        this.mInventory[0] = null;
                    }
                }
                if (this.mInventory[1] == null && this.mItemStack != null) {
                    this.mInventory[1] = this.mItemStack.copy();
                    this.mInventory[1].stackSize = Math.min(this.mItemStack.getMaxStackSize(), this.mItemCount);
                    this.mItemCount -= this.mInventory[1].stackSize;
                } else if (this.mItemCount > 0 && GT_Utility.areStacksEqual(this.mInventory[1], this.mItemStack)
                        && this.mInventory[1].getMaxStackSize() > this.mInventory[1].stackSize) {
                            final int tmp = Math.min(
                                    this.mItemCount,
                                    this.mInventory[1].getMaxStackSize() - this.mInventory[1].stackSize);
                            final ItemStack itemStack = this.mInventory[1];
                            itemStack.stackSize += tmp;
                            this.mItemCount -= tmp;
                        }
                if (this.mItemStack != null) {
                    this.mInventory[2] = this.mItemStack.copy();
                    this.mInventory[2].stackSize = Math.min(this.mItemStack.getMaxStackSize(), this.mItemCount);
                } else {
                    this.mInventory[2] = null;
                }
            } catch (Throwable t) {
                if (t instanceof ArrayIndexOutOfBoundsException) {}
            }
        }
    }

    private int getItemCount() {
        return this.mItemCount;
    }

    public void setItemCount(final int aCount) {
        this.mItemCount = aCount;
    }

    public int getProgresstime() {
        try {
            return this.mItemCount + ((this.mInventory[0] == null) ? 0 : this.mInventory[0].stackSize)
                    + ((this.mInventory[1] == null) ? 0 : this.mInventory[1].stackSize);
        } catch (Throwable t) {
            return 0;
        }
    }

    public int maxProgresstime() {
        return this.getMaxItemCount();
    }

    public int getMaxItemCount() {
        return this.mSize;
    }

    public boolean allowPullStack(final IGregTechTileEntity aBaseMetaTileEntity, final int aIndex, final byte aSide,
            final ItemStack aStack) {
        return aIndex == 1;
    }

    public boolean allowPutStack(final IGregTechTileEntity aBaseMetaTileEntity, final int aIndex, final byte aSide,
            final ItemStack aStack) {
        return aIndex == 0 && ((this.mInventory[0] == null && this.mItemStack == null)
                || GT_Utility.areStacksEqual(this.mInventory[0], aStack)
                || (this.mItemStack != null && GT_Utility.areStacksEqual(this.mItemStack, aStack)));
    }

    public String[] getInfoData() {
        if (this.mItemStack == null) {
            return new String[] { this.getLocalName(), "No Items Stored",
                    "Free Space: " + Integer.toString(this.getMaxItemCount()) };
        }
        return new String[] { this.getLocalName(),
                "Storing: " + this.mItemStack.getDisplayName() + " x" + Integer.toString(this.mItemCount),
                "Space Remaining: " + Integer.toString(this.getMaxItemCount() - this.getItemCount())
                        + "/"
                        + Integer.toString(this.getMaxItemCount()) };
    }

    @Override
    public String[] getDescription() {
        String[] mSuper = super.getDescription();
        String[] desc = new String[mSuper.length + 1];
        for (int i = 0; i > mSuper.length; i++) {
            desc[i] = mSuper[i];
        }
        desc[desc.length - 1] = "Control + Rmb block to check contents";
        return desc;
    }

    @Override
    public boolean isItemValidForSlot(int aIndex, ItemStack aStack) {
        Logger.INFO("1:" + aIndex);
        if (aIndex == 2) {
            if (ItemStack.areItemStacksEqual(aStack, mItemStack)) {
                return true;
            } else {
                if (mItemStack == null) {
                    return true;
                }
                return false;
            }
        }
        return super.isItemValidForSlot(aIndex, aStack);
    }

    @Override
    public boolean canInsertItem(int aIndex, ItemStack aStack, int aSide) {
        Logger.INFO("2:" + aIndex);
        if (aIndex == 0) {
            if (ItemStack.areItemStacksEqual(aStack, mItemStack)) {
                return true;
            } else {
                if (mItemStack == null) {
                    return true;
                }
                return false;
            }
        }
        return super.canInsertItem(aIndex, aStack, aSide);
    }

    @Override
    public boolean useModularUI() {
        return true;
    }

    @Override
    public void addUIWidgets(ModularWindow.Builder builder, UIBuildContext buildContext) {
        builder.widget(
                new DrawableWidget().setDrawable(GT_UITextures.PICTURE_SCREEN_BLACK).setPos(7, 16).setSize(71, 45))
                .widget(
                        new SlotWidget(inventoryHandler, 0)
                                .setBackground(getGUITextureSet().getItemSlot(), GT_UITextures.OVERLAY_SLOT_IN)
                                .setPos(79, 16))
                .widget(new TextWidget("Item Amount").setDefaultColor(COLOR_TEXT_WHITE.get()).setPos(10, 20)).widget(
                        TextWidget.dynamicString(() -> GT_Utility.parseNumberToString(mItemCount))
                                .setDefaultColor(COLOR_TEXT_WHITE.get()).setPos(10, 30));
    }
}
