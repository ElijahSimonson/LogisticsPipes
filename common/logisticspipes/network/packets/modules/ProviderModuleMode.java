package logisticspipes.network.packets.modules;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.modules.ModuleProvider;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class ProviderModuleMode extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private int mode;

	public ProviderModuleMode(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ProviderModuleMode(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		final ModuleProvider module = this.getLogisticsModule(player, ModuleProvider.class);
		if (module == null) {
			return;
		}
		module.setExtractionMode(mode);
	}

	@Override
	public void writeData(LPDataOutput output) throws IOException {
		super.writeData(output);
		output.writeInt(mode);
	}

	@Override
	public void readData(LPDataInput input) throws IOException {
		super.readData(input);
		mode = input.readInt();
	}
}
