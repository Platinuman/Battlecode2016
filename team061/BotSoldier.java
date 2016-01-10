package team061;

import java.util.Random;

import battlecode.common.*;

public class BotSoldier extends Bot {
	
	static MapLocation archonLoc;
	static int archonID;
	
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		init();
		// Debug.init("micro");
		while (true) {
			try {
			//	turn();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}
	
	private static void init() throws GameActionException {
		// atScoutLocation = false;
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.MOBILE_ARCHON_LOCATION){
				int[] decodedMessage = MessageEncode.MOBILE_ARCHON_LOCATION.decode(signals[i].getLocation(), message);
				archonLoc = new MapLocation(decodedMessage[0], decodedMessage[1]);
				archonID = signals[i].getID();
				//rc.setIndicatorString(0	,"got archon loc");
				break;
			}
		}
	}

	private static void turn() throws GameActionException {
		int acceptableRangeSquared = RobotType.ARCHON.sensorRadiusSquared;
		// Check where moving Archon is
		here = rc.getLocation();
		updateArchonLoc();
		// Check for nearby enemies
		RobotInfo[] enemies = rc.senseHostileRobots(here, RobotType.SCOUT.sensorRadiusSquared);
		// If within acceptable range of archon
		if (here.distanceSquaredTo(archonLoc) < acceptableRangeSquared) {
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
			else if(here.distanceSquaredTo(archonLoc) > 8) {
				//don't block archon
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(archonLoc, theSafety);
			}
			else if(here.distanceSquaredTo(archonLoc) < 4){
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				boolean moved = Nav.moveInDir(here.directionTo(archonLoc).opposite(), theSafety);
				//we're too close
			}
		}
		// else not within acceptable range of archon
		else {
			// If enemy is near attack
			if (nearEnemies(enemies, here))
				Combat.shootAtNearbyEnemies();
			// else no enemy move to archon
			else if (here.distanceSquaredTo(archonLoc) > 0){
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(archonLoc, theSafety);
			}
			//TODO what to do if lost?
		}
	}

	private static void updateArchonLoc() {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, us);
		for(RobotInfo ally : allies){
			if(ally.ID == archonID){
				archonLoc = ally.location;
				break;
			}
		}
	}
/*
	private static MapLocation checkScoutArchonLoc(Signal[] signals) {
		MapLocation archonLoc;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
				continue;
			}
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.MOBILE_ARCHON_LOCATION) {
				int[] decodedMessage = MessageEncode.MOBILE_ARCHON_LOCATION.decode(message);
				archonLoc = new MapLocation(decodedMessage[0], decodedMessage[1]);
				return archonLoc;
			}
		}
//	}*/

	private static boolean nearEnemies(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		if (closestEnemy != null && here.distanceSquaredTo(closestEnemy.location) > closestEnemy.type.attackRadiusSquared)
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
