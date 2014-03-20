package powercraft.traffic.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import powercraft.api.PC_Vec3I;
import powercraft.api.entity.PC_Entities;
import powercraft.api.entity.PC_Entity;
import powercraft.api.gres.PC_GresBaseWithInventory;
import powercraft.api.gres.PC_IGresGui;
import powercraft.api.gres.PC_IGresGuiOpenHandler;
import powercraft.api.inventory.PC_IInventory;
import powercraft.api.inventory.PC_InventoryUtils;
import powercraft.api.renderer.PC_EntityRenderer;
import powercraft.api.renderer.model.PC_Model;
import powercraft.traffic.entity.container.PCtf_ContainerMiner;
import powercraft.traffic.gui.PCtf_GuiMiner;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PCtf_EntityMiner extends PC_Entity implements PC_IGresGuiOpenHandler, PC_IInventory{
	
	private int instruction;
	
	protected ItemStack[] inventoryContents;
	
	public PCtf_EntityMiner(World world) {
		super(world);
		setSize(2.0f, 2.0f);
		this.yOffset = 0F;
		this.entityCollisionReduction = 1.0F;
		this.stepHeight = 0.6F;
		this.isImmuneToFire = true;
	}
	
	public PCtf_EntityMiner(World world, PC_Vec3I pos) {
		this(world);
		setPosition(pos.x, pos.y, pos.z);
	}

	@Override
	protected void entityInit() {
		this.dataWatcher.addObject(17, new Integer(0));
		this.dataWatcher.addObject(18, new Integer(1));
		this.dataWatcher.addObject(19, new Float(0));
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		moveEntity(0, this.motionY, 1);
	}

	@Override
	public boolean canBeCollidedWith(){
        return !this.isDead;
    }
	
	@Override
	public AxisAlignedBB getCollisionBox(Entity entity){
		if (entity instanceof EntityItem || entity instanceof EntityXPOrb) { 
			return null; 
		}
		return entity.boundingBox;
	}
	
	@Override
	public AxisAlignedBB getBoundingBox(){
        return this.boundingBox;
    }
	
	@Override
	public boolean attackEntityFrom(DamageSource damagesource, float i) {
		// all but void and explosion is ignored.
		if (damagesource != DamageSource.outOfWorld
				&& (this.worldObj.isRemote || this.isDead || (damagesource.getSourceOfDamage() == null && damagesource.isExplosion()))) {
			return true;
		}
		setForwardDirection(-getForwardDirection());
		setTimeSinceHit(10);
		setDamageTaken(getDamageTaken() + i * 7);
		setBeenAttacked();
		if (getDamageTaken() > 40) {
			if (this.riddenByEntity != null) {
				this.riddenByEntity.mountEntity(this); // unmount
			}

			kill();
		}
		return true;
	}

	@Override
	public void performHurtAnimation() {
		setForwardDirection(-getForwardDirection());
		setTimeSinceHit(10);
		setDamageTaken(getDamageTaken() * 11);
	}
	
	@Override
	public float getShadowSize() {
		return 1.0F;
	}
	
	public void setDamageTaken(float i) {
		this.dataWatcher.updateObject(19, Float.valueOf(i));
	}

	public float getDamageTaken() {
		return this.dataWatcher.getWatchableObjectFloat(19);
	}

	public void setTimeSinceHit(int i) {
		this.dataWatcher.updateObject(17, Integer.valueOf(i));
	}

	public int getTimeSinceHit() {
		return this.dataWatcher.getWatchableObjectInt(17);
	}

	public void setForwardDirection(int i) {
		this.dataWatcher.updateObject(18, Integer.valueOf(i));
	}

	public int getForwardDirection() {
		return this.dataWatcher.getWatchableObjectInt(18);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void doRender(PC_EntityRenderer<?> renderer, double x, double y, double z, float rotYaw, float timeStamp) {
		PC_Model model = PC_Entities.getEntityType(this).getModel();
		model.render(this, 0, 0, 0, 0, 0, 0);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public PC_IGresGui openClientGui(EntityPlayer player, NBTTagCompound serverData) {
		return new PCtf_GuiMiner(player, this);
	}

	@Override
	public PC_GresBaseWithInventory openServerGui(EntityPlayer player) {
		return new PCtf_ContainerMiner(player, this);
	}

	@Override
	public NBTTagCompound sendOnGuiOpenToClient(EntityPlayer player) {
		return null;
	}
	
	@Override
	public int getSizeInventory() {
		return this.inventoryContents.length;
	}
	
	@Override
	public ItemStack getStackInSlot(int i) {
		return this.inventoryContents[i];
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		if (this.inventoryContents[i] != null) {
			ItemStack itemstack;
			if (this.inventoryContents[i].stackSize <= j) {
				itemstack = this.inventoryContents[i];
				this.inventoryContents[i] = null;
				markDirty();
				return itemstack;
			} 
			itemstack = this.inventoryContents[i].splitStack(j);
			if (this.inventoryContents[i].stackSize == 0) {
				this.inventoryContents[i] = null;
			}
			markDirty();
			return itemstack;
		}
		return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		if (this.inventoryContents[i] != null) {
			ItemStack itemstack = this.inventoryContents[i];
			this.inventoryContents[i] = null;
			return itemstack;
		} 
		return null;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		this.inventoryContents[i] = itemstack;
		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
			itemstack.stackSize = getInventoryStackLimit();
		}
		markDirty();
	}
	
	@Override
	public String getInventoryName() {
		return "Miner";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return true;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer entityplayer) {
		return true;
	}

	@Override
	public void openInventory() {
		//
	}

	@Override
	public void closeInventory() {
		//
	}

	@Override
	public boolean isItemValidForSlot(int i, ItemStack itemstack) {
		return true;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return PC_InventoryUtils.makeIndexList(0, this.inventoryContents.length);
	}

	@Override
	public boolean canInsertItem(int i, ItemStack itemstack, int side) {
		return false;
	}

	@Override
	public boolean canExtractItem(int i, ItemStack itemstack, int side) {
		return false;
	}

	@Override
	public int getSlotStackLimit(int i) {
		return getInventoryStackLimit();
	}

	@Override
	public boolean canTakeStack(int i, EntityPlayer entityPlayer) {
		return true;
	}
	
	@Override
	public boolean canDropStack(int i) {
		return true;
	}
	
	@Override
	public void onTick(World world) {
		PC_InventoryUtils.onTick(this, this.worldObj);
	}

	@Override
	public void markDirty() {
		//
	}

	@Override
	public int[] getAppliedGroups(int i) {
		return null;
	}

	@Override
	public int[] getAppliedSides(int i) {
		return null;
	}
	
}
