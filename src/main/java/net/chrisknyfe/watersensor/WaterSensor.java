package net.chrisknyfe.watersensor;

import java.util.logging.Logger;

import org.bukkit.Material; //uppercase is the enum
import org.bukkit.material.Lever; // lowercase is the namespace
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

/**
 * @author Zach Bernal (Chrisknyfe)
 *
 * WaterSensor - A robust, lightweight water sensor plugin.
 * 
 * This is the main plugin class, where the sensor's logic is executed, 
 * and various plugin bookkeeping tasks are done.
 * 
 * Design goals of this plugin:
 * 
 * Robust (it Just Fucking Works.)
 * Non-intrusive to other plugins.
 * Doesn't grief your structures (no block placing or removal.)
 * Doesn't need to register anything, or make any new threads, so that plugins like TempleCraft can use it.
 * Easy to read and use as an example for your own plugins.
 * 
 */
public class WaterSensor extends JavaPlugin {
	protected Configuration CONFIG;
	protected Logger log = Logger.getLogger("Minecraft");
	private final WaterSensorBlockListener blockListener = new WaterSensorBlockListener(this);
	private final WaterSensorPlayerListener playerListener = new WaterSensorPlayerListener(this);
	
	/**
	 * The Block ID of the water sensor (loaded from configuration.)
	 */
	int sensorBlockId;
	/**
	 * Enable or disable debug printing.
	 */
	boolean debugPrint;
	/**
	 * Used to iterate through all the adjacent blocks of the selected block.
	 */
	public final BlockFace adjacents[] = {BlockFace.UP,
			BlockFace.DOWN,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,};
	
	public void onEnable(){
		// Register Listeners
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.BLOCK_FROMTO, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PISTON_EXTEND, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_BUCKET_FILL, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_BUCKET_EMPTY, playerListener, Event.Priority.Normal, this);
		
		// Load & Create Configuration
		CONFIG = getConfiguration();
		CONFIG.load();
		sensorBlockId = CONFIG.getInt("sensorBlockId", 22);
		CONFIG.setProperty("sensorBlockId", sensorBlockId);
		debugPrint = CONFIG.getBoolean("debugPrint", false);
		CONFIG.setProperty("debugPrint", debugPrint);
		CONFIG.save();
		
		// Greet the server.
		log.info("[WaterSensor] has been enabled!");
	}
	
	public void onDisable(){
		CONFIG.save();
		log.info("[WaterSensor] has been disabled.");
	}
	
	/**
	 *  Logging mechanism: easy to turn off with config!
	 * @param msg The string to print.
	 */
	public void debugprint(String msg){
		if (debugPrint) log.info(msg);
	}
	
	/**
	 * Is this block a detectable block?
	 * @param b Block to check.
	 * @return Whether this block is detectable by the plugin
	 */
	public boolean isBlockStateDetectable(BlockState b){
		return (b.getType() == Material.WATER 
				|| b.getType() == Material.STATIONARY_WATER);
	}
	
	/**
	 * Can this block cause a sensor update? Should include detectable blocks, the sensor block itself, and levers.
	 * @param b Block to check.
	 * @return Whether this block should trigger sensor updates when placed.
	 */
	public boolean isBlockStateAnUpdater(BlockState b){
		return (isBlockStateDetectable(b) 
				|| b.getTypeId() == sensorBlockId
				|| b.getType() == Material.LEVER );
	}
	
	/**
	 * Look at adjacent sensors if this block is relevant for sensing
	 * @param b Block that is triggering the sensor checking (generally a water block.)
	 * @param evaporation If true, ignore the triggering block because its water is decaying.
	 * @param event Event that triggered this sensor update. Debugging purposes only.
	 * @return True if blocks were modified (only levers are changed.)
	 */
	public boolean executeSensorsAroundBlock(BlockState b, boolean evaporation, Event event){
		//debugprint("Detectable event " + event.getType());
		boolean isEvaporating = false;
		
		/*
		// Debug BLOCK_PHYSICS events: does water have a value?
		if (event.getType() == Event.Type.BLOCK_PHYSICS){
			debugprint(b.getType() + " physics data == " + b.getData() );
		}
		*/
		
		// If this is a BLOCK_PHYSICS event, only evaporate if water is about to drain
		if ( evaporation && (event.getType() == Event.Type.BLOCK_PHYSICS) ){
			debugprint("Physics event...");
			if ( (b.getType() == Material.WATER) && (b.getData().getData() >= 6) ) isEvaporating = true;
			else if ( (b.getType() == Material.STATIONARY_WATER) && (b.getData().getData() == 1) ) isEvaporating = true;
		}
		else
		{
			isEvaporating = evaporation;
		}
		
		
		
		// Check all blocks surrounding the water.
		Block bBlock = b.getBlock();
		Block here;
		boolean wasLeverTurned = false;
		for(BlockFace direction: adjacents) {
			here = bBlock.getRelative(direction);
			if (isEvaporating){
				if ( executeSensor(here, direction.getOppositeFace(), event) ) wasLeverTurned = true;				
			} 
			else {
				// since the water sensor will never check itself for water... this is a HACK.
				if ( executeSensor(here, BlockFace.SELF, event) ) wasLeverTurned = true; 
			}
		}
		
		return wasLeverTurned;
	}
	
	/**
	 * Run a sensor pass on the current location.
	 * @param b Sensor Block that is doing the sensing.
	 * @param ignoreDirection Ignore this direction (generally because there's still water there that will decay next tick.)
	 * @return True if blocks were modified (only levers are changed.)
	 */
	public boolean executeSensor(Block b, BlockFace ignoreDirection, Event event){
		// ======== Only sense if this is a sensor block. ======== 
		if ( b.getTypeId() != sensorBlockId ) return false;
		
		boolean isPowered = false;
		boolean wasLeverTurned = false;
		// ======== Sense! Adjacent water will activate this sensor. ========
		Block here;		
		for(BlockFace direction: adjacents) {
			here = b.getRelative(direction);
			if (direction == ignoreDirection) {
				debugprint("Ignoring direction " + direction);
				continue;
			}
			if (isBlockStateDetectable(here.getState())){
				isPowered = true;
				debugprint("Found water!");
			}
		}
		
		// ======== Actuate! Adjacent levers will be turned on. ======== 
		for(BlockFace direction: adjacents) {
			here = b.getRelative(direction);
			if (here.getType() == Material.LEVER){
				Lever lev = new Lever(Material.LEVER, here.getData());
				lev.setPowered(isPowered);
				here.setData( lev.getData() );
				debugprint("Turned a lever " + isPowered );
				wasLeverTurned = true;
			}
		}
		
		/*// DEBUG
		// Freeze the block that's evaporating anyway!
		if (ignoreDirection != BlockFace.SELF){
			b.getRelative(ignoreDirection).setType(Material.SNOW_BLOCK);
			debugprint("Snowed block " + b);
		}
		*/
		
		debugprint("Ran sensor pass on " + b + ", Event " + event );
		return wasLeverTurned;
	}
}
