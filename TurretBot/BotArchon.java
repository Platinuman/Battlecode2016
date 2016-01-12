package TurretBot;

import battlecode.common.*;

import java.util.Random;

public class BotArchon extends Bot {
	static MapLocation alpha;
	static boolean isAlphaArchon;
	static int maxRange;
	static int numScoutsCreated = 0;

	private static boolean checkRubbleAndClear(Direction dir) throws GameActionException {

		if (rc.senseRubble(rc.getLocation().add(dir)) > 0) {
			rc.clearRubble(dir);
			return true;
		}
		return false;
	}

	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		init();
		// Debug.init("micro");
		while (true) {
			try {
				turn(rand);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		maxRange = 4;
		Signal[] signals = rc.emptySignalQueue();
		if (!signalsFromOurTeam(signals)) {
			MapLocation myLocation = rc.getLocation();
			int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { myLocation.x, myLocation.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
			isAlphaArchon = true;
			alpha = myLocation;
			rc.setIndicatorString(0, "I am the Darth Jar Jar. Fear me.");
			rc.setIndicatorString(1, "The ability to destroy a planet is insignificant next to the power of the Force.");
			rc.setIndicatorString(2, "I hope so for your sake, the emperor is not as forgiving as I am.");
		} else {
			for (int i = 0; i < signals.length; i++) {
				if(signals[i].getTeam()!=us){
					continue;
				}
				int[] message = signals[i].getMessage();
				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
				if (signals[i].getTeam() == us && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
					int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(message);
					alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
					break;
				}
			}
			isAlphaArchon = false;
			rc.setIndicatorString(0, "We make.");
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

	private static void constructNeededUnits(RobotType neededUnit) throws GameActionException {
		// Check for sufficient parts
		if (rc.hasBuildRequirements(neededUnit)) {
			Direction dirToBuild = directions[rand.nextInt(8)];
			Direction bestDir = dirToBuild;
			if (neededUnit == RobotType.SCOUT) {
				int bestScore = Integer.MAX_VALUE;
				for (int i = 0; i < 8; i++) {
					int score = BotScout.distToNearestScout(here.add(dirToBuild));
					if (rc.canBuild(dirToBuild, neededUnit) && score < bestScore) {
						bestScore = score;
						bestDir = dirToBuild;
					}
				}
			}
			dirToBuild = bestDir;
			for (int i = 0; i < 8; i++) {
				if (rc.canBuild(dirToBuild, neededUnit)) {
					rc.build(dirToBuild, neededUnit);
					if (neededUnit == RobotType.SCOUT) {
						numScoutsCreated++;
					}
					int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y });
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 3);
					int[] message = MessageEncode.PROXIMITY_NOTIFICATION.encode(new int[] { maxRange });
					rc.broadcastMessageSignal(message[0], message[1], (maxRange + 1) * (maxRange + 1));
					break;
				}
				dirToBuild = dirToBuild.rotateLeft();
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
		Boolean surrounded = true;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)) {
				surrounded = false;
				break;
			}
			dir = dir.rotateLeft();
		}
		return surrounded;
	}

	private static void repairBotMostInNeed() throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, us);
		if (allies.length > 0) {
			RobotInfo mostInNeed = Util.leastHealth(allies, 1);
			if (mostInNeed != null) {
				rc.repair(mostInNeed.location);
			}
		}
	}

	private static void turn(Random rand) throws GameActionException {
		repairBotMostInNeed();
		here = rc.getLocation();
		RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, them);
		if (rc.isCoreReady()) {
			if (isAlphaArchon || here.distanceSquaredTo(alpha) < 2) {
				aarons_shitty_strat();
			} else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(alpha, theSafety);
			}
		}
	}

	private static void aarons_shitty_strat() throws GameActionException {
		Direction dirToBuild = directions[rand.nextInt(8)];
		for (int i = 0; i < 8; i++) {
			if (checkRubbleAndClear(dirToBuild)) {
				break;
			}
			dirToBuild = dirToBuild.rotateRight();
		}
		if (rc.isCoreReady()) {
			RobotType needed = RobotType.TURRET;
			if (isScoutNeeded()) {
				needed = RobotType.SCOUT;
			}
			constructNeededUnits(needed);
		}

	}

	private static boolean isScoutNeeded() {
		RobotInfo[] teammates = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
		int nearbyScouts = 0;
		for (int i = 0; i < teammates.length; i++) {
			if (teammates[i].type == RobotType.SCOUT) {
				nearbyScouts++;
			}
		}
		if (numScoutsCreated < rc.getRoundNum() / 100 && nearbyScouts < (int) teammates.length / 6.0
				&& teammates.length > 10) {
			return true;
		}
		return false;
	}
}
