package powercraft.transport;

import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRailBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import powercraft.api.PC_Direction;
import powercraft.api.PC_MathHelper;
import powercraft.api.PC_Sound;
import powercraft.api.PC_Utils;
import powercraft.api.PC_Vec3I;
import powercraft.api.inventory.PC_InventoryUtils;
import powercraft.api.reflect.PC_Fields;
import powercraft.transport.block.PCtr_BlockBeltNormal;
import powercraft.transport.block.PCtr_BlockBeltScriptable;

public final class PCtr_BeltHelper {
	
	public static final float HEIGHT = 0.0625F;

	public static final float HEIGHT_SELECTED = HEIGHT;

	public static final float HEIGHT_COLLISION = HEIGHT;

	public static final double MAX_HORIZONTAL_SPEED = 0.5F;

	public static final double HORIZONTAL_BOOST = 0.14D;

	public static final double BORDERS = 0.35D;

	public static final double BORDER_BOOST = 0.063D;

	public static final float STORAGE_BORDER = 0.5F;

	public static final float STORAGE_BORDER_LONG = 0.8F;

	public static final float STORAGE_BORDER_V = 0.6F;
	
	private PCtr_BeltHelper(){
		PC_Utils.staticClassConstructor();
	}
	
	public static boolean isStickyItem(ItemStack itemStack){
		if(itemStack==null)
			return false;
		if(itemStack.getItem() == PCtr_Transport.SLIME_BOOTS)
			return true;
		NBTTagCompound nbtTagCompound = PC_Utils.getNBTTagOf(itemStack);
		if(nbtTagCompound==null)
			return false;
		return nbtTagCompound.getBoolean("sticky");
	}
	
	public static boolean isEntityIgnored(Entity entity) {
		if (entity == null || !entity.isEntityAlive()
				|| PC_Utils.isEntityFX(entity) || entity.ridingEntity != null) {
			return true;
		}

		if (entity instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) entity;
			if (player.isSneaking()) {
				return true;
			}
			ItemStack boots = player.inventory.armorItemInSlot(0);
			return isStickyItem(boots);
		}

