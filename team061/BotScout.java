package team061;

import java.util.Random;

import battlecode.common.*;

public class BotScout extends Bot {
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		// Debug.init("micro");
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
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		for (int i = 0; i < enemyRobots.length; i++) {
			MapLocation loc = enemyRobots[i].location;
			double health = enemyRobots[i].health;
			RobotType type = enemyRobots[i].type;
			int[] message = MessageEncode.TURRET_TARGET.encode(new int[]{(int)(health), type.ordinal(), loc.x, loc.y});
			rc.broadcastMessageSignal(message[0],message[1],(int)(RobotType.SCOUT.sensorRadiusSquared*GameConstants.BROADCAST_RANGE_MULTIPLIER));
		}
	}
}
    	
