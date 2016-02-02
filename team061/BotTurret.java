package team061;

import battlecode.common.*;

public class BotTurret extends Bot {
	protected static MapLocation targetLoc;
	protected static MapLocation alpha;
	static MapLocation[] lastScoutNotifiedArray;
	static int currentIndexOfLastArray = 0;
	static int lastTimeTargetChanged;
	protected static int range; // NEW not necessary for mobile
	protected static int turretType; // NEW 0 = turtling; 1 = offensive; 2 = map control?
	protected static boolean isTTM;

	public static void loop(RobotController theRC) throws GameActionException {
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
		// TODO have bot choose what type of turret it is
		// if it is a mobile turret it needs to have a target loc
		chooseTurretType();
		isTTM = false;
		// MessageEncode.getMobileArchonLocation(); //NEW This should be a
		// method
		if (turretType == 0) { // WE ARE TURTLING
			range = 2;
			Signal[] signals = rc.emptySignalQueue();
			for (int i = 0; i < signals.length; i++) {
				if (signals[i].getTeam() == them) {
					continue;
				}
				int[] message = signals[i].getMessage();
				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
				if (signals[i].getTeam() == us && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
					int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(signals[i].getLocation(),
							message);
					alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
					break;
				}
			}
		}
		if (turretType == 1) { // OFFENSIVE
			// MessageEncode.readMessagesAndUpdateInfo();
			targetLoc = center; // for now
			Signal[] signals = rc.emptySignalQueue();
			boolean canSeeHostiles = rc.senseHostileRobots(here, type.sensorRadiusSquared).length > 0;
			Harass.updateTargetLocWithoutSignals();
			// TODO: UPDATE THINGS FROM SIGNALS (see the big Harass.updateInfoFromSignals method)
			// Harass.updateTargetLoc();
			// this should set its target
		}
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		RobotInfo[] enemies = rc.senseHostileRobots(here, type.sensorRadiusSquared);
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
		Signal[] signals = rc.emptySignalQueue();
		boolean canSeeHostiles = rc.senseHostileRobots(here, type.sensorRadiusSquared).length > 0;
		Harass.updateTargetLocWithoutSignals();
		// TODO: UPDATE THINGS FROM SIGNALS (see the big Harass.updateInfoFromSignals method)
//		for(Signal s: signals){
//			Harass.updateTargetLoc(s, canSeeHostiles);
//		}
		//rc.setIndicatorString(1, "target at " + targetLoc.x + ", " + targetLoc.y);

		// MessageEncode.updateRange(); //NEW update the range and get list of
		// possible targets in same loop to conserve bytecode

		if (turretType == 1 && targetLoc != null) {
			if (!isTTM) {
				// shoot anything in range and use scout
				attackIfApplicable(signals);
				if (enemies.length == 0 && type.sensorRadiusSquared < here.distanceSquaredTo(targetLoc)) {
					rc.pack();
					isTTM = true;
				}
			} else {
				if (rc.isCoreReady()) {
					if (RobotType.TURRET.sensorRadiusSquared > here.distanceSquaredTo(targetLoc)) {
						rc.unpack();
						isTTM = false;
					} else {
						//rc.setIndicatorString(2, "moving my butt");
						Nav.goTo(targetLoc, theSafety);
					}
				}
			}
		} else if (turretType == 0) {

			if (!isTTM) {
				attackIfApplicable(signals);
				Boolean rangeUpdated = updateMaxRange(signals);
				if (rc.isCoreReady()) {
					moveToLocFartherThanAlphaIfPossible(here);
				}
			} else {
				if (rc.isCoreReady()) {
					moveToLocFartherThanAlphaIfPossible(here);
				}
			}

		}
	}

	// NEW OPTIMIZE ALL OF THIS

	private static void chooseTurretType() {
		// We need to decide how we're going to choose this
		turretType = 1;
	}

	private static void moveToLocFartherThanAlphaIfPossible(MapLocation here) throws GameActionException {
		Direction dir = Direction.NORTH;
		boolean moved = false;
		boolean startedAsTTM = isTTM;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)
					&& rc.senseRubble(newLoc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				double distanceToAlpha = newLoc.distanceSquaredTo(alpha);
				RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.sensorRadiusSquared);
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
				if (distanceToAlpha > here.distanceSquaredTo(alpha) && distanceToAlpha < range
						&& theSafety.isSafeToMoveTo(newLoc)) {
					if (!isTTM) {
						rc.pack();
						isTTM = true;
						break;
					} else if (rc.canMove(dir)) {
						rc.move(dir);
						moved = true;
						break;
					}
				}
			}
			dir = dir.rotateLeft();
		}
		if (startedAsTTM && !moved) {
			rc.unpack();
			isTTM = false;
		}
	}

	private static boolean updateMaxRange(Signal[] signals) {
		boolean rangeUpdated = false;
		for (int i = 0; i < signals.length; i++) {
			if (signals[i].getTeam() == them) {
				continue;
			}
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.PROXIMITY_NOTIFICATION) {
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(signals[i].getLocation(), message);
				range = decodedMessage[0]; // System.out.println(range);
				rangeUpdated = true;
				break;
			}
		}
		return rangeUpdated;
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
				if (signals[i].getMessage() == null) {
					continue;
				}
				int[] message = signals[i].getMessage();
				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
				int[] decodedMessage = MessageEncode.TURRET_TARGET.decode(signals[i].getLocation(), message);
				if (msgType == MessageEncode.TURRET_TARGET) {
					MapLocation enemyLocation = new MapLocation(decodedMessage[2], decodedMessage[3]);
					if (rc.canAttackLocation(enemyLocation)) {
						rc.attackLocation(enemyLocation);
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
	}

	private static int[] getIndicesOfTargetSignals(Signal[] signals) {
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
