package team061;

import java.util.Random;

import battlecode.common.*;

public class BotSoldier extends Bot {

	static MapLocation targetLoc;
	static int archonID;

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
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.MOBILE_ARCHON_LOCATION) {
				int[] decodedMessage = MessageEncode.MOBILE_ARCHON_LOCATION.decode(signals[i].getLocation(), message);
				targetLoc = new MapLocation(decodedMessage[0], decodedMessage[1]);
				archonID = signals[i].getID();
				rc.setIndicatorString(0, "got archon loc");
				break;
			}
		}
	}

	private static void turn() throws GameActionException {
		int acceptableRangeSquared = RobotType.SOLDIER.attackRadiusSquared;
		// Check where moving Archon is
		here = rc.getLocation();
		// Check for nearby enemies
		RobotInfo[] enemies = rc.senseHostileRobots(here, RobotType.SOLDIER.sensorRadiusSquared);
		boolean targetUpdated = updateTargetLoc();
		rc.setIndicatorString(0, "target = " + targetLoc);
		// If within acceptable range of target
		if (here.distanceSquaredTo(targetLoc) < acceptableRangeSquared) {
			// If we are within range of enemies
			if (rc.isWeaponReady() && enemies.length > 0) {
				Combat.shootAtNearbyEnemies();
			}
			//Try to get in front of the bad guys
			if(nearEnemies(enemies, targetLoc)){
			    moveInFrontOfTheArchon(Util.closest(enemies, targetLoc));
			}
			// If we can move out of danger without leaving the target in danger
			if (nearEnemies(enemies, here) && couldMoveOut(enemies, here) && (!nearEnemies(enemies, targetLoc))
					|| Util.closest(enemies, targetLoc).type != RobotType.ZOMBIEDEN ) {
				Combat.retreat(Util.closest(enemies, targetLoc).location);
			}
			// else not within acceptable range of target
		} else {
			// If enemy is near attack
			if (enemies.length > 0 && rc.isWeaponReady())
				Combat.shootAtNearbyEnemies();
			// else no enemy move to archon
			if (rc.isCoreReady()) {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(targetLoc, theSafety);
			// we are lost and are just gonna try to find some guys
			}/*else if(rc.isCoreReady()&& !updateTargetLoc()){ //this sucks since two lost soldiers are just gonna find each other and chill
				rc.setIndicatorString(0, "I am totally lost");
				RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
				RobotInfo farthestSoldier = Util.closestSpecificType(allies, here, RobotType.SOLDIER);
				if(farthestSoldier != null){
					rc.setIndicatorString(0,"going to " + farthestSoldier.location);
					targetLoc = farthestSoldier.location; // close enough lel
					Nav.goTo(targetLoc, new SafetyPolicyAvoidAllUnits(enemies));
				}
			}*/
		}
	}

	private static boolean updateTargetLoc() {
		Signal[] signals = rc.emptySignalQueue();
		for (Signal signal : signals) {
			if (signal.getTeam() == us && signal.getID() == archonID) {
				rc.setIndicatorString(1, "updating from message");
				int[] message = signal.getMessage();
				if (message != null) {
					MapLocation senderloc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.DIRECT_MOBILE_ARCHON) {
						int[] data = purpose.decode(senderloc, message);
						targetLoc = new MapLocation(data[0], data[1]);
						return true;
					}
				}
			}
		}
		/*
		 * RobotInfo[] allies =
		 * rc.senseNearbyRobots(RobotType.SOLDIER.sensorRadiusSquared, us);
		 * for(RobotInfo ally : allies){ if(ally.ID == archonID){ targetLoc =
		 * ally.location; return true; } }
		 */
		return false;
	}
	/*
	 * private static MapLocation checkScouttargetLoc(Signal[] signals) {
	 * MapLocation targetLoc; for (int i = 0; i < signals.length; i++) { if
	 * (signals[i].getTeam() == them) { continue; } int[] message =
	 * signals[i].getMessage(); MessageEncode msgType =
	 * MessageEncode.whichStruct(message[0]); if (signals[i].getTeam() == us &&
	 * msgType == MessageEncode.MOBILE_ARCHON_LOCATION) { int[] decodedMessage =
	 * MessageEncode.MOBILE_ARCHON_LOCATION.decode(message); targetLoc = new
	 * MapLocation(decodedMessage[0], decodedMessage[1]); return targetLoc; } }
	 * // }
	 */

	private static void moveInFrontOfTheArchon(RobotInfo closestEnemy) {
	//	RobotInfo closestEnemy = Util.closest(enemies, here);
		Direction directionToEnemyFromArchon = targetLoc.directionTo(closestEnemy.location);
		MapLocation goToHere = targetLoc.add(directionToEnemyFromArchon);
		RobotInfo[] stuff = {};
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(stuff);
		try {
			Nav.goTo(goToHere, theSafety);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean nearEnemies(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		if (closestEnemy != null
				&& here.distanceSquaredTo(closestEnemy.location) < closestEnemy.type.attackRadiusSquared)
			return true;
		return false;
	}

	private static boolean couldMoveOut(RobotInfo[] enemies, MapLocation here) {
		RobotInfo closestEnemy = Util.closest(enemies, here);
		int range = closestEnemy.type.attackRadiusSquared - here.distanceSquaredTo(closestEnemy.location);
		if (range > -1)
			return true;
		return false;
	}
}