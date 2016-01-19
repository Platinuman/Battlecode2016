package OrganizedBot;

import battlecode.common.*;

public class BotSoldier extends Bot {
	protected static MapLocation targetLoc;
	protected static int soldierType; // 0 = turret helper; 1 = mobile helper

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		Harass.isPreparingForCrunch = false;
		//MessageEncode.readMessagesAndUpdateInfo();
		//TODO make new data types in encode and use the to notify us when a bot is turtling
		Harass.doHarass();
	}
}