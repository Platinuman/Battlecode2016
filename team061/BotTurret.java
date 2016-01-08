package team061;

import battlecode.common.*;

public class BotTurret extends Bot {
	static MapLocation alpha;
	static int range;
	static boolean isTTM;
	static boolean shouldMove;
	static boolean firstTurn = true;

	public static void loop(RobotController theRC) throws GameActionException {
		if(firstTurn){
			firstTurn = false;
			Clock.yield();
		}
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
		isTTM = false;
		range = 2;
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
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
		if (!isTTM) {
			Signal[] signals = rc.emptySignalQueue();
			attackIfApplicable(signals);
			Boolean rangeUpdated = updateMaxRange(signals);
			if (rangeUpdated || shouldMove) {
				if (rc.isCoreReady()) {
					moveToLocFartherThanAlphaIfPossible(here);
					shouldMove = false;
				}
				else{
					shouldMove = true;
				}
			}
		}
		else{
			if(rc.isCoreReady()){
				moveToLocFartherThanAlphaIfPossible(here);
				rc.unpack();
				isTTM = false;
			}
		}
		// if the maxrange was sent update it // only do this if you are not a
		// ttm
		// if ur core is still ready (no enemies), and there is a u can move to
		// a location that is farther from the alpha, (but also safe and within
		// max range, pack up and move there
	}

	private static void moveToLocFartherThanAlphaIfPossible(MapLocation here) throws GameActionException {
		Direction dir = Direction.NORTH;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)) {
				double distanceToAlpha = newLoc.distanceSquaredTo(alpha);
				if(rc.canMove(dir) && distanceToAlpha > here.distanceSquaredTo(alpha) && distanceToAlpha < range*range){
					rc.pack();
					isTTM = true;
					break;
				}
			}
			dir = dir.rotateLeft();
		}
	}

	private static boolean updateMaxRange(Signal[] signals) {
		boolean rangeUpdated = false;
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.PROXIMITY_NOTIFICATION) {
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(message);
				range = decodedMessage[0];
				rangeUpdated = true;
				break;
			}
		}
		return rangeUpdated;
	}

	private static void attackIfApplicable(Signal[] signals) throws GameActionException {
		if (rc.isWeaponReady()) {
			Combat.shootAtNearbyEnemies();
			if (rc.isWeaponReady()) {
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