		return false;
	}

	public static void soundEffectChest(World world, PC_Vec3I pos) {
		PC_Sound.playSound(pos.x + 0.5D, pos.y + 0.5D, pos.z + 0.5D,
				"random.pop", (world.rand.nextFloat() + 0.7F) / 5.0F,
				0.5F + world.rand.nextFloat() * 0.3F);
	}

	public static boolean isBlocked(World world, PC_Vec3I blockPos) {
		boolean isWall = !world.isAirBlock(blockPos.x, blockPos.y, blockPos.z)
				&& !isConveyorAt(world, blockPos);

		if (isWall) {
			Block block = PC_Utils.getBlock(world, blockPos);

			if (block != null) {
				if (!block.getMaterial().blocksMovement()) {
					isWall = false;
				}
			}
		}

		return isWall;
	}

	public static boolean isConveyorAt(World world, PC_Vec3I pos) {
		Block block = PC_Utils.getBlock(world, pos);
		return block instanceof PCtr_BlockBeltScriptable || block instanceof PCtr_BlockBeltNormal;
	}
	
	public static boolean isConveyorAt(World world, int x, int y, int z) {
		Block block = PC_Utils.getBlock(world, x, y, z);
		if(block instanceof PCtr_BlockBeltNormal){
			return (PC_Utils.getMetadata(world, x, y, z)&3)!=3;
		}
		return block instanceof PCtr_BlockBeltScriptable;
	}
	
	/**
	 * @param entity
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @param dir
	 * @return
	 * <br>
	 * <br>
	 * 0 -> entity can't be stored (wrong type)<br>
	 * 1 -> nothing found to store to (no inventory found in range)<br>
	 * 2 -> entity entirely stored<br>
	 * 3 -> entity partially stored<br>
	 * 4 -> no slot found to store to (inv is full)
	 */
	public static int tryToStoreEntity(Entity entity, World world, int x, int y, int z, PC_Direction dir){
		if(entity instanceof EntityItem){
			AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(-0.6, -0.6, -0.6, 0.6, 0.6, 0.6).offset(x+dir.offsetX+0.5, y+dir.offsetY+0.5, z+dir.offsetZ+0.5);
			if(aabb.intersectsWith(entity.boundingBox)){
				ItemStack is = ((EntityItem)entity).getEntityItem();
				IInventory inventory = PC_InventoryUtils.getInventoryAt(world, x+dir.offsetX, y+dir.offsetY, z+dir.offsetZ, false);
				if(inventory!=null){
					int storeResult = PC_InventoryUtils.storeItemStackToInventoryFrom(inventory, is, dir.getOpposite()); 
					if(storeResult==0){
						entity.setDead();
						return 2;
					}else if(storeResult==1){
						((EntityItem)entity).setEntityItemStack(is);
						return 3;
					}
					return 4;
				}
			}
			return 1;
		}
		return 0;
	}
	
	@SuppressWarnings("unused")
	public static void moveEntity(Entity entity, World world, int x, int y, int z, boolean elevator, PC_Direction dir, double speed, boolean upwards){
		final double FAC = 0.5;
		int passing = canPassTo(world, new PC_Vec3I(x, y+(upwards?1:0), z), dir);
		boolean canPass = passing==0 || (passing==1 && (entity instanceof EntityMinecart));
		entity.motionX = dir.offsetX!=0?dir.offsetX*speed:(x+0.5-entity.posX)*FAC;
		if(elevator){
			entity.motionY = dir.offsetY!=0?dir.offsetY*speed:(y+0.5-entity.posY)*FAC;
			entity.onGround = false;
		}
		entity.motionZ = dir.offsetZ!=0?dir.offsetZ*speed:(z+0.5-entity.posZ)*FAC;
		entity.velocityChanged = true;
		if(canPass)
			return;
		double diff, tmp;
		if(dir.isVertical() && (diff=PC_MathHelper.abs_double((y+0.5+dir.offsetY*0.5)-entity.posY))<2*speed){
			entity.motionY = getSpeedForDiff(diff)*dir.offsetY;
		}else if(dir.isHorizontalX() && (diff=PC_MathHelper.abs_double((x+0.5+dir.offsetX*0.5)-entity.posX))<2*speed){
			entity.motionX = getSpeedForDiff(diff)*dir.offsetX;
		}else if(dir.isHorizontalZ() && (diff=PC_MathHelper.abs_double((z+0.5+dir.offsetZ*0.5)-entity.posZ))<2*speed){
			entity.motionZ = getSpeedForDiff(diff)*dir.offsetZ;
		}
	}
	
	private static double getSpeedForDiff(double diff){
		if(diff<0.1) return 0;
		if(diff<0.2) return 0.05;
		if(diff<0.3) return 0.1;
		if(diff<0.4) return 0.2;
		if(diff<0.6) return 0.3;
		return 0.5;
	}
	
	public static void preventDespawn(Entity entity, boolean preventPickup){
		if(entity instanceof EntityItem){
			EntityItem item = (EntityItem)entity;
			if(preventPickup){
				item.delayBeforeCanPickup = 7;
			}
			if (item.age >= 5000) {
				if (item.worldObj.getEntitiesWithinAABBExcludingEntity(
						null,
						AxisAlignedBB.getBoundingBox(item.posX-0.5, item.posY-0.5, item.posZ-0.5,
								item.posX+0.5, item.posY+0.5, item.posZ+0.5)).size() < 40) {
					item.age = 4000;
				}
			}
		}else if (entity instanceof EntityXPOrb) {
			EntityXPOrb xp = (EntityXPOrb)entity;
			if(preventPickup){
				xp.field_70532_c = 7;
				PC_Fields.EntityXPOrb_xpTargetColor.setValue(xp, Integer.valueOf((xp.xpColor - 20 + xp.getEntityId() % 100) + 7));
				PC_Fields.EntityXPOrb_closestPlayer.setValue(xp, null);
			}
			if (xp.xpOrbAge >= 5000) {
				if (xp.worldObj.getEntitiesWithinAABBExcludingEntity(
						null,
						AxisAlignedBB.getBoundingBox(xp.posX-0.5, xp.posY-0.5, xp.posZ-0.5,
								xp.posX+0.5, xp.posY+0.5, xp.posZ+0.5)).size() < 40) {
					xp.xpOrbAge = 4000;
				}
			}
		}
	}
	
	public static boolean handleEntity(Entity entity, World world, int x, int y, int z, boolean elevator, boolean preventPickup, boolean upwards){
		if(isEntityIgnored(entity))
			return false;
		if(entity.stepHeight<1.0f/16.0f){
			entity.stepHeight=1.0f/16.0f;
		}
		NBTTagCompound compound = PC_Utils.getNBTTagOf(entity);
		if(compound!=null && compound.hasKey("dir")){
			PC_Direction dir = PC_Direction.fromSide(compound.getInteger("dir"));
			if(!world.isRemote){
				int result = tryToStoreEntity(entity, world, x, y, z, dir);
				if(result>=2)
					return result==2;
			}
			moveEntity(entity, world, x, y, z, elevator, dir, 0.2, upwards);
		}
		preventDespawn(entity, preventPickup);
		return false;
	}
	
	/**
	 * @param world
	 * @param pos
	 * @param dir
	 * @return value that indicates what blocks the movement<br>
	 * <br>
	 * 0 -> no blocking<br>
	 * 1 -> rails<br>
	 * 2 -> anything else<br>
	 */
	public static int canPassTo(World world, PC_Vec3I pos, PC_Direction dir){
		PC_Vec3I target = pos.offset(dir);
		if(BlockRailBase.func_150051_a(PC_Utils.getBlock(world, target.x, target.y, target.z)))
			return 1;
		if(isBlocked(world, target))
			return 2;
		return 0;
	}

	public static boolean combineEntityItems(Entity entity) {
		if(entity instanceof EntityItem){
			entity.prevPosX = entity.posX;
			entity.prevPosY = entity.posY;
			entity.prevPosZ = entity.posZ;
			if(!entity.worldObj.isRemote){
				Iterator<?> iterator = entity.worldObj.getEntitiesWithinAABB(EntityItem.class, entity.boundingBox.expand(0.5D, 0.0D, 0.5D)).iterator();
		        while (iterator.hasNext()){
		            EntityItem entityitem = (EntityItem)iterator.next();
		            canCombineAndDo(((EntityItem)entity), entityitem);
		        }
			}
			if(entity.ticksExisted % 25 == 0){
				entity.ticksExisted++;
				return true;
			}
		}
		return false;
	}
	
	public static void canCombineAndDo(EntityItem entityitem1, EntityItem entityitem2){
		if(entityitem1==entityitem2)
			return;
		NBTTagCompound tagCompound1 = PC_Utils.getNBTTagOf(entityitem1);
		NBTTagCompound tagCompound2 = PC_Utils.getNBTTagOf(entityitem2);
		if(tagCompound1!=null && tagCompound2!=null && tagCompound1.hasKey("dir") && tagCompound2.hasKey("dir") && 
				tagCompound1.getInteger("dir")==tagCompound2.getInteger("dir")){
			entityitem1.combineItems(entityitem2);
		}
	}
	
	public static boolean hasValidGround(World world, int x, int y, int z){
		return isValidGround(world, x, y-1, z);
	}

	public static boolean isValidGround(World world, int x, int y, int z){
		return PC_Utils.isBlockSideSolid(world, x, y, z, PC_Direction.UP);
	}
	
}
