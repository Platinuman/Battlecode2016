package team061;

import battlecode.common.*;

interface NavSafetyPolicy {
	public boolean isSafeToMoveTo(MapLocation loc);
}

class SafetyPolicyAvoidAllUnits extends Bot implements NavSafetyPolicy {

	RobotInfo[] nearbyEnemies;

	public SafetyPolicyAvoidAllUnits(RobotInfo[] nearbyEnemies) {
		this.nearbyEnemies = nearbyEnemies;
	}

	public boolean isSafeToMoveTo(MapLocation loc) {
		for (RobotInfo enemy : nearbyEnemies) {
			switch (enemy.type) {
			case ARCHON:
				break;
			case ZOMBIEDEN:
				if (enemy.type.attackRadiusSquared + 2 >= loc.distanceSquaredTo(enemy.location)
						- ((type == RobotType.ARCHON) ? 24 : 0))// hardcoded
					return false;
				break;
			case TURRET:
				if (enemy.type.attackRadiusSquared +((type != RobotType.SCOUT)? 10 + ((type == RobotType.ARCHON) ? 30 : 0) : 0) >= loc.distanceSquaredTo(enemy.location)){
					return false;
				}
				break;
			default:
				if (enemy.type.attackRadiusSquared +((enemy.type != RobotType.SCOUT && type == RobotType.SCOUT)
						? 10 : 0) >= loc.distanceSquaredTo(enemy.location))// hardcoded
					return false;
				break;
			}
		}
		return true;
	}
}

public class Nav extends Bot {

	private static MapLocation dest;
	private static NavSafetyPolicy safety;

	private enum BugState {
		DIRECT, BUG
	}

	public enum WallSide {
		LEFT, RIGHT
	}

	private static BugState bugState;
	public static WallSide bugWallSide = null;
	private static int bugStartDistSq;
	private static Direction bugLastMoveDir;
	private static Direction bugLookStartDir;
	private static int bugRotationCount;
	private static int bugMovesSinceSeenObstacle = 0;
	private static int bugMovesSinceMadeProgress = 0;
	private static Direction lastRetreatDir;
	private static boolean move(Direction dir) throws GameActionException {
		if (rc.canMove(dir)) {
			if(type == RobotType.SCOUT || type == RobotType.TTM || type == RobotType.TURRET|| rc.senseRubble(here.add(dir)) < GameConstants.RUBBLE_SLOW_THRESH){
			rc.move(dir);
			}
			else{
				rc.clearRubble(dir);
			}
			return true;
		}
		return false;
	}

	private static boolean checkRubble(int threshold) {
		if (rc.getType() == RobotType.TTM) {
			return false;
		}
		double rubbleCount = Util.rubbleBetweenHereAndThere(here, dest);
		return rubbleCount <= threshold;
	}

	private static boolean canMove(Direction dir) {
		return rc.canMove(dir) && safety.isSafeToMoveTo(here.add(dir));
	}

	private static boolean tryMoveDirect() throws GameActionException {
		Direction toDest = here.directionTo(dest);

		if (canMove(toDest)) {
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[2];
		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		if (here.add(dirLeft).distanceSquaredTo(dest) < here.add(dirRight).distanceSquaredTo(dest)) {
			dirs[0] = dirLeft;
			dirs[1] = dirRight;
		} else {
			dirs[0] = dirRight;
			dirs[1] = dirLeft;
		}
		for (Direction dir : dirs) {
			if (canMove(dir)) {
				move(dir);
				return true;
			}
		}
		return false;
	}

	private static void startBug() throws GameActionException {
		bugStartDistSq = here.distanceSquaredTo(dest);
		bugLastMoveDir = here.directionTo(dest);
		bugLookStartDir = here.directionTo(dest);
		bugRotationCount = 0;
		bugMovesSinceSeenObstacle = 0;
		bugMovesSinceMadeProgress = 0;
		if (bugWallSide == null) {
			// try to intelligently choose on which side we will keep the wall
			Direction leftTryDir = bugLastMoveDir.rotateLeft();
			for (int i = 0; i < 3; i++) {
				if (!canMove(leftTryDir))
					leftTryDir = leftTryDir.rotateLeft();
				else
					break;
			}
			Direction rightTryDir = bugLastMoveDir.rotateRight();
			for (int i = 0; i < 3; i++) {
				if (!canMove(rightTryDir))
					rightTryDir = rightTryDir.rotateRight();
				else
					break;
			}
			if (dest.distanceSquaredTo(here.add(leftTryDir)) < dest.distanceSquaredTo(here.add(rightTryDir))) {
				bugWallSide = WallSide.RIGHT;
			} else {
				bugWallSide = WallSide.LEFT;
			}
		}

	}

	private static Direction findBugMoveDir() throws GameActionException {
		bugMovesSinceSeenObstacle++;
		Direction dir = bugLookStartDir;
		for (int i = 8; i-- > 0;) {
			if (canMove(dir))
				return dir;
			dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir.rotateLeft());
			bugMovesSinceSeenObstacle = 0;
		}
		return null;
	}

