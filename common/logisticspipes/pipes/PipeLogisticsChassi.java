/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.HashMap;
import java.util.UUID;

import logisticspipes.config.Textures;
import logisticspipes.gui.GuiChassiPipe;
import logisticspipes.interfaces.ILegacyActiveModule;
import logisticspipes.interfaces.ILogisticsModule;
import logisticspipes.interfaces.ISendRoutedItem;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.items.ItemModule;
import logisticspipes.logic.BaseChassiLogic;
import logisticspipes.logisticspipes.ChassiModule;
import logisticspipes.logisticspipes.ChassiTransportLayer;
import logisticspipes.logisticspipes.IInventoryProvider;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.logisticspipes.SidedInventoryAdapter;
import logisticspipes.logisticspipes.TransportLayer;
import logisticspipes.main.LogisticsPromise;
import logisticspipes.main.LogisticsTransaction;
import logisticspipes.main.RoutedPipe;
import logisticspipes.main.SimpleServiceLocator;
import logisticspipes.network.NetworkConstants;
import logisticspipes.network.packets.PacketCoordinates;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.SimpleInventory;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraftforge.common.ISidedInventory;
import buildcraft.api.core.Orientations;
import buildcraft.api.core.Position;
import buildcraft.core.utils.Utils;
import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.network.PacketDispatcher;

public abstract class PipeLogisticsChassi extends RoutedPipe implements ISimpleInventoryEventHandler, IInventoryProvider, ISendRoutedItem, IProvideItems, IWorldProvider{

	private final ChassiModule _module;
	private final SimpleInventory _moduleInventory;
	private boolean switchOrientationOnTick = true;
	private boolean init = false;
	private long tick = 0;
	BaseChassiLogic ChassiLogic;
	private boolean convertFromMeta = false;

	public PipeLogisticsChassi(int itemID) {
		super(new BaseChassiLogic(), itemID);
		ChassiLogic = (BaseChassiLogic) logic;
		_moduleInventory = new SimpleInventory(getChassiSize(), "Chassi pipe", 1);
		_moduleInventory.addListener(this);
		_module = new ChassiModule(getChassiSize(), this);
	}
	
	public Orientations getPointedOrientation(){
		return ChassiLogic.orientation;
	}
	
	public TileEntity getPointedTileEntity(){
		Position pos = new Position(xCoord, yCoord, zCoord, ChassiLogic.orientation);
		pos.moveForwards(1.0);
		return worldObj.getBlockTileEntity((int)pos.x, (int)pos.y, (int)pos.z);
	}
	
	public void nextOrientation() {
		for (int l = 0; l < 6; ++l) {
			ChassiLogic.orientation = Orientations.values()[(ChassiLogic.orientation.ordinal() + 1) % 6];
			if (!isValidOrientation(ChassiLogic.orientation)) continue;
			return;
		}
	}
	
	private boolean isValidOrientation(Orientations connection){
		if (getRouter().isRoutedExit(connection)) return false;
		Position pos = new Position(xCoord, yCoord, zCoord, connection);
		pos.moveForwards(1.0);
		TileEntity tile = worldObj.getBlockTileEntity((int)pos.x, (int)pos.y, (int)pos.z);

		if (tile == null) return false;
		return SimpleServiceLocator.buildCraftProxy.checkPipesConnections(this.container, tile);
	}
	
	public IInventory getModuleInventory(){
		return this._moduleInventory;
	}
	
	@Override
	public int getCenterTexture() {
		return Textures.LOGISTICSPIPE_TEXTURE;
	}
	
	@Override
	public int getRoutedTexture(Orientations connection) {
		return Textures.LOGISTICSPIPE_CHASSI_ROUTED_TEXTURE;
	}
	
	@Override
	public int getNonRoutedTexture(Orientations connection) {
		if (connection.equals(ChassiLogic.orientation)){
			return Textures.LOGISTICSPIPE_CHASSI_DIRECTION_TEXTURE;
		}
		return Textures.LOGISTICSPIPE_CHASSI_NOTROUTED_TEXTURE;
	}
	
