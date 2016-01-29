package dune_buggy;

import battlecode.common.*;

public class BotArchon extends Bot {
	static int numScoutsCreated = 0;
	static int numSoldiersCreated = 0;
	static boolean targetIsNeutral;
	static RobotType typeToBuild;
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
		typeToBuild = null;
	}

	private static void turn() throws GameActionException {
		here = rc.getLocation();
		RobotInfo[] hostiles = rc.senseHostileRobots(here, -1);
		Harass.updateInfoFromSignals(rc.emptySignalQueue());
		repairBotMostInNeed();
		if (rc.isCoreReady()) {
			if (activateNeutralIfPossible()) {
				return;
			}
			if (typeToBuild == null)
				determineTypeToBuild();
			updateTargetLocMySelf();
			if (rc.isCoreReady() && rc.hasBuildRequirements(typeToBuild)
					&& (!targetIsNeutral || here.distanceSquaredTo(targetLoc) > type.sensorRadiusSquared || Util.rubbleBetweenHereAndThere(here, targetLoc) > GameConstants.RUBBLE_OBSTRUCTION_THRESH) || numScoutsCreated == 0) {
				buildUnitInDir(here.directionTo(center), typeToBuild);
				typeToBuild = null;
				return;
			}
			updateAndMoveTowardtargetLoc(hostiles);

		}
	}
	private static void determineTypeToBuild() {
		if(numScoutsCreated * 30 <= numSoldiersCreated)
			typeToBuild = RobotType.SCOUT;
		else if(false)
			typeToBuild = RobotType.GUARD;
		else if (false)
			typeToBuild = RobotType.TURRET;
		else
			typeToBuild = RobotType.SOLDIER;
	}
	private static void updateAndMoveTowardtargetLoc(RobotInfo[] hostiles) throws GameActionException {
		NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(hostiles);
		if (targetLoc != null &&targetLoc.equals(here)){
			targetLoc = null;
			targetIsNeutral = false;
		}
		if (targetLoc != null)
			Nav.goTo(targetLoc, theSafety);
	}

	private static boolean activateNeutralIfPossible() throws GameActionException {
		RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		boolean activated = false;
		if (neutrals.length > 0) {
			rc.activate(neutrals[0].location);
			sendNewUnitImportantData();
			if (targetLoc != null && neutrals[0].location.equals(targetLoc)) {
				targetIsNeutral = false;
				targetLoc = null;
			}
			activated = true;
		}
		return activated;
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

	private static void broadcastTargetDen() throws GameActionException {
		int myMsg[];
		int[] dens = {here.x, here.y,here.x, here.y,here.x, here.y};
		MapLocation t;
		int i =0;
		for(int j = 0; j < targetDenSize; j++){
			if(targetDens[j] == null)
				continue;
			if(rc.getMessageSignalCount() == 19)
				break;
			t = targetDens[j];
			dens[(i%3)*2] = t.x;
			dens[(i%3)*2+1] = t.y;
			if(i % 3 == 2){
				//send
				myMsg = MessageEncode.RELAY_DEN_INFO.encode(dens);
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
				dens = new int[]{here.x, here.y,here.x, here.y,here.x, here.y};
			}
			i++;
		}
		myMsg = MessageEncode.RELAY_DEN_INFO.encode(dens);
		rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
	}

	private static boolean updateTargetLocMySelf() throws GameActionException { 
		RobotInfo[] neutrals = rc.senseNearbyRobots(-1, Team.NEUTRAL);
		MapLocation closestLoc = null;
		int smallestDistance = Integer.MAX_VALUE;
		for (RobotInfo ri : neutrals) {
			int distanceToLoc = here.distanceSquaredTo(ri.location);
			if (distanceToLoc < smallestDistance && Util.rubbleBetweenHereAndThere(here, ri.location) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
				closestLoc = ri.location;
				smallestDistance = distanceToLoc;
				targetIsNeutral = true;
			}
		}
		if(closestLoc == null){
			MapLocation[] partLocations = rc.sensePartLocations(-1);
			for (MapLocation loc : partLocations) {
				int distanceToLoc = here.distanceSquaredTo(loc);
				if (distanceToLoc < smallestDistance  && !rc.isLocationOccupied(loc) && Util.rubbleBetweenHereAndThere(here, loc) < rc.senseParts(loc)*10) {
					closestLoc = loc;
					smallestDistance = distanceToLoc;
					targetIsNeutral = false;
				}
			}
		}
		if (closestLoc != null) {
			targetLoc = closestLoc;
			return true;
		}
		return false;
	}
	private static boolean buildUnitInDir(Direction dir, RobotType r) throws GameActionException {// New Util
		for (int i : directionOrder) {
			if (rc.canBuild(Direction.values()[(dir.ordinal()+i+8)%8], r)) {
				rc.build(Direction.values()[(dir.ordinal()+i+8)%8], r);
				sendNewUnitImportantData();
				incrementTypeCount(r);
				typeToBuild = null;
				return true;
			}
		}
		return false;
	}
	private static void incrementTypeCount(RobotType r) {
		switch (r){
		case SCOUT:
			numScoutsCreated++;
			break;
		case SOLDIER:
				numSoldiersCreated++;
			break;
		default:
			break;
		}
	}
	private static void sendNewUnitImportantData() throws GameActionException {
		if (targetDenSize > 0)
			broadcastTargetDen();
	}
}