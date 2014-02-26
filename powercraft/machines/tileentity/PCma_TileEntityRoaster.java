package powercraft.machines.tileentity;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import powercraft.api.PC_Direction;
import powercraft.api.PC_Field;
import powercraft.api.PC_Field.Flag;
import powercraft.api.PC_Utils;
import powercraft.api.block.PC_TileEntity;
import powercraft.api.energy.PC_EnergyGrid;
import powercraft.api.energy.PC_IEnergyGridConsumer;
import powercraft.api.energy.PC_IEnergyGridTile;
import powercraft.api.grid.PC_GridHelper;
import powercraft.api.grid.PC_IGridHolder;
import powercraft.api.grid.PC_IGridSided;
import powercraft.api.grid.PC_IGridTile;
import powercraft.api.redstone.PC_RedstoneWorkType;

public class PCma_TileEntityRoaster extends PC_TileEntity implements PC_IGridSided, PC_IEnergyGridConsumer, PC_IGridHolder {

	private static Random random = new Random();
	
	private PC_EnergyGrid grid;
	@PC_Field(flags={Flag.SYNC})
	private boolean working;
	
	public PCma_TileEntityRoaster(){
		workWhen = PC_RedstoneWorkType.EVER;
	}
	
	@Override
	public void setGrid(PC_EnergyGrid grid) {
		this.grid = grid;
	}

	@Override
	public PC_EnergyGrid getGrid() {
		return grid;
	}

	@Override
	public void getGridIfNull() {
		PC_GridHelper.getGridIfNull(worldObj, xCoord, yCoord, zCoord, 0x3D, this, PC_EnergyGrid.factory, PC_IEnergyGridTile.class);
	}

	@Override
	public void removeFormGrid() {
		PC_GridHelper.removeFormGrid(worldObj, (PC_IEnergyGridTile)this);
	}

	@Override
	public float getEnergyRequested() {
		if(!isWorking())
			return 0;
		float f = getEntities().size()*10+getItems().size()*10+10;
		return f>100?100:f;
	}

	@Override
	public void useEnergy(float energy) {
		if(getEnergyRequested()!=0 && energy>0){
			energy -= 10;
			if(!working){
				working = true;
				sendWorking();
			}
			for (EntityLivingBase entity : getEntities()) {
				if (energy >= 10) {
					if (!entity.isImmuneToFire()) {
						entity.attackEntityFrom(DamageSource.inFire, 3);
					}
					if (!entity.isWet()) {
						entity.setFire(15);
					}
					energy -= 10;
				}else{
					break;
				}
			}
			for (EntityItem item : getItems()) {
				if (energy >= 10) {
					if (item.getEntityItem().stackSize > 0) {
						EntityItem eitem = new EntityItem(worldObj, item.posX - 0.1F + random.nextFloat() * 0.2F, item.posY, item.posZ - 0.1F
								+ random.nextFloat() * 0.2F, PC_Utils.getSmeltingResult(item.getEntityItem()).splitStack(1));
						eitem.motionX = item.motionX;
						eitem.motionY = item.motionY;
						eitem.motionZ = item.motionZ;
						eitem.delayBeforeCanPickup = 7;
						PC_Utils.spawnEntity(worldObj, eitem);
						if (--item.getEntityItem().stackSize <= 0) {
							item.setDead();
						}
						energy -= 10;
					}
				}else{
					break;
				}
			}
			
		}else{
			if(working){
				working = false;
				sendWorking();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<EntityLivingBase> getEntities() {

		List<EntityLivingBase> entities = worldObj.getEntitiesWithinAABB(EntityLivingBase.class,
				AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 2, zCoord + 1));
		Iterator<EntityLivingBase> iterator = entities.iterator();
		while (iterator.hasNext()) {
			EntityLivingBase entity = iterator.next();
			if (entity.isDead) {
				iterator.remove();
			}
		}
		return entities;
	}


	@SuppressWarnings("unchecked")
	private List<EntityItem> getItems() {

		List<EntityItem> items = worldObj.getEntitiesWithinAABB(EntityItem.class,
				AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1, yCoord + 2, zCoord + 1));
		Iterator<EntityItem> iterator = items.iterator();
		while (iterator.hasNext()) {
			EntityItem item = iterator.next();
			ItemStack itemStack = item.getEntityItem();
			if (item.isDead) {
				iterator.remove();
				continue;
			}
			ItemStack result = PC_Utils.getSmeltingResult(itemStack);
			if (result == null) {
				iterator.remove();
			}
		}
		return items;
	}

	public void sendWorking(){
		NBTTagCompound nbtTagCompound = new NBTTagCompound();
		nbtTagCompound.setInteger("type", 0);
		nbtTagCompound.setBoolean("working", working);
		sendMessage(nbtTagCompound);
	}
	
	@Override
	public void onClientMessage(EntityPlayer player, NBTTagCompound nbtTagCompound) {
		if(nbtTagCompound.getInteger("type")==0){
			working = nbtTagCompound.getBoolean("working");
		}
	}
	
	@Override
	public float getMaxPercentToWork() {
		return 0.1f;
	}

	@Override
	public <T extends PC_IGridTile<?, T, ?, ?>> T getTile(PC_Direction side, Class<T> tileClass) {
		if(side==PC_Direction.UP)
			return null;
		if(tileClass==PC_IEnergyGridTile.class)
			return tileClass.cast(this);
		return null;
	}
	
	@Override
	public void randomDisplayTick() {
		if(working){
			if (random.nextInt(24) == 0) {
				worldObj.playSoundEffect(xCoord + 0.5F, yCoord + 0.5F, zCoord + 0.5F, "fire.fire", 1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F);
			}
	
			for (int c = 0; c < 5; c++) {
				float y = yCoord + 0.74F + (random.nextFloat() * 0.3F);
				float x = xCoord + 0.2F + (random.nextFloat() * 0.6F);
				float z = zCoord + 0.2F + (random.nextFloat() * 0.6F);
				worldObj.spawnParticle("smoke", x, y, z, 0.0D, 0.0D, 0.0D);
				worldObj.spawnParticle("flame", x, y, z, 0.0D, 0.0D, 0.0D);
			}
	
			for (int c = 0; c < 5; c++) {
				float y = yCoord + 1.3F;
				float x = xCoord + 0.2F + (random.nextFloat() * 0.6F);
				float z = zCoord + 0.2F + (random.nextFloat() * 0.6F);
				worldObj.spawnParticle("smoke", x, y, z, 0.0D, 0.0D, 0.0D);
			}
		}
	}
	
	@Override
	public PC_RedstoneWorkType[] getAllowedRedstoneWorkTypes() {
		return new PC_RedstoneWorkType[]{null, PC_RedstoneWorkType.EVER, PC_RedstoneWorkType.ON_ON, PC_RedstoneWorkType.ON_OFF};
	}
	
}