	private static int numRightRotations(Direction start, Direction end) {
		return (end.ordinal() - start.ordinal() + 8) % 8;
	}

	private static int numLeftRotations(Direction start, Direction end) {
		return (-end.ordinal() + start.ordinal() + 8) % 8;
	}

	private static int calculateBugRotation(Direction moveDir) {
		if (bugWallSide == WallSide.LEFT) {
			return numRightRotations(bugLookStartDir, moveDir) - numRightRotations(bugLookStartDir, bugLastMoveDir);
		} else {
			return numLeftRotations(bugLookStartDir, moveDir) - numLeftRotations(bugLookStartDir, bugLastMoveDir);
		}
	}

	private static void bugMove(Direction dir) throws GameActionException {
		if (move(dir)) {
			bugRotationCount += calculateBugRotation(dir);
			bugLastMoveDir = dir;
			if (bugWallSide == WallSide.LEFT)
				bugLookStartDir = dir.rotateLeft().rotateLeft();
			else
				bugLookStartDir = dir.rotateRight().rotateRight();
		}
	}

	private static boolean detectBugIntoEdge() throws GameActionException {
		if (bugWallSide == WallSide.LEFT) {
			return !rc.onTheMap(here.add(bugLastMoveDir.rotateLeft()));
		} else {
			return !rc.onTheMap(here.add(bugLastMoveDir.rotateRight()));
		}
	}

	private static void reverseBugWallFollowDir() throws GameActionException {
		bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
		startBug();
	}

	private static void bugTurn() throws GameActionException {
		if (detectBugIntoEdge()) {
			reverseBugWallFollowDir();
		}
		Direction dir = findBugMoveDir();
		if (dir != null) {
			bugMove(dir);
		}
	}

	private static boolean canEndBug() {
		if (bugMovesSinceSeenObstacle >= 4)
			return true;
		return (bugRotationCount <= 0 || bugRotationCount >= 8) && here.distanceSquaredTo(dest) <= bugStartDistSq;
	}

	private static void bugMove() throws GameActionException {
		// Check if we can stop bugging at the *beginning* of the turn
//		rc.setIndicatorString(2, "I've been bugging for " +bugMovesSinceMadeProgress+ "turns.");
//		rc.setIndicatorString(1, "bugMovesSinceSeenObstacle = " +
//				 bugMovesSinceSeenObstacle + "; bugRotatoinCount = " +
//				 bugRotationCount);
		if (bugState == BugState.BUG) {
			if (canEndBug()) {
				bugState = BugState.DIRECT;
				bugMovesSinceMadeProgress = 0;
			}
		}

		// If DIRECT mode, try to go directly to target

		if (bugState == BugState.DIRECT) {
			if (!tryMoveDirect()) {
				if (type != RobotType.SCOUT && type != RobotType.TURRET && type != RobotType.TTM &&  rc.onTheMap(here.add(here.directionTo(dest))) && !rc.isLocationOccupied(here.add(here.directionTo(dest)))&&checkRubble(2000)) {
					rc.clearRubble(here.directionTo(dest));
				} else {
					bugState = BugState.BUG;
					startBug();
				}
			}
			// checkRubbleAndClear(here.directionTo(dest));
		}
		if (rc.isCoreReady() &&  type != RobotType.SCOUT && type != RobotType.TURRET && type != RobotType.TTM) {
			if (here.distanceSquaredTo(dest) < type.attackRadiusSquared) {
				Util.checkRubbleAndClear(here.directionTo(dest), true);
				return;
			} else if (bugState == BugState.BUG && bugMovesSinceMadeProgress > 20) {
				if (Util.checkRubbleAndClear(here.directionTo(dest), true)) {
					return;
				}
			}

		}
		// If that failed, or if bugging, bug
		
		if (bugState == BugState.BUG) {
			bugTurn();
			bugMovesSinceMadeProgress++;
		}
	}

