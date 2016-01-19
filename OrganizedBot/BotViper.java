package OrganizedBot;

import battlecode.common.*;

public class BotViper extends Bot {
	protected static MapLocation targetLoc;
	
	private static void turn() throws GameActionException {
		here = rc.getLocation();
		//Harass.doMobileViper();
	}
}