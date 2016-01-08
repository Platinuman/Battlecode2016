package team061;

import java.util.Random;

import battlecode.common.*;

public class BotScout extends Bot {
	static MapLocation alpha;
	static MapLocation[] preferredScoutLocations;
	static boolean atScoutLocation;
	public static void loop(RobotController theRC) throws GameActionException {
		Bot.init(theRC);
		// Debug.init("micro");
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
				// check if its an archon signal
			}
		}
		MapLocation[] preferredScoutLocations = {alpha.add(2,2), alpha.add(2,-2),alpha.add(-2,2),alpha.add(-2,-2)};
	}
	private static void turn() throws GameActionException {
		here = rc.getLocation();
		RobotInfo[] enemyRobots = rc.senseHostileRobots(rc.getLocation(), RobotType.SCOUT.sensorRadiusSquared);
		for (int i = 0; i < enemyRobots.length; i++) {
			MapLocation loc = enemyRobots[i].location;
			double health = enemyRobots[i].health;
			RobotType type = enemyRobots[i].type;
			int[] message = MessageEncode.TURRET_TARGET.encode(new int[]{(int)(health), type.ordinal(), loc.x, loc.y});
			rc.broadcastMessageSignal(message[0],message[1],(int)(RobotType.SCOUT.sensorRadiusSquared*GameConstants.BROADCAST_RANGE_MULTIPLIER));
		}
		
		
		if (!atScoutLocation){
			for (int i = 0; i < preferredScoutLocations.length ; i++){
				if (preferredScoutLocations[i].equals(here)){
					atScoutLocation = true;
				}
			}
			for (int i = 0; i < preferredScoutLocations.length ; i++){
				if(!rc.isLocationOccupied(preferredScoutLocations[i])){
					NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemyRobots);
					Nav.goTo(preferredScoutLocations[i], theSafety);
				}
			}
			
			
		}
		
	}
}
    	
