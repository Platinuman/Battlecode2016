package OrganizedBot;

import battlecode.common.*;

public class BotSoldier extends Bot {
	protected static MapLocation targetLoc;
	protected static int soldierType; // 0 = turret helper; 1 = mobile helper

	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		init();
		while (true) {
			try {
				turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	private static void turn() throws GameActionException {
		here = rc.getLocation();
		//rc.setIndicatorString(0, "");
//		String s = "";
//		for(int i = 0; i < turretSize; i++){
//			s += "[" + enemyTurrets[i].location.x + ", " + enemyTurrets[i].location.y +"], "; 
//		}
//		rc.setIndicatorString(1, s + " " + turretSize);
		//MessageEncode.readMessagesAndUpdateInfo();
		//TODO make new data types in encode and use the to notify us when a bot is turtling
		Harass.doHarass();
		String s = "";
		for (int i = 0; i < targetDenSize; i++) {
			if(targetDens[i] == null)
				continue;
			s += "[" + targetDens[i].x + ", " + targetDens[i].y + "], ";
		}
		rc.setIndicatorString(1, "numDensToHunt = " + numDensToHunt);
		rc.setIndicatorString(2, "den array = " + s);
	}
	
	private static void init() throws GameActionException {
		Harass.crunching = false;
		return;
	}
}