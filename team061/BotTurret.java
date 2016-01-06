package team061;

import battlecode.common.*;

public class BotTurret extends Bot {
    public static void loop(RobotController theRC) throws GameActionException {
        Bot.init(theRC);
        
//        Debug.init("micro");

        while (true) {
            try {
                turn();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    private static void turn() throws GameActionException {
    	Team myTeam = rc.getTeam();
		Team enemyTeam = myTeam.opponent();
    	int myAttackRange = rc.getType().attackRadiusSquared;
        here = rc.getLocation();
        if (rc.isWeaponReady()) {
            RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
            RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
            if (enemiesWithinRange.length > 0) {
                for (RobotInfo enemy : enemiesWithinRange) {
                    // Check whether the enemy is in a valid attack range (turrets have a minimum range)
                    if (rc.canAttackLocation(enemy.location)) {
                        rc.attackLocation(enemy.location);
                        break;
                    }
                }
            } else if (zombiesWithinRange.length > 0) {
                for (RobotInfo zombie : zombiesWithinRange) {
                    if (rc.canAttackLocation(zombie.location)) {
                        rc.attackLocation(zombie.location);
                        break;
                    }
                }
            }
        }
    }
}