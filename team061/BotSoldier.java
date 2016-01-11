package team061;

import java.util.Random;

import battlecode.common.*;

public class BotSoldier extends Bot {

	static MapLocation archonLoc;
	static int archonID;
	static boolean isStill;

	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		init();
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

	private static void init() throws GameActionException {
		// atScoutLocation = false;
		isStill = false;
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.MOBILE_ARCHON_LOCATION) {
				int[] decodedMessage = MessageEncode.MOBILE_ARCHON_LOCATION.decode(signals[i].getLocation(), message);
				archonLoc = new MapLocation(decodedMessage[0], decodedMessage[1]);
				archonID = signals[i].getID();
				rc.setIndicatorString(0, "got archon loc");
				break;
			}
		}
	}

	private static void turn() throws GameActionException {
		int acceptableRangeSquared = RobotType.SOLDIER.sensorRadiusSquared;
		// Check where moving Archon is
		here = rc.getLocation();
		// Check for nearby enemies
		RobotInfo[] enemies = rc.senseHostileRobots(here, RobotType.SOLDIER.sensorRadiusSquared);
		// If within acceptable range of archon
		if (here.distanceSquaredTo(archonLoc) < acceptableRangeSquared) {
			// Check if we need to protect the archon
			if (nearEnemies(enemies, archonLoc)  /*&& updateArchonLoc()*/ ) {
				//get in front
				moveInFrontOfTheArchon(enemies);
				rc.setIndicatorString(0, "my life for the archon");
			}
			if (nearEnemies(enemies, here) && rc.isWeaponReady()) {
				Combat.shootAtNearbyEnemies();
				rc.setIndicatorString(0, "kill");
			}
			//
			else if (rc.isCoreReady() && isStill && enemies.length>0) {
				rc.setIndicatorString(0, "I'm tryna engage");
				RobotInfo closestEnemy = Util.closest(enemies, here);
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
			    Nav.goTo(closestEnemy.location,theSafety);

			} else if (rc.isCoreReady() && updateArchonLoc()) {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(archonLoc, theSafety);
				rc.setIndicatorString(1, "most likely chilling");

			}
		}
		// else not within acceptable range of archon
		else {
			// If enemy is near attack
			if (nearEnemies(enemies, here) && rc.isWeaponReady())
				Combat.shootAtNearbyEnemies();
			// else no enemy move to archon
			else if (here.distanceSquaredTo(archonLoc) > 0 && rc.isCoreReady()) {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(archonLoc, theSafety);
			}
			// TODO what to do if lost?
			else if (rc.isCoreReady()) {
				RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
				RobotInfo farthestSoldier = Util.closestSpecificType(allies, here, RobotType.SOLDIER);
				if (farthestSoldier != null) {
					rc.setIndicatorString(0, "going to " + farthestSoldier.location);
					archonLoc = farthestSoldier.location; // close enough lel
					Nav.goTo(archonLoc, new SafetyPolicyAvoidAllUnits(enemies));
				}
			}
		}
	}

	private static boolean updateArchonLoc() {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
		for (RobotInfo ally : allies) {
			if (ally.ID == archonID) {
				archonLoc = ally.location;
				return true;
			}
		}
		return false;
	}
	/*
	 * private static MapLocation checkScoutArchonLoc(Signal[] signals) {
	 * MapLocation archonLoc; for (int i = 0; i < signals.length; i++) { if
	 * (signals[i].getTeam() == them) { continue; } int[] message =
	 * signals[i].getMessage(); MessageEncode msgType =
	 * MessageEncode.whichStruct(message[0]); if (signals[i].getTeam() == us &&
	 * msgType == MessageEncode.MOBILE_ARCHON_LOCATION) { int[] decodedMessage =
	 * MessageEncode.MOBILE_ARCHON_LOCATION.decode(message); archonLoc = new
	 * MapLocation(decodedMessage[0], decodedMessage[1]); return archonLoc; } }
	 * // }
	 */

	private static void moveInFrontOfTheArchon(RobotInfo[] enemies){
		//try {
			RobotInfo closestEnemy = Util.closest(enemies, here);
			Direction directionToEnemyFromArchon = archonLoc.directionTo(closestEnemy.location);
			MapLocation goToHere = archonLoc.add(directionToEnemyFromArchon);
			//if (rc.canMove(here.directionTo(goToHere))) {
				//Nav.move(here.directionTo(goToHere));
			//}		} catch (Exception e) {
			//e.printStackTrace();
		//}
			RobotInfo[] stuff ={};
			NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(stuff);
			try{
			Nav.goTo(goToHere,theSafety);
	         } catch (Exception e){
		e.printStackTrace();
	}
	}

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
