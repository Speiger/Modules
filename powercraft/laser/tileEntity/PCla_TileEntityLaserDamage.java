package powercraft.laser.tileEntity;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import powercraft.api.PC_3DRotationY;
import powercraft.api.PC_Direction;
import powercraft.api.PC_Utils;
import powercraft.api.PC_Vec3;
import powercraft.api.beam.PC_LightValue;
import powercraft.api.block.PC_TileEntityRotateable;
import powercraft.api.reflect.PC_Fields;
import powercraft.laser.PCla_Beam;
import powercraft.laser.PCla_IBeamHandler;
import powercraft.laser.PCla_LaserRenderer;
import powercraft.laser.block.PCla_BlockLaserDamage;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PCla_TileEntityLaserDamage extends PC_TileEntityRotateable implements PCla_IBeamHandler {
	
	private static DamageSource LASER_DAMAGE = new PC_DamageSourceLaser();
	
	private static class PC_DamageSourceLaser extends DamageSource{

		PC_DamageSourceLaser() {
			super("Laser");
		}

		@Override
		public IChatComponent func_151519_b(EntityLivingBase living) {
	        return new ChatComponentTranslation("laser.kill.entity", living.func_145748_c_());
		}

		@Override
		public boolean isDifficultyScaled() {
			return false;
		}
		
	}
	
	@SuppressWarnings("unused")
	@Override
	public void onTick() {
		super.onTick();
		PC_Direction dir = get3DRotation().getSidePosition(PC_Direction.NORTH);
		PC_Vec3 vec = new PC_Vec3(dir.offsetX, dir.offsetY, dir.offsetZ);
		new PCla_Beam(this.worldObj, this, 20, new PC_Vec3(this.xCoord+0.5, this.yCoord+0.5, this.zCoord+0.5).add(vec.mul(0.1)), vec, new PC_LightValue(450*PC_LightValue.THz, 1));
	}

	@Override
	public boolean onHitBlock(World world, int x, int y, int z, PCla_Beam beam) {
		Block block = PC_Utils.getBlock(world, x, y, z);
		return block==null||!block.isNormalCube();
	}

	@Override
	public boolean onHitEntity(World world, Entity entity, PCla_Beam beam) {
		if(entity instanceof EntityItem || entity instanceof EntityXPOrb)
			return true;
		if(!world.isRemote){
			PC_Fields.EntityLivingBase_recentlyHit.setValue(entity, Integer.valueOf(60));
			entity.attackEntityFrom(LASER_DAMAGE, (float) (10*beam.getLightValue().getIntensity()));
		}
		return true;
	}

	@Override
	public void onFinished(PCla_Beam beam) {
		//
	}

	@Override
	public void onAdded(EntityPlayer player) {
		set3DRotation(new PC_3DRotationY(player));
		super.onAdded(player);
	}
	
	@Override
	public void onBlockPostSet(PC_Direction side, ItemStack stack, EntityPlayer player, float hitX, float hitY, float hitZ) {
		if(this.rotation==null)
			set3DRotation(new PC_3DRotationY(player));
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean renderWorldBlock(int modelId, RenderBlocks renderer) {
		
		PC_Direction dir = get3DRotation().getSidePosition(PC_Direction.NORTH);
		
		PCla_LaserRenderer.renderLaser(this.worldObj, this.xCoord, this.yCoord, this.zCoord, dir, renderer, PCla_BlockLaserDamage.side, PCla_BlockLaserDamage.inside, PCla_BlockLaserDamage.black, PCla_BlockLaserDamage.white);
		
		return true;
	}
	
}
