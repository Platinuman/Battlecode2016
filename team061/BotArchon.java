package team061;

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
		Random rand = new Random(rc.getID());
		while (true) {
			try {
				turn(rand);
				// Direction dir = chooseMoveLocAndDir(rc.getLocation());
				// rc.move(dir);
				// RobotInfo[] ourUnits = rc.senseNearbyRobots(attackRadiusSq,
				// us);
				// RobitType neededUnit = checkNeededUnits(ourUnits);
				// constructNeededUnits(neededUnits);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	private static void init() throws GameActionException {
		maxRange = 2;
		Signal[] signals = rc.emptySignalQueue();
		if (!signalsFromOurTeam(signals)) {
			MapLocation myLocation = rc.getLocation();
			int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { myLocation.x, myLocation.y });
			rc.broadcastMessageSignal(myMsg[0],myMsg[1],10000);
			isAlphaArchon = true;
			alpha = myLocation;
		} else {
			for (int i = 0; i < signals.length; i++) {
				int[] message = signals[i].getMessage();
				MessageEncode msgType = MessageEncode.whichStruct(message[0]);
				if (signals[i].getTeam() == us && msgType == MessageEncode.ALPHA_ARCHON_LOCATION) {
					int[] decodedMessage = MessageEncode.ALPHA_ARCHON_LOCATION.decode(message);
					alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
					break;
				}
			}
			isAlphaArchon = false;
		}
	}

	private static Boolean signalsFromOurTeam(Signal[] signals) {
		if(signals.length == 0){
			return false;
		}
		else{
			for (Signal sig: signals) {
				if(sig.getTeam() == us)
					return true;
			}
		}
		return false;
		
	}

	private static void constructNeededUnits(RobotType neededUnit) throws GameActionException {//TODO if ur the alpha broadcast after u create // after u create if theres 8 spots increase max range by 1/2
		// Check for sufficient parts
		if (rc.hasBuildRequirements(neededUnit)) {
			// Choose a random direction to try to build in
			Direction dirToBuild = Direction.NORTH;
			Boolean built = false;
			for (int i = 0; i < 8; i++) {
				// If possible, build in this direction
				if (rc.canBuild(dirToBuild, neededUnit)) {
					rc.build(dirToBuild, neededUnit);
					if (neededUnit == RobotType.SCOUT) {
						numScoutsCreated++;
					}
					// tell the unit you just created the location
					int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y });
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 3);
					built = true;
					break;
				} else {
					// Rotate the direction to try
					dirToBuild = dirToBuild.rotateLeft();
				}
			}
			if(built && isAlphaArchon && isSurrounded()){
				maxRange++;
				int[] message = MessageEncode.PROXIMITY_NOTIFICATION.encode(new int[]{maxRange});
				rc.broadcastMessageSignal(message[0], message[1], maxRange);
			}
		}

	}

	private static boolean isSurrounded() throws GameActionException {
		Direction dir = Direction.NORTH;
		Boolean surrounded = true;
		for (int i = 0; i < 8; i++) {
			MapLocation newLoc = here.add(dir);
			if(rc.onTheMap(newLoc) && !rc.isLocationOccupied(newLoc)){
				surrounded = false;
				break;
			}
			dir = dir.rotateLeft();
		}
		return surrounded;
	}

	private static void checkNeededUnits(RobotInfo[] ourUnits) {
		// We need to pick unit ratios
		// Then produce more of whatever is needed most to achieve that ratio
	}

	private static void chooseMoveLocAndDir(MapLocation loc) {
		// If enemies are near retreat
		// return opposite dir of nearest enemy
		// If scrap is near take it
		// If scrap && enemies aren't near move towards nearest scrap
		// return dir of nearest scrap
	}

	private static void repairBotMostInNeed() throws GameActionException {
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.attackRadiusSquared, us);
		if (allies.length > 0) {
			RobotInfo mostInNeed = Util.leastHealth(allies, 1);
			if (mostInNeed != null) {
				rc.setIndicatorString(0, "Repairing" + mostInNeed.location.toString());
				rc.repair(mostInNeed.location);
			}
		}
	}

	private static void turn(Random rand) throws GameActionException {
		Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
				Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
		RobotType[] robotTypes = { RobotType.SCOUT, RobotType.SOLDIER, RobotType.GUARD, RobotType.VIPER,
				RobotType.TURRET };
		repairBotMostInNeed();
		here = rc.getLocation();
		// int fate = rand.nextInt(1000);
		// Check if this ARCHON's core is ready
		// if (fate % 10 == 2) {
		// Send a message signal containing the data (6370, 6147)
		// rc.broadcastMessageSignal(6370, 6147, 80);
		// }
		/*
		 * if (signals.length > 0) { // Set an indicator string that can be
		 * viewed in the client rc.setIndicatorString(0,
		 * "I received a signal this turn!"); } else { rc.setIndicatorString(0,
		 * "I don't any signal buddies"); } if (rc.isCoreReady()) { if (fate <
		 * 800) { // Choose a random direction to try to move in Direction
		 * dirToMove = directions[fate % 8]; // Check the rubble in that
		 * direction if (rc.senseRubble(rc.getLocation().add(dirToMove)) >=
		 * GameConstants.RUBBLE_OBSTRUCTION_THRESH) { // Too much rubble, so I
		 * should clear it rc.clearRubble(dirToMove); // Check if I can move in
		 * this direction } else if (rc.canMove(dirToMove)) { // Move
		 * rc.move(dirToMove); } } else { // Choose a random unit to build
		 * RobotType typeToBuild = robotTypes[fate % 8]; // Check for sufficient
		 * parts if (rc.hasBuildRequirements(typeToBuild)) { // Choose a random
		 * direction to try to build in Direction dirToBuild =
		 * directions[rand.nextInt(8)]; for (int i = 0; i < 8; i++) { // If
		 * possible, build in this direction if (rc.canBuild(dirToBuild,
		 * typeToBuild)) { rc.build(dirToBuild, typeToBuild); break; } else { //
		 * Rotate the direction to try dirToBuild = dirToBuild.rotateLeft(); } }
		 * } }
		 */
		RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, them);
		if (rc.isCoreReady()) {
			
			if (isAlphaArchon || here.distanceSquaredTo(alpha) <= 2) {
				aarons_shitty_strat();
			} else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(alpha, theSafety);
			}
		}
	}

	private static void aarons_shitty_strat() throws GameActionException {
		// alpha archon created scouts
		RobotType needed = RobotType.TURRET;
		if(isAlphaArchon && numScoutsCreated < 4){
			needed = RobotType.SCOUT;
		}
		constructNeededUnits(needed);

	}
}
