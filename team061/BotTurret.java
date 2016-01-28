package team061;

import battlecode.common.*;

public class BotTurret extends Bot {
	static MapLocation alpha;
	static int range;
	static boolean isTTM;
	// static MapLocation lastSignal;
	// static int patience;

	public static void loop(RobotController theRC) throws GameActionException {
		Clock.yield();
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
		// patience = 0;
		alpha = MapAnalysis.getAlphaLocation();
		// lastSignal = alpha;
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		Signal[] signals = rc.emptySignalQueue();
		updateMaxRange(signals);
		if (!isTTM) {
			attackIfApplicable(signals);
		}
		if (rc.isCoreReady()) {
			moveToLocFartherThanAlphaIfPossible();
		}
		if (rc.isWeaponReady()) {
			shootRandomly();
		}
	}

	private static void shootRandomly() throws GameActionException{
		if (rc.getRoundNum() > 500) {
			Direction trueAway = alpha.directionTo(here);
			Direction away = trueAway;
			MapLocation enemyLocation = here;
			int count = 0;
			while (count < 4 || rc.canAttackLocation(enemyLocation.add(away))) {
				enemyLocation = enemyLocation.add(away);
				away = (new Direction[] { trueAway, trueAway.rotateLeft(), trueAway.rotateRight(),
						trueAway.rotateLeft().rotateLeft(), trueAway.rotateRight().rotateRight() })[rand.nextInt(5)];
				count++;
				if(rc.canSenseLocation(enemyLocation) && !rc.onTheMap(enemyLocation)){
					return;
				}
			}
			if (rc.canAttackLocation(enemyLocation)) {
				rc.attackLocation(enemyLocation);
				return;
			}
		}
	}

	private static void moveToLocFartherThanAlphaIfPossible() throws GameActionException {
		Direction dir = here.directionTo(center);
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
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(signals[i].getLocation(), message);
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
		}
	}

	private static void shootWithoutThinking(Signal[] signals) throws GameActionException {
		for (int i = signals.length - 1; i >= 0; i--) {
			if (signals[i].getTeam() == us) {
				int[] message = signals[i].getMessage();
				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
				int[] decodedMessage = MessageEncode.TURRET_TARGET.decode(signals[i].getLocation(), message);
				if (msgType == MessageEncode.TURRET_TARGET) {
					MapLocation enemyLocation = new MapLocation(decodedMessage[0], decodedMessage[1]);
					if (rc.canAttackLocation(enemyLocation)) {
						rc.attackLocation(enemyLocation);
						// lastSignal = enemyLocation;
						// patience = rc.getRoundNum();
						return;
					}
				}
			} else {
				MapLocation enemyLocation = signals[i].getLocation();
				if (rc.canAttackLocation(enemyLocation)) {
					rc.attackLocation(enemyLocation);
					return;
				}
			}
		}
		// if (lastSignal != alpha && rc.getRoundNum() - patience <= 27) {
		// if (rc.canAttackLocation(lastSignal)) {
		// rc.attackLocation(lastSignal);
		// }
		// }
		// if (false && rc.getRoundNum() > 500) {
		// Direction trueAway = alpha.directionTo(here);
		// Direction away = trueAway;
		// MapLocation enemyLocation = here;
		// int count = 0;
		// while (count < 4 || rc.canAttackLocation(enemyLocation.add(away))) {
		// enemyLocation = enemyLocation.add(away);
		// away = (new Direction[] { trueAway, trueAway.rotateLeft(),
		// trueAway.rotateRight() })[rand.nextInt(3)];
		// count++;
		//
		// }
		// if (rc.canAttackLocation(enemyLocation)) {
		// rc.attackLocation(enemyLocation);
		// return;
		// }
		// }
	}
}