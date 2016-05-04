package logisticspipes.network.packets.module;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.modules.ModuleApiaristAnalyser;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class ApiaristAnalyserMode extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private int mode;

	public ApiaristAnalyserMode(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ApiaristAnalyserMode(getId());
	}

	@Override
	public void processPacket(EntityPlayer player) {
		ModuleApiaristAnalyser module = this.getLogisticsModule(player, ModuleApiaristAnalyser.class);
		if (module == null) {
			return;
		}
		module.setExtractMode(mode);
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
