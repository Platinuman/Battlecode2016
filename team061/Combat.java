package team061;

import battlecode.common.*;

public class Combat extends Bot { //NEW up to you guys what to do here, but please optimize everything and make it usable for multiple strategies
									//NEW most of these methods should be called ONLY by the Harass class
	public static RobotInfo scoutChooseTarget(){
		RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, them);
		RobotInfo[] zombies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, Team.ZOMBIE);
		RobotInfo target = chooseTarget(enemies, 1);
		RobotInfo zombieTarget = chooseTarget(zombies, 1);
		if (zombieTarget != null
				&& (target == null || zombieTarget.type == RobotType.ZOMBIEDEN)) {
			target = zombieTarget;
		}
		return target;
	}
	public static MapLocation shootBestEnemyTakingIntoAccountScoutInfo(MapLocation[] locations, int[] healths, RobotType[] types) throws GameActionException{
		double minHealth = 999999;
		double maxPower = -1;
		MapLocation targetLocation = null;
		for (int i = locations.length; i-- > 0;) {
			double attackPower = types[i].attackPower;
			int health = healths[i];
			Team targetTeam = Team.ZOMBIE;
			Team team = Util.getTeam(types[i]);
			MapLocation location = locations[i];
			if (rc.canAttackLocation(location) && (attackPower > maxPower || (team == us.opponent() && targetTeam == Team.ZOMBIE))) {
				maxPower = attackPower;
				minHealth = health;
				targetTeam = team;
				targetLocation = location;
			} else if (attackPower == maxPower && health < minHealth && rc.canAttackLocation(location)) {
				minHealth = health;
				targetLocation = location;
				targetTeam = team;
			} 
		}
		if(targetLocation != null && rc.isWeaponReady()){
			rc.attackLocation(targetLocation);
		}
		return targetLocation;
	}
	public static void shootAtNearbyEnemies() throws GameActionException {
		RobotType type = rc.getType();
		int attackRadiusSq = type.attackRadiusSquared;
		RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, them);
		RobotInfo[] zombies = rc.senseNearbyRobots(attackRadiusSq, Team.ZOMBIE);

		RobotInfo target = chooseTarget(enemies,0);
		RobotInfo zombieTarget = chooseTarget(zombies,0);
		if (zombieTarget != null
				&& (type == RobotType.GUARD || target == null || zombieTarget.type == RobotType.ZOMBIEDEN)) {
			target = zombieTarget;
		}

		if (target != null) {
			rc.attackLocation(target.location);
		}
	}

	public static RobotInfo chooseTarget(RobotInfo[] enemies, int isScout) {
		double minHealth = 999999;
		double maxPower = -999999;
		RobotInfo target = null;
		for (RobotInfo enemy : enemies) {
			if (enemy.attackPower > maxPower && (rc.canAttackLocation(enemy.location)) || isScout == 1) {
				maxPower = enemy.attackPower;
				minHealth = enemy.health;
				target = enemy;
			} else if (enemy.attackPower == maxPower && enemy.health < minHealth && (rc.canAttackLocation(enemy.location)) || isScout == 1) {
				minHealth = enemy.health;
				target = enemy;
			}
		}
		return target;
	}

	public static boolean isSafe(MapLocation loc) {

		if(isInRangeOfTurrets(loc))
			return false;
		RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, type.sensorRadiusSquared, them);
		for (RobotInfo enemy : potentialAttackers) {
			if(enemy.type == RobotType.ARCHON){
				continue;
			}
			if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
				return false;
			}
		}

		return true;
	}

//	public static void retreat(MapLocation enemyLoc) throws GameActionException {
//		Direction[] retreatDirs = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
//				Direction.NORTH_EAST, Direction.SOUTH_EAST, Direction.SOUTH_WEST, Direction.NORTH_WEST };
//
//		for (Direction dir : retreatDirs) {
//			if (rc.canMove(dir) && isSafe(here.add(dir)) && rc.isCoreReady()) {
//				rc.move(dir);
//				return;
//			}
//		}
//		Direction retreatDir = enemyLoc.directionTo(here);
//		Direction[] dirs = new Direction[3];
//		dirs[0] = retreatDir;
//		dirs[1] = retreatDir.rotateLeft();
//		dirs[2] = retreatDir.rotateRight();
//		for (Direction dir : dirs) {
//			if (rc.canMove(dir)) {
//				rc.move(dir);
//				return;
//			}
//		}
//	}
}
