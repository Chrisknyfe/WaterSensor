package chrisknyfe.watersensor;

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

/* Design goals of this plugin:
 * 
 * Robust (it Just Fucking Works.)
 * Non-intrusive to other plugins.
 * Doesn't grief your structures (no block placing or removal.)
 * Doesn't need to register anything, or make any new threads, so that plugins like TempleCraft can use it.
 * Easy to read and use as an example for your own plugins.
 * 
*/

/**
 * @author zbernal
 *
 * WaterSensor - sense water adjacent to a Lapiz Lazuli block (or whatever block you specify)
 * 
 */
public class WaterSensor extends JavaPlugin {
	protected Configuration CONFIG;
	protected Logger log = Logger.getLogger("Minecraft");
	private final WaterSensorBlockListener blockListener = new WaterSensorBlockListener(this);
	private final WaterSensorPlayerListener playerListener = new WaterSensorPlayerListener(this);
	
	int sensorBlockId;
	boolean debugPrint; //enable or disable debug printing
	// Used to iterate through all the adjacent blocks of the selected block.
	public final BlockFace adjacents[] = {BlockFace.UP,
			BlockFace.DOWN,
			BlockFace.NORTH,
			BlockFace.SOUTH,
			BlockFace.EAST,
			BlockFace.WEST,};
	
	public void onEnable(){
		// Listeners
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.BLOCK_FROMTO, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PISTON_EXTEND, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_BUCKET_FILL, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_BUCKET_EMPTY, playerListener, Event.Priority.Normal, this);
		
		// Configuration
		CONFIG = getConfiguration();
		CONFIG.load();
		sensorBlockId = CONFIG.getInt("sensorBlockId", 22);
		CONFIG.setProperty("sensorBlockId", sensorBlockId);
		debugPrint = CONFIG.getBoolean("debugPrint", false);
		CONFIG.setProperty("debugPrint", debugPrint);
		CONFIG.save();
		
		// Celebration
		debugprint("[WaterSensor] has been enabled!");
	}
	
	public void onDisable(){
		CONFIG.save();
		debugprint("[WaterSensor] has been disabled.");
	}
	
	// Logging mechanism: easy to turn off with config!
	public void debugprint(String msg){
		if (debugPrint) log.info(msg);
	}
	
	// Is this block a detectable block?
	public boolean isBlockStateDetectable(BlockState b){
		return (b.getType() == Material.WATER 
				|| b.getType() == Material.STATIONARY_WATER);
	}
	
	// Can this block cause a sensor update? Should include detectable blocks.
	public boolean isBlockStateAnUpdater(BlockState b){
		return (isBlockStateDetectable(b) 
				|| b.getTypeId() == sensorBlockId
				|| b.getType() == Material.LEVER );
	}
	
	// Look at adjacent sensors if this block is relevant for sensing
	public boolean executeSensorsAroundBlock(BlockState b, boolean evaporation, Event event){
		debugprint("Detectable event " + event.getType());
		// Check all blocks surrounding the water.
		Block bBlock = b.getBlock();
		Block here;
		boolean wasLeverTurned = false;
		for(BlockFace direction: adjacents) {
			here = bBlock.getRelative(direction);
			if (evaporation){
				if ( executeSensor(here, direction.getOppositeFace()) ) wasLeverTurned = true;
			} 
			else {
				// since the water sensor will never check itself for water... this is a HACK.
				if ( executeSensor(here, BlockFace.SELF) ) wasLeverTurned = true; 
			}
		}
		return wasLeverTurned;
	}
	
	// Run a sensor pass on the current location.
	// Return true if blocks were modified (a lever was turned!)
	public boolean executeSensor(Block b, BlockFace ignoreDirection){
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
			if (here.getType() == Material.WATER || here.getType() == Material.STATIONARY_WATER){
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
		
		debugprint("Ran sensor pass on " + b );
		return wasLeverTurned;
	}
}
