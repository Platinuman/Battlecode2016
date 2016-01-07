package team061;

import battlecode.common.*;

public class Combat extends Bot {
    public static void shootAtNearbyEnemies() throws GameActionException {
        int attackRadiusSq = rc.getType().attackRadiusSquared;
        RobotInfo[] enemies = rc.senseNearbyRobots(attackRadiusSq, them);

        RobotInfo target = null;
        double minHealth = 999999;
        for (RobotInfo enemy : enemies) {
            if (enemy.health < minHealth) {
                minHealth = enemy.health;
                target = enemy;
            }
        }

        if (target != null) {
            rc.attackLocation(target.location);
        }
    }

    public static boolean isSafe(MapLocation loc) {

        RobotInfo[] potentialAttackers = rc.senseNearbyRobots(loc, 15, them);
        for (RobotInfo enemy : potentialAttackers) {
            if (enemy.type.attackRadiusSquared >= loc.distanceSquaredTo(enemy.location)) {
                return false;
            }
        }

        return true;
    }

    public static void retreat(MapLocation[] enemyArchons) throws GameActionException {
        Direction[] retreatDirs = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH_EAST, Direction.SOUTH_EAST,
                Direction.SOUTH_WEST, Direction.NORTH_WEST };

        for (Direction dir : retreatDirs) {
            if (rc.canMove(dir) && isSafe(here.add(dir))) {
                rc.move(dir);
                return;
            }
        }
    }
}
