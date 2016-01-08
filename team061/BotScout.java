package team061;

import java.util.Random;

import battlecode.common.*;

public class BotScout extends Bot {
	static MapLocation alpha;
	static MapLocation[] preferredScoutLocations;
	static MapLocation dest;
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
		preferredScoutLocations = new MapLocation[] { alpha.add(2, 2), alpha.add(2, -2), alpha.add(-2, 2),
				alpha.add(-2, -2), alpha.add(4, 2), alpha.add(4, -2), alpha.add(-4, 2), alpha.add(-4, -2),
				alpha.add(2, 4), alpha.add(2, -4), alpha.add(-2, 4), alpha.add(-2, -4) };
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		for (int i = 0; i < enemyRobots.length; i++) {
			MapLocation loc = enemyRobots[i].location;
			double health = enemyRobots[i].health;
			RobotType type = enemyRobots[i].type;
			int[] message = MessageEncode.TURRET_TARGET.encode(new int[] { (int) (health), type.ordinal(), loc.x, loc.y });
			rc.broadcastMessageSignal(message[0], message[1], (int) (RobotType.SCOUT.sensorRadiusSquared * GameConstants.BROADCAST_RANGE_MULTIPLIER));
		}

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

	}
}