	// private static void runAway() throws GameActionException {
	// Direction away =
	// here.directionTo(Util.centroidOfUnits(rc.senseHostileRobots(here, -1)));
	// if (rc.canMove(away)) {
	// rc.move(away);
	// } else if (rc.canMove(away.rotateLeft())) {
	// rc.move(away.rotateLeft());
	// } else if (rc.canMove(away.rotateRight())) {
	// rc.move(away.rotateRight());
	// }
	// }

	public static void goTo(MapLocation theDest, NavSafetyPolicy theSafety) throws GameActionException {
		if (!theDest.equals(dest)) {
			dest = theDest;
			bugState = BugState.DIRECT;
			bugMovesSinceMadeProgress = 0;
		}

		if (here.equals(dest))
			return;

		safety = theSafety;

		bugMove();
	}

	private static boolean tryMoveDirectScout(Direction toDest) throws GameActionException {
		// Direction toDest = here.directionTo(dest);

		if (canMove(toDest) && rc.onTheMap(here.add(toDest, (int) (Math.sqrt(type.sensorRadiusSquared / 2.0))))) {
			move(toDest);
			return true;
		}

		Direction[] dirs = new Direction[((BotScout.patience > BotScout.PATIENCESTART / 2) ? 4 : 2)];
		Direction dirLeft = toDest.rotateLeft();
		Direction dirRight = toDest.rotateRight();
		dirs[0] = dirLeft;
		dirs[1] = dirRight;
		if(BotScout.patience > BotScout.PATIENCESTART / 2){
			dirs[2] = dirLeft.rotateLeft();
			dirs[3] = dirRight.rotateRight();
		}
		boolean notSafeToMoveAhead = false;
		for (Direction dir : dirs) {
			if(dir == directionIAmMoving){
				if (canMove(dir) )
					if( rc.onTheMap(here.add(dir,(int) (Math.sqrt(type.sensorRadiusSquared / 2.0))))) {
						move(dir);
						return true;
					}
					else notSafeToMoveAhead = true;
			}else if (canMove(dir)
					&& (notSafeToMoveAhead || rc.onTheMap(here.add(dir,(int) (Math.sqrt(type.sensorRadiusSquared / 2.0)))))) {
				move(dir);
				return true;
			}
		}
		return false;
	}

