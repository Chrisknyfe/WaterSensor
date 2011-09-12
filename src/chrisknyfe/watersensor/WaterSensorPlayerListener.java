package chrisknyfe.watersensor;

import org.bukkit.block.BlockState;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerListener;


public class WaterSensorPlayerListener extends PlayerListener{
	public static WaterSensor plugin;
	
	public WaterSensorPlayerListener(WaterSensor instance) {
		plugin = instance;
	}
	
	public void onPlayerBucketFill(PlayerBucketFillEvent event){
		if (event.isCancelled()) return;
		
		BlockState b = event.getBlockClicked().getState();
		// Evaporation event. Duh, we're filling pails!
		if ( plugin.isBlockStateDetectable(b) ){
			plugin.executeSensorsAroundBlock(b, true, event);
		}
	}
	
	public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event){
		if (event.isCancelled()) return;
		
		BlockState b = event.getBlockClicked().getState();
		// Emptying the bucket
		if ( plugin.isBlockStateDetectable(b) ){
			plugin.executeSensorsAroundBlock(b, false, event);
		}	
	}

}

