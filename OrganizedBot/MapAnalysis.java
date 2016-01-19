package Battlecode2016.OrganizedBot;

import battlecode.common.*;

public class MapAnalysis extends Bot {
	enum MapSymmetry {
		ROTATION, REFLECTION, UNKNOWN
	}

	protected static MapSymmetry mapSymmetry = null;
	protected static int[] zombieRounds = null;

	private static void determineMapSymmetry(MapLocation[] ourArchons, MapLocation[] theirArchons) {
		mapSymmetry = null;// lol this needs to be fixed
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

	}

	public static void analyze() {
		MapLocation[] ourArchons = rc.getInitialArchonLocations(us);
		MapLocation[] theirArchons = rc.getInitialArchonLocations(them);
		determineMapSymmetry(ourArchons, theirArchons);
		setCenter(ourArchons,theirArchons);
		zombieRounds = rc.getZombieSpawnSchedule().getRounds();
		return;
	}

}