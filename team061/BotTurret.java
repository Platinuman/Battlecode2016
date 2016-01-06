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
            Combat.shootAtNearbyEnemies(RobotType.TURRET);
        }
    }
}