package dune_buggy;

import battlecode.common.*;

public class BotGuard extends Bot {
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
		Harass.doHarass();
	}
	
	private static void init() throws GameActionException {
		return;
	}
}
