package team061;

import battlecode.common.*;

import java.util.Random;

public class BotArchon extends Bot {
	static MapLocation alpha, hunter;
	static boolean isAlphaArchon;
	static boolean isMobileArchon;
	static int maxRange;
	static int numScoutsCreated = 0;
	static Random rand;
	static RobotType[] robotTypes = { RobotType.SCOUT, RobotType.SOLDIER, RobotType.GUARD, RobotType.VIPER,
			RobotType.TURRET };
	// static int numTurretsCreated = 0;

	//mobile archon fields here:
	static MapLocation targetLocation;//partsLoc, denLoc, neutralLoc;
	static int cautionLevel = 16; //how close a zombie has to be to run away
	static final int NO_SCOUT = -1000;
	static int lastTurnSeenScout = NO_SCOUT;
	static MapLocation[] densToHunt;
	static int numDensToHunt;
	static int denArraySize;
	static boolean huntingDen;
	static int bestIndex;
	static boolean scoutCreated;

	private static boolean checkRubbleAndClear(Direction dir) {

		if (rc.senseRubble(here.add(dir)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
			try {
				rc.clearRubble(dir);
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
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
		rand = new Random(rc.getID());
		Signal[] signals = rc.emptySignalQueue();
		here = rc.getLocation();
		rc.setIndicatorString(0	,"no one special");
		if (!signalsFromOurTeam(signals)) {
			MapLocation myLocation = here;
			int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { myLocation.x, myLocation.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
			isAlphaArchon = true;
			rc.setIndicatorString(0	,"alpha");
			alpha = myLocation;
		} else {
			for (int i = 0; i < signals.length; i++) {
				if(signals[i].getTeam() == us){
					int[] message = signals[i].getMessage(); //can move this outside the if when we deal with signals from other teams
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					int[] decodedMessage = purpose.decode(signals[i].getLocation(), message);
					if(purpose == MessageEncode.ALPHA_ARCHON_LOCATION){
						alpha = new MapLocation(decodedMessage[0], decodedMessage[1]);
					} else if( purpose == MessageEncode.MOBILE_ARCHON_LOCATION){
						hunter = new MapLocation(decodedMessage[0], decodedMessage[1]);break;
					}
				}
			}
			if (hunter == null){
				MapLocation myLocation = rc.getLocation();
				int[] myMsg = MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[] { myLocation.x, myLocation.y });
				rc.broadcastMessageSignal(myMsg[0], myMsg[1], 10000);
				isMobileArchon = true;
				rc.setIndicatorString(0	,"mobile");
				hunter = myLocation;
				densToHunt = new MapLocation[100];
				denArraySize = 0;
				numDensToHunt = 0;
				bestIndex = 0;
				huntingDen = false;
			}
		}
	}

	private static boolean signalsFromOurTeam(Signal[] signals) {
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
			// Choose a random direction to try to build in
			Direction dirToBuild = directions[rand.nextInt(8)];
			//Boolean built = false;
			for (int i = 0; i < 8; i++) {
				// If possible, build in this direction
				if (rc.canBuild(dirToBuild, neededUnit)) {
					rc.build(dirToBuild, neededUnit);
					if (neededUnit == RobotType.SCOUT) {
						numScoutsCreated++;
					}
					// else{
					// numTurretsCreated++;
					// }
					// tell the unit you just created the location
					int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { alpha.x, alpha.y });
					rc.broadcastMessageSignal(myMsg[0], myMsg[1], 3);
				//	built = true;
					break;
				} else if (!checkRubbleAndClear(dirToBuild)) {
					// Rotate the direction to try
					dirToBuild = dirToBuild.rotateLeft();
				}
				else{

					break;
				}
			}

			if (isAlphaArchon && isSurrounded()&&rc.getRoundNum()%10==0) {
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
				// rc.setIndicatorString(0, "Repairing" +
				// mostInNeed.location.toString());
				rc.repair(mostInNeed.location);
			}
		}
	}

	private static void turn(Random rand) throws GameActionException {
		repairBotMostInNeed();
		here = rc.getLocation();
		RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, them);
		if (rc.isCoreReady()) {
			if (isMobileArchon){
				beMobileArchon(enemies);
			} else if (isAlphaArchon || here.distanceSquaredTo(alpha) <= 2) {
				aarons_shitty_strat();
			} else {
				NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
				Nav.goTo(alpha, theSafety);
			}
		}
	}

