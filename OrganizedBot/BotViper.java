package OrganizedBot;

import battlecode.common.*;

public class BotViper extends Bot {
	protected static MapLocation targetLoc;
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
	private static void init(){
		return;
	}
	
	private static void turn() throws GameActionException {
		here = rc.getLocation();
		//Harass.doMobileViper();
	}
}