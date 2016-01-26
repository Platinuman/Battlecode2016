package OrganizedBot;

import battlecode.common.*;

public class MapAnalysis extends Bot {
	enum MapSymmetry {
		ROTATION, VERTICAL, HORIZONTAL, UNKNOWN
	}

	protected static MapSymmetry mapSymmetry = null;
	protected static int[] zombieRounds = null;
	protected static int mapDifficulty = 1; //0 = can turtle, 1 = cannot turtle

	private static void determineMapSymmetry(MapLocation[] ourArchons, MapLocation[] theirArchons) {
		mapSymmetry = MapSymmetry.UNKNOWN;// lol this needs to be fixed
		switch (ourArchons.length) {
		case 1:
			if (ourArchons[0].x == theirArchons[0].x || ourArchons[0].y == theirArchons[0].y) {
				return;
			}
		default:
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
		int bigZombieCount = 0;
		if (false){
		return; // for now
		}
		ZombieSpawnSchedule spawnSchedule = rc.getZombieSpawnSchedule();
		for (int i = 0; i < zombieRounds.length; i++){
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
	public static void analyze() {
		MapLocation[] ourArchons = rc.getInitialArchonLocations(us);
		MapLocation[] theirArchons = rc.getInitialArchonLocations(them);
		Bot.initialEnemyArchonLocs = theirArchons;
		determineMapSymmetry(ourArchons, theirArchons);
		setCenter(ourArchons,theirArchons);
		zombieRounds = rc.getZombieSpawnSchedule().getRounds();
		determineMapDifficulty();
		return;
	}

}