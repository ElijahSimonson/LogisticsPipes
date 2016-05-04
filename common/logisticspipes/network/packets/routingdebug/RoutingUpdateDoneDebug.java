package logisticspipes.network.packets.routingdebug;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.routing.debug.ClientViewController;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class RoutingUpdateDoneDebug extends ModernPacket {

	public RoutingUpdateDoneDebug(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) throws IOException {}

	@Override
	public void processPacket(EntityPlayer player) {
		ClientViewController.instance().done(this);
	}

	@Override
	public void writeData(LPDataOutput output) throws IOException {}

	@Override
	public ModernPacket template() {
		return new RoutingUpdateDoneDebug(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