	/*	public static void fleeNumerical(RobotInfo[]unfriendly){
		Direction bestRetreatDir = chooseNumericalyRetreat(unfriendly);
		if (bestRetreatDir != null && rc.isCoreReady() && rc.canMove(bestRetreatDir)) {
			rc.move(bestRetreatDir);
		}
		}
	private static Direction chooseNumericalyRetreat(RobotInfo[] unfriendly) {
		Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
				Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
		int[] directionWeights = {0,0,0,0,0,0,0,0};
		for(int i =0;i<directionWeights.length;i++){
		directionWeights[i] =
				+ // #enemies that can shoot this spot
				+ // rubble level
				+ // how soon you will run into wall
				+ //
				+ ((type == RobotType.VIPER && enemy.viperInfectedTurns == 0)?50:0);
		}
		return bestWeight(directions,directionWeights);
	}
	private static Direction bestWeight(Direction[] directions,int[] directionWeights){
		int bestIndex = Integer.MIN_VALUE;
		for(int i = 0; i < directionWeights.length; i++) {
			if(directionWeights[i] > directionWeights[bestIndex]) {
				bestIndex = i;
			}
		}
		return directions[bestIndex];
	}
	*/
	public static void flee(RobotInfo[] unfriendly,RobotInfo[] allies) throws GameActionException {
		Direction bestRetreatDir = null;
		RobotInfo currentClosestEnemy = Util.closest(unfriendly, here);
		double bestDistSq = -10000;
		boolean spotToClear = false;
		for (Direction dir : Direction.values()) {
			MapLocation retreatLoc = here.add(dir);
			if (!rc.canMove(dir)){
				if(rc.senseRubble(retreatLoc)>GameConstants.RUBBLE_OBSTRUCTION_THRESH &&type!= RobotType.SCOUT && type!= RobotType.TURRET && type!= RobotType.TTM&& !rc.isLocationOccupied(retreatLoc))
					spotToClear = true;
				continue;
			}
			double turretMod = 0;
			if (isInRangeOfTurrets(retreatLoc)) {
				if (spotToClear)
					continue;
				else
					turretMod = 100;
			}

			RobotInfo closestEnemy = Util.closest(unfriendly, retreatLoc);
			int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
			double rubble = rc.senseRubble(retreatLoc);
			double rubbleMod = rubble<GameConstants.RUBBLE_SLOW_THRESH?0:rubble*2.3/GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			double wallMod = wallModCalc(retreatLoc,dir);
			double allyMod = Harass.numOtherAlliesInAttackRange(here.add(dir), allies);
			//rc.setIndicatorString(2, ""+rubbleMod);
			if (distSq-rubbleMod-turretMod+wallMod+allyMod > bestDistSq) {
				bestDistSq = distSq-rubbleMod+wallMod+allyMod-turretMod;
				bestRetreatDir = dir;
			}
		}
		if (bestRetreatDir != null) {
			rc.move(bestRetreatDir);
			lastRetreatDir = bestRetreatDir;
			lastTurnFled = rc.getRoundNum();
		}else if(spotToClear){
			bestDistSq = -10000;
			for (Direction dir : Direction.values()) {
				MapLocation retreatLoc = here.add(dir);
				if (rc.senseRubble(retreatLoc)<GameConstants.RUBBLE_OBSTRUCTION_THRESH || type == RobotType.SCOUT || type == RobotType.TURRET || type == RobotType.TTM || rc.isLocationOccupied(retreatLoc) )
					continue;
				double turretMod = 0;
				if(isInRangeOfTurrets(retreatLoc))
					turretMod = 100;
				RobotInfo closestEnemy = Util.closest(unfriendly, retreatLoc);
				int distSq = retreatLoc.distanceSquaredTo(closestEnemy.location);
				double rubble = rc.senseRubble(retreatLoc);
				double rubbleMod = rubble<GameConstants.RUBBLE_SLOW_THRESH?0:rubble*2.3/GameConstants.RUBBLE_OBSTRUCTION_THRESH;
				double wallMod = wallModCalc(retreatLoc,dir);
				double allyMod = Harass.numOtherAlliesInAttackRange(here.add(dir), allies);
				if (distSq-rubbleMod-turretMod+wallMod+allyMod> bestDistSq) {
					bestDistSq = distSq-rubbleMod+wallMod+allyMod-turretMod;
					bestRetreatDir = dir;
				}
			}
			if(rc.isCoreReady() && bestRetreatDir!=null )
				Util.checkRubbleAndClear(bestRetreatDir,true);
			    lastRetreatDir = bestRetreatDir;
			    lastTurnFled = rc.getRoundNum();

		}
//		if(bestRetreatDir==null && rc.isCoreReady()){
//			bestRetreatDir = Util.closest(unfriendly, here).location.directionTo(here);
//			if(rc.canMove(bestRetreatDir)){
//			System.out.println("had to do a simple run");
//				rc.move(bestRetreatDir);
//			}
//		}
	}
	private static double wallModCalc(MapLocation retreatLoc,Direction dir) throws GameActionException{
		double mod = 0;
		while(here.distanceSquaredTo(retreatLoc)<type.sensorRadiusSquared&&rc.onTheMap(retreatLoc)&&rc.senseRubble(retreatLoc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH){
			retreatLoc = retreatLoc.add(dir);
			mod+=1.0;

		}
		return mod;

	}
	// public static void explore() throws GameActionException { // NEW INTO
	// HARASS
	// // explore
	// RobotInfo[] hostileRobots = rc.senseHostileRobots(here,
	// type.sensorRadiusSquared);
	// RobotInfo[] allies = rc.senseNearbyRobots(here, type.sensorRadiusSquared,
	// us);
	// NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(
	// Util.combineTwoRIArrays(enemyTurrets, turretSize, hostileRobots));
	// if (rc.isCoreReady()) {
	// if (type != RobotType.SCOUT && allies.length > 1) {
	// directionIAmMoving = here.directionTo(Util.centroidOfUnits(allies));
	// }
	// if (directionIAmMoving == null) {
	// int fate = rand.nextInt(1000);
	// directionIAmMoving = Direction.values()[fate % 8];
	// }
	// boolean moved = Nav.moveInDir(directionIAmMoving, theSafety);
	// if (!moved) {
	// for (int i = 0; i < 8; i++) {
	// directionIAmMoving = directionIAmMoving.rotateRight();
	// moved = Nav.moveInDir(directionIAmMoving, theSafety);
	// if (moved) {
	// break;
	// }
	// }
	// }
	// if (!moved && hostileRobots.length > 0) {
	// flee(hostileRobots);
	// // Combat.retreat(Util.closest(hostileRobots, here).location);
	// }
	// }
	// }

	public static void explore(RobotInfo[] hostileRobots, RobotInfo[] allies) throws GameActionException {
		// explore
		safety = new SafetyPolicyAvoidAllUnits(Util.combineTwoRIArrays(enemyTurrets, turretSize, hostileRobots));
		RobotInfo[] scouts = Util.getUnitsOfType(allies, RobotType.SCOUT);
		if (directionIAmMoving == null) {
			directionIAmMoving = center.directionTo(here);
			BotScout.patience = BotScout.PATIENCESTART; 
			BotScout.farthestLoc = here;
		} else if(scouts.length > 0){
			Direction tempDirection = Util.centroidOfUnits(scouts).directionTo(here);
			directionIAmMoving = (new Direction[] {
					tempDirection.rotateLeft(),
					tempDirection,
					tempDirection.rotateRight() })[rand.nextInt(3)];
			BotScout.patience = BotScout.PATIENCESTART; 
			BotScout.farthestLoc = here;
		} else if (BotScout.patience < 1)
			scrambleDirectionIAmMoving();
		//		} else if(hostileRobots.length > 0 && directionIAmMoving == here.directionTo(Util.centroidOfUnits(hostileRobots)))
		//			directionIAmMoving = directionIAmMoving.rotateRight();
		if(tryMoveDirectScout(directionIAmMoving))return;
		else{
			scrambleDirectionIAmMoving();
		}
		if( tryMoveDirectScout(directionIAmMoving)) return;
		if(hostileRobots.length > 0)
			flee(hostileRobots,allies);
		// Combat.retreat(Util.closest(hostileRobots, here).location);
	}

	private static void scrambleDirectionIAmMoving() {
		directionIAmMoving = (new Direction[] {
				directionIAmMoving.opposite().rotateLeft(),
				directionIAmMoving.opposite().rotateRight() })[rand.nextInt(2)];
		BotScout.patience = BotScout.PATIENCESTART; 
		BotScout.farthestLoc = here;
	}

	public static void goAwayFrom(MapLocation loc, NavSafetyPolicy theSafety) throws GameActionException{
		int farthestDist = -1;
		MapLocation bestLoc = null;
		for(Direction dir: Direction.values()){
			if(!rc.canMove(dir))
				continue;
			MapLocation newLoc = here.add(dir);
			if(rc.onTheMap(newLoc) && loc.distanceSquaredTo(newLoc) > farthestDist){
				bestLoc = newLoc;
				farthestDist = loc.distanceSquaredTo(newLoc);
			}
		}
		if(bestLoc != null)
			Nav.goTo(bestLoc, theSafety);
	}

	public static void followFriends(RobotInfo[] friends, RobotInfo[] enemies) throws GameActionException {
		 NavSafetyPolicy theSafety = new SafetyPolicyAvoidAllUnits(enemies);
		if(friends.length > 0){
			Nav.goTo(Util.centroidOfUnits(friends), theSafety);
		}
	}
}
