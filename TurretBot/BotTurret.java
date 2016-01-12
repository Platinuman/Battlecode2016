package TurretBot;

import battlecode.common.*;

public class BotTurret extends Bot {
	static MapLocation alpha;
	static int range;
	static boolean isTTM;
	static boolean shouldMove;
	// static boolean firstTurn = true;

	public static void loop(RobotController theRC) throws GameActionException {
		/*
		 * if(firstTurn){ firstTurn = false; Clock.yield(); }
		 */
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

	private static void init() throws GameActionException {
		rc.setIndicatorString(0, "We shoot.");
		isTTM = false;
		range = 4;
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
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
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		Signal[] signals = rc.emptySignalQueue();
		updateMaxRange(signals);
		if (!isTTM) {
			attackIfApplicable(signals);
		}
		if (rc.isCoreReady()) {
			moveToLocFartherThanAlphaIfPossible(here);

		}
	}

	private static void moveToLocFartherThanAlphaIfPossible(MapLocation here) throws GameActionException {
		Direction dir = directions[rand.nextInt(8)];
		boolean shouldMove = false;
		Direction bestDir = dir;
		int bestScore = 0;
		int distanceToAlpha = here.distanceSquaredTo(alpha);
		boolean startedAsTTM = isTTM;
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.sensorRadiusSquared);
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)
					&& rc.senseRubble(newLoc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				int newDistanceToAlpha = newLoc.distanceSquaredTo(alpha);
				if (newDistanceToAlpha > distanceToAlpha && newDistanceToAlpha <= range
						&& theSafety.isSafeToMoveTo(newLoc)) {
					if (newDistanceToAlpha > bestScore)
						bestDir = dir;
					bestScore = newDistanceToAlpha;
					shouldMove = true;
				}
			}
			dir = dir.rotateLeft();
		}
		if (shouldMove) {
			if (!isTTM) {
				rc.pack();
				isTTM = true;
			} else {
				rc.move(bestDir);
			}
		} else if (startedAsTTM) {
			rc.unpack();
			isTTM = false;
		}
	}

	private static void updateMaxRange(Signal[] signals) {
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
				continue;
			}
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.PROXIMITY_NOTIFICATION) {
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(message);
				range = decodedMessage[0];
				break;
			}
		}
	}

	private static void attackIfApplicable(Signal[] signals) throws GameActionException {
		if (rc.isWeaponReady()) {
			Combat.shootAtNearbyEnemies();
		}
		if (rc.isWeaponReady()) {
			shootWithoutThinking(signals);
			/*
			int[] indicesOfTargetSignals = getIndicesOfTargetSignals(signals);
			if (indicesOfTargetSignals.length > 0) {
				int numTargetSignals = indicesOfTargetSignals[indicesOfTargetSignals.length - 1];
				MapLocation[] hostileLocations = new MapLocation[numTargetSignals];
				int[] healths = new int[numTargetSignals];
				RobotType[] hostileTypes = new RobotType[numTargetSignals];
				for (int i = 0; i < numTargetSignals; i++) {
					int[] message = signals[indicesOfTargetSignals[i]].getMessage();
					int[] decodedMessage = MessageEncode.TURRET_TARGET.decode(message);
					healths[i] = decodedMessage[0];
					hostileTypes[i] = RobotType.values()[decodedMessage[1]];
					hostileLocations[i] = new MapLocation(decodedMessage[2], decodedMessage[3]);

				}
				Combat.shootBestEnemyTakingIntoAccountScoutInfo(hostileLocations, healths, hostileTypes);
			}
			*/

		}
	}

	private static void shootWithoutThinking(Signal[] signals) throws GameActionException {
		for (int i = signals.length - 1; i >= 0; i--) {
			if (signals[i].getTeam() == us) {
				int[] message = signals[i].getMessage();
				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
				int[] decodedMessage = MessageEncode.TURRET_TARGET.decode(message);
				if (msgType == MessageEncode.TURRET_TARGET) {
					MapLocation enemyLocation = new MapLocation(decodedMessage[2], decodedMessage[3]);
					if (rc.canAttackLocation(enemyLocation)) {
						rc.attackLocation(enemyLocation);
						return;
					}
				}
			}
		}
	}

	private static int[] getIndicesOfTargetSignals(Signal[] signals) throws GameActionException {
		int[] indicesOfTargetSignals = new int[signals.length + 1];
		int count = 0;
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (msgType == MessageEncode.TURRET_TARGET && signals[i].getTeam() == us) {
				indicesOfTargetSignals[count] = i;
				count++;
			}
		}
		indicesOfTargetSignals[signals.length] = count;
		return indicesOfTargetSignals;
	}
}