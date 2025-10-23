package roboifsc;

import robocode.*;
import java.awt.Color;

public class RobobatalhaIFSC extends AdvancedRobot {

    double energiaAnterior = 100;
    boolean andandoFrente = true;

    public void run() {
        setColors(Color.red, Color.blue, Color.blue);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            setTurnRadarRight(360);
            setAhead(andandoFrente ? 100 : -100);
            evitarParede();
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double quedaDeEnergia = energiaAnterior - e.getEnergy();
        energiaAnterior = e.getEnergy();

        if (quedaDeEnergia > 0 && quedaDeEnergia <= 3) {
            setTurnRight(e.getBearing() + (Math.random() > 0.5 ? 90 : -90));
            andandoFrente = !andandoFrente;
        }

        double radarLock = getHeading() - getRadarHeading() + e.getBearing();
        setTurnRadarRight(radarLock);
        execute();
    }

    private void evitarParede() {
        double margem = 50;
        if (getX() < margem || getX() > getBattleFieldWidth() - margem ||
            getY() < margem || getY() > getBattleFieldHeight() - margem) {
            setBack(100);
            andandoFrente = !andandoFrente;
        }
    }
}
