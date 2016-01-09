package team061;

import java.util.Random;

import battlecode.common.*;

public class BotSoldier extends Bot {
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
		int acceptableRangeSquared = RobotType.ARCHON.sensorRadiusSquared;
		// Check where moving Archon is
		here = rc.getLocation();
		Signal[] signals = rc.emptySignalQueue();
		MapLocation archonLoc = checkScoutArchonLoc(signals);
		// Check for nearby enemies
		RobotInfo[] enemies = rc.senseHostileRobots(here, RobotType.SCOUT.sensorRadiusSquared);
		// If within acceptable range of archon
		if (here.distanceSquaredTo(archonLoc) > acceptableRangeSquared) {
			// If we are within enemy range and could step out do so
			if (nearEnemies(enemies, here) && couldMoveOut(enemies, here)) {
				MapLocation[] locEnemies = { Util.closest(enemies, here).location };
				Combat.retreat(locEnemies);
			}
			// else if we are within enemy range and could not step out attack
			else if (nearEnemies(enemies, here) && !couldMoveOut(enemies, here)) {
				Combat.shootAtNearbyEnemies();
			}
			// else move to Archon
			else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(archonLoc, theSafety);
			}
		}
		// else not within acceptable range of archon
		else {
			// If enemy is near attack
			if (nearEnemies(enemies, here))
				Combat.shootAtNearbyEnemies();
			// else no enemy move to archon
			else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(archonLoc, theSafety);
			}
		}
	}

	private static MapLocation checkScoutArchonLoc(Signal[] signals) {
		MapLocation archonLoc;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
				continue;
			}
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.SCOUT_ARCHON_LOCATION) {
				int[] decodedMessage = MessageEncode.SCOUT_ARCHON_LOCATION.decode(message);
				archonLoc = new MapLocation(decodedMessage[0], decodedMessage[1]);
				return archonLoc;
			}
		}
	}

	private static boolean nearEnemies(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		if (here.distanceSquaredTo(closestEnemy.location) > closestEnemy.type.attackRadiusSquared)
			return true;
		return false;
	}

	private static boolean couldMoveOut(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		int range = here.distanceSquaredTo(closestEnemy.location) - closestEnemy.type.attackRadiusSquared;
		if (range > -1)
			return true;
		return false;
	}

}
