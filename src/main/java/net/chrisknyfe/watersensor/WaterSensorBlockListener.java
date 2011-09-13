package net.chrisknyfe.watersensor;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * @author Zach Bernal (Chrisknyfe)
 * 
 * BlockListener for the Water Sensor plugin.
 * 
 * Update sensors when water flows, when water sensor components are placed,
 * when water is replaced by another block, when water drains, 
 * and when pistons affect water or water sensors.
 *
 */
public class WaterSensorBlockListener extends BlockListener{
	public static WaterSensor plugin;
	
	public WaterSensorBlockListener(WaterSensor instance) {
		plugin = instance;
	}
	
	/**
	 * Detect an event where water is flowing.
	 */
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
	
	/** 
	 * When you just gotta place some 8... or replace it.
	 * Also works when you place water sensor components (block or lever)
	 */
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
	
	/**
	 * Detect when water drains.
	 */
	public void onBlockPhysics(BlockPhysicsEvent event){
		if (event.isCancelled()) return;
		
		BlockState b = event.getBlock().getState();
		// As far as I know, these are always evaporation events.
		if ( plugin.isBlockStateDetectable(b) ){
			plugin.executeSensorsAroundBlock(b, true, event);
		}
	}
	
	/**
	 * Detect when pistons affect water or sensor components. PISTONS MAH BOI!
	 */
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

