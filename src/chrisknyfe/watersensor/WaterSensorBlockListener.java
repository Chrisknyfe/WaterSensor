package chrisknyfe.watersensor;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class WaterSensorBlockListener extends BlockListener{
	public static WaterSensor plugin;
	
	public WaterSensorBlockListener(WaterSensor instance) {
		plugin = instance;
	}
	
	// Flowing Water
	public void onBlockFromTo(BlockFromToEvent event) {
		if (event.isCancelled()) return;
		
		BlockState fromstate = event.getBlock().getState();
		if ( plugin.isBlockStateDetectable(fromstate) ){
			plugin.executeSensorsAroundBlock(fromstate, false, event);
		}
		BlockState tostate = event.getToBlock().getState();
		if ( plugin.isBlockStateDetectable(fromstate) ){
			plugin.executeSensorsAroundBlock(tostate, false, event);
		}
	}
	
	// When you just gotta place some 8... or replace it. Also works for levers!
	public void onBlockPlace(BlockPlaceEvent event){
		if (event.isCancelled()) return;
		
		BlockState tostate = event.getBlockPlaced().getState();
		if ( plugin.isBlockStateAnUpdater(tostate) ){
			plugin.executeSensorsAroundBlock(tostate, false, event);
		}
		// If we're destroying water, it's an evaporation event. Doesn't really matter with levers...
		BlockState fromstate = event.getBlockReplacedState();
		if ( plugin.isBlockStateAnUpdater(fromstate) ){
			plugin.executeSensorsAroundBlock(fromstate, true, event);
		}
	}
	
	// When water drains.
	public void onBlockPhysics(BlockPhysicsEvent event){
		if (event.isCancelled()) return;
		
		BlockState b = event.getBlock().getState();
		// As far as I know, these are always evaporation events.
		if ( plugin.isBlockStateDetectable(b) ){
			plugin.executeSensorsAroundBlock(b, true, event);
		}
	}
	
	// PISTONS MAH BOI.
	public void onBlockPistonExtend(BlockPistonExtendEvent event){
		if (event.isCancelled()) return;
		
		// Get the block just at the end of the piston chain (that will be replaced when the chain is pushed)
		Block endblock = event.getBlock().getRelative( event.getDirection(), event.getLength() + 1 );
		BlockState b = endblock.getState();
		plugin.debugprint("Block at end of piston chain " + b.getType());
		if ( plugin.isBlockStateAnUpdater(b) ){
			plugin.executeSensorsAroundBlock(b, true, event);
		}
		
	}
	
}

