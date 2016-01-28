package TurretBot;

import battlecode.common.*;

public class MapAnalysis extends Bot {
	enum MapSymmetry {
		ROTATION, REFLECTION, UNKNOWN
	}

	protected static MapSymmetry mapSymmetry = null;
	protected static int[] zombieRounds = null;
	protected static int mapDifficulty = 0; //0 = can turtle, 1 = cannot turtle

	private static void determineMapSymmetry(MapLocation[] ourArchons, MapLocation[] theirArchons) {
		mapSymmetry = null;// lol this needs to be fixed
	}

	private static void setCenter(MapLocation[] ourArchons, MapLocation[] theirArchons) { // not always center for reflective map
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

	}
	private static void determineMapDifficulty(){
		int weakZombieCount = 0;
		int bigZombieCount = 0;
		if (false){
		return; // for now
		}
		ZombieSpawnSchedule spawnSchedule = rc.getZombieSpawnSchedule();
		for (int i = 0; i < zombieRounds.length; i++){ // zombie counts for the first 500 rounds
			if(zombieRounds[i] > 500){
				break;
			}
			ZombieCount countArray[] = spawnSchedule.getScheduleForRound(zombieRounds[i]);
			for (int j = 0; j < countArray.length; j++){
				if (countArray[j].getType() == RobotType.BIGZOMBIE){
					bigZombieCount += countArray[j].getCount();
				}
				else{
					weakZombieCount += countArray[j].getCount();
				}
			}
		}
		if (weakZombieCount <= 30 && bigZombieCount <= 2){
			mapDifficulty = 0;
		}
		else{
			mapDifficulty = 1;
		}
	}
	public static MapLocation getAlphaLocation(){
		int maxDist = 0;
		MapLocation alpha = null;
		for (MapLocation loc : rc.getInitialArchonLocations(us)){
			int dist = loc.distanceSquaredTo(center);
			if (dist > maxDist){
				maxDist = dist;
				alpha = loc;
			}
		}
		return alpha;
	}
	public static void analyze() {
		MapLocation[] ourArchons = rc.getInitialArchonLocations(us);
		MapLocation[] theirArchons = rc.getInitialArchonLocations(them);
		determineMapSymmetry(ourArchons, theirArchons);
		setCenter(ourArchons,theirArchons);
		zombieRounds = rc.getZombieSpawnSchedule().getRounds();
		//determineMapDifficulty();
		return;
	}

}