package net.chrisknyfe.watersensor;

import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * @author Zach Bernal (Chrisknyfe)
 *
 * PlayerListener for the Water Sensor plugin.
 * 
 * Detects when players fill and empty buckets.
 */
public class WaterSensorPlayerListener implements Listener{
	public static WaterSensor plugin;
	
	public WaterSensorPlayerListener(WaterSensor instance) {
		plugin = instance;
	}
	
	/**
	 * Detect when a player fills their bucket, destroying a
	 * water block.
	 */
	@EventHandler
	public void onPlayerBucketFill(PlayerBucketFillEvent event){
		if (event.isCancelled()) return;
		
		BlockState b = event.getBlockClicked().getState();
		// Evaporation event. Duh, we're filling pails!
		if ( plugin.isBlockStateDetectable(b) ){
			plugin.executeSensorsAroundBlock(b, true, event);
		}
	}
	
	/**
	 * Detect when a player empties their bucket, creating a
	 * water block.
	 */
	@EventHandler
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event){
		if (event.isCancelled()) return;
		
		BlockState b = event.getBlockClicked().getState();
		// Emptying the bucket
		if ( plugin.isBlockStateDetectable(b) ){
			plugin.executeSensorsAroundBlock(b, false, event);
		}	
	}

}

