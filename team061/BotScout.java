package team061;

import java.util.Random;

import battlecode.common.*;

public class BotScout extends Bot {
	static MapLocation alpha;
	static MapLocation[] preferredScoutLocations;
	static MapLocation dest;
	static int range;
	static boolean atScoutLocation;
	static boolean firstTurn = true;

	public static void loop(RobotController theRC) throws GameActionException {
		if (firstTurn) {
			firstTurn = false;
			Clock.yield();
		}
		// Debug.init("micro");
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
		atScoutLocation = false;
		range = 3;
		Signal[] signals = rc.emptySignalQueue();
		for (int i = 0; i < signals.length; i++) {
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
				int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(signals[i].getLocation(), message);
				alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
				//rc.setIndicatorString(0	,"i have an alpha");
				break;
			}
		}
		/*
		preferredScoutLocations = new MapLocation[] { alpha.add(2, 2), alpha.add(2, -2), alpha.add(-2, 2),
				alpha.add(-2, -2), alpha.add(4, 2), alpha.add(4, -2), alpha.add(-4, 2), alpha.add(-4, -2),
				alpha.add(2, 4), alpha.add(2, -4), alpha.add(-2, 4), alpha.add(-2, -4) };
		*/
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		for (int i = 0; i < enemyRobots.length; i++) {
			if(i == 20){
				break;
			}
			MapLocation loc = enemyRobots[i].location;
			double health = enemyRobots[i].health;
			RobotType type = enemyRobots[i].type;
			int[] message = MessageEncode.TURRET_TARGET.encode(new int[] { (int) (health), type.ordinal(), loc.x, loc.y });
			rc.broadcastMessageSignal(message[0], message[1], (int) (RobotType.SCOUT.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
		}
		Signal[] signals = rc.emptySignalQueue();
		Boolean rangeUpdated = updateMaxRange(signals);
		if (rc.isCoreReady()){
			moveToLocFartherThanAlphaIfPossible(here);
		}
		/*
		if (!atScoutLocation) {
			for (int i = 0; i < preferredScoutLocations.length; i++) {
				if (preferredScoutLocations[i].equals(here)) {
					atScoutLocation = true;
				}
			}
		}

		if (!atScoutLocation && dest == null) {
			if (rc.isCoreReady()) {
				for (int i = 0; i < preferredScoutLocations.length; i++) {
					MapLocation scoutLocation = preferredScoutLocations[i];
					if (rc.canSense(scoutLocation)) {
						if (!rc.isLocationOccupied(scoutLocation) && rc.onTheMap(scoutLocation)) {
							NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
							if(theSafety.isSafeToMoveTo(scoutLocation)){
								dest = scoutLocation;
								Nav.goTo(scoutLocation, theSafety);
							}
						}
					}
				}
			}
		}
		else if (!atScoutLocation && rc.isCoreReady()){
			NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
			if(theSafety.isSafeToMoveTo(dest)){
				Nav.goTo(dest, theSafety);
			}
			else{
				dest = null;
			}
		}
		*/
	}
	
	private static void moveToLocFartherThanAlphaIfPossible(MapLocation here) throws GameActionException {
		Direction dir = Direction.NORTH;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if (rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc) && rc.senseRubble(newLoc)<GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				double distanceToAlpha = newLoc.distanceSquaredTo(alpha);
				RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.TURRET.sensorRadiusSquared);
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
				if(distanceToAlpha > here.distanceSquaredTo(alpha) && distanceToAlpha < range && theSafety.isSafeToMoveTo(newLoc)){
					if (rc.canMove(dir)){
						rc.move(dir);
						break;
					}
				}
			}
			dir = dir.rotateLeft();
		}
	}

	private static boolean updateMaxRange(Signal[] signals) {
		boolean rangeUpdated = false;
		for (int i = 0; i < signals.length; i++) {
			if(signals[i].getTeam() == them){
				continue;
			}
			int[] message = signals[i].getMessage();
			MessageEncode msgType = MessageEncode.whichStruct(message[0]);
			if (signals[i].getTeam() == us && msgType == MessageEncode.PROXIMITY_NOTIFICATION) {
				int[] decodedMessage = MessageEncode.PROXIMITY_NOTIFICATION.decode(signals[i].getLocation(), message);
				range = decodedMessage[0] - 1;
				//System.out.println(range);
				rangeUpdated = true;
				break;
			}
		}
		return rangeUpdated;
	}
}