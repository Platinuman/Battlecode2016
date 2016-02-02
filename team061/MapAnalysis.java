package team061;

import battlecode.common.*;

public class MapAnalysis extends Bot {
	enum MapSymmetry {
		ROTATION, VERTICAL, HORIZONTAL, UNKNOWN
	}

	protected static MapSymmetry mapSymmetry = null;
	protected static int[] zombieRounds = null;
	protected static int mapDifficulty = 1; //0 = can turtle, 1 = cannot turtle
	protected static int mapSize = 0;

	private static void determineMapSymmetry(MapLocation[] ourArchons, MapLocation[] theirArchons) {
		mapSymmetry = MapSymmetry.UNKNOWN;// lol this needs to be fixed
		if(true){
			return;
		}
		switch (ourArchons.length) {
		case 1:
			if (ourArchons[0].x == theirArchons[0].x || ourArchons[0].y == theirArchons[0].y) {
				return;
			}
		default:
			boolean possiblyRotation = true;
			for (int i = 0; i < ourArchons.length; i++){
				MapLocation testCenter = new MapLocation(Math.round((ourArchons[i].x + theirArchons[theirArchons.length-1-i].x)/2),Math.round((ourArchons[i].y + theirArchons[theirArchons.length-1-i].y)/2));
				//System.out.println("testCenter = " + testCenter);
				//System.out.println("Center = " + center);
				if (testCenter.equals(center)){
					continue;
				}else{
					possiblyRotation = false;
				}
			}
			if (possiblyRotation){
				mapSymmetry = MapSymmetry.ROTATION;
			}else{
				return;
			}
			return;
		}
	}

	private static void setCenter(MapLocation[] ourArchons, MapLocation[] theirArchons) {
		int xavg = 0, yavg = 0;
		for (int i = 0; i < ourArchons.length; i++) {
			xavg += ourArchons[i].x;
			yavg += ourArchons[i].y;
		}
		for (int i = 0; i < theirArchons.length; i++) {
			xavg += theirArchons[i].x;
			yavg += theirArchons[i].y;
		}
		
		center = new MapLocation(Math.round(xavg / (ourArchons.length+theirArchons.length)), Math.round(yavg /(ourArchons.length+theirArchons.length)));
		//System.out.println("center at: " + center.x + ", " + center.y);
	}
	private static void determineMapDifficulty(){
		int weakZombieCount = 0;
		//int bigZombieCount = 0;
//		if (true){
//		return; // for now
//		}
		ZombieSpawnSchedule spawnSchedule = rc.getZombieSpawnSchedule();
		for (int i = 0; i < zombieRounds.length; i++){
			if(zombieRounds[i] > 500){
				break;
			}
			ZombieCount countArray[] = spawnSchedule.getScheduleForRound(zombieRounds[i]);
			for (int j = 0; j < countArray.length; j++){
					weakZombieCount += countArray[j].getCount();
			}
		}
		if (weakZombieCount <= 30){
			mapDifficulty = 0;
		}
		else{
			mapDifficulty = 1;
		}
	}
	private static void guessMapSize(MapLocation[] ourArchons, MapLocation[] theirArchons){
		mapSize = (int)(ourArchons[0].distanceSquaredTo(theirArchons[theirArchons.length-1])/ourArchons.length);
	}
	public static void analyze() {
		MapLocation[] ourArchons = rc.getInitialArchonLocations(us);
		MapLocation[] theirArchons = rc.getInitialArchonLocations(them);
		Bot.initialEnemyArchonLocs = theirArchons;
		setCenter(ourArchons,theirArchons);
		guessMapSize(ourArchons,theirArchons);
		determineMapSymmetry(ourArchons, theirArchons);
		//System.out.println(mapSymmetry + "");
		zombieRounds = rc.getZombieSpawnSchedule().getRounds();
		determineMapDifficulty();
		return;
	}

}
