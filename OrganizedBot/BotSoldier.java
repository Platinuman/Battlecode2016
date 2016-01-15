package Battlecode2016.OrganizedBot;

import battlecode.common.*;

public class BotSoldier extends Bot {
	protected static MapLocation targetLoc;
	protected static int soldierType; // 0 = turret helper; 1 = mobile helper

	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
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
}