	@Override
	public void onNeighborBlockChange_Logistics() {
		if (!isValidOrientation(ChassiLogic.orientation)){
			nextOrientation();
		}
	};
	
	@Override
	public void onBlockPlaced() {
		super.onBlockPlaced();
		switchOrientationOnTick = true;
	}
	
	
	/*** IInventoryProvider ***/
	
	@Override
	public IInventory getRawInventory() {
		TileEntity tile = getPointedTileEntity();
		if (tile instanceof TileGenericPipe) return null;
		if (!(tile instanceof IInventory)) return null;
		return Utils.getInventory((IInventory) tile);
	}
	
	@Override
	public IInventory getInventory() {
		IInventory rawInventory = getRawInventory();
		if (rawInventory instanceof ISidedInventory) return new SidedInventoryAdapter((ISidedInventory) rawInventory, this.getPointedOrientation().reverse());
		return rawInventory;
	}

	@Override
	public Orientations inventoryOrientation() {
		return getPointedOrientation();
	}
	
	/*** ISendRoutedItem ***/
	
	public java.util.UUID getSourceUUID() {
		return this.getRouter().getId();
	};
	
	@Override
	public void sendStack(ItemStack stack) {
		IRoutedItem itemToSend = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(stack, this.worldObj);
		//itemToSend.setSource(this.getRouter().getId());
		itemToSend.setTransportMode(TransportMode.Passive);
		super.queueRoutedItem(itemToSend, getPointedOrientation());
	}
	
	@Override
	public void sendStack(ItemStack stack, UUID destination) {
		IRoutedItem itemToSend = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(stack, this.worldObj);
		itemToSend.setSource(this.getRouter().getId());
		itemToSend.setDestination(destination);
		itemToSend.setTransportMode(TransportMode.Active);
		super.queueRoutedItem(itemToSend, getPointedOrientation());
	}
	
	
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		try {
			super.readFromNBT(nbttagcompound);
			_moduleInventory.readFromNBT(nbttagcompound, "chassi");
			InventoryChanged(_moduleInventory);
			_module.readFromNBT(nbttagcompound, "");
			ChassiLogic.orientation = Orientations.values()[nbttagcompound.getInteger("Orientation") % 6];
			if(nbttagcompound.getInteger("Orientation") == 0) {
				convertFromMeta = true;
			}
			switchOrientationOnTick = false;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		_moduleInventory.writeToNBT(nbttagcompound, "chassi");
		_module.writeToNBT(nbttagcompound, "");
		nbttagcompound.setInteger("Orientation", ChassiLogic.orientation.ordinal());
	}

	@Override
	public void onBlockRemoval() {
		super.onBlockRemoval();
		_moduleInventory.removeListener(this);
		if(MainProxy.isServer(this.worldObj)) {
			for(int i=0;i<_moduleInventory.getSizeInventory();i++) {
				if(_moduleInventory.getStackInSlot(i) != null) {
					ItemModuleInformationManager.saveInfotmation(_moduleInventory.getStackInSlot(i), this.getLogisticsModule().getSubModule(i), this.worldObj);
				}
			}
			_moduleInventory.dropContents(this.worldObj, this.xCoord, this.yCoord, this.zCoord);
		}
	}

