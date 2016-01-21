package TurretBot;

import battlecode.common.*;

import java.util.Random;

public class BotArchon extends Bot {
	static MapLocation alpha;
	static boolean isAlphaArchon;
	static int maxRange;
	static int numScoutsCreated = 0;

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
		if (MapAnalysis.mapDifficulty == 0) {
			maxRange = 4;
			Signal[] signals = rc.emptySignalQueue();
			if (!signalsFromOurTeam(signals)) {
				MapLocation myLocation = rc.getLocation();
				int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { myLocation.x, myLocation.y });
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 80 * 80 * 2);
				isAlphaArchon = true;
				alpha = myLocation;
				rc.setIndicatorString(0, "I am the Darth Jar Jar. Fear me.");
				rc.setIndicatorString(1,
						"The ability to destroy a planet is insignificant next to the power of the Force.");
				rc.setIndicatorString(2, "I hope so for your sake, the emperor is not as forgiving as I am.");
			} else {
				for (int i = 0; i < signals.length; i++) {
					if (signals[i].getTeam() != us) {
						continue;
					}
					int[] message = signals[i].getMessage();
					MessageEncode msgType = MessageEncode.whichStruct(message[0]);
					if (msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
						int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(signals[i].getLocation(),
								message);
						alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
						rc.setIndicatorString(1, "found alpha");
						break;
					}
				}
				isAlphaArchon = false;
				rc.setIndicatorString(0, "We make.");
			}
		} else {
			return;
		}
	}

	public static Boolean signalsFromOurTeam(Signal[] signals) {
		if (signals.length == 0) {
			return false;
		} else {
			for (Signal sig : signals) {
				if (sig.getTeam() == us)
					return true;
			}
		}
		return false;
	}

	public static int distToNearest(MapLocation loc, RobotType myType) throws GameActionException {
		RobotInfo[] nearbyAllies = rc.senseNearbyRobots(loc, RobotType.ARCHON.sensorRadiusSquared, us);
		double nearbyUnits = 0;
		for (int i = 0; i < nearbyAllies.length; i++) {
			if (nearbyAllies[i].type == myType) {
				nearbyUnits += 1000.0 / nearbyAllies[i].location.distanceSquaredTo(loc);
			}
		}
		return (int) nearbyUnits;
	}

	private static void constructNeededUnits(RobotType neededUnit) throws GameActionException {
		if (rc.hasBuildRequirements(neededUnit)) {
			Direction dirToBuild = here.directionTo(center);
			Direction bestDir = dirToBuild;
			int bestScore = Integer.MAX_VALUE;
			for (int i = 0; i < 8; i++) {
				if (rc.canBuild(dirToBuild, neededUnit)) {
					int score = distToNearest(here.add(dirToBuild), neededUnit);
					if (score < bestScore) {
						bestScore = score;
						bestDir = dirToBuild;
					}
				}
				dirToBuild = dirToBuild.rotateRight();
			}
			dirToBuild = bestDir;
			if (rc.canBuild(dirToBuild, neededUnit)) {
				rc.build(dirToBuild, neededUnit);
				if (neededUnit == RobotType.SCOUT) {
					numScoutsCreated++;
				}
				int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y });
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 3);
				int[] message = MessageEncode.PROXIMITY_NOTIFICATION.encode(new int[] { maxRange });
				rc.broadcastMessageSignal(message[0], message[1], 3);
			}

			if (isAlphaArchon && isSurrounded() && rc.getRoundNum() % 20 == 0) {
				maxRange++;
				int[] message = MessageEncode.PROXIMITY_NOTIFICATION.encode(new int[] { maxRange });
				rc.broadcastMessageSignal(message[0], message[1], (maxRange + 1) * (maxRange + 1));
			}
		}
	}

	private static boolean isSurrounded() throws GameActionException {
		Direction dir = Direction.NORTH;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)
					&& rc.senseRubble(newLoc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				return false;
			}
			dir = dir.rotateLeft();
		}
		return true;
	}

	private static void repairBotMostInNeed() throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, us);
		if (allies.length > 0) {
			RobotInfo mostInNeed = Util.leastHealth(allies, 1);
			if (mostInNeed != null) {
				rc.setIndicatorString(2, "repairing unit at: " + mostInNeed.location.x + " "+ mostInNeed.location.y);
				rc.repair(mostInNeed.location);
			}
		}
	}

	private static void turn() throws GameActionException {
		repairBotMostInNeed();
		here = rc.getLocation();
		if (MapAnalysis.mapDifficulty == 0) {
			RobotInfo[] enemies = rc.senseHostileRobots(here, RobotType.ARCHON.sensorRadiusSquared);
			if (rc.isCoreReady()) {
				if (isAlphaArchon || here.distanceSquaredTo(alpha) <= 1) {
					aarons_shitty_strat();
				} else {
					NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
					Nav.goTo(alpha, theSafety);
				}
			}
		} else {
			return;
		}
	}

	private static void aarons_shitty_strat() throws GameActionException {

		RobotType needed = RobotType.TURRET;
		if (isScoutNeeded()) {
			needed = RobotType.SCOUT;
		}
		constructNeededUnits(needed);
		if (rc.isCoreReady()) {
			Util.checkRubbleAndClear(here.directionTo(center));
		}

	}

	private static boolean isScoutNeeded() {
		RobotInfo[] teammates = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
		int nearbyScouts = 0;
		int nearbyTurrets = 0;
		for (int i = 0; i < teammates.length; i++) {
			if (teammates[i].type == RobotType.SCOUT) {
				nearbyScouts++;
			} else if (teammates[i].type == RobotType.TURRET) {
				nearbyTurrets++;
			}
		}
		if (nearbyScouts < (int) teammates.length / 7.0 && nearbyTurrets >= 4) {
			return true;
		}
		return false;
	}
}
