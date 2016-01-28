package ARMAGEDDONBOT;

import battlecode.common.*;

public class MapAnalysis extends Bot {
	enum MapSymmetry {
		ROTATION, VERTICAL, HORIZONTAL, UNKNOWN
	}
	private static void setCenter(MapLocation[] ourArchons) {
		int xavg = 0, yavg = 0;
		for (int i = 0; i < ourArchons.length; i++) {
			xavg += ourArchons[i].x;
			yavg += ourArchons[i].y;
		}
		center = new MapLocation(Math.round(xavg / (ourArchons.length)), Math.round(yavg / (ourArchons.length)));
	}
	public static void analyze() {
		MapLocation[] ourArchons = rc.getInitialArchonLocations(us);
		setCenter(ourArchons);
		return;
	}

}