	@Override
	public void InventoryChanged(SimpleInventory inventory) {
		boolean reInitGui = false;
		for (int i = 0; i < inventory.getSizeInventory(); i++){
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack == null){
				if (_module.hasModule(i)){
					_module.removeModule(i);
					reInitGui = true;
				}
				continue;
			}
			
			if (stack.getItem() instanceof ItemModule){
				ILogisticsModule current = _module.getModule(i);
				ILogisticsModule next = ((ItemModule)stack.getItem()).getModuleForItem(stack, _module.getModule(i), this, this, this);
				if (current != next){
					_module.installModule(i, next);
					ItemModuleInformationManager.readInformation(stack, next, this.worldObj);
					ItemModuleInformationManager.removeInformation(stack);
				}
			}
		}
		if (reInitGui) {
			if(MainProxy.isClient(this.worldObj)) {
				if (FMLClientHandler.instance().getClient().currentScreen instanceof GuiChassiPipe){
					FMLClientHandler.instance().getClient().currentScreen.initGui();
				}
			}
		}
	}

	@Override
	public void updateEntity() {
		super.updateEntity();
		if (switchOrientationOnTick){
			switchOrientationOnTick = false;
			if(MainProxy.isServer(this.worldObj)) {
				nextOrientation();
				//TODO
				//PacketDispatcher.sendToPlayers(this.getDescriptionPacket(), worldObj, xCoord, yCoord, zCoord, DefaultProps.NETWORK_UPDATE_RANGE, mod_BuildCraftCore.instance);
			}
		}
		if(convertFromMeta && worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != 0) {
			ChassiLogic.orientation = Orientations.values()[worldObj.getBlockMetadata(xCoord, yCoord, zCoord) % 6];
			worldObj.setBlockMetadata(xCoord, yCoord, zCoord, 0);
		}
		if(!init) {
			init = true;
			if(MainProxy.isClient(this.worldObj)) {
				PacketDispatcher.sendPacketToServer(new PacketCoordinates(NetworkConstants.REQUEST_PIPE_UPDATE, xCoord, yCoord, zCoord).getPacket());
			}
		}
	}
	
	public abstract int getChassiSize();
	
	@Override
	public final ILogisticsModule getLogisticsModule() {
		return _module;
	}
	
	@Override
	public TransportLayer getTransportLayer() {
		if (this._transportLayer == null){
			_transportLayer = new ChassiTransportLayer(this);
		}
		return _transportLayer;
	}
	
	@Override
	public boolean blockActivated(World world, int i, int j, int k,	EntityPlayer entityplayer) {
		if (entityplayer.getCurrentEquippedItem() == null) return super.blockActivated(world, i, j, k, entityplayer);
		
		if (entityplayer.getCurrentEquippedItem().getItem() == buildcraft.BuildCraftCore.wrenchItem){
			if (entityplayer.isSneaking()){
				((PipeLogisticsChassi)this.container.pipe).nextOrientation();
				return true;
			}
		}
		return super.blockActivated(world, i, j, k, entityplayer);
	}
	
	/*** IProvideItems ***/
	@Override
	public void canProvide(LogisticsTransaction transaction) {
		
		if (!isEnabled()){
			return;
		}
		
		for (int i = 0; i < this.getChassiSize(); i++){
			ILogisticsModule x = _module.getSubModule(i);
			if (x instanceof ILegacyActiveModule){
				((ILegacyActiveModule)x).canProvide(transaction);
			}
		}
	}
	
	@Override
	public void fullFill(LogisticsPromise promise, IRequestItems destination) {
		if (!isEnabled()){
			return;
		}
		for (int i = 0; i < this.getChassiSize(); i++){
			ILogisticsModule x = _module.getSubModule(i);
			if (x instanceof ILegacyActiveModule){
				((ILegacyActiveModule)x).fullFill(promise, destination);
			}
		}
	}
	
	@Override
	public int getAvailableItemCount(ItemIdentifier item) {
		if (!isEnabled()){
			return 0;
		}
		
		for (int i = 0; i < this.getChassiSize(); i++){
			ILogisticsModule x = _module.getSubModule(i);
			if (x instanceof ILegacyActiveModule){
				return ((ILegacyActiveModule)x).getAvailableItemCount(item);
			}
		}
		return 0;
	}
	
	@Override
	public HashMap<ItemIdentifier, Integer> getAllItems() {
		if (!isEnabled()){
			return new HashMap<ItemIdentifier, Integer>();
		}
		for (int i = 0; i < this.getChassiSize(); i++){
			ILogisticsModule x = _module.getSubModule(i);
			if (x instanceof ILegacyActiveModule){
				return ((ILegacyActiveModule)x).getAllItems();
			}
		}
		return new HashMap<ItemIdentifier, Integer>();
	}
	
	@Override
	public IRouter getRouter() {
		return super.getRouter();
	}
	
	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}
	
	@Override
	public World getWorld() {
		return this.worldObj;
	}
}