	private static void updateInfoFromScouts() {
		Signal[] signals = rc.emptySignalQueue();
		for (Signal signal : signals){
			if (signal.getTeam() == us){
				int[] message = signal.getMessage();
				if (message != null){
					MapLocation senderloc = signal.getLocation();
					MessageEncode purpose = MessageEncode.whichStruct(message[0]);
					if (purpose == MessageEncode.DIRECT_MOBILE_ARCHON){
						int[] data = purpose.decode(senderloc, message);
						if(rc.getRoundNum() < roundToStopHuntingDens && numDensToHunt > 0){
							densToHunt[denArraySize] = new MapLocation(data[0],data[1]);
							huntingDen = true;
							numDensToHunt++;
							denArraySize++;
						}
						else{
							if(rc.getRoundNum() < roundToStopHuntingDens){
								huntingDen = true;
								numDensToHunt++;
							}
							huntingDen = false;
							targetLocation = new MapLocation(data[0],data[1]);
						}
					} else if (purpose == MessageEncode.STOP_BEING_MOBILE){
						int[] data = purpose.decode(senderloc, message);
						alpha = new MapLocation(data[0],data[1]);
						isMobileArchon = false;
					}						
				}
			}
		}
	}

	private static void beMobileArchon(RobotInfo[] enemies) throws GameActionException {
		updateInfoFromScouts();
		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
		RobotInfo[] zombies = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, Team.ZOMBIE);
		RobotInfo[] neutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		rc.setIndicatorString(0, "target = " + targetLocation);
		if (rc.getRoundNum() == roundToStopHuntingDens) {// found in Bot class
			int[] msg = MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[] { here.x, here.y });
			rc.broadcastMessageSignal(msg[0], msg[1], 10000);
		} 
		else if(huntingDen){//behavior is very different
			if(rc.canSenseLocation(targetLocation)){//within range of den
				RobotInfo ri = rc.senseRobotAtLocation(targetLocation);
				if(ri == null){//den is dead
					targetLocation = null;
					densToHunt[bestIndex] = null;
					numDensToHunt--;
					huntingDen = false;
				}
				else if(here.distanceSquaredTo(targetLocation) > cautionLevel)
					Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemies, zombies)));
			}
			else{
				Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemies, zombies)));
			}
		}
		else {
			if (neutrals.length > 0) {
				rc.activate(neutrals[0].location);
				if (targetLocation != null && neutrals[0].location.equals(targetLocation)) {
					targetLocation = null;
					huntingDen = false;
				}
			}
			if (targetLocation != null && targetLocation.equals(here)){
				targetLocation = null;
				huntingDen = false;
			}
			if (rc.isCoreReady() && inDanger(allies, enemies, zombies))
				flee(allies, enemies, zombies);
			else if (rc.isCoreReady()) {
				if (isMobileScoutNeeded(allies)) {
					if (buildUnitInDir(directions[rand.nextInt(8)], RobotType.SCOUT)) {
						scoutCreated = true;
					}
				}
				if (targetLocation == null) {// no known things worth pursuing
					if (!haveEnoughFighters(allies))
						buildUnitInDir(directions[rand.nextInt(8)], RobotType.GUARD);
					else{
						if(numDensToHunt > 0){
							bestIndex = Util.closestLocation(densToHunt, here, denArraySize);
							targetLocation = densToHunt[bestIndex];
							huntingDen = true;
						}
						if(updateTargetLocationMySelf())
							Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemies, zombies)));
					}
				} 
				else {
					Nav.goTo(targetLocation, new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemies, zombies)));
				}

			}
		}
	}

	//	private static MapLocation chooseNextTarget(RobotInfo[] allies, RobotInfo[] zombies){
	//		// TODO: may be keep track of more possible targets, also may want to switch priorities
	//		MapLocation closest = partsLoc;
	//		if(denLoc != null && (closest == null || here.distanceSquaredTo(denLoc) < here.distanceSquaredTo(closest)))
	//			closest = denLoc;
	//		if(neutralLoc != null && (closest == null || here.distanceSquaredTo(neutralLoc) < here.distanceSquaredTo(closest)))
	//			closest = neutralLoc;
	//		if(closest == denLoc){
	//			//actually want to stay kinda far away from denLoc
	//			if (here.distanceSquaredTo(denLoc) < cautionLevel)
	//				closest = null;
	//		}
	//		return closest;
	//	}

	private static boolean updateTargetLocationMySelf() throws GameActionException{
		MapLocation[] possibleLocs = here.getAllMapLocationsWithinRadiusSq(here, RobotType.ARCHON.sensorRadiusSquared); 
		MapLocation closestLoc = null;
		int smallestDistance = 1000000;
		for(MapLocation loc: possibleLocs){
			if(!rc.canSense(loc)){
				continue;
			}
			int distanceToLoc = here.distanceSquaredTo(loc);
			if(rc.senseParts(loc) > 0 ){
				if(distanceToLoc < smallestDistance){
					closestLoc = loc;
					smallestDistance = distanceToLoc;
				}
			}
			else{
				RobotInfo ri = rc.senseRobotAtLocation(loc);
				if(ri != null && ri.team == Team.NEUTRAL){
					if(distanceToLoc < smallestDistance){
						closestLoc = loc;
						smallestDistance = distanceToLoc;
					}
				}
			}
		}
		if(closestLoc != null){
			targetLocation = closestLoc;
			huntingDen = false;
			return true;
		}
		return false;
	}

	private static boolean haveEnoughFighters(RobotInfo[] allies){
		int fighters = 0;
		for (RobotInfo a: allies)
			if (a.type == RobotType.GUARD || a.type == RobotType.SOLDIER)
				fighters++;
		return fighters >= 5;
	}

	private static boolean inDanger(RobotInfo[] allies, RobotInfo[]enemies, RobotInfo[] zombies){
		if( enemies.length > 0
				|| zombies.length > allies.length
				|| zombies.length > 0 && here.distanceSquaredTo(Util.closest(zombies, here).location) < cautionLevel)
			return true;
		return false;
	}

	private static void flee(RobotInfo[] allies, RobotInfo[] enemies, RobotInfo[] zombies)throws GameActionException{
		// TODO: make it try to maximize the number of units protected too
		RobotInfo[] unfriendly = Util.combineTwoRIArrays(enemies, zombies);
		Direction runAway = Util.centroidOfUnits(unfriendly).directionTo(here);
		Nav.moveInDir(runAway, new SafetyPolicyAvoidAllUnits(unfriendly));
		rc.setIndicatorString(3	,"AHHHHHHHHH I'M TRAPPED :(");
		// meat shield
		if(rc.hasBuildRequirements(RobotType.SOLDIER) && rc.isCoreReady()){
			buildUnitInDir(runAway.opposite(), RobotType.SOLDIER);
		}
		//if that doesn't work.... idk what to do
	}

	private static boolean buildUnitInDir(Direction dir, RobotType r)throws GameActionException{
		dir = dir.rotateLeft();
		for (int i = 0; i< 8; i++){
			if( rc.canBuild(dir, r) && rc.isCoreReady()){
				rc.build(dir, r);
				notifyNewUnitOfCreator();
				return true;
			}
			dir = dir.rotateRight();
		}
		return false;
	}

	private static void notifyNewUnitOfCreator()throws GameActionException{
		if(isMobileArchon){
			int[] myMsg = MessageEncode.MOBILE_ARCHON_LOCATION.encode(new int[] { here.x, here.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
		} else if(isAlphaArchon){
			int[] myMsg = MessageEncode.ALPHA_ARCHON_LOCATION.encode(new int[] { here.x, here.y });
			rc.broadcastMessageSignal(myMsg[0], myMsg[1], 2);
		}
	}

	private static void aarons_shitty_strat() throws GameActionException {
		// alpha archon created scouts

		RobotType needed = RobotType.TURRET;
		if (isScoutNeeded()) {
			needed = RobotType.SCOUT;
		}
		constructNeededUnits(needed);

	}
	
	private static boolean isMobileScoutNeeded(RobotInfo[] teammates) throws GameActionException{
		if(!scoutCreated){
			return true;
		}
		if(rc.getRoundNum() > roundToStopHuntingDens + 50){
			for (int i = 0; i < teammates.length; i++) {
				if (teammates[i].type == RobotType.SCOUT)
					return false;
			}
			return true;
		}
		return false;
	}

	private static boolean isScoutNeeded() {
		RobotInfo[] teammates = rc.senseNearbyRobots(RobotType.ARCHON.sensorRadiusSquared, us);
		int nearbyScouts = 0;
		for (int i = 0; i < teammates.length; i++) {
			if (teammates[i].type == RobotType.SCOUT) {
				nearbyScouts++;
			}
		}
		if (numScoutsCreated < 10 && nearbyScouts < (double) teammates.length / 5.0) {
			return true;
		}
		return false;
	}
}
