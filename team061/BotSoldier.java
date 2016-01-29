package team061;

import battlecode.common.*;

public class BotSoldier extends Bot {
	protected static MapLocation targetLoc;
	
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		//init();
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
//		rc.setIndicatorString(0, "");
//		rc.setIndicatorString(1, "");
//		rc.setIndicatorString(2, "");
		//MessageEncode.readMessagesAndUpdateInfo();
		//TODO make new data types in encode and use the to notify us when a bot is turtling
		Harass.doHarass();
//		String s = "";
//		for (int i = 0; i < targetDenSize; i++) {
//			if(targetDens[i] == null)
//				continue;
//			s += "[" + targetDens[i].x + ", " + targetDens[i].y + "], ";
//		}
//		rc.setIndicatorString(1, "numDensToHunt = " + numDensToHunt);
//		rc.setIndicatorString(2, "den array = " + s);
	}
	
	private static void init() throws GameActionException {
		return;
